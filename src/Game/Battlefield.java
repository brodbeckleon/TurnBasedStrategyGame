package Game;

import java.awt.*;

public class Battlefield {
    private final int width;
    private final int height;

    public Battlefield(int x, int y) {
        this.width = x;
        this.height = y;
    }

    public Point getSize() {
        return new Point(width, height);
    }
}
