package Game;

import Units.Unit;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerDeck {
    private final Base base;
    private final HashMap<Integer, Unit> units = new HashMap<>();
    private static int unitID = 0;

    public PlayerDeck(Point positionBase) {
        base = new Base(positionBase);
    }

    public void addUnit(Unit unit) {
        units.put(getUnitID(), unit);
        unitIDCounter();
    }
    public void removeUnit(int unitID) {
        units.remove(unitID);
    }

    public void moveUnit(int unitID, Point newPosition) {
        getUnit(unitID).setPosition(newPosition);
    }
    public Base getBase() {
        return base;
    }

    public HashMap<Integer, Unit> getUnits() {
        return units;
    }

    public Unit getUnit(int unitID) {
        return units.get(unitID);
    }

    public String toString() {
        String string = "";
        string += "Base: \t\t\t(" + getBase().getPosition().x + ", " + getBase().getPosition().y + ")" + "\n";
        string += "\t Health: \t" + getBase().getHealthPoints() + "/" + getBase().getMaxHealthPoints() +"\n";
        string += "Units:\n";
        for (int unitID : units.keySet()) {
            string += singleUnitToString(unitID);
        }
        return string;
    }

    public String singleUnitToString(int unitID) {
        Unit unit = getUnit(unitID);
        Point position = unit.getPosition();

        String string = "";
        string += unitID + "\t" + unit.getUnitName() + ": \t(" + position.x + ", " + position.y + ")" + "\n";
        string += "\t Health: \t" + unit.getHealthPoints() + "/" + unit.getMaxHealthPoints() + "\n";

        return string;
    }

    public boolean checkIfPositionIsAvailable(Point position, Point battlefieldSize) {
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
    private void unitIDCounter(){
        unitID++;
    }
    private int getUnitID(){
        return unitID;
    }

    public ArrayList<Integer> getUnitIDsInRange(Point midpoint, int radius){
        ArrayList<Point> coordinatesInRange = getCoordinatesInRange(midpoint, radius);
        ArrayList<Integer> targetIDs = new ArrayList<>();

        for (Point coordinate : coordinatesInRange) {
            for (int unitID : units.keySet()) {
                if (getUnit(unitID).getPosition().equals(coordinate)) {
                    targetIDs.add(unitID);
                }
            }
        }
        return targetIDs;
    }

    public ArrayList<Point> getCoordinatesInRange(Point midpoint, int radius){
        ArrayList<Point> coordinatesInRange = new ArrayList<>();
        int midX = midpoint.x;
        int midY = midpoint.y;

        for (int y = 0; y <= radius * 2; y++) {
            for (int x = 0; x <= radius * 2; x++) {
                int deltaX = radius - x;
                int deltaY = radius - y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                // Point lies outside of the circle
                if (distance - radius > 1) {
                    continue;
                }

                // Edge threshold
                if ((double) radius / distance < 0.9) {
                    continue;
                }

                coordinatesInRange.add(new Point(midX - radius + x, midY - radius + y));
            }
        }
        return coordinatesInRange;
    }

    public void setUnitsAvailable() {
        for (Unit unit : units.values()) {
            unit.setAvailability(true);
        }
    }

    public void setUnitUnavailable(int unitID) {
        getUnit(unitID).setAvailability(false);
    }
}
