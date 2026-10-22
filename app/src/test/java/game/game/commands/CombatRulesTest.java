package game.game.commands;

import game.units.AssaultClass;
import game.enums.TerrainEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitAttacked;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDestroyed;
import game.game.Player;
import game.game.map.Battlefield;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FFourEJKai;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightyOneC;
import game.units.assaultClasses.antiAir.TypeNinetyThree;
import game.units.assaultClasses.artillery.TypeNinetyNineSPH;
import game.units.infrastructures.Radarstation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CombatRulesTest {

    private Game game;
    private Player player;
    private MoveCommand move;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        move = new MoveCommand(game);
        attack = new AttackCommand(game);
    }

    // --- Moving and attacking in one turn ---

    @Test
    void lightAndMediumUnitsCanAttackAfterMovingButNotMoveTwice() {
        TypeTen tank = new TypeTen(1, new Point(3, 5));
        int tankId = Fixtures.place(game, 1, tank);
        int target = Fixtures.place(game, 2, new TypeEightyOneC(2, new Point(9, 5)));

        assertTrue(move.execute(player, tankId, new Point(5, 5)).wasSuccessful());

        assertTrue(tank.isAvailable());
        assertFalse(move.execute(player, tankId, new Point(6, 5)).wasSuccessful(), "only one move per turn");
        assertTrue(attack.execute(player, tankId, target).wasSuccessful());
        assertFalse(tank.isAvailable());
    }

    @Test
    void heavyUnitsArtilleryAndHeavyAntiAirEitherMoveOrAttack() {
        for (var unit : List.of(new TypeNinety(1, new Point(3, 5)), new TypeNinetyNineSPH(1, new Point(3, 5)),
                new TypeEightyOneC(1, new Point(3, 5)))) {
            Game fresh = Fixtures.plainGame();
            int id = Fixtures.place(fresh, 1, unit);
            Fixtures.place(fresh, 2, new TypeTen(2, new Point(6, 5)));

            assertTrue(new MoveCommand(fresh).execute(fresh.getPlayerOne(), id, new Point(4, 5)).wasSuccessful());

            assertFalse(unit.isAvailable(), unit.getUnitName() + " is done after moving");
        }
    }

    @Test
    void movedUnitsGetTheirTurnBackNextRound() {
        TypeTen tank = new TypeTen(1, new Point(3, 5));
        int tankId = Fixtures.place(game, 1, tank);
        move.execute(player, tankId, new Point(4, 5));

        game.switchTurn();
        game.switchTurn();

        assertFalse(tank.hasMoved());
        assertTrue(move.execute(player, tankId, new Point(5, 5)).wasSuccessful());
    }

    // --- Counterattacks ---

    @Test
    void aSurvivingDefenderInRangeStrikesBack() {
        TypeTen attacker = new TypeTen(1, new Point(5, 5));
        int attackerId = Fixtures.place(game, 1, attacker);
        TypeNinety defender = new TypeNinety(2, new Point(8, 5));
        int defenderId = Fixtures.place(game, 2, defender);

        List<GameEvent> events = attack.execute(player, attackerId, defenderId).getEvents();

        assertEquals(4, events.size());
        UnitAttacked counter = assertInstanceOf(UnitAttacked.class, events.get(2));
        assertEquals(defenderId, counter.attackerId());
        assertEquals(attackerId, counter.targetId());
        UnitDamaged hurt = assertInstanceOf(UnitDamaged.class, events.get(3));
        assertEquals(attack.counterDamage(defender, attacker), hurt.damage());
        assertEquals(attacker.getMaxHealthPoints() - hurt.damage(), attacker.getHealthPoints());
    }

    @Test
    void noCounterattackFromOutOfRangeFromTheDeadOrFromBuildings() {
        TypeNinetyNineSPH artillery = new TypeNinetyNineSPH(1, new Point(2, 5));
        int artilleryId = Fixtures.place(game, 1, artillery);
        int farTank = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 5)));
        assertEquals(2, attack.execute(player, artilleryId, farTank).getEvents().size(), "the tank cannot reach back");

        TypeTen killer = new TypeTen(1, new Point(5, 9));
        int killerId = Fixtures.place(game, 1, killer);
        TypeNinety doomed = new TypeNinety(2, new Point(7, 9));
        int doomedId = Fixtures.place(game, 2, doomed);
        doomed.setHealthPoints(-(doomed.getMaxHealthPoints() - 1));
        List<GameEvent> kill = attack.execute(player, killerId, doomedId).getEvents();
        assertInstanceOf(UnitDestroyed.class, kill.get(kill.size() - 2));
        assertEquals(killer.getMaxHealthPoints(), killer.getHealthPoints(), "the dead do not strike back");

        TypeTen raider = new TypeTen(1, new Point(10, 12));
        int raiderId = Fixtures.place(game, 1, raider);
        int radar = Fixtures.place(game, 2, new Radarstation(2, new Point(12, 12)));
        assertEquals(2, attack.execute(player, raiderId, radar).getEvents().size(), "buildings do not strike back");
    }

    @Test
    void aBatteredDefenderStrikesBackWeaker() {
        TypeTen fresh = new TypeTen(1, new Point(5, 5));
        TypeTen other = new TypeTen(1, new Point(5, 9));
        int freshId = Fixtures.place(game, 1, fresh);
        int otherId = Fixtures.place(game, 1, other);
        TypeNinety healthy = new TypeNinety(2, new Point(8, 5));
        TypeNinety battered = new TypeNinety(2, new Point(8, 9));
        int healthyId = Fixtures.place(game, 2, healthy);
        int batteredId = Fixtures.place(game, 2, battered);
        battered.setHealthPoints(-battered.getMaxHealthPoints() / 2);

        attack.execute(player, freshId, healthyId);
        attack.execute(player, otherId, batteredId);

        int fromHealthy = fresh.getMaxHealthPoints() - fresh.getHealthPoints();
        int fromBattered = other.getMaxHealthPoints() - other.getHealthPoints();
        assertTrue(fromBattered < fromHealthy, fromBattered + " < " + fromHealthy);
        double expected = attack.predictDamage(battered, other) * battered.getHealthPoints()
                / (double) battered.getMaxHealthPoints();
        assertEquals(Math.round(expected), fromBattered);
    }

    @Test
    void aCounterattackCanDestroyTheAttacker() {
        TypeTen attacker = new TypeTen(1, new Point(5, 5));
        int attackerId = Fixtures.place(game, 1, attacker);
        attacker.setHealthPoints(-(attacker.getMaxHealthPoints() - 10));
        int defenderId = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        List<GameEvent> events = attack.execute(player, attackerId, defenderId).getEvents();

        assertEquals(new UnitDestroyed(1, attackerId, "Type 10", new Point(5, 5), false), events.get(events.size() - 2));
        assertNull(game.findUnitById(attackerId));
        // the kill counts for the defender
        assertInstanceOf(GameEvent.UnitPromoted.class, events.get(events.size() - 1));
        assertEquals(1, ((AssaultClass) game.findUnitById(defenderId)).getRank());
    }

    // --- Anti-air cover ---

    @Test
    void anAircraftAttackingUnderEnemyAntiAirIsShotAt() {
        FTwo jet = new FTwo(1, new Point(5, 5));
        int jetId = Fixtures.place(game, 1, jet);
        int tank = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 5)));
        TypeNinetyThree flak = new TypeNinetyThree(2, new Point(8, 7));
        int flakId = Fixtures.place(game, 2, flak);

        List<GameEvent> events = attack.execute(player, jetId, tank).getEvents();

        List<Integer> shooters = events.stream().filter(e -> e instanceof UnitAttacked a && a.targetId() == jetId)
                .map(e -> ((UnitAttacked) e).attackerId()).toList();
        assertEquals(List.of(tank, flakId), shooters, "the tank strikes back, then the anti-air fires");
    }

    @Test
    void antiAirOutOfRangeAndTheAttackedAntiAirItselfFireOnlyOnce() {
        FFourEJKai jet = new FFourEJKai(1, new Point(5, 5));
        int jetId = Fixtures.place(game, 1, jet);
        int flak = Fixtures.place(game, 2, new TypeEightyOneC(2, new Point(8, 5))); // survives the hit
        Fixtures.place(game, 2, new TypeNinetyThree(2, new Point(14, 14)));

        List<GameEvent> events = attack.execute(player, jetId, flak).getEvents();

        long shotsAtJet = events.stream().filter(e -> e instanceof UnitAttacked a && a.targetId() == jetId).count();
        assertEquals(1, shotsAtJet);
    }

    // --- Fairness ---

    @Test
    void bothPlayersStartEqual() {
        // with fog of war, moving first is hardly an advantage any more (balanceLab, 1000 games: 52/48)
        Game fresh = new Game();
        assertEquals(fresh.getPlayerOne().getResourcePoints(), fresh.getPlayerTwo().getResourcePoints());
    }

    @Test
    void generatedMapsAlwaysConnectBothBasesOverLand() {
        for (int i = 0; i < 40; i++) {
            Battlefield battlefield = new Game().getBattlefield();
            assertTrue(landPath(battlefield, new Point(0, 0), new Point(15, 15)), "map " + i);
        }
    }

    @Test
    void bothBasesGetTheSameClearingOfOpenGround() {
        for (int i = 0; i < 20; i++) {
            Battlefield battlefield = new Game().getBattlefield();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    if (x * x + y * y <= 4.4 * 4.4) {
                        assertEquals(TerrainEnum.PLAIN, battlefield.getGeographyOfPoint(new Point(x, y)));
                        assertEquals(TerrainEnum.PLAIN, battlefield.getGeographyOfPoint(new Point(15 - x, 15 - y)));
                    }
                }
            }
            assertNull(battlefield.getGeographyOfPoint(new Point(16, 15)), "nothing off the board");
        }
    }

    private static boolean landPath(Battlefield battlefield, Point from, Point to) {
        Set<Point> seen = new HashSet<>(List.of(from));
        Deque<Point> queue = new ArrayDeque<>(List.of(from));
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            if (point.equals(to)) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Point next = new Point(point.x + d[0], point.y + d[1]);
                TerrainEnum terrain = battlefield.getGeographyOfPoint(next);
                if ((terrain == TerrainEnum.PLAIN || terrain == TerrainEnum.FOREST) && seen.add(next)) queue.add(next);
            }
        }
        return false;
    }
}
