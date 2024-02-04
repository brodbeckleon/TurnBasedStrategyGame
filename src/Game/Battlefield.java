package Game;

import java.awt.*;

public class Battlefield {
    private final int width;
    private final int height;

    public Battlefield(Point size) {
        this.width = size.x;
        this.height = size.y;
    }

    public Battlefield () {
        this.width = 64;
        this.height = 64;
    }
    public Point getSize() {
        return new Point(width, height);
    }
}
