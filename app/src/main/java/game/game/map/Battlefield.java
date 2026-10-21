package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Battlefield {
    private final int width;
    private final int height;
    private final boolean isRandom;
    private HashMap<Point, TerrainEnum> geography;
    private final Set<Point> baseSites = new LinkedHashSet<>();
    private final MapGenerator generator;
    private final MapType type;
    private final List<Point> basePositions;

    public Battlefield(CircleGenerator circleGenerator, int x, int y, boolean b) {
        this(circleGenerator, x, y, MapType.CONTINENTS, 2);
    }

    /** A random map of the given type with room for the given number of headquarters. */
    public Battlefield(CircleGenerator circleGenerator, int x, int y, MapType type, int players) {
        this.width = x;
        this.height = y;
        this.isRandom = true;
        this.type = type;
        this.basePositions = MapGenerator.basePositions(x, y, players);
        generator = new MapGenerator(circleGenerator, x, y, type, basePositions);

        if(isRandom) generateRandomMap();
        else generateMap();

    }

    public MapType getType() {
        return type;
    }

    /** Where each player's headquarters stands, player one first. */
    public List<Point> getBasePositions() {
        return basePositions;
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
