package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapGeneratorTest {

    @Test
    void everyMapHasRuinsAndARecoveryVehicleCanReachThemAll() {
        for (int i = 0; i < 1000; i++) {
            MapGenerator generator = new MapGenerator(new CircleGenerator(), 16, 16);
            HashMap<Point, TerrainEnum> map = generator.generate();
            Set<Point> sites = generator.generateBaseSites(map);
            Set<Point> land = landReachableFromHq(map);
            assertFalse(sites.isEmpty(), "map " + i + " has no ruins");
            for (Point site : sites) {
                boolean reachable = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        reachable |= (dx != 0 || dy != 0) && land.contains(new Point(site.x + dx, site.y + dy));
                    }
                }
                assertTrue(reachable, "ruins at " + site + " cannot be reached on map " + i);
            }
        }
    }

    private static boolean nextToAny(Point site, Set<Point> land) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0 || dy != 0) && land.contains(new Point(site.x + dx, site.y + dy))) return true;
            }
        }
        return false;
    }

    private static Set<Point> landReachableFromHq(HashMap<Point, TerrainEnum> map) {
        return landReachableFrom(map, new Point(0, 0));
    }

    private static Set<Point> landReachableFrom(HashMap<Point, TerrainEnum> map, Point start) {
        Set<Point> reached = new HashSet<>(Set.of(start));
        Deque<Point> queue = new ArrayDeque<>(reached);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Point next = new Point(point.x + dx, point.y + dy);
                    TerrainEnum terrain = map.get(next);
                    if ((terrain == TerrainEnum.PLAIN || terrain == TerrainEnum.FOREST) && reached.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return reached;
    }

    @Test
    void everyMapTypeAndSizeHasRuinsForEveryone() {
        for (MapType type : MapType.values()) {
            for (MapSize size : MapSize.values()) {
                for (int players = 2; players <= 5; players++) {
                    int tiles = size.tiles();
                    List<Point> bases = MapGenerator.basePositions(tiles, tiles, players);
                    MapGenerator generator = new MapGenerator(new CircleGenerator(), tiles, tiles, type, bases);
                    HashMap<Point, TerrainEnum> map = generator.generate();
                    Set<Point> sites = generator.generateBaseSites(map);
                    assertEquals(tiles * tiles, map.size());
                    assertFalse(sites.isEmpty(), type + " " + size + " with " + players + " players has ruins");
                    for (Point base : bases) {
                        assertEquals(TerrainEnum.PLAIN, map.get(base), "headquarters on open ground");
                        if (!type.landBridges()) {
                            Set<Point> island = landReachableFrom(map, base);
                            assertTrue(sites.stream().anyMatch(site -> nextToAny(site, island)),
                                    "on " + type + " every headquarters gets ruins on its own island");
                        }
                    }
                }
            }
        }
    }

    @Test
    void islandMapsAreMostlyWater() {
        double water = 0, tiles = 0;
        for (int i = 0; i < 50; i++) {
            MapGenerator generator = new MapGenerator(new CircleGenerator(), 16, 16, MapType.WATERWORLD,
                    MapGenerator.basePositions(16, 16, 2));
            for (TerrainEnum terrain : generator.generate().values()) {
                if (terrain == TerrainEnum.WATER) water++;
                tiles++;
            }
        }
        assertTrue(water / tiles > 0.6, "waterworld is " + Math.round(100 * water / tiles) + "% water");
    }
}
