package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent.UnitDestroyed;
import game.events.GameEvent.UnitMoved;
import game.game.Movement;
import game.game.Player;
import game.game.UnitFactory;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FFifteen;
import game.units.assaultClasses.ships.Soryu;
import game.units.transports.Izumo;
import game.units.transports.Osumi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Units on top of buildings and ships: deploying onto them, boarding, sailing, landing and shielding. */
class NavalTransportTest {

    private Game game;
    private Player player;
    private AddCommand add;
    private MoveCommand move;
    private AttackCommand attack;
    private final UnitFactory factory = new UnitFactory();

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        // a strait from x = 5 to x = 9 splits the board in two
        for (int x = 5; x <= 9; x++) {
            for (int y = 0; y < 16; y++) Fixtures.setTerrain(game, new Point(x, y), TerrainEnum.WATER);
        }
        player = game.getPlayerOne();
        add = new AddCommand(game);
        move = new MoveCommand(game);
        attack = new AttackCommand(game);
        player.addResourcePoints(1000);
    }

    private Set<Point> deployable(UnitEnum type) {
        return add.findDeployablePositions(player, factory.createUnit(1, type, new Point()));
    }

    private void nextTurn() {
        game.switchTurn();
        game.switchTurn();
    }

    @Test
    void aGroundUnitBoardsAnOsumiIsCarriedAcrossAndGoesAshore() {
        Osumi ship = new Osumi(1, new Point(5, 3));
        int shipId = Fixtures.place(game, 1, ship);
        TypeTen tank = new TypeTen(1, new Point(3, 3));
        int tankId = Fixtures.place(game, 1, tank);

        assertTrue(Movement.reachable(game, tank).containsKey(new Point(5, 3)), "it can drive onto the ship");
        assertFalse(Movement.reachable(game, tank).containsKey(new Point(6, 3)), "but not on across the water");
        assertTrue(move.execute(player, tankId, new Point(5, 3)).wasSuccessful());
        assertSame(ship, game.getHostOf(tank));
        assertSame(tank, game.getUnitAt(new Point(5, 3)), "the unit on deck is the one seen and attacked first");

        nextTurn();
        CommandResult sailed = move.execute(player, shipId, new Point(9, 3));
        assertTrue(sailed.wasSuccessful());
        assertEquals(new Point(9, 3), tank.getPosition(), "it sails along");
        assertEquals(2, sailed.getEvents().size());
        UnitMoved passenger = assertInstanceOf(UnitMoved.class, sailed.getEvents().get(1));
        assertEquals(tankId, passenger.unitId());
        assertEquals(((UnitMoved) sailed.getEvents().get(0)).path(), passenger.path());

        nextTurn();
        assertTrue(move.execute(player, tankId, new Point(10, 4)).wasSuccessful(), "ashore on the far side");
        assertNull(game.getGuestOf(ship));
    }

    @Test
    void anOsumiDeploysLightUnitsOnlyWhileDockedAtTheCoast() {
        Fixtures.place(game, 1, new Osumi(1, new Point(5, 3))); // land at x = 4
        Fixtures.place(game, 1, new Osumi(1, new Point(7, 9))); // open water all around

        assertTrue(deployable(UnitEnum.INFANTRY).contains(new Point(5, 3)));
        assertTrue(deployable(UnitEnum.TYPESIXTEEN).contains(new Point(5, 3)), "a light vehicle");
        assertFalse(deployable(UnitEnum.TYPENINETY).contains(new Point(5, 3)), "a heavy tank needs a base");
        assertFalse(deployable(UnitEnum.INFANTRY).contains(new Point(7, 9)), "not docked");

        assertTrue(add.execute(player, UnitEnum.INFANTRY.name(), new Point(5, 3)).wasSuccessful());
        assertFalse(deployable(UnitEnum.INFANTRY).contains(new Point(5, 3)), "one unit on deck at a time");
    }

    @Test
    void anIzumoIsAMobileAirBase() {
        Izumo carrier = new Izumo(1, new Point(7, 3));
        Fixtures.place(game, 1, carrier);

        assertEquals(Set.of(new Point(7, 3)), deployable(UnitEnum.FFIFTEEN), "no air base needed");
        assertTrue(add.execute(player, UnitEnum.FFIFTEEN.name(), new Point(7, 3)).wasSuccessful());
        var jet = game.getUnitAt(new Point(7, 3));
        int jetId = game.getUnitID(jet);
        assertSame(carrier, game.getHostOf(jet));

        nextTurn();
        assertTrue(move.execute(player, jetId, new Point(7, 8)).wasSuccessful(), "it takes off");
        nextTurn();
        assertTrue(Movement.reachable(game, jet).containsKey(new Point(7, 3)), "and can land again");
        Izumo docked = new Izumo(1, new Point(5, 12));
        Fixtures.place(game, 1, docked);
        assertFalse(Movement.reachable(game, new TypeTen(1, new Point(4, 12))).containsKey(new Point(5, 12)),
                "tanks cannot board a carrier");
    }

    @Test
    void unitsAreDeployedOnTopOfTheirBuildingOneAtATime() {
        assertEquals(Set.of(new Point(0, 0)), deployable(UnitEnum.TYPETEN));
        assertTrue(add.execute(player, UnitEnum.TYPETEN.name(), new Point(0, 0)).wasSuccessful());
        assertTrue(deployable(UnitEnum.TYPETEN).isEmpty(), "the base is taken until the tank drives off");

        var tank = game.getUnitAt(new Point(0, 0));
        nextTurn();
        assertTrue(move.execute(player, game.getUnitID(tank), new Point(2, 2)).wasSuccessful());
        assertEquals(Set.of(new Point(0, 0)), deployable(UnitEnum.TYPETEN));
    }

    @Test
    void attacksOnABaseHitTheUnitOnTopFirst() {
        TypeTen guard = new TypeTen(1, new Point(0, 0));
        int guardId = Fixtures.place(game, 1, guard);
        int enemy = Fixtures.place(game, 2, new TypeNinety(2, new Point(2, 0)));
        Player bot = game.getPlayerTwo();
        int baseId = player.getPlayerDeck().getBase().getBaseID();

        assertTrue(attack.findTargets(bot, enemy).contains(guardId));
        assertFalse(attack.findTargets(bot, enemy).contains(baseId), "the tank on top shields the base");

        player.getPlayerDeck().removeUnit(guardId);
        assertTrue(attack.findTargets(bot, enemy).contains(baseId));
    }

    @Test
    void aSunkTransportTakesTheUnitOnDeckDownWithIt() {
        Osumi ship = new Osumi(1, new Point(7, 3));
        int shipId = Fixtures.place(game, 1, ship);
        TypeTen tank = new TypeTen(1, new Point(7, 3));
        int tankId = Fixtures.place(game, 1, tank);
        int submarine = Fixtures.place(game, 2, new Soryu(2, new Point(8, 4)));

        // a submarine cannot hit the tank, so the ship under it is fair game
        assertTrue(attack.findTargets(game.getPlayerTwo(), submarine).contains(shipId));
        CommandResult result = attack.execute(game.getPlayerTwo(), submarine, shipId);

        assertTrue(result.wasSuccessful());
        assertNull(player.getPlayerDeck().getUnit(shipId));
        assertNull(player.getPlayerDeck().getUnit(tankId));
        assertTrue(result.getEvents().stream().anyMatch(e -> e instanceof UnitDestroyed d && d.unitId() == tankId));
    }

    @Test
    void aircraftCanLandOnTheirOwnAirBaseButNotOnAnEnemyOne() {
        FFifteen jet = new FFifteen(1, new Point(2, 2));
        Fixtures.place(game, 1, jet);
        Fixtures.place(game, 2, new game.units.infrastructures.AirForceBase(2, new Point(3, 3)));
        Fixtures.place(game, 1, new game.units.infrastructures.AirForceBase(1, new Point(1, 3)));

        assertTrue(Movement.reachable(game, jet).containsKey(new Point(1, 3)));
        assertFalse(Movement.reachable(game, jet).containsKey(new Point(3, 3)));
    }

    @Test
    void beingCarriedIsThePassengersMoveForTheTurn() {
        int shipId = Fixtures.place(game, 1, new Osumi(1, new Point(5, 3)));
        TypeTen tank = new TypeTen(1, new Point(3, 3));
        int tankId = Fixtures.place(game, 1, tank);
        assertTrue(move.execute(player, tankId, new Point(5, 3)).wasSuccessful());

        nextTurn();
        assertTrue(move.execute(player, shipId, new Point(9, 3)).wasSuccessful());

        assertTrue(tank.hasMoved());
        assertFalse(move.execute(player, tankId, new Point(10, 4)).wasSuccessful(), "no driving on after the crossing");
    }
}
