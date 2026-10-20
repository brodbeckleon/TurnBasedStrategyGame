package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import game.tools.PerlinNoise;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MapGenerator {
    private final int width, height;
    private final HashMap<Point, TerrainEnum> map = new HashMap<>();
    private final PerlinNoise perlinNoise = new PerlinNoise();
    private final CircleGenerator circleGenerator;
    private final double scale = 2.0; // Adjust for more or less detail in the noise
    private static final int SITE_PAIRS = 3;
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
        generatePlainAroundBase();
        return map;
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
        ArrayList<Point> baseTwoPlainCircle = circleGenerator.getCoordinatesInRange(new Point(width,height), 4);
        iterator = baseTwoPlainCircle.iterator();
        while (iterator.hasNext()) {
            Point point = iterator.next();
            if (point.x > width || point.y > height) {
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
        List<Point> candidates = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Point point = new Point(x, y);
                Point mirror = mirror(point);
                if (x + y < width - 1 && isGoodSite(map, point) && isGoodSite(map, mirror)
                        && point.distance(0, 0) >= MIN_SITE_DISTANCE_FROM_HQ
                        && point.distance(width - 1, height - 1) >= MIN_SITE_DISTANCE_FROM_HQ) {
                    candidates.add(point);
                }
            }
        }
        Collections.shuffle(candidates, random);

        Set<Point> sites = new LinkedHashSet<>();
        candidates.stream()
                .filter(candidate -> isCoastal(map, candidate) && isCoastal(map, mirror(candidate)))
                .findFirst()
                .ifPresent(candidate -> addPair(sites, candidate));
        for (Point candidate : candidates) {
            if (sites.size() >= SITE_PAIRS * 2) break;
            addPair(sites, candidate);
        }
        return sites;
    }

    /** Adds the site and its mirror image, unless that would crowd the sites already chosen. */
    private void addPair(Set<Point> sites, Point site) {
        Point mirror = mirror(site);
        boolean spaced = site.distance(mirror) >= MIN_SITE_SPACING && sites.stream()
                .allMatch(other -> other.distance(site) >= MIN_SITE_SPACING && other.distance(mirror) >= MIN_SITE_SPACING);
        if (spaced) {
            sites.add(site);
            sites.add(mirror);
        }
    }

    private Point mirror(Point point) {
        return new Point(width - 1 - point.x, height - 1 - point.y);
    }

    /** Open ground with at least two open neighbours, so a vehicle can get next to it. */
    private boolean isGoodSite(HashMap<Point, TerrainEnum> map, Point point) {
        if (map.get(point) != TerrainEnum.PLAIN) return false;
        int open = 0;
        for (Point neighbour : neighbours(point)) {
            if (map.get(neighbour) == TerrainEnum.PLAIN) open++;
        }
        return open >= 2;
    }

    private boolean isCoastal(HashMap<Point, TerrainEnum> map, Point point) {
        return neighbours(point).stream().anyMatch(neighbour -> map.get(neighbour) == TerrainEnum.WATER);
    }

    private static List<Point> neighbours(Point point) {
        return List.of(new Point(point.x + 1, point.y), new Point(point.x - 1, point.y),
                new Point(point.x, point.y + 1), new Point(point.x, point.y - 1));
    }

    private TerrainEnum generateTerrain(int x, int y) {
        double noiseValue = perlinNoise.noise(x * scale, y * scale, 0.1, 13);
        // Adjust these thresholds to change the distribution of terrains
        if (noiseValue < -0.18) return TerrainEnum.WATER;
        else if (noiseValue < 0.5) return TerrainEnum.PLAIN;
        else return TerrainEnum.MOUNTAIN;
    }
}

