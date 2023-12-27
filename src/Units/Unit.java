package Units;

import java.awt.*;

public abstract class Unit {
    private Point position;

    public Unit (Point position) {
        this.position = position;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }
}
