package Units;

import java.awt.*;

public abstract class Unit {
    private final String name;
    private Point position;
    private final int resourceCost;

    public Unit (String name, Point position, int resourceCost) {
        this.name = name;
        this.position = position;
        this.resourceCost = resourceCost;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public int getResourceCost() {
        return resourceCost;
    }
    public String getUnitName() {
        return name;
    }
}
