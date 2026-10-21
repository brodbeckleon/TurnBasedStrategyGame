package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import game.tools.PerlinNoise;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MapGenerator {
    private final int width, height;
    private final HashMap<Point, TerrainEnum> map = new HashMap<>();
    private final PerlinNoise perlinNoise = new PerlinNoise();
    private final CircleGenerator circleGenerator;
    private final double scale = 2.0; // Adjust for more or less detail in the noise
    private static final int SITE_PAIRS = 3;
    private static final double FOREST_THRESHOLD = 0.25;
    private static final double MIN_SITE_DISTANCE_FROM_HQ = 6;
    private static final double MIN_SITE_SPACING = 4.5;
    private final Random random = new Random();

    public MapGenerator(CircleGenerator circleGenerator, int width, int height) {
        this.circleGenerator = circleGenerator;
        this.width = width;
        this.height = height;
    }

    public HashMap<Point, TerrainEnum> generate() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.put(new Point(x, y), generateTerrain(x, y));
            }
        }
        generateForests();
        generatePlainAroundBase();
        connectBases();
        return map;
    }

    /**
     * Makes sure ground units can drive from one base to the other: if water or mountains cut the map in two,
     * the fewest tiles needed to join them are turned into open ground (a land bridge or a pass).
     */
    private void connectBases() {
        Point start = new Point(0, 0);
        Point goal = new Point(width - 1, height - 1);
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

    private void generatePlainAroundBase() {

        ArrayList<Point> baseOnePlainCircle = circleGenerator.getCoordinatesInRange(new Point(0,0), 4);
        Iterator<Point> iterator = baseOnePlainCircle.iterator();
        while (iterator.hasNext()) {
            Point point = iterator.next();
            if (point.x < 0 || point.y < 0) {
                iterator.remove();
            }
        }
        // the second base sits in the opposite corner, so its clearing mirrors the first one exactly
        ArrayList<Point> baseTwoPlainCircle = circleGenerator.getCoordinatesInRange(new Point(width - 1, height - 1), 4);
        iterator = baseTwoPlainCircle.iterator();
        while (iterator.hasNext()) {
            Point point = iterator.next();
            if (point.x >= width || point.y >= height) {
                iterator.remove();
            }
        }

        for (Point p : baseOnePlainCircle) {
            map.put(p, TerrainEnum.PLAIN);
        }
        for (Point p : baseTwoPlainCircle) {
            map.put(p, TerrainEnum.PLAIN);
        }
    }

    /**
     * Picks the base ruins on a generated map: pairs mirrored through the centre so both players get the same
     * chances, away from both headquarters, on open ground with room around it. The first pair sits on the
     * coast when the map has one, so naval bases are possible.
     */
    public Set<Point> generateBaseSites(HashMap<Point, TerrainEnum> map) {
        Set<Point> sites = pickSites(map, candidateSites(map, true));
        if (sites.isEmpty()) {
            // no pair where both sides are good land: take pairs with one good side, the other is cleared below
            sites = pickSites(map, candidateSites(map, false));
        }
        sites.forEach(site -> map.put(site, TerrainEnum.PLAIN)); // ruins in a forest stand in a clearing
        for (Point site : sites) {
            connectToHq(map, site);
        }
        return sites;
    }

    /** Up to SITE_PAIRS well spaced pairs from the candidates, the first on the coast if possible. */
    private Set<Point> pickSites(HashMap<Point, TerrainEnum> map, List<Point> candidates) {
        Collections.shuffle(candidates, random);
        Set<Point> sites = new LinkedHashSet<>();
        for (Point candidate : candidates) {
            if (isCoastal(map, candidate) && isCoastal(map, mirror(candidate)) && addPair(sites, candidate)) break;
        }
        for (Point candidate : candidates) {
            if (sites.size() >= SITE_PAIRS * 2) break;
            addPair(sites, candidate);
        }
        return sites;
    }

    /** Sites on the first player's half whose pair is far enough from both headquarters. */
    private List<Point> candidateSites(HashMap<Point, TerrainEnum> map, boolean bothSidesGood) {
        List<Point> candidates = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Point point = new Point(x, y);
                Point mirror = mirror(point);
                boolean good = bothSidesGood ? isGoodSite(map, point) && isGoodSite(map, mirror)
                        : isGoodSite(map, point) || isGoodSite(map, mirror);
                if (x + y < width - 1 && good
                        && point.distance(0, 0) >= MIN_SITE_DISTANCE_FROM_HQ
                        && point.distance(width - 1, height - 1) >= MIN_SITE_DISTANCE_FROM_HQ) {
                    candidates.add(point);
                }
            }
        }
        return candidates;
    }

    /** Adds the site and its mirror image, unless that would crowd the sites already chosen; true if added. */
    private boolean addPair(Set<Point> sites, Point site) {
        Point mirror = mirror(site);
        boolean spaced = site.distance(mirror) >= MIN_SITE_SPACING && sites.stream()
                .allMatch(other -> other.distance(site) >= MIN_SITE_SPACING && other.distance(mirror) >= MIN_SITE_SPACING);
        if (spaced) {
            sites.add(site);
            sites.add(mirror);
        }
        return spaced;
    }

    private Point mirror(Point point) {
        return new Point(width - 1 - point.x, height - 1 - point.y);
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
     * island or behind mountains get the fewest tiles needed turned into open ground, as connectBases does.
     */
    private void connectToHq(HashMap<Point, TerrainEnum> map, Point site) {
        Set<Point> reachable = reachableLand(map);
        if (aroundSite(site).stream().anyMatch(reachable::contains)) return;
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
                if (!map.containsKey(next)) continue;
                int nextCost = cost.get(point) + (isLand(map.get(next)) ? 0 : 1);
                if (nextCost < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                    cost.put(next, nextCost);
                    previous.put(next, point);
                    if (isLand(map.get(next))) queue.addFirst(next); else queue.addLast(next);
                }
            }
        }
        for (Point point = end; point != null; point = previous.get(point)) {
            if (!isLand(map.get(point))) {
                map.put(point, TerrainEnum.PLAIN);
            }
        }
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
     * as units do (connectBases joins the second headquarters to it).
     */
    private Set<Point> reachableLand(HashMap<Point, TerrainEnum> map) {
        Set<Point> reached = new HashSet<>();
        Deque<Point> queue = new ArrayDeque<>();
        Point start = new Point(0, 0);
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
        double noiseValue = perlinNoise.noise(x * scale, y * scale, 0.1, 13);
        // Adjust these thresholds to change the distribution of terrains
        if (noiseValue < -0.18) return TerrainEnum.WATER;
        else if (noiseValue < 0.5) return TerrainEnum.PLAIN;
        else return TerrainEnum.MOUNTAIN;
    }
}

