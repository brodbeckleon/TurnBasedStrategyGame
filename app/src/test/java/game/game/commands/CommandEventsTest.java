package game.game.commands;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitAttacked;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDeployed;
import game.events.GameEvent.UnitDestroyed;
import game.events.GameEvent.UnitMoved;
import game.events.Weapon;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Base;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.ships.Yamato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandEventsTest {

    private Game game;
    private AddCommand add;
    private MoveCommand move;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        add = new AddCommand(game);
        move = new MoveCommand(game);
        attack = new AttackCommand(game);
    }

    @Test
    void commandResultWithoutEventsHasAnEmptyList() {
        CommandResult result = new CommandResult(true, "ok");

        assertTrue(result.getEvents().isEmpty());
    }

    @Test
    void commandResultEventsCannotBeModified() {
        CommandResult result = new CommandResult(true, "ok", List.of(new UnitDestroyed(1, 3, "Type 90", new Point(1, 1), false)));

        assertThrows(UnsupportedOperationException.class, () -> result.getEvents().clear());
    }

    @Test
    void addEmitsUnitDeployedWithTheAssignedId() {
        CommandResult result = add.execute(game.getPlayerOne(), UnitEnum.TYPENINETY.name(), new Point(1, 1));

        assertTrue(result.wasSuccessful());
        int unitId = game.getUnitID(game.getUnitAt(new Point(1, 1)));
        assertEquals(List.of(new UnitDeployed(1, unitId, "Type 90", new Point(1, 1))), result.getEvents());
    }

    @Test
    void failedAddsEmitNothing() {
        assertTrue(add.execute(game.getPlayerOne(), UnitEnum.TYPENINETY.name(), new Point(9, 9)).getEvents().isEmpty(),
                "outside the deployment radius");
        assertTrue(add.execute(game.getPlayerOne(), UnitEnum.TYPENINETY.name(), new Point(0, 0)).getEvents().isEmpty(),
                "position taken by the base");
        assertTrue(add.execute(game.getPlayerOne(), UnitEnum.YAMATO.name(), new Point(1, 1)).getEvents().isEmpty(),
                "ship on land");

        assertTrue(add.execute(game.getPlayerOne(), UnitEnum.TYPENINETY.name(), new Point(1, 1)).wasSuccessful());
        CommandResult broke = add.execute(game.getPlayerOne(), UnitEnum.TYPENINETY.name(), new Point(2, 1));
        assertFalse(broke.wasSuccessful(), "only one Type 90 can be paid with 6 RP");
        assertTrue(broke.getEvents().isEmpty());
    }

    @Test
    void moveEmitsUnitMovedWithOldAndNewPosition() {
        int id = Fixtures.place(game, 1, new TypeNinety(1, new Point(4, 4)));

        CommandResult result = move.execute(game.getPlayerOne(), id, new Point(5, 5));

        assertTrue(result.wasSuccessful());
        assertEquals(List.of(new UnitMoved(1, id, "Type 90", new Point(4, 4), new Point(5, 5))), result.getEvents());
        assertEquals(new Point(5, 5), game.findUnitById(id).getPosition());
    }

    @Test
    void eventPositionsAreCopiesOfTheModelPositions() {
        int id = Fixtures.place(game, 1, new TypeNinety(1, new Point(4, 4)));

        UnitMoved moved = assertInstanceOf(UnitMoved.class, move.execute(game.getPlayerOne(), id, new Point(5, 5)).getEvents().get(0));
        moved.to().translate(3, 3);

        assertEquals(new Point(5, 5), game.findUnitById(id).getPosition());
    }

    @Test
    void failedMovesEmitNothing() {
        int id = Fixtures.place(game, 1, new TypeNinety(1, new Point(4, 4)));
        Fixtures.place(game, 1, new TypeNinety(1, new Point(6, 4)));

        assertTrue(move.execute(game.getPlayerOne(), id, new Point(12, 12)).getEvents().isEmpty(), "out of range");
        assertTrue(move.execute(game.getPlayerOne(), id, new Point(6, 4)).getEvents().isEmpty(), "occupied");
        Fixtures.setTerrain(game, new Point(5, 4), TerrainEnum.WATER);
        assertTrue(move.execute(game.getPlayerOne(), id, new Point(5, 4)).getEvents().isEmpty(), "water");
        game.findUnitById(id).setIsAvailable(false);
        assertTrue(move.execute(game.getPlayerOne(), id, new Point(4, 5)).getEvents().isEmpty(), "unavailable");
    }

    @Test
    void attackOnAUnitEmitsAttackedThenDamaged() {
        int attacker = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        CommandResult result = attack.execute(game.getPlayerOne(), attacker, target);

        assertTrue(result.wasSuccessful());
        assertEquals(List.of(
                new UnitAttacked(1, attacker, "Type 10", new Point(5, 5), target, new Point(7, 5), false, Weapon.SHELL),
                new UnitDamaged(2, target, "Type 90", new Point(7, 5), 200, 600, 400, 600, false)
        ), result.getEvents());
    }

    @Test
    void attackThatKillsEmitsDestroyedLastAndRemovesTheUnit() {
        int attacker = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        TypeNinety victim = new TypeNinety(2, new Point(7, 5));
        int target = Fixtures.place(game, 2, victim);
        victim.setHealthPoints(-500);

        CommandResult result = attack.execute(game.getPlayerOne(), attacker, target);

        List<GameEvent> events = result.getEvents();
        assertEquals(3, events.size());
        assertInstanceOf(UnitAttacked.class, events.get(0));
        assertEquals(new UnitDamaged(2, target, "Type 90", new Point(7, 5), 200, 100, -100, 600, false), events.get(1));
        assertEquals(new UnitDestroyed(2, target, "Type 90", new Point(7, 5), false), events.get(2));
        assertNull(game.findUnitById(target));
    }

    @Test
    void attackOnPlayerOneUnitsReportsPlayerOneAsOwner() {
        int target = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        int attacker = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        List<GameEvent> events = attack.execute(game.getPlayerTwo(), attacker, target).getEvents();

        assertEquals(new UnitAttacked(2, attacker, "Type 90", new Point(7, 5), target, new Point(5, 5), false, Weapon.SHELL), events.get(0));
        assertEquals(new UnitDamaged(1, target, "Type 10", new Point(5, 5), 180, 400, 220, 400, false), events.get(1));
    }

    @Test
    void attackThatDoesNoDamageStillEmitsDamagedWithZero() {
        int attacker = Fixtures.place(game, 1, new TypeNinety(1, new Point(14, 15)));
        game.getPlayerTwo().getPlayerDeck().getBase().setArmorValue(5000);

        List<GameEvent> events = attack.execute(game.getPlayerOne(), attacker, 2).getEvents();

        assertEquals(new UnitDamaged(2, 2, "Base", new Point(15, 15), 0, 10000, 10000, 10000, true), events.get(1));
    }

    @Test
    void attackOnTheBaseIsFlaggedAndKeepsTheBaseInTheModel() {
        int attacker = Fixtures.place(game, 1, new TypeNinety(1, new Point(14, 15)));

        List<GameEvent> events = attack.execute(game.getPlayerOne(), attacker, 2).getEvents();

        assertEquals(new UnitAttacked(1, attacker, "Type 90", new Point(14, 15), 2, new Point(15, 15), true, Weapon.SHELL), events.get(0));
        assertEquals(new UnitDamaged(2, 2, "Base", new Point(15, 15), 760, 10000, 9240, 10000, true), events.get(1));
        assertEquals(2, events.size());
    }

    @Test
    void destroyingTheBaseEmitsDestroyedForTheBase() {
        int attacker = Fixtures.place(game, 1, new TypeNinety(1, new Point(14, 15)));
        Base base = game.getPlayerTwo().getPlayerDeck().getBase();
        base.setHealthPoints(-9500);

        List<GameEvent> events = attack.execute(game.getPlayerOne(), attacker, 2).getEvents();

        assertEquals(3, events.size());
        assertEquals(new UnitDamaged(2, 2, "Base", new Point(15, 15), 760, 500, -260, 10000, true), events.get(1));
        assertEquals(new UnitDestroyed(2, 2, "Base", new Point(15, 15), true), events.get(2));
        assertSame(base, game.getPlayerTwo().getPlayerDeck().getBase());
    }

    @Test
    void shipsAttackWithTheNavalGun() {
        Fixtures.setTerrain(game, new Point(5, 5), TerrainEnum.WATER);
        int attacker = Fixtures.place(game, 1, new Yamato(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 5)));

        List<GameEvent> events = attack.execute(game.getPlayerOne(), attacker, target).getEvents();

        UnitAttacked attacked = assertInstanceOf(UnitAttacked.class, events.get(0));
        assertEquals(Weapon.NAVAL_GUN, attacked.weapon());
        assertEquals("Yamato", attacked.attackerName());
        assertEquals(1100, assertInstanceOf(UnitDamaged.class, events.get(1)).damage());
    }

    @Test
    void failedAttacksEmitNothing() {
        int attacker = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        int far = Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 5)));
        int near = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        assertTrue(attack.execute(game.getPlayerOne(), attacker, far).getEvents().isEmpty(), "out of range");
        assertTrue(attack.execute(game.getPlayerOne(), 9999, near).getEvents().isEmpty(), "no such attacker");
        assertTrue(attack.execute(game.getPlayerOne(), 1, near).getEvents().isEmpty(), "the base cannot attack");

        game.findUnitById(attacker).setIsAvailable(false);
        CommandResult unavailable = attack.execute(game.getPlayerOne(), attacker, near);
        assertFalse(unavailable.wasSuccessful());
        assertTrue(unavailable.getEvents().isEmpty());
        assertEquals(600, game.findUnitById(near).getHealthPoints());
    }

    @Test
    void eventsMatchTheModelAfterwards() {
        int attacker = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        List<GameEvent> events = attack.execute(game.getPlayerOne(), attacker, target).getEvents();

        UnitDamaged damaged = assertInstanceOf(UnitDamaged.class, events.get(1));
        assertEquals(game.findUnitById(target).getHealthPoints(), damaged.hpAfter());
        assertEquals(damaged.hpBefore() - damaged.damage(), damaged.hpAfter());
    }
}
