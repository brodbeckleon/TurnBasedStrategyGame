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

    /** A fresh game whose whole battlefield is plain terrain, without base ruins or fog of war. */
    public static Game plainGame() {
        Game game = new Game();
        game.getBattlefield().getGeography().replaceAll((point, terrain) -> TerrainEnum.PLAIN);
        game.getBattlefield().getBaseSites().clear();
        game.setFogOfWar(false); // tests of other rules see everything; FogOfWarTest switches it on
        return game;
    }

    /** Puts base ruins on the battlefield. */
    public static void addBaseSite(Game game, Point point) {
        game.getBattlefield().getBaseSites().add(point);
    }

    public static void setTerrain(Game game, Point point, TerrainEnum terrain) {
        game.getBattlefield().getGeography().put(point, terrain);
    }

    /** Puts a unit into the deck of the given player and returns the unit id the model assigned. */
    public static int place(Game game, int playerId, Unit unit) {
        var player = game.getPlayer(playerId);
        player.getPlayerDeck().addUnit(unit);
        return game.getUnitID(unit);
    }
}
