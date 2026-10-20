package game.testing;

import game.enums.TerrainEnum;
import game.model.Game;
import game.units.Unit;

import java.awt.Point;

/**
 * Builds small, deterministic game situations for the model tests.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** A fresh game whose whole battlefield is plain terrain. */
    public static Game plainGame() {
        Game game = new Game();
        game.getBattlefield().getGeography().replaceAll((point, terrain) -> TerrainEnum.PLAIN);
        return game;
    }

    public static void setTerrain(Game game, Point point, TerrainEnum terrain) {
        game.getBattlefield().getGeography().put(point, terrain);
    }

    /** Puts a unit into the deck of the given player and returns the unit id the model assigned. */
    public static int place(Game game, int playerId, Unit unit) {
        var player = playerId == 1 ? game.getPlayerOne() : game.getPlayerTwo();
        player.getPlayerDeck().addUnit(unit);
        return game.getUnitID(unit);
    }
}
