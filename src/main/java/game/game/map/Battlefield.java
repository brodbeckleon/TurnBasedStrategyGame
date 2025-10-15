package main.java.game.game.map;

import main.java.game.enums.TerrainEnum;
import main.java.game.tools.CircleGenerator;

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
        else {
            generateMap();
        }

    }
    private void generateRandomMap() {
        this.geography = generator.generate();
        printMap();
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

    private void printMap() {
        for (int b = 0; b < height; b++) {
            for (int a = 0; a < width; a++) {
                TerrainEnum terrain = geography.get(new Point(a, b));
                char symbol = ' ';
                switch (terrain) {
                    case PLAIN -> symbol = '.';
                    case WATER -> symbol = '~';
                    case MOUNTAIN -> symbol = '^';
                }
                System.out.print(symbol);
            }
            System.out.println();
        }
    }
}
