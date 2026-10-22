package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    /** A generated map with its ruins, from a fixed seed so a failure can be replayed. */
    private record Generated(MapType type, int tiles, int players, long seed, List<Point> bases,
                             HashMap<Point, TerrainEnum> map, Set<Point> sites) {
        @Override
        public String toString() {
            return type + " " + tiles + "x" + tiles + " with " + players + " players, seed " + seed;
        }

        /** The site and its mirror images, as the generator places them. */
        List<Point> images(Point site) {
            Set<Point> images = new LinkedHashSet<>();
            images.add(site);
            images.add(new Point(tiles - 1 - site.x, tiles - 1 - site.y));
            if (players > 2) {
                images.add(new Point(tiles - 1 - site.x, site.y));
                images.add(new Point(site.x, tiles - 1 - site.y));
            }
            return List.copyOf(images);
        }

        /** True if the ruins come with all their mirror images (the extra harbour ruins on island maps do not). */
        boolean mirrored(Point site) {
            return sites.containsAll(images(site));
        }
    }

    /** Every map type in every size for two to five players, from sixteen seeds each; made once for all tests. */
    private static final class Sweep {
        static final List<Generated> MAPS = generate(16);
    }

    /** The sweep's maps of the given types. */
    private static List<Generated> sweep(MapType... types) {
        return Sweep.MAPS.stream().filter(generated -> List.of(types).contains(generated.type())).toList();
    }

    private static List<Generated> generate(int seeds) {
        List<Generated> maps = new ArrayList<>();
        for (MapType type : MapType.values()) {
            for (MapSize size : MapSize.values()) {
                for (int players = 2; players <= 5; players++) {
                    for (long seed = 0; seed < seeds; seed++) {
                        int tiles = size.tiles();
                        List<Point> bases = MapGenerator.basePositions(tiles, tiles, players);
                        MapGenerator generator =
                                new MapGenerator(new CircleGenerator(), tiles, tiles, type, bases, seed);
                        HashMap<Point, TerrainEnum> map = generator.generate();
                        Set<Point> sites = generator.generateBaseSites(map);
                        maps.add(new Generated(type, tiles, players, seed, bases, map, sites));
                    }
                }
            }
        }
        return maps;
    }

    private static boolean isLand(TerrainEnum terrain) {
        return terrain == TerrainEnum.PLAIN || terrain == TerrainEnum.FOREST;
    }

    /** As Command.bordersWater: one of the four tiles next to the point is water. */
    private static boolean bordersWater(HashMap<Point, TerrainEnum> map, Point point) {
        return orthogonal(point).stream().anyMatch(next -> map.get(next) == TerrainEnum.WATER);
    }

    private static List<Point> orthogonal(Point point) {
        return List.of(new Point(point.x + 1, point.y), new Point(point.x - 1, point.y),
                new Point(point.x, point.y + 1), new Point(point.x, point.y - 1));
    }

    @Test
    void theSameSeedGivesTheSameMap() {
        for (MapType type : MapType.values()) {
            List<Point> bases = MapGenerator.basePositions(16, 16, 4);
            MapGenerator first = new MapGenerator(new CircleGenerator(), 16, 16, type, bases, 42);
            MapGenerator second = new MapGenerator(new CircleGenerator(), 16, 16, type, bases, 42);
            HashMap<Point, TerrainEnum> firstMap = first.generate();
            HashMap<Point, TerrainEnum> secondMap = second.generate();
            assertEquals(first.generateBaseSites(firstMap), second.generateBaseSites(secondMap), type + " ruins");
            assertEquals(firstMap, secondMap, type + " terrain");
        }
    }

    @Test
    void mirroredRuinsAllStandOnOpenGroundWithLandAround() {
        for (Generated generated : sweep(MapType.values())) {
            for (Point site : generated.sites()) {
                assertEquals(TerrainEnum.PLAIN, generated.map().get(site), "ruins at " + site + " on " + generated);
                if (!generated.mirrored(site)) {
                    assertFalse(generated.type().landBridges(),
                            "ruins at " + site + " lack mirror images on " + generated);
                    continue;
                }
                // a vehicle can get next to every image of the group, and none stands alone in the sea
                long land = orthogonal(site).stream().filter(next -> isLand(generated.map().get(next))).count();
                assertTrue(land >= 2,
                        "ruins at " + site + " have " + land + " land tiles next to them on " + generated);
            }
        }
    }

    @Test
    void islandRuinsAreNeverLoneTilesInTheSea() {
        for (Generated generated : sweep(MapType.ARCHIPELAGO, MapType.WATERWORLD)) {
            for (Point site : generated.sites()) {
                Set<Point> island = landReachableFrom(generated.map(), site);
                assertTrue(island.size() >= 3, "ruins at " + site + " sit on an island of " + island.size()
                        + " tiles on " + generated);
            }
        }
    }

    @Test
    void homeRuinsOnIslandMapsAreOnTheShoreWheneverTheIslandHasOne() {
        for (Generated generated : sweep(MapType.ARCHIPELAGO, MapType.WATERWORLD)) {
            HashMap<Point, TerrainEnum> map = generated.map();
            for (Point site : generated.sites()) {
                if (generated.mirrored(site) || bordersWater(map, site)) continue;
                // ruins inland are only fine if no free tile of the island touches the sea
                Set<Point> island = landReachableFrom(map, site);
                Point shore = island.stream()
                        .filter(point -> bordersWater(map, point))
                        .filter(point -> generated.bases().stream().allMatch(base -> base.distance(point) >= 2))
                        .filter(point -> generated.sites().stream()
                                .allMatch(other -> other.equals(site) || other.distance(point) >= 2))
                        .findAny().orElse(null);
                assertNull(shore, "home ruins at " + site + " cannot host a harbour, though " + shore
                        + " on the same island could, on " + generated);
            }
        }
    }

    @Test
    void landMapsWithACoastGetRuinsOnIt() {
        int coasts = 0, missed = 0;
        for (Generated generated : sweep(MapType.CONTINENTS, MapType.BIG_CONTINENT, MapType.HIGHLANDS)) {
            // on smaller boards the coast is often closer to a headquarters than any ruins may be
            if (generated.tiles() < MapSize.MEDIUM.tiles()) continue;
            HashMap<Point, TerrainEnum> map = generated.map();
            boolean coast = map.keySet().stream().anyMatch(point -> isLand(map.get(point)) && bordersWater(map, point));
            if (!coast) continue;
            coasts++;
            boolean harbour = generated.sites().stream().anyMatch(site -> bordersWater(map, site));
            if (!harbour) missed++;
            if (generated.type() == MapType.BIG_CONTINENT) {
                assertTrue(harbour, "no ruins on the coast of " + generated);
            }
        }
        // now and then the only water is a lake where no mirrored group of ruins fits
        assertTrue(missed <= coasts / 100, missed + " of " + coasts + " maps with a coast have no ruins on it");
    }

    @Test
    void ruinsOnLandMapsCanAllBeReachedFromEveryHeadquarters() {
        for (Generated generated : sweep(MapType.CONTINENTS, MapType.BIG_CONTINENT, MapType.HIGHLANDS)) {
            for (Point base : generated.bases()) {
                Set<Point> land = landReachableFrom(generated.map(), base);
                for (Point site : generated.sites()) {
                    assertTrue(nextToAny(site, land), "ruins at " + site + " cannot be reached from " + base
                            + " on " + generated);
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
