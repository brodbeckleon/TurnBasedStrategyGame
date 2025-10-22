package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;

import java.awt.*;
import java.util.HashMap;

public class Battlefield {
    private final int width;
    private final int height;
    private final boolean isRandom;
    private HashMap<Point, TerrainEnum> geography;
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

    public TerrainEnum getGeographyOfPoint(Point point) {
        return geography.get(point);
    }
}
