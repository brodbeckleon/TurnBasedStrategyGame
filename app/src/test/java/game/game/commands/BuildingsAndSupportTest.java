package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDestroyed;
import game.events.GameEvent.UnitHealed;
import game.game.Player;
import game.game.UnitFactory;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Infrastructure;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightyOneC;
import game.units.infrastructures.AirForceBase;
import game.units.infrastructures.ArmyBase;
import game.units.infrastructures.NavyBase;
import game.units.infrastructures.Radarstation;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredReconaissanceVehicle;
import game.units.supportClasses.ArmoredRecoveryVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuildingsAndSupportTest {

    private final UnitFactory factory = new UnitFactory();
    private Game game;
    private Player player;
    private AddCommand add;
    private AttackCommand attack;
    private HealCommand heal;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        add = new AddCommand(game);
        attack = new AttackCommand(game);
        heal = new HealCommand(game);
    }

    private void giveResources(int amount) {
        while (player.getResourcePoints() < amount) player.addResourcePoints();
    }

    private Set<Point> deployable(UnitEnum type) {
        return add.findDeployablePositions(player, factory.createUnit(1, type, new Point()));
    }

    @Test
    void everyUnitTypeCanBeDeployedOnceItsBuildingStands() {
        for (int y = 0; y < 16; y++) Fixtures.setTerrain(game, new Point(6, y), TerrainEnum.WATER);
        Fixtures.place(game, 1, new AirForceBase(1, new Point(3, 3)));
        Fixtures.place(game, 1, new NavyBase(1, new Point(5, 8)));
        // ruins (one by the water) for the bases, each with a recovery vehicle next to it
        for (Point site : List.of(new Point(9, 3), new Point(9, 9), new Point(5, 11))) {
            Fixtures.addBaseSite(game, site);
            Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(site.x - 1, site.y - 1)));
        }
        giveResources(1000);

        for (UnitEnum type : UnitEnum.values()) {
            Set<Point> positions = deployable(type);
            assertFalse(positions.isEmpty(), "somewhere to deploy a " + type);
            Point position = positions.iterator().next();

            CommandResult result = add.execute(player, type.name(), position);

            assertTrue(result.wasSuccessful(), type + ": " + result.getMessage());
            assertEquals(type, typeOf(game.getUnitAt(position)));
        }
    }

    private UnitEnum typeOf(Unit unit) {
        for (UnitEnum type : UnitEnum.values()) {
            if (factory.createUnit(1, type, new Point()).getClass() == unit.getClass()) return type;
        }
        return null;
    }

    @Test
    void everyDeployableUnitHasArtForBothPlayers() {
        for (UnitEnum type : UnitEnum.values()) {
            String name = factory.createUnit(1, type, new Point()).getUnitName();
            for (int playerId = 1; playerId <= 2; playerId++) {
                assertNotNull(getClass().getResource("/images/units/player" + playerId + "/" + name + ".png"),
                        "sprite of " + name + " for player " + playerId);
            }
        }
    }

    @Test
    void aircraftNeedAnAirBase() {
        giveResources(20);

        assertTrue(deployable(UnitEnum.FFIFTEEN).isEmpty());
        assertFalse(add.execute(player, UnitEnum.FFIFTEEN.name(), new Point(1, 1)).wasSuccessful());

        Fixtures.place(game, 1, new AirForceBase(1, new Point(4, 4)));

        assertFalse(add.execute(player, UnitEnum.FFIFTEEN.name(), new Point(1, 1)).wasSuccessful(),
                "the base itself does not deploy aircraft");
        assertTrue(add.execute(player, UnitEnum.FFIFTEEN.name(), new Point(5, 5)).wasSuccessful());
    }

    @Test
    void shipsDeployOnTheWaterAroundANavalBase() {
        Fixtures.setTerrain(game, new Point(5, 1), TerrainEnum.WATER);
        Fixtures.setTerrain(game, new Point(6, 1), TerrainEnum.WATER);
        Fixtures.addBaseSite(game, new Point(5, 2));
        Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(4, 3)));
        giveResources(20);

        assertTrue(deployable(UnitEnum.YAMATO).isEmpty(), "no naval base yet");
        assertEquals(Set.of(new Point(5, 2)), deployable(UnitEnum.NAVYBASE));

        assertTrue(add.execute(player, UnitEnum.NAVYBASE.name(), new Point(5, 2)).wasSuccessful());

        assertEquals(Set.of(new Point(5, 1), new Point(6, 1)), deployable(UnitEnum.YAMATO));
    }

    @Test
    void navalBasesNeedRuinsByTheWater() {
        Fixtures.addBaseSite(game, new Point(8, 8));
        Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(7, 8)));
        giveResources(20);

        assertTrue(deployable(UnitEnum.NAVYBASE).isEmpty());
        assertFalse(add.execute(player, UnitEnum.NAVYBASE.name(), new Point(8, 8)).wasSuccessful());
        assertEquals(Set.of(new Point(8, 8)), deployable(UnitEnum.ARMYBASE));
    }

    @Test
    void anArmyBaseExtendsWhereUnitsAndBuildingsCanGo() {
        Fixtures.addBaseSite(game, new Point(7, 0));
        Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(6, 1)));
        giveResources(20);
        assertFalse(add.execute(player, UnitEnum.TYPENINETY.name(), new Point(8, 0)).wasSuccessful(), "too far from the base");

        assertTrue(add.execute(player, UnitEnum.ARMYBASE.name(), new Point(7, 0)).wasSuccessful());

        assertTrue(add.execute(player, UnitEnum.TYPENINETY.name(), new Point(8, 0)).wasSuccessful());
        assertTrue(deployable(UnitEnum.RADARSTATION).contains(new Point(10, 0)), "within 3 tiles of the army base");
        assertFalse(deployable(UnitEnum.RADARSTATION).contains(new Point(11, 0)));
    }

    @Test
    void buildingsNeverActAndAreNotCountedAsAvailable() {
        Infrastructure radar = new Radarstation(1, new Point(3, 3));
        Fixtures.place(game, 1, radar);
        player.getPlayerDeck().setUnitsAvailable();

        assertFalse(radar.isAvailable());
    }

    @Test
    void ambulanceHealsAnAdjacentFriendUpToItsMaximum() {
        int ambulance = Fixtures.place(game, 1, new ArmoredAmbulanceVehicle(1, new Point(5, 5)));
        TypeNinety tank = new TypeNinety(1, new Point(6, 5));
        int tankId = Fixtures.place(game, 1, tank);
        tank.setHealthPoints(-100);

        assertEquals(List.of(tankId), heal.findTargets(player, ambulance));
        CommandResult result = heal.execute(player, ambulance, tankId);

        assertTrue(result.wasSuccessful());
        int max = tank.getMaxHealthPoints();
        assertEquals(List.of(new UnitHealed(1, ambulance, "Armored Ambulance", new Point(5, 5), tankId, "Type 90",
                new Point(6, 5), 100, max - 100, max, max)), result.getEvents());
        assertEquals(max, tank.getHealthPoints());
        assertFalse(game.findUnitById(ambulance).isAvailable());
    }

    @Test
    void healingNeedsADamagedFriendInRangeOfTheRightKind() {
        int ambulance = Fixtures.place(game, 1, new ArmoredAmbulanceVehicle(1, new Point(5, 5)));
        TypeNinety far = new TypeNinety(1, new Point(8, 5));
        int farId = Fixtures.place(game, 1, far);
        far.setHealthPoints(-100);
        int healthy = Fixtures.place(game, 1, new TypeNinety(1, new Point(5, 6)));
        int enemy = Fixtures.place(game, 2, new TypeNinety(2, new Point(4, 5)));
        ArmyBase depot = new ArmyBase(1, new Point(6, 6));
        int depotId = Fixtures.place(game, 1, depot);
        depot.setHealthPoints(-100);

        assertFalse(heal.execute(player, ambulance, farId).wasSuccessful(), "out of range");
        assertFalse(heal.execute(player, ambulance, healthy).wasSuccessful(), "full health");
        assertFalse(heal.execute(player, ambulance, enemy).wasSuccessful(), "enemy");
        assertFalse(heal.execute(player, ambulance, depotId).wasSuccessful(), "ambulances do not repair buildings");
        assertTrue(heal.findTargets(player, ambulance).isEmpty());
    }

    @Test
    void recoveryVehicleRepairsTheBase() {
        int recovery = Fixtures.place(game, 1, new ArmoredRecoveryVehicle(1, new Point(1, 0)));
        var base = player.getPlayerDeck().getBase();
        base.setHealthPoints(-2000);

        CommandResult result = heal.execute(player, recovery, base.getBaseID());

        assertTrue(result.wasSuccessful(), result.getMessage());
        assertEquals(base.getMaxHealthPoints() - 2000 + 600, base.getHealthPoints());
    }

    @Test
    void aSpotterLetsUnitsAroundItShootOneTileFurther() {
        TypeNinety tank = new TypeNinety(1, new Point(5, 5));
        int tankId = Fixtures.place(game, 1, tank);
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(10, 5)));

        assertEquals(4, game.getShootingRange(tank));
        assertFalse(attack.execute(player, tankId, target).wasSuccessful(), "5 tiles away");

        Fixtures.place(game, 1, new ArmoredReconaissanceVehicle(1, new Point(4, 7)));

        assertEquals(5, game.getShootingRange(tank));
        assertTrue(attack.execute(player, tankId, target).wasSuccessful());
    }

    @Test
    void antiAirHitsAircraftTwiceAsHardAndOtherGroundUnitsHalfAsHard() {
        int antiAir = Fixtures.place(game, 1, new TypeEightyOneC(1, new Point(5, 5)));
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 7)));
        int jet = Fixtures.place(game, 2, new FTwo(2, new Point(7, 6)));
        int otherJet = Fixtures.place(game, 2, new FTwo(2, new Point(7, 7)));

        UnitDamaged flak = assertInstanceOf(UnitDamaged.class, attack.execute(player, antiAir, jet).getEvents().get(1));
        UnitDamaged shell = assertInstanceOf(UnitDamaged.class, attack.execute(player, tank, otherJet).getEvents().get(1));

        assertEquals(2 * new TypeEightyOneC(1, new Point()).getDamage(), flak.damage());
        assertEquals(Math.round(new TypeTen(1, new Point()).getDamage() * 0.75), shell.damage());
    }

    @Test
    void destroyedBuildingsAreRemovedLikeUnits() {
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        Radarstation radar = new Radarstation(2, new Point(7, 5));
        int radarId = Fixtures.place(game, 2, radar);
        radar.setHealthPoints(-(radar.getMaxHealthPoints() - 100));

        var events = attack.execute(player, tank, radarId).getEvents();

        UnitDamaged damaged = assertInstanceOf(UnitDamaged.class, events.get(1));
        assertEquals(Math.round(new TypeTen(1, new Point()).getDamage() * 1.5 * (100 - radar.getArmorValue()) / 100.0),
                damaged.damage(), "tanks deal one and a half times their damage to buildings, minus the armour percentage");
        UnitDestroyed destroyed = assertInstanceOf(UnitDestroyed.class, events.get(2));
        assertFalse(destroyed.isBase());
        assertNull(game.findUnitById(radarId));
    }
}
