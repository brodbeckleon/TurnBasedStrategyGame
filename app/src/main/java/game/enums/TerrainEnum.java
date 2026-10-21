package game.enums;

public enum TerrainEnum {
    PLAIN,
    WATER,
    MOUNTAIN,
    FOREST;

    /** The share of damage (in percent) a unit standing here is spared: trees and rocks give cover. */
    public int coverPercent() {
        return switch (this) {
            case FOREST -> 20;
            case MOUNTAIN -> 30;
            case PLAIN, WATER -> 0;
        };
    }
}
