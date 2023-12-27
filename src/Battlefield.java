import Units.Unit;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Battlefield {
    private Base base1;
    private Base base2;
    private final int width;
    private final int height;
    private Map<Unit, Point> units = new HashMap<Unit, Point>();

    public Battlefield(int width, int height, Base base1, Base base2) {
        this.width = width;
        this.height = height;
        this.base1 = base1;
        this.base2 = base2;
    }

    public void addUnit(Unit unit, Point position) {
        units.put(unit, position);
    }
    public void removeUnit(Unit unit) {
        units.remove(unit);
    }
    public void moveUnit(Unit unit, Point position) {
        units.put(unit, position);
    }

}
