package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import game.tools.PerlinNoise;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class MapGenerator {
    private final int width, height;
    private final HashMap<Point, TerrainEnum> map = new HashMap<>();
    private final PerlinNoise perlinNoise;
    private final CircleGenerator circleGenerator;
    private final MapType type;
    private final List<Point> bases;
    private final double scale = 2.0; // Adjust for more or less detail in the noise
    private static final int SITE_PAIRS = 3; // on the classic 16 x 16 board
    private static final double FOREST_THRESHOLD = 0.25;
    private static final double MIN_SITE_DISTANCE_FROM_HQ = 6;
    private static final double MIN_SITE_SPACING = 4.5;
    private static final double SEA_RIM = 3; // how far from the edge the big continent's coast reaches
    private static final double[] ROOMS = {1, 0.8, 0.6}; // a crowded board may need the ruins closer together
    private final Random random;

    /** The classic map: continents, the two headquarters in opposite corners. */
    public MapGenerator(CircleGenerator circleGenerator, int width, int height) {
        this(circleGenerator, width, height, MapType.CONTINENTS, basePositions(width, height, 2));
    }

    public MapGenerator(CircleGenerator circleGenerator, int width, int height, MapType type, List<Point> bases) {
        this(circleGenerator, width, height, type, bases, new Random());
    }

    /** The same seed always gives the same map and ruins. */
    MapGenerator(CircleGenerator circleGenerator, int width, int height, MapType type, List<Point> bases, long seed) {
        this(circleGenerator, width, height, type, bases, new Random(seed));
    }

    private MapGenerator(CircleGenerator circleGenerator, int width, int height, MapType type, List<Point> bases,
                         Random random) {
        this.circleGenerator = circleGenerator;
        this.width = width;
        this.height = height;
        this.type = type;
        this.bases = List.copyOf(bases);
        this.random = random;
        this.perlinNoise = new PerlinNoise(random.nextGaussian() * 255);
    }

    /**
     * Where the headquarters of the given number of players stand: player one top left, player two in the opposite
     * corner, then the two other corners, and a fifth player in the middle.
     */
    public static List<Point> basePositions(int width, int height, int players) {
        List<Point> corners = List.of(new Point(0, 0), new Point(width - 1, height - 1), new Point(width - 1, 0),
                new Point(0, height - 1), new Point(width / 2, height / 2));
        return corners.subList(0, players);
    }

    public HashMap<Point, TerrainEnum> generate() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.put(new Point(x, y), generateTerrain(x, y));
            }
        }
        generateForests();
        generatePlainAroundBases();
        if (type.landBridges()) {
            for (Point base : bases.subList(1, bases.size())) {
                connect(bases.get(0), base);
            }
        }
        return map;
    }

    /**
     * Makes sure ground units can drive from one point to the other: if water or mountains cut the map in two,
     * the fewest tiles needed to join them are turned into open ground (a land bridge or a pass).
     */
    private void connect(Point start, Point goal) {
        // 0-1 breadth-first search: open ground is free to cross, every other tile costs one conversion
        Map<Point, Integer> cost = new HashMap<>();
        Map<Point, Point> previous = new HashMap<>();
        Deque<Point> queue = new ArrayDeque<>();
        cost.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            for (Point next : neighbours(point)) {
                if (!map.containsKey(next)) continue;
                int step = isLand(map.get(next)) ? 0 : 1;
                int nextCost = cost.get(point) + step;
                if (nextCost < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                    cost.put(next, nextCost);
                    previous.put(next, point);
                    if (step == 0) queue.addFirst(next); else queue.addLast(next);
                }
            }
        }
        for (Point point = goal; point != null; point = previous.get(point)) {
            if (!isLand(map.get(point))) {
                map.put(point, TerrainEnum.PLAIN);
            }
        }
    }

    /** Open ground around every headquarters, the same size for everyone. */
    private void generatePlainAroundBases() {
        for (Point base : bases) {
            for (Point point : circleGenerator.getCoordinatesInRange(base, type.homeRadius())) {
                if (map.containsKey(point)) {
                    map.put(point, TerrainEnum.PLAIN);
                }
            }
        }
    }

    /**
     * Picks the base ruins on a generated map: groups mirrored through the centre (and, with more than two
     * players, across both axes) so everyone gets the same chances, away from every headquarters, on open ground
     * with room around it. The first group sits on the coast when the map has one, so naval bases are possible.
     * If a group has to use water or mountains for some images, those get the land their mirror images have around
     * them. On island maps every headquarters also gets ruins on its own shore, so it can build a harbour.
     */
    public Set<Point> generateBaseSites(HashMap<Point, TerrainEnum> map) {
        // on island maps, the land each headquarters can drive to
        List<Set<Point>> homeIslands = type.landBridges() ? List.of()
                : bases.stream().map(base -> reachableLand(map, base)).toList();
        Set<Point> sites = new LinkedHashSet<>();
        // a crowded board (small, many players) may need the ruins closer to the headquarters and to each other
        for (double room : ROOMS) {
            List<Point> coast = coastalGroup(map, room, homeIslands);
            sites = pickSites(map, coast, candidateSites(map, true, room), room, homeIslands);
            if (sites.isEmpty()) {
                // no group where every image is good land: take the groups with the most good images, levelled below
                sites = pickSites(map, coast, candidateSites(map, false, room), room, homeIslands);
            }
            if (!sites.isEmpty()) break;
        }
        for (Point site : List.copyOf(sites)) {
            levelGroup(map, site);
        }
        if (!type.landBridges()) {
            for (Point base : bases) {
                homeShore(map, base, sites).ifPresent(sites::add);
            }
        }
        sites.forEach(site -> map.put(site, TerrainEnum.PLAIN)); // ruins in a forest stand in a clearing
        if (type.landBridges()) {
            // the bridges keep clear of the water next to the ruins, so ruins on the coast can still get a harbour
            Set<Point> shore = new HashSet<>();
            for (Point site : sites) {
                neighbours(site).stream().filter(next -> map.get(next) == TerrainEnum.WATER).forEach(shore::add);
            }
            for (Point site : sites) {
                connectToHq(map, site, shore);
            }
        }
        return sites;
    }

    /** The coastal group, then up to the map's number of well spaced groups from the candidates. */
    private Set<Point> pickSites(HashMap<Point, TerrainEnum> map, List<Point> coast, List<Point> candidates,
                                 double room, List<Set<Point>> homeIslands) {
        int wanted = siteCount();
        Set<Point> sites = new LinkedHashSet<>(coast);
        for (Point candidate : shuffled(map, candidates, homeIslands)) {
            if (sites.size() >= wanted) break;
            addGroup(sites, candidate, room);
        }
        return sites;
    }

    /**
     * The first group of ruins, on the coast so naval bases are possible: as far from the headquarters as the
     * other ruins, but if need be closer to each other than they are. Every image on the shore if the board allows
     * that, else as many as it can; groups where every image is good land and with more room go first. Empty if no
     * group can touch the sea.
     */
    private List<Point> coastalGroup(HashMap<Point, TerrainEnum> map, double room, List<Set<Point>> homeIslands) {
        List<List<Point>> candidates = new ArrayList<>();
        for (boolean allSidesGood : new boolean[]{true, false}) {
            candidates.add(shuffled(map, candidateSites(map, allSidesGood, room), homeIslands));
        }
        for (long coastal = groupSize(); coastal >= 1; coastal--) {
            for (List<Point> pool : candidates) {
                for (double spacing : ROOMS) {
                    if (spacing > room) continue;
                    for (Point candidate : pool) {
                        Set<Point> group = new LinkedHashSet<>();
                        if (coastalImages(map, candidate) >= coastal && addGroup(group, candidate, spacing)) {
                            return List.copyOf(group);
                        }
                    }
                }
            }
        }
        return List.of();
    }

    /**
     * The candidates in random order, then those with the most good images first and, on island maps, those that
     * put every player's image on its own island or none of them (so nobody needs a ship where another does not).
     */
    private List<Point> shuffled(HashMap<Point, TerrainEnum> map, List<Point> candidates,
                                 List<Set<Point>> homeIslands) {
        Collections.shuffle(candidates, random);
        candidates.sort(Comparator.comparingLong((Point candidate) -> -goodImages(map, candidate))
                .thenComparing(candidate -> !evenlyPlaced(candidate, homeIslands)));
        return candidates;
    }

    /** True if every player's image of the group is next to that player's home island, or none is. */
    private boolean evenlyPlaced(Point site, List<Set<Point>> homeIslands) {
        List<Point> images = mirrors(site);
        Set<Boolean> atHome = new HashSet<>();
        for (int i = 0; i < Math.min(images.size(), homeIslands.size()); i++) {
            atHome.add(aroundSite(images.get(i)).stream().anyMatch(homeIslands.get(i)::contains));
        }
        return atHome.size() <= 1;
    }

    /** How many images of the group border water, once levelGroup has raised the land around them. */
    private long coastalImages(HashMap<Point, TerrainEnum> map, Point site) {
        Set<Point> raised = raisedByLevelling(map, site);
        return images(site).stream()
                .filter(image -> neighbours(image).stream()
                        .anyMatch(next -> map.get(next) == TerrainEnum.WATER && !raised.contains(next)))
                .count();
    }

    private long goodImages(HashMap<Point, TerrainEnum> map, Point site) {
        return images(site).stream().filter(image -> isGoodSite(map, image)).count();
    }

    /**
     * Gives the images of a group that are not good land the land their mirror images have around them, so no ruin
     * is left alone in the sea or among the mountains while its mirror image has room around it.
     */
    private void levelGroup(HashMap<Point, TerrainEnum> map, Point site) {
        raisedByLevelling(map, site).forEach(tile -> map.put(tile, TerrainEnum.PLAIN));
    }

    /**
     * The tiles levelGroup turns into open ground: on and around every image that is not good land, wherever a
     * good image has land on the mirrored tile. Nothing if every image is good land.
     */
    private Set<Point> raisedByLevelling(HashMap<Point, TerrainEnum> map, Point site) {
        List<Boolean> good = mirrors(site).stream().map(image -> isGoodSite(map, image)).toList();
        Set<Point> raised = new HashSet<>();
        if (!good.contains(false)) return raised;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                List<Point> tiles = mirrors(new Point(site.x + dx, site.y + dy)); // tile i lies next to image i
                if (!tiles.stream().allMatch(map::containsKey)) continue;
                boolean land = false;
                for (int i = 0; i < tiles.size(); i++) {
                    land |= good.get(i) && isLand(map.get(tiles.get(i)));
                }
                for (int i = 0; i < tiles.size() && land; i++) {
                    if (!good.get(i) && !isLand(map.get(tiles.get(i)))) raised.add(tiles.get(i));
                }
            }
        }
        return raised;
    }

    /** How many ruins the map gets: six on the classic board, more on bigger ones. */
    private int siteCount() {
        return Math.max(4, (int) Math.round(SITE_PAIRS * 2 * width * height / 256.0));
    }

    /**
     * Sites in the first player's share of the map whose group is far enough from every headquarters; {@code room}
     * scales the distance down on a crowded board.
     */
    private List<Point> candidateSites(HashMap<Point, TerrainEnum> map, boolean allSidesGood, double room) {
        List<Point> candidates = new ArrayList<>();
        double minDistance = room * Math.min(MIN_SITE_DISTANCE_FROM_HQ, Math.max(width, height) * 0.375);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Point point = new Point(x, y);
                boolean inShare = bases.size() == 2 ? x + y < width - 1 : x <= (width - 1) / 2 && y <= (height - 1) / 2;
                if (!inShare) continue;
                List<Point> group = images(point);
                boolean good = allSidesGood ? group.stream().allMatch(image -> isGoodSite(map, image))
                        : group.stream().anyMatch(image -> isGoodSite(map, image));
                boolean away = group.stream().allMatch(image -> bases.stream()
                        .allMatch(base -> image.distance(base) >= minDistance));
                if (good && away) {
                    candidates.add(point);
                }
            }
        }
        return candidates;
    }

    /** The site and its mirror images: through the centre, and across both axes with more than two players. */
    private List<Point> images(Point point) {
        return List.copyOf(new LinkedHashSet<>(mirrors(point)));
    }

    /** The point and its mirror images, always in the same order (a point on an axis is its own image). */
    private List<Point> mirrors(Point point) {
        List<Point> mirrors = new ArrayList<>();
        mirrors.add(point);
        mirrors.add(new Point(width - 1 - point.x, height - 1 - point.y));
        if (bases.size() > 2) {
            mirrors.add(new Point(width - 1 - point.x, point.y));
            mirrors.add(new Point(point.x, height - 1 - point.y));
        }
        return mirrors;
    }

    /** How many ruins a group has: two with two players, four with more. */
    private int groupSize() {
        return bases.size() > 2 ? 4 : 2;
    }

    /** Adds the site and its mirror images, unless that would crowd the sites already chosen; true if added. */
    private boolean addGroup(Set<Point> sites, Point site, double room) {
        double spacing = room * MIN_SITE_SPACING * Math.min(1, Math.max(width, height) / 16.0);
        List<Point> group = images(site);
        boolean spaced = group.size() == groupSize();
        for (int i = 0; i < group.size() && spaced; i++) {
            for (int j = i + 1; j < group.size(); j++) {
                spaced &= group.get(i).distance(group.get(j)) >= spacing;
            }
        }
        spaced &= sites.stream().allMatch(other -> group.stream().allMatch(image -> other.distance(image) >= spacing));
        if (spaced) {
            sites.addAll(group);
        }
        return spaced;
    }

    /**
     * Ruins on the headquarters' own island, so it can always build a harbour: land at least two tiles from it,
     * on the shore whenever the island has one, as close as possible (open ground before a forest clearing, then
     * towards the middle of the map on a tie).
     */
    private Optional<Point> homeShore(HashMap<Point, TerrainEnum> map, Point base, Set<Point> taken) {
        Point centre = new Point(width / 2, height / 2);
        Set<Point> island = reachableLand(map, base);
        return island.stream()
                .filter(point -> isLand(map.get(point)) && point.distance(base) >= 2)
                .filter(point -> !bases.contains(point) && bases.stream().allMatch(other -> other.distance(point) >= 2))
                .filter(point -> taken.stream().allMatch(other -> other.distance(point) >= 2))
                .min(Comparator.comparing((Point point) -> !isCoastal(map, point))
                        .thenComparingDouble(point -> point.distance(base)
                                + (map.get(point) == TerrainEnum.FOREST ? 1 : 0))
                        .thenComparingDouble(point -> point.distance(centre)));
    }

    /** Land with at least two land neighbours, so a vehicle can get next to it. */
    private boolean isGoodSite(HashMap<Point, TerrainEnum> map, Point point) {
        if (!isLand(map.get(point))) return false;
        int open = 0;
        for (Point neighbour : neighbours(point)) {
            if (isLand(map.get(neighbour))) open++;
        }
        return open >= 2;
    }

    /**
     * Makes sure a recovery vehicle can drive from the headquarters to a tile next to the ruins: ruins on an
     * island or behind mountains get the fewest tiles needed turned into open ground, as connect does. The bridge
     * goes around the shore tiles if it can.
     */
    private void connectToHq(HashMap<Point, TerrainEnum> map, Point site, Set<Point> shore) {
        Set<Point> reachable = reachableLand(map);
        if (aroundSite(site).stream().anyMatch(reachable::contains)) return;
        List<Point> bridge = bridge(map, site, reachable, shore);
        if (bridge.isEmpty()) {
            bridge = bridge(map, site, reachable, Set.of());
        }
        for (Point point : bridge) {
            if (!isLand(map.get(point))) {
                map.put(point, TerrainEnum.PLAIN);
            }
        }
    }

    /** The cheapest way from the ruins to the reachable land without crossing the tiles to avoid; empty if none. */
    private List<Point> bridge(HashMap<Point, TerrainEnum> map, Point site, Set<Point> reachable, Set<Point> avoid) {
        // 0-1 breadth-first search from the ruins to the land the headquarters reaches
        Map<Point, Integer> cost = new HashMap<>();
        Map<Point, Point> previous = new HashMap<>();
        Deque<Point> queue = new ArrayDeque<>();
        cost.put(site, 0);
        queue.add(site);
        Point end = null;
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            if (reachable.contains(point)) {
                end = point;
                break;
            }
            for (Point next : neighbours(point)) {
                if (!map.containsKey(next) || avoid.contains(next)) continue;
                int nextCost = cost.get(point) + (isLand(map.get(next)) ? 0 : 1);
                if (nextCost < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                    cost.put(next, nextCost);
                    previous.put(next, point);
                    if (isLand(map.get(next))) queue.addFirst(next); else queue.addLast(next);
                }
            }
        }
        List<Point> bridge = new ArrayList<>();
        for (Point point = end; point != null; point = previous.get(point)) {
            bridge.add(point);
        }
        return bridge;
    }

    private static List<Point> aroundSite(Point site) {
        List<Point> around = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) around.add(new Point(site.x + dx, site.y + dy));
            }
        }
        return around;
    }

    /**
     * The land a vehicle can drive to from the first headquarters, stepping to any of the eight tiles around it
     * as units do (connect joins the other headquarters to it).
     */
    private Set<Point> reachableLand(HashMap<Point, TerrainEnum> map) {
        return reachableLand(map, bases.get(0));
    }

    /** The land a vehicle can drive to from the start. */
    private static Set<Point> reachableLand(HashMap<Point, TerrainEnum> map, Point start) {
        Set<Point> reached = new HashSet<>();
        Deque<Point> queue = new ArrayDeque<>();
        reached.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Point next = new Point(point.x + dx, point.y + dy);
                    if (isLand(map.get(next)) && reached.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return reached;
    }

    /** Ground that vehicles can drive over. */
    private static boolean isLand(TerrainEnum terrain) {
        return terrain == TerrainEnum.PLAIN || terrain == TerrainEnum.FOREST;
    }

    private boolean isCoastal(HashMap<Point, TerrainEnum> map, Point point) {
        return neighbours(point).stream().anyMatch(neighbour -> map.get(neighbour) == TerrainEnum.WATER);
    }

    private static List<Point> neighbours(Point point) {
        return List.of(new Point(point.x + 1, point.y), new Point(point.x - 1, point.y),
                new Point(point.x, point.y + 1), new Point(point.x, point.y - 1));
    }

    /** Patches of forest on open ground, from a second, independent noise field. */
    private void generateForests() {
        double offset = 37.5 + random.nextInt(100);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Point point = new Point(x, y);
                double noise = perlinNoise.noise((x + offset) * scale, (y + offset) * scale, 0.1, 13);
                if (map.get(point) == TerrainEnum.PLAIN && noise > FOREST_THRESHOLD) {
                    map.put(point, TerrainEnum.FOREST);
                }
            }
        }
    }

    private TerrainEnum generateTerrain(int x, int y) {
        double noiseValue = perlinNoise.noise(x * type.scale(), y * type.scale(), 0.1, 13);
        if (type == MapType.BIG_CONTINENT) {
            // the sea rises towards the edges of the board, so the land forms one continent
            double edge = Math.min(Math.min(x, width - 1 - x), Math.min(y, height - 1 - y));
            noiseValue -= 1.6 * Math.max(0, 1 - edge / SEA_RIM);
        }
        if (noiseValue < type.waterBelow()) return TerrainEnum.WATER;
        else if (noiseValue < type.mountainAbove()) return TerrainEnum.PLAIN;
        else return TerrainEnum.MOUNTAIN;
    }
}
