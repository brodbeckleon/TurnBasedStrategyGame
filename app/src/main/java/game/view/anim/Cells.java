package game.view.anim;

import javafx.geometry.Point2D;

import java.awt.Point;

/**
 * Board geometry: where a grid cell lies in the pixel coordinates of the board and the effects layer.
 */
public final class Cells {

    public static final int SIZE = 32;

    private Cells() {
    }

    public static Point2D origin(Point cell) {
        return new Point2D(cell.x * SIZE, cell.y * SIZE);
    }

    public static Point2D center(Point cell) {
        return new Point2D(cell.x * SIZE + SIZE / 2.0, cell.y * SIZE + SIZE / 2.0);
    }
}
