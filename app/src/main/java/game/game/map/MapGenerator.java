package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import game.tools.PerlinNoise;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MapGenerator {
    private final int width, height;
    private final HashMap<Point, TerrainEnum> map = new HashMap<>();
    private final PerlinNoise perlinNoise = new PerlinNoise();
    private final CircleGenerator circleGenerator;
    private final double scale = 2.0; // Adjust for more or less detail in the noise

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

    private TerrainEnum generateTerrain(int x, int y) {
        double noiseValue = perlinNoise.noise(x * scale, y * scale, 0.1, 13);
        // Adjust these thresholds to change the distribution of terrains
        if (noiseValue < -0.18) return TerrainEnum.WATER;
        else if (noiseValue < 0.5) return TerrainEnum.PLAIN;
        else return TerrainEnum.MOUNTAIN;
    }
}

