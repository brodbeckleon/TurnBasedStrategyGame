package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitAttacked;
import game.game.Movement;
import game.game.Player;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.helicopters.AHSixtyFourD;
import game.units.assaultClasses.infantry.AntiTankTeam;
import game.units.assaultClasses.infantry.RifleSquad;
import game.units.assaultClasses.ships.Kongo;
import game.units.assaultClasses.ships.Soryu;
import game.units.assaultClasses.ships.Yamato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewUnitsTest {

    private Game game;
    private Player player;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        attack = new AttackCommand(game);
    }

    private void sea() {
        game.getBattlefield().getGeography().replaceAll((point, terrain) -> point.x >= 4 ? TerrainEnum.WATER : terrain);
    }

    @Test
    void infantryClimbsMountainsThatStopVehicles() {
        Fixtures.setTerrain(game, new Point(6, 5), TerrainEnum.MOUNTAIN);
        RifleSquad squad = new RifleSquad(1, new Point(5, 5));
        TypeTen tank = new TypeTen(1, new Point(5, 7));
        Fixtures.place(game, 1, squad);
        Fixtures.place(game, 1, tank);

        assertTrue(Movement.reachable(game, squad).containsKey(new Point(6, 5)));
        assertFalse(Movement.reachable(game, tank).containsKey(new Point(6, 5)));
    }

    @Test
    void antiTankTeamsHitTanksTwiceAsHard() {
        AntiTankTeam team = new AntiTankTeam(1, new Point(5, 5));
        TypeNinety tank = new TypeNinety(2, new Point(7, 5));
        RifleSquad squad = new RifleSquad(2, new Point(7, 6));

        int vsTank = attack.predictDamage(team, tank);
        int vsInfantry = attack.predictDamage(team, squad);

        assertEquals(vsInfantry, team.getDamage());
        assertEquals(Math.round(team.getDamage() * 2 * (100 - tank.getArmorValue()) / 100.0), vsTank);
    }

    @Test
    void helicoptersTakeFullGroundFireAndDeployAtTheBase() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        AHSixtyFourD apache = new AHSixtyFourD(2, new Point(7, 5));
        FTwo jet = new FTwo(2, new Point(7, 6));

        assertEquals(tank.getDamage(), attack.predictDamage(tank, apache));
        assertTrue(attack.predictDamage(tank, jet) < tank.getDamage());

        while (player.getResourcePoints() < 20) player.addResourcePoints();
        assertFalse(new AddCommand(game).findDeployablePositions(player, new AHSixtyFourD(1, new Point())).isEmpty(),
                "no air base needed");
    }

    @Test
    void aDestroyerDoublesAgainstAircraftAndCoversTheFleet() {
        sea();
        Kongo destroyer = new Kongo(2, new Point(8, 5));
        Fixtures.place(game, 2, destroyer);
        Yamato wreck = new Yamato(2, new Point(8, 7));
        int yamato = Fixtures.place(game, 2, wreck);
        wreck.setHealthPoints(-(wreck.getMaxHealthPoints() - 50)); // sinks to the jet, so it cannot shoot it down first
        FTwo jet = new FTwo(1, new Point(6, 6));
        int jetId = Fixtures.place(game, 1, jet);

        assertEquals(destroyer.getDamage() * 2, attack.predictDamage(destroyer, jet));
        List<GameEvent> events = attack.execute(player, jetId, yamato).getEvents();
        assertTrue(events.stream().anyMatch(e -> e instanceof UnitAttacked a && a.attackerName().equals("Kongo")),
                "the destroyer fires at the attacking jet");
    }

    @Test
    void submarinesOnlyFightShipsAndOnlyShipsAndAircraftFindThem() {
        sea();
        Fixtures.setTerrain(game, new Point(3, 5), TerrainEnum.PLAIN);
        Soryu submarine = new Soryu(2, new Point(5, 5));
        int subId = Fixtures.place(game, 2, submarine);
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(3, 5)));
        Kongo destroyer = new Kongo(1, new Point(7, 5));
        int destroyerId = Fixtures.place(game, 1, destroyer);

        assertFalse(attack.execute(player, tank, subId).wasSuccessful(), "a tank cannot find a submarine");
        assertTrue(attack.findTargets(player, destroyerId).contains(subId));
        assertEquals(submarine.getDamage() * 2, attack.predictDamage(submarine, destroyer), "torpedoes");
        assertFalse(AttackCommand.canTarget(submarine, new TypeTen(1, new Point())));

        // the submarine could strike back at the destroyer, never at the tank, but the destroyer's salvo sinks it first
        assertTrue(attack.canStrikeBack(submarine, destroyer));
        assertFalse(attack.canStrikeBack(submarine, game.findUnitById(tank)));
        List<GameEvent> events = attack.execute(player, destroyerId, subId).getEvents();
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.UnitDestroyed d && d.unitId() == subId));
        assertTrue(events.stream().noneMatch(e -> e instanceof UnitAttacked a && a.attackerId() == subId));
    }

    @Test
    void everyNewUnitTypeCanBeBuilt() {
        for (UnitEnum type : List.of(UnitEnum.INFANTRY, UnitEnum.ANTITANKTEAM, UnitEnum.AHSIXTYFOURD, UnitEnum.KONGO, UnitEnum.SORYU)) {
            assertNotNull(new game.game.UnitFactory().createUnit(1, type, new Point()));
        }
    }
}
