package game.game;

import game.enums.TerrainEnum;
import game.events.GameEvent.UnitMoved;
import game.game.commands.AttackCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FTwo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MovementTest {

    private Game game;
    private MoveCommand move;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        move = new MoveCommand(game);
    }

    private void wall(int x, int fromY, int toY, TerrainEnum terrain) {
        for (int y = fromY; y <= toY; y++) Fixtures.setTerrain(game, new Point(x, y), terrain);
    }

    @Test
    void onOpenGroundAUnitReachesEveryTileWithinItsMovement() {
        TypeTen tank = new TypeTen(1, new Point(7, 7)); // moves 3
        Fixtures.place(game, 1, tank);

        Map<Point, List<Point>> reachable = Movement.reachable(game, tank);

        assertTrue(reachable.containsKey(new Point(10, 10)), "three diagonal steps");
        assertFalse(reachable.containsKey(new Point(11, 7)), "four steps away");
        assertFalse(reachable.containsKey(new Point(7, 7)), "not its own tile");
    }

    @Test
    void vehiclesDriveAroundWaterInsteadOfJumpingIt() {
        wall(8, 0, 9, TerrainEnum.WATER); // a lake with a way round at y >= 10
        TypeTen tank = new TypeTen(1, new Point(7, 5));
        Fixtures.place(game, 1, tank);

        assertFalse(Movement.reachable(game, tank).containsKey(new Point(9, 5)), "two tiles away, but across the lake");

        tank.setPosition(new Point(7, 8));
        List<Point> path = Movement.path(game, tank, new Point(9, 9));
        assertEquals(4, path.size(), "three steps round the end of the lake: " + path);
        assertPathOverLand(path);
    }

    @Test
    void forestCostsVehiclesDoubleButAircraftFlyOverEverything() {
        wall(8, 0, 15, TerrainEnum.FOREST);
        TypeTen tank = new TypeTen(1, new Point(7, 5)); // moves 3
        Fixtures.place(game, 1, tank);
        FTwo jet = new FTwo(1, new Point(7, 9));
        Fixtures.place(game, 1, jet);
        wall(9, 6, 12, TerrainEnum.MOUNTAIN);

        assertTrue(Movement.reachable(game, tank).containsKey(new Point(9, 5)), "1 + 2 into the forest, then 1 out");
        assertFalse(Movement.reachable(game, tank).containsKey(new Point(10, 5)), "forest ate the movement");
        assertTrue(Movement.reachable(game, jet).containsKey(new Point(11, 9)), "over forest and mountains");
    }

    @Test
    void enemiesBlockThePathButFriendsCanBePassed() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, tank);
        wall(6, 0, 15, TerrainEnum.WATER);
        Fixtures.setTerrain(game, new Point(6, 5), TerrainEnum.PLAIN); // a single ford

        Fixtures.place(game, 1, new TypeNinety(1, new Point(6, 5)));
        assertTrue(Movement.reachable(game, tank).containsKey(new Point(7, 5)), "past the friend on the ford");

        game.getPlayerOne().getPlayerDeck().getUnits().values().removeIf(unit -> unit.getPosition().equals(new Point(6, 5)));
        Fixtures.place(game, 2, new TypeNinety(2, new Point(6, 5)));
        assertFalse(Movement.reachable(game, tank).containsKey(new Point(7, 5)), "the enemy holds the ford");
    }

    @Test
    void ruinsCanBeCrossedButNotStoppedOn() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, tank);
        Fixtures.addBaseSite(game, new Point(6, 5));

        Map<Point, List<Point>> reachable = Movement.reachable(game, tank);

        assertFalse(reachable.containsKey(new Point(6, 5)));
        assertTrue(reachable.containsKey(new Point(7, 5)));
    }

    @Test
    void theMovedEventCarriesThePath() {
        wall(8, 0, 9, TerrainEnum.WATER);
        TypeTen tank = new TypeTen(1, new Point(7, 8));
        int id = Fixtures.place(game, 1, tank);

        UnitMoved moved = assertInstanceOf(UnitMoved.class, move.execute(game.getPlayerOne(), id, new Point(9, 9)).getEvents().get(0));

        assertEquals(new Point(7, 8), moved.path().get(0));
        assertEquals(new Point(9, 9), moved.path().get(moved.path().size() - 1));
        assertPathOverLand(moved.path());
        assertFalse(move.execute(game.getPlayerOne(), id, new Point(9, 5)).wasSuccessful(), "already moved");
    }

    /** Every step goes to a neighbouring tile on open ground. */
    private void assertPathOverLand(List<Point> path) {
        for (int i = 1; i < path.size(); i++) {
            Point a = path.get(i - 1), b = path.get(i);
            assertTrue(Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1, a + " -> " + b + " is one step");
            assertEquals(TerrainEnum.PLAIN, game.getBattlefield().getGeographyOfPoint(b), b + " is dry land");
        }
    }

    @Test
    void unitsInForestOrOnMountainsTakeLessDamage() {
        AttackCommand attack = new AttackCommand(game);
        TypeTen attacker = new TypeTen(1, new Point(5, 5));
        TypeNinety target = new TypeNinety(2, new Point(7, 5));
        int open = attack.predictDamage(attacker, target);

        Fixtures.setTerrain(game, new Point(7, 5), TerrainEnum.FOREST);
        assertEquals(Math.round(open * 0.8), attack.predictDamage(attacker, target));
        Fixtures.setTerrain(game, new Point(7, 5), TerrainEnum.MOUNTAIN);
        assertEquals(Math.round(open * 0.7), attack.predictDamage(attacker, target));

        FTwo jet = new FTwo(2, new Point(7, 5));
        assertEquals(0, attack.coverPercent(jet, new Point(7, 5)), "aircraft get no cover");
    }

    @Test
    void aMoveCanBeTakenBackUntilTheUnitActs() {
        TypeNinety heavy = new TypeNinety(1, new Point(3, 3)); // done after moving
        int id = Fixtures.place(game, 1, heavy);
        UnitMoved moved = (UnitMoved) move.execute(game.getPlayerOne(), id, new Point(5, 3)).getEvents().get(0);
        assertFalse(heavy.isAvailable());

        var undone = move.undo(game.getPlayerOne(), id, moved.path());

        assertTrue(undone.wasSuccessful());
        assertEquals(new Point(3, 3), heavy.getPosition());
        assertTrue(heavy.isAvailable());
        assertFalse(heavy.hasMoved());
        UnitMoved back = assertInstanceOf(UnitMoved.class, undone.getEvents().get(0));
        assertEquals(moved.path().get(moved.path().size() - 1), back.path().get(0));
        assertTrue(move.execute(game.getPlayerOne(), id, new Point(4, 4)).wasSuccessful(), "it may move again");
    }

    @Test
    void noUndoAfterAttackingOrWhenTheWayBackIsTaken() {
        TypeTen tank = new TypeTen(1, new Point(3, 3));
        int id = Fixtures.place(game, 1, tank);
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 3)));
        UnitMoved moved = (UnitMoved) move.execute(game.getPlayerOne(), id, new Point(5, 3)).getEvents().get(0);
        new AttackCommand(game).execute(game.getPlayerOne(), id, target);

        assertFalse(move.undo(game.getPlayerOne(), id, moved.path()).wasSuccessful(), "it already attacked");

        TypeTen other = new TypeTen(1, new Point(3, 8));
        int otherId = Fixtures.place(game, 1, other);
        UnitMoved otherMove = (UnitMoved) move.execute(game.getPlayerOne(), otherId, new Point(5, 8)).getEvents().get(0);
        Fixtures.place(game, 1, new TypeNinety(1, new Point(3, 8)));
        assertFalse(move.undo(game.getPlayerOne(), otherId, otherMove.path()).wasSuccessful(), "the way back is blocked");
    }
}
