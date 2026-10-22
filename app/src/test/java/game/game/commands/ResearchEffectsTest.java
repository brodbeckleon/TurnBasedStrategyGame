package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitDeployed;
import game.events.GameEvent.UnitDestroyed;
import game.game.Ability;
import game.game.Player;
import game.game.Research;
import game.model.Game;
import game.testing.Fixtures;
import game.units.AssaultClass;
import game.units.Base;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightySeven;
import game.units.assaultClasses.infantry.RifleSquad;
import game.units.assaultClasses.ships.Kongo;
import game.units.assaultClasses.ships.Soryu;
import game.units.infrastructures.ArmyBase;
import game.units.transports.Osumi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** What each research project changes, and the two abilities that came with the bigger tree. */
class ResearchEffectsTest {

    private Game game;
    private Player player;
    private AddCommand add;
    private AttackCommand attack;
    private MoveCommand move;
    private AbilityCommand abilities;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        add = new AddCommand(game);
        attack = new AttackCommand(game);
        move = new MoveCommand(game);
        abilities = new AbilityCommand(game);
        player.addResourcePoints(200);
    }

    private void research(Research... projects) {
        for (Research project : projects) player.completeResearch(project);
    }

    // --- Logistics ---

    @Test
    void industrialExpansionOpensAFourthBaseLevel() {
        UpgradeCommand upgrade = new UpgradeCommand(game);
        Base base = player.getPlayerDeck().getBase();
        upgrade.execute(player, base.getBaseID());
        upgrade.execute(player, base.getBaseID());

        CommandResult refused = upgrade.execute(player, base.getBaseID());
        assertFalse(refused.wasSuccessful());
        assertTrue(refused.getMessage().contains("Industrial Expansion"), refused.getMessage());

        research(Research.INDUSTRIAL_EXPANSION);
        assertTrue(upgrade.execute(player, base.getBaseID()).wasSuccessful());
        assertEquals(4, base.getLevel());
        assertEquals(5, base.getIncome());
        assertFalse(upgrade.execute(player, base.getBaseID()).wasSuccessful(), "four is the top");
    }

    @Test
    void rapidDeploymentLetsANewUnitActAtOnceAndBootCampTrainsIt() {
        add.execute(player, UnitEnum.TYPETEN.name(), new Point(0, 0));
        Unit untrained = game.getUnitAt(new Point(0, 0));
        assertFalse(untrained.isAvailable());
        assertEquals(0, ((AssaultClass) untrained).getRank());
        player.getPlayerDeck().removeUnit(game.getUnitID(untrained));

        research(Research.RAPID_DEPLOYMENT, Research.BOOT_CAMP);
        add.execute(player, UnitEnum.TYPETEN.name(), new Point(0, 0));
        Unit trained = game.getUnitAt(new Point(0, 0));
        assertTrue(trained.isAvailable(), "can move and act this turn");
        assertEquals(1, ((AssaultClass) trained).getRank(), "starts as Private First Class");
    }

    @Test
    void forwardCommandPlacesBuildingsFurtherFromTheBase() {
        Point far = new Point(6, 3); // about 6.7 tiles from the headquarters: past its radius of 5
        assertFalse(add.execute(player, UnitEnum.RADARSTATION.name(), far).wasSuccessful());

        research(Research.FORWARD_COMMAND);
        assertTrue(add.execute(player, UnitEnum.RADARSTATION.name(), far).wasSuccessful());
    }

    // --- Ground ---

    @Test
    void compositeArmourTakesMoreOffTheHitsOnTanks() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, tank);
        TypeTen enemy = new TypeTen(2, new Point(7, 5));
        Fixtures.place(game, 2, enemy);
        int before = attack.predictDamage(enemy, tank);

        research(Research.COMPOSITE_ARMOUR);

        assertEquals(tank.getArmorValue() + Game.COMPOSITE_ARMOUR_BONUS, game.getArmorValue(tank));
        assertTrue(attack.predictDamage(enemy, tank) < before);
        assertEquals(before, attack.predictDamage(tank, enemy), "only the researcher's tanks");
    }

    @Test
    void thermalSightsLetGroundUnitsSeeFurther() {
        game.setFogOfWar(true);
        Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        TypeTen enemy = new TypeTen(2, new Point(5, 9)); // four tiles off: one past a tank's sight
        Fixtures.place(game, 2, enemy);
        assertFalse(game.isVisibleTo(player, enemy));

        research(Research.THERMAL_SIGHTS);
        assertTrue(game.isVisibleTo(player, enemy));
    }

    // --- Air ---

    @Test
    void aegisUpgradeExtendsTheAntiAirRange() {
        TypeEightySeven flak = new TypeEightySeven(1, new Point(5, 5));
        Fixtures.place(game, 1, flak);
        int range = game.getShootingRange(flak);

        research(Research.AEGIS_UPGRADE);

        assertEquals(range + Game.AEGIS_RANGE_BONUS, game.getShootingRange(flak));
    }

    @Test
    void aegisUpgradeLetsTheKongosRadarSeeFurther() {
        game.setFogOfWar(true);
        Fixtures.place(game, 1, new Kongo(1, new Point(5, 5)));
        FTwo jet = new FTwo(2, new Point(5, 11)); // six tiles off: two past a ship's sight
        Fixtures.place(game, 2, jet);
        assertFalse(game.isVisibleTo(player, jet));

        research(Research.AEGIS_UPGRADE);
        assertTrue(game.isVisibleTo(player, jet));
    }

    @Test
    void precisionMunitionsMakeAircraftBetterAtBreakingBuildings() {
        FTwo jet = new FTwo(1, new Point(5, 5));
        Fixtures.place(game, 1, jet);
        ArmyBase target = new ArmyBase(2, new Point(8, 5));
        Fixtures.place(game, 2, target);
        int before = attack.predictDamage(jet, target);

        research(Research.PRECISION_MUNITIONS);

        assertEquals(Math.round(jet.getDamage() * 1.25 * (100 - target.getArmorValue()) / 100.0),
                attack.predictDamage(jet, target));
        assertTrue(attack.predictDamage(jet, target) > before);
    }

    // --- Navy ---

    @Test
    void improvedSonarFindsSubmarinesFurtherAway() {
        game.setFogOfWar(true);
        Fixtures.place(game, 1, new Kongo(1, new Point(2, 8)));
        Soryu submarine = new Soryu(2, new Point(8, 8)); // six tiles off: past the sonar's four
        Fixtures.place(game, 2, submarine);
        assertFalse(game.isVisibleTo(player, submarine));

        research(Research.IMPROVED_SONAR);
        assertTrue(game.isVisibleTo(player, submarine));
    }

    @Test
    void amphibiousDoctrineFitsTwoUnitsOnTheOsumiAndTheyShareItsFate() {
        for (int y = 0; y < 16; y++) {
            for (int x = 5; x < 16; x++) Fixtures.setTerrain(game, new Point(x, y), TerrainEnum.WATER);
        }
        Osumi ship = new Osumi(1, new Point(5, 5));
        int shipId = Fixtures.place(game, 1, ship);
        int first = Fixtures.place(game, 1, new RifleSquad(1, new Point(4, 5)));
        int second = Fixtures.place(game, 1, new RifleSquad(1, new Point(4, 6)));

        assertTrue(move.execute(player, first, new Point(5, 5)).wasSuccessful());
        assertFalse(move.execute(player, second, new Point(5, 5)).wasSuccessful(), "one unit only without research");

        research(Research.AMPHIBIOUS_DOCTRINE);
        assertTrue(move.execute(player, second, new Point(5, 5)).wasSuccessful());
        assertEquals(List.of(game.findUnitById(first), game.findUnitById(second), ship), game.getStackAt(new Point(5, 5)));
        assertFalse(game.hasRoomOnTop(ship));

        assertTrue(move.execute(player, shipId, new Point(8, 5)).wasSuccessful());
        assertEquals(new Point(8, 5), game.findUnitById(first).getPosition(), "both sail along");
        assertEquals(new Point(8, 5), game.findUnitById(second).getPosition());

        // a submarine only hits ships, so the squads on deck do not shield it; when it sinks, they go down too
        int sub = Fixtures.place(game, 2, new Soryu(2, new Point(10, 5)));
        ship.setHealthPoints(-(ship.getMaxHealthPoints() - 1));
        List<GameEvent> events = attack.execute(game.getPlayerTwo(), sub, shipId).getEvents();
        assertEquals(3, events.stream().filter(e -> e instanceof UnitDestroyed).count(), events.toString());
        assertNull(game.findUnitById(first));
        assertNull(game.findUnitById(second));
    }

    @Test
    void onlyTheUnitOnTopOfADeckCanBeAttacked() {
        for (int y = 0; y < 16; y++) {
            for (int x = 5; x < 16; x++) Fixtures.setTerrain(game, new Point(x, y), TerrainEnum.WATER);
        }
        research(Research.AMPHIBIOUS_DOCTRINE);
        Fixtures.place(game, 1, new Osumi(1, new Point(5, 5)));
        int top = Fixtures.place(game, 1, new RifleSquad(1, new Point(5, 5)));
        Fixtures.place(game, 1, new RifleSquad(1, new Point(5, 5)));
        int tank = Fixtures.place(game, 2, new TypeTen(2, new Point(3, 5)));

        assertEquals(List.of(top), attack.findTargets(game.getPlayerTwo(), tank));
    }

    // --- Command: the new abilities ---

    @Test
    void theCruiseMissileHitsAnyEnemyBuildingUnseenAndItselfEvenWithAUnitOnTop() {
        game.setFogOfWar(true);
        research(Research.CRUISE_MISSILE);
        ArmyBase target = new ArmyBase(2, new Point(12, 12));
        Fixtures.place(game, 2, target);
        TypeTen guard = new TypeTen(2, new Point(12, 12));
        Fixtures.place(game, 2, guard);
        assertFalse(game.getVisibleTiles(player).contains(target.getPosition()));

        assertFalse(abilities.execute(player, Ability.CRUISE_MISSILE, new Point(9, 9)).wasSuccessful(), "no building");
        assertTrue(abilities.execute(player, Ability.CRUISE_MISSILE, target.getPosition()).wasSuccessful());

        int expected = (int) Math.round(Ability.CRUISE_MISSILE.damage() * (100 - target.getArmorValue()) / 100.0);
        assertEquals(target.getMaxHealthPoints() - expected, target.getHealthPoints());
        assertEquals(guard.getMaxHealthPoints(), guard.getHealthPoints());
        assertTrue(target.isUnderFire());
    }

    @Test
    void paratroopersLandOnFreeGroundInSightAndCountTowardsTheLimit() {
        game.setFogOfWar(true);
        research(Research.PARATROOPERS);
        Fixtures.setTerrain(game, new Point(1, 2), TerrainEnum.WATER);
        assertFalse(abilities.execute(player, Ability.PARATROOPERS, new Point(10, 10)).wasSuccessful(), "out of sight");
        assertFalse(abilities.execute(player, Ability.PARATROOPERS, new Point(1, 2)).wasSuccessful(), "water");

        List<GameEvent> events = abilities.execute(player, Ability.PARATROOPERS, new Point(2, 2)).getEvents();

        assertInstanceOf(UnitDeployed.class, events.get(1));
        Unit squad = game.getUnitAt(new Point(2, 2));
        assertInstanceOf(RifleSquad.class, squad);
        assertFalse(squad.isAvailable(), "waits for the next turn like any new unit");
        assertEquals(1, game.getArmySize(player));

        int limit = game.getArmyLimit(player);
        for (int i = 1; i < limit; i++) Fixtures.place(game, 1, new RifleSquad(1, new Point(0, i)));
        player.startTurn();
        player.startTurn();
        player.startTurn(); // cooled down
        CommandResult full = abilities.execute(player, Ability.PARATROOPERS, new Point(3, 1));
        assertFalse(full.wasSuccessful());
        assertTrue(full.getMessage().contains("limit"), full.getMessage());
    }
}
