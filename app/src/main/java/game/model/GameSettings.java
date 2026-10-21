package game.model;

import game.game.map.MapSize;
import game.game.map.MapType;

/** What the main menu sets up: the kind of map, its size and how many bots play (free-for-all). */
public record GameSettings(MapType mapType, MapSize mapSize, int enemies) {
    public static final int MAX_ENEMIES = 4;

    public GameSettings {
        if (enemies < 1 || enemies > MAX_ENEMIES) {
            throw new IllegalArgumentException("1 to " + MAX_ENEMIES + " enemies, not " + enemies);
        }
    }

    /** The classic game: continents on the medium board against one bot. */
    public static GameSettings standard() {
        return new GameSettings(MapType.CONTINENTS, MapSize.MEDIUM, 1);
    }

    public int players() {
        return enemies + 1;
    }
}
