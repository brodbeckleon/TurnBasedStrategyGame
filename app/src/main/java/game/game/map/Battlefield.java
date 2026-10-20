package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class Battlefield {
    private final int width;
    private final int height;
    private final boolean isRandom;
    private HashMap<Point, TerrainEnum> geography;
    private final Set<Point> baseSites = new LinkedHashSet<>();
    private final MapGenerator generator;

    public Battlefield(CircleGenerator circleGenerator, int x, int y, boolean b) {
        generator = new MapGenerator(circleGenerator, x, y);
        this.width = x;
        this.height = y;
        this.isRandom = b;

        if(isRandom) generateRandomMap();
        else generateMap();

    }
    private void generateRandomMap() {
        this.geography = generator.generate();
        baseSites.addAll(generator.generateBaseSites(geography));
    }

    private void generateMap() {
        //TODO:
    }

    public Point getSize() {
        return new Point(width, height);
    }

    public HashMap<Point, TerrainEnum> getGeography() {
        return geography;
    }

    /** The base ruins: the only places where army, air and naval bases can be built. */
    public Set<Point> getBaseSites() {
        return baseSites;
    }

    public boolean isBaseSite(Point point) {
        return baseSites.contains(point);
    }

    public TerrainEnum getGeographyOfPoint(Point point) {
        return geography.get(point);
    }
}
