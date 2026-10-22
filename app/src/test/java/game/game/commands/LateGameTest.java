package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.events.GameEvent.AbilityUsed;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitPromoted;
import game.game.Ability;
import game.game.Player;
import game.game.Research;
import game.model.Game;
import game.testing.Fixtures;
import game.units.AssaultClass;
import game.units.Base;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.infantryFightingVehicles.TypeEightyNine;
import game.units.assaultClasses.ships.Soryu;
import game.units.infrastructures.AirForceBase;
import game.units.infrastructures.ArmyBase;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredRecoveryVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The rules that keep the late game moving: the army limit, repairs under fire, research, abilities, veterancy. */
class LateGameTest {

    private Game game;
    private Player player;
    private AddCommand add;
    private AttackCommand attack;
    private HealCommand heal;
    private ResearchCommand research;
    private AbilityCommand abilities;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        add = new AddCommand(game);
        attack = new AttackCommand(game);
        heal = new HealCommand(game);
        research = new ResearchCommand(game);
        abilities = new AbilityCommand(game);
        player.addResourcePoints(200);
    }

    /** Ends player one's turn and lets player two pass, so player one's next turn starts. */
    private void nextRound() {
        game.switchTurn();
        game.switchTurn();
    }

    private void researchAll(Player who, Research... projects) {
        for (Research project : projects) {
            who.completeResearch(project);
        }
    }

    // --- Army limit ---

    @Test
    void theArmyLimitGrowsWithBasesAndLogistics() {
        assertEquals(Game.BASE_ARMY_LIMIT + Game.ARMY_LIMIT_PER_BASE, game.getArmyLimit(player), "headquarters only");
        Fixtures.place(game, 1, new ArmyBase(1, new Point(4, 4)));
        assertEquals(Game.BASE_ARMY_LIMIT + 2 * Game.ARMY_LIMIT_PER_BASE, game.getArmyLimit(player));
        researchAll(player, Research.SUPPLY_LINES, Research.FIELD_WORKSHOPS, Research.MASS_MOBILISATION);
        assertEquals(Game.BASE_ARMY_LIMIT + 2 * Game.ARMY_LIMIT_PER_BASE + Game.SUPPLY_LINES_BONUS
                + Game.MASS_MOBILISATION_BONUS, game.getArmyLimit(player));
    }

    @Test
    void noUnitCanBeDeployedPastTheLimitButBuildingsStillCan() {
        int limit = game.getArmyLimit(player);
        for (int i = 0; i < limit; i++) {
            Fixtures.place(game, 1, new TypeEightyNine(1, new Point(8, i)));
        }
        assertEquals(limit, game.getArmySize(player));

        CommandResult refused = add.execute(player, UnitEnum.INFANTRY.name(), new Point(0, 0));
        assertFalse(refused.wasSuccessful());
        assertTrue(refused.getMessage().contains("limit"), refused.getMessage());

        assertTrue(add.execute(player, UnitEnum.RADARSTATION.name(), new Point(2, 2)).wasSuccessful(),
                "buildings do not count");
    }

    // --- Repairs under fire ---

    @Test
    void aBaseAttackedLastRoundCannotBeRepairedUntilAQuietRoundPassed() {
        Base base = player.getPlayerDeck().getBase();
        int recovery = Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(0, 1)));
        int raider = Fixtures.place(game, 2, new TypeTen(2, new Point(2, 0)));
        game.switchTurn(); // the enemy's turn: it shells the base
        assertTrue(attack.execute(game.getPlayerTwo(), raider, base.getBaseID()).wasSuccessful());
        game.getPlayerTwo().getPlayerDeck().removeUnit(raider);
        game.switchTurn(); // player one's turn right after the attack

        assertTrue(base.isUnderFire());
        assertTrue(heal.findTargets(player, recovery).isEmpty());
        CommandResult refused = heal.execute(player, recovery, base.getBaseID());
        assertFalse(refused.wasSuccessful());
        assertTrue(refused.getMessage().contains("attacked last round"), refused.getMessage());

        nextRound(); // a round without attacks

        assertFalse(base.isUnderFire());
        assertTrue(heal.execute(player, recovery, base.getBaseID()).wasSuccessful());
    }

    @Test
    void fieldWorkshopsRestoreHalfAsMuchAgain() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        int tankId = Fixtures.place(game, 1, tank);
        tank.setHealthPoints(-490);
        int ambulance = Fixtures.place(game, 1, new ArmoredAmbulanceVehicle(1, new Point(5, 6)));
        player.completeResearch(Research.SUPPLY_LINES);
        player.completeResearch(Research.FIELD_WORKSHOPS);

        heal.execute(player, ambulance, tankId);

        assertEquals(10 + 450, tank.getHealthPoints(), "300 HP and half of it again");
    }

    // --- Research ---

    @Test
    void researchFollowsItsBranchCostsResourcesAndTakesOneProjectPerTurn() {
        int rp = player.getResourcePoints();
        assertFalse(research.execute(player, Research.FIELD_WORKSHOPS).wasSuccessful(), "Supply Lines comes first");

        CommandResult done = research.execute(player, Research.SUPPLY_LINES);
        assertTrue(done.wasSuccessful());
        assertEquals(List.of(new GameEvent.ResearchCompleted(1, "Supply Lines")), done.getEvents());
        assertEquals(rp - Research.SUPPLY_LINES.cost(), player.getResourcePoints());
        assertTrue(player.hasResearched(Research.SUPPLY_LINES));

        CommandResult second = research.execute(player, Research.FIELD_WORKSHOPS);
        assertFalse(second.wasSuccessful(), "one project per turn");
        assertFalse(research.execute(player, Research.SUPPLY_LINES).wasSuccessful(), "already done");

        nextRound();
        assertTrue(research.execute(player, Research.FIELD_WORKSHOPS).wasSuccessful());
    }

    @Test
    void theStealthFighterIsLockedUntilItsResearchIsDone() {
        Fixtures.place(game, 1, new AirForceBase(1, new Point(3, 3)));

        CommandResult locked = add.execute(player, UnitEnum.FTHIRTYFIVE.name(), new Point(3, 3));
        assertFalse(locked.wasSuccessful());
        assertTrue(locked.getMessage().contains("Stealth Program"), locked.getMessage());

        player.completeResearch(Research.AEGIS_UPGRADE);
        nextRound();
        research.execute(player, Research.STEALTH_PROGRAM);
        assertTrue(add.execute(player, UnitEnum.FTHIRTYFIVE.name(), new Point(3, 3)).wasSuccessful());
        assertTrue(player.hasUnlocked(UnitEnum.TYPETEN), "everything else is open from the start");
    }

    // --- Abilities ---

    @Test
    void anAbilityNeedsItsResearchResourcesAndThenCoolsDown() {
        Point target = new Point(8, 8);
        assertFalse(abilities.execute(player, Ability.RADAR_SWEEP, target).wasSuccessful(), "not researched");
        researchAll(player, Research.RADAR_SWEEP);
        int rp = player.getResourcePoints();

        assertTrue(abilities.execute(player, Ability.RADAR_SWEEP, target).wasSuccessful());
        assertEquals(rp - Ability.RADAR_SWEEP.cost(), player.getResourcePoints());
        assertFalse(abilities.execute(player, Ability.RADAR_SWEEP, target).wasSuccessful(), "cooling down");

        for (int turn = 1; turn < Ability.RADAR_SWEEP.cooldown(); turn++) {
            nextRound();
            assertNotNull(abilities.notReady(player, Ability.RADAR_SWEEP), "still cooling down after " + turn);
        }
        nextRound();
        assertNull(abilities.notReady(player, Ability.RADAR_SWEEP));
    }

    @Test
    void aRadarSweepRevealsTheAreaUntilTheNextTurnAndLetsTheBarrageAimThere() {
        game.setFogOfWar(true);
        researchAll(player, Research.RADAR_SWEEP, Research.ARTILLERY_BARRAGE);
        Point far = new Point(10, 10);
        TypeEightyNine hidden = new TypeEightyNine(2, far);
        Fixtures.place(game, 2, hidden);
        assertFalse(game.isVisibleTo(player, hidden));
        assertFalse(abilities.execute(player, Ability.ARTILLERY_BARRAGE, far).wasSuccessful(), "no eyes on the target");

        abilities.execute(player, Ability.RADAR_SWEEP, far);

        assertTrue(game.isVisibleTo(player, hidden));
        game.switchTurn();
        assertTrue(game.isVisibleTo(player, hidden), "still revealed during the enemy turn");
        assertTrue(abilities.execute(player, Ability.ARTILLERY_BARRAGE, far).wasSuccessful());
        game.switchTurn();
        assertFalse(game.isVisibleTo(player, hidden), "gone at the start of player one's next turn");
    }

    @Test
    void theBarrageHitsEveryEnemyAroundTheTargetButNoFriendsAndNoSubmarines() {
        researchAll(player, Research.RADAR_SWEEP, Research.ARTILLERY_BARRAGE);
        TypeEightyNine centre = new TypeEightyNine(2, new Point(8, 8));
        TypeEightyNine corner = new TypeEightyNine(2, new Point(9, 9));
        TypeEightyNine outside = new TypeEightyNine(2, new Point(10, 10));
        TypeEightyNine friend = new TypeEightyNine(1, new Point(7, 8));
        Fixtures.setTerrain(game, new Point(7, 7), TerrainEnum.WATER);
        Soryu submarine = new Soryu(2, new Point(7, 7));
        for (var unit : List.of(centre, corner, outside, submarine)) Fixtures.place(game, 2, unit);
        Fixtures.place(game, 1, friend);
        int damage = Ability.ARTILLERY_BARRAGE.damage(); // no armour, no cover on plain ground

        List<GameEvent> events = abilities.execute(player, Ability.ARTILLERY_BARRAGE, new Point(8, 8)).getEvents();

        assertInstanceOf(AbilityUsed.class, events.get(0));
        assertEquals(9, ((AbilityUsed) events.get(0)).area().size());
        assertEquals(2, events.stream().filter(e -> e instanceof UnitDamaged).count());
        assertEquals(centre.getMaxHealthPoints() - damage, centre.getHealthPoints());
        assertEquals(corner.getMaxHealthPoints() - damage, corner.getHealthPoints());
        assertEquals(outside.getMaxHealthPoints(), outside.getHealthPoints());
        assertEquals(friend.getMaxHealthPoints(), friend.getHealthPoints());
        assertEquals(submarine.getMaxHealthPoints(), submarine.getHealthPoints());
    }

    @Test
    void anAirstrikeHitsOneEnemyAfterArmourDrawsNoFireAndBlocksRepairs() {
        researchAll(player, Research.RADAR_SWEEP, Research.ARTILLERY_BARRAGE, Research.AIRSTRIKE);
        assertFalse(abilities.execute(player, Ability.AIRSTRIKE, new Point(8, 8)).wasSuccessful(), "nobody there");
        Base enemyBase = game.getPlayerTwo().getPlayerDeck().getBase();
        Fixtures.place(game, 1, new TypeTen(1, new Point(13, 13))); // eyes on the enemy base

        List<GameEvent> events = abilities.execute(player, Ability.AIRSTRIKE, enemyBase.getPosition()).getEvents();

        int expected = (int) Math.round(Ability.AIRSTRIKE.damage() * (100 - enemyBase.getArmorValue()) / 100.0);
        assertEquals(enemyBase.getMaxHealthPoints() - expected, enemyBase.getHealthPoints());
        assertEquals(2, events.size(), "the strike and the damage, nothing strikes back");
        assertTrue(enemyBase.isUnderFire());
    }

    // --- Veterancy ---

    @Test
    void everyKillAddsTenPercentDamageUpToFiveRanks() {
        TypeTen veteran = new TypeTen(1, new Point(5, 5));
        int veteranId = Fixtures.place(game, 1, veteran);
        int base = veteran.getBaseDamage();

        for (int kill = 1; kill <= AssaultClass.MAX_RANK + 1; kill++) {
            TypeEightyNine victim = new TypeEightyNine(2, new Point(7, 5));
            int victimId = Fixtures.place(game, 2, victim);
            victim.setHealthPoints(-(victim.getMaxHealthPoints() - 1));
            veteran.setIsAvailable(true);

            List<GameEvent> events = attack.execute(player, veteranId, victimId).getEvents();

            int rank = Math.min(kill, AssaultClass.MAX_RANK);
            assertEquals(rank, veteran.getRank());
            assertEquals(Math.round(base * (1 + 0.1 * rank)), veteran.getDamage());
            assertEquals(kill <= AssaultClass.MAX_RANK, events.stream().anyMatch(e -> e instanceof UnitPromoted),
                    "promoted on kill " + kill);
        }
        assertEquals(Math.round(base * 1.5), veteran.getDamage(), "+50% at most");
    }

    @Test
    void killsByAbilitiesEarnNoRank() {
        researchAll(player, Research.RADAR_SWEEP, Research.ARTILLERY_BARRAGE, Research.AIRSTRIKE);
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, tank);
        TypeEightyNine victim = new TypeEightyNine(2, new Point(7, 5));
        Fixtures.place(game, 2, victim);
        victim.setHealthPoints(-(victim.getMaxHealthPoints() - 1));

        abilities.execute(player, Ability.AIRSTRIKE, new Point(7, 5));

        assertNull(game.findUnitById(game.getUnitID(victim)));
        assertEquals(0, tank.getRank());
    }
}
