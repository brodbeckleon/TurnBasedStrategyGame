package game.view;

import java.awt.Point;
import java.util.Set;

/**
 * What the board marks for the player: the selected unit, where it may move, the area it can shoot at, the
 * enemies it can hit right now, and the tiles a new unit may be deployed on.
 */
public record BoardHighlights(int selectedUnitId, Set<Point> moveTiles, Set<Point> attackRange,
                              Set<Point> targets, Set<Point> deployTiles) {

    public static final BoardHighlights NONE = new BoardHighlights(-1, Set.of(), Set.of(), Set.of(), Set.of());

    public boolean isActionTile(Point cell) {
        return moveTiles.contains(cell) || targets.contains(cell) || deployTiles.contains(cell);
    }
}
