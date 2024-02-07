package Game;

import Units.Unit;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerDeck {
    private Base base;
    private Unit unit;
    //private Map<Unit, Point> units = new HashMap<Unit, Point>();
    private Map<Integer, Unit> units = new HashMap<Integer, Unit>();

    public PlayerDeck(Point positionBase) {
        base = new Base(positionBase);
    }

    public void addUnit(int unitID, Unit unit) {
        units.put(unitID, unit);
    }
    public void removeUnit(Unit unit) {
        units.remove(unit);
    }
    public void moveUnit(int unitID, Unit unit) {
        units.put(unitID, unit);
    }
    public Base getBase() {
        return base;
    }

    public Map getUnitsMap() {
        return units;
    }

    public Unit getUnit(int unitID) {
        return units.get(unitID);
    }

    public String[] getUnits() {
        String[] unitsString = new String[units.size()];
        int i = 0;
        for (Unit unit : units.values()) {
            unitsString[i] = unit.getUnitName();
            i++;
        }
        return unitsString;
    }
    public String toString() {
        String unitsString = "";
        unitsString += "Base: \t\t\t( " + getBase().getPosition().x + ", " + getBase().getPosition().y + " )" + "\n";
        unitsString += "\t Health: \t" + getBase().getHealthPoints() + "/" + getBase().getMaxHealthPoints() +"\n";
        unitsString += "Units:\n";
        for (Unit unit : units.values()) {
            Point position = unit.getPosition();
            unitsString += unit.getUnitName() + ": \t\t( " + position.x + ", " + position.y + " )" + "\n";
            unitsString += "\t Health: \t" + unit.getHealthPoints() + "/" + unit.getMaxHealthPoints() + "\n";
        }
        return unitsString;
    }

    public boolean checkIfPoistionIsAvailable(Point position, Point battlefieldSize) {
        //if the postition is out of bounds
        if (position.x < 0 || position.x > battlefieldSize.x || position.y < 0 || position.y > battlefieldSize.y) {
            return false;
        }
        //if the position is the same as the base
        if (base.getPosition().equals(position)) {
            return false;
        }
        //if the position is the same as another unit
        for (Unit unit : units.values()) {
            if (unit.getPosition().equals(position)) {
                return false;
            }
        }
        return true;
    }
}
