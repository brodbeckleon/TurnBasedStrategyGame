package game.game;

import game.enums.TerrainEnum;
import game.interfaces.MovableUnit;
import game.interfaces.mobilityType.ByAir;
import game.interfaces.mobilityType.ByFoot;
import game.interfaces.mobilityType.ByRoad;
import game.interfaces.mobilityType.ByWater;
import game.model.Game;
import game.units.Unit;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * How units get around: every step to a neighbouring tile (diagonals included) costs movement points depending on
 * the terrain and how the unit travels. Vehicles need roads or open ground (forest costs them double), infantry
 * crosses forest freely and climbs mountains slowly, ships stay on water and aircraft fly anywhere. Enemy ground and
 * sea units block the way; friends can be passed but not stopped on.
 */
public final class Movement {
    public static final int IMPASSABLE = Integer.MAX_VALUE;
    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private Movement() {
    }

    /** The movement points one step onto this terrain costs the unit, or {@link #IMPASSABLE}. */
    public static int cost(Unit unit, TerrainEnum terrain) {
        if (terrain == null) return IMPASSABLE;
        if (unit instanceof ByAir) return 1;
        if (unit instanceof ByWater) return terrain == TerrainEnum.WATER ? 1 : IMPASSABLE;
        if (unit instanceof ByFoot) {
            return switch (terrain) {
                case PLAIN, FOREST -> 1;
                case MOUNTAIN -> 2;
                case WATER -> IMPASSABLE;
            };
        }
        if (unit instanceof ByRoad) {
            return switch (terrain) {
                case PLAIN -> 1;
                case FOREST -> 2;
                case MOUNTAIN, WATER -> IMPASSABLE;
            };
        }
        return IMPASSABLE;
    }

    /**
     * Every tile the unit can end its move on this turn, each with the cheapest way there (the unit's tile first,
     * the destination last). The unit's own tile is not included.
     */
    public static Map<Point, List<Point>> reachable(Game game, Unit unit) {
        Map<Point, List<Point>> destinations = new LinkedHashMap<>();
        if (!(unit instanceof MovableUnit movable) || movable.getMovingRange() <= 0) {
            return destinations;
        }
        Point start = unit.getPosition();
        Player owner = unit.getPlayerID() == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        Set<Point> seen = game.isFogOfWar() ? Vision.visibleTiles(game, owner) : Set.of();
        Map<Point, Integer> spent = new HashMap<>();
        Map<Point, Point> previous = new HashMap<>();
        PriorityQueue<Point> queue = new PriorityQueue<>((a, b) -> Integer.compare(spent.get(a), spent.get(b)));
        spent.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            for (int[] step : STEPS) {
                Point next = new Point(point.x + step[0], point.y + step[1]);
                int cost = cost(unit, game.getBattlefield().getGeographyOfPoint(next));
                if (cost == IMPASSABLE || blocks(game, unit, next, owner, seen)) continue;
                int total = spent.get(point) + cost;
                if (total <= movable.getMovingRange() && total < spent.getOrDefault(next, IMPASSABLE)) {
                    spent.put(next, total);
                    previous.put(next, point);
                    queue.remove(next);
                    queue.add(next);
                }
            }
        }
        for (Point point : spent.keySet()) {
            if (!point.equals(start) && (game.isPositionFree(point) || isHiddenEnemy(game, unit, point, owner, seen))) {
                destinations.put(point, path(previous, start, point));
            }
        }
        return destinations;
    }

    /** The cheapest way from the unit's tile to the destination, or an empty list if it cannot get there. */
    public static List<Point> path(Game game, Unit unit, Point destination) {
        return reachable(game, unit).getOrDefault(destination, List.of());
    }

    /**
     * Enemy units the mover's side can see bar the way of ground and sea units; aircraft fly over everything.
     * Enemies hidden in the fog are planned through: walking into one is an ambush (see MoveCommand).
     */
    private static boolean blocks(Game game, Unit unit, Point point, Player owner, Set<Point> seen) {
        if (unit instanceof ByAir) return false;
        Unit other = game.getUnitAt(point);
        return other != null && other.getPlayerID() != unit.getPlayerID() && !(other instanceof ByAir)
                && !isHiddenEnemy(game, unit, point, owner, seen);
    }

    /** True if an enemy the mover's side cannot see stands on the tile. */
    public static boolean isHiddenEnemy(Game game, Unit mover, Point point) {
        Player owner = mover.getPlayerID() == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        return isHiddenEnemy(game, mover, point, owner, game.isFogOfWar() ? Vision.visibleTiles(game, owner) : Set.of());
    }

    /**
     * True if a hidden enemy on the tile stops the mover there: one it would bump into if it could see it (aircraft
     * fly over everything, and nothing on the ground is stopped by an aircraft), or one on the tile it means to stop on.
     */
    public static boolean isAmbush(Game game, Unit mover, Point point, boolean destination) {
        if (!isHiddenEnemy(game, mover, point)) return false;
        return destination || (!(mover instanceof ByAir) && !(game.getUnitAt(point) instanceof ByAir));
    }

    private static boolean isHiddenEnemy(Game game, Unit mover, Point point, Player owner, Set<Point> seen) {
        if (!game.isFogOfWar()) return false;
        Unit other = game.getUnitAt(point);
        return other != null && other.getPlayerID() != mover.getPlayerID() && !Vision.canSee(owner, other, seen);
    }

    private static List<Point> path(Map<Point, Point> previous, Point start, Point end) {
        List<Point> path = new ArrayList<>();
        for (Point point = end; point != null && !point.equals(start); point = previous.get(point)) {
            path.add(point);
        }
        path.add(start);
        Collections.reverse(path);
        return path;
    }
}
