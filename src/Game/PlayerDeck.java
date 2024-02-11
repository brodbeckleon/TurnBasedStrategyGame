package Game;

import Tools.CircleGenerator;
import Units.Unit;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerDeck {
    private final Base base;
    private final HashMap<Integer, Unit> units = new HashMap<>();
    private final CircleGenerator circleGenerator;
    private static int unitID = 3;

    public PlayerDeck(CircleGenerator circleGenerator, int baseID, Point positionBase) {
        this.circleGenerator = circleGenerator;
        base = new Base(baseID, positionBase);
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
        String string = getBase().toString();

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
    private void unitIDCounter() {
        unitID++;
    }
    public void setUnitID(int unitID) {
        PlayerDeck.unitID = unitID;
    }
    private int getUnitID() {
        return unitID;
    }

    public ArrayList<Integer> getUnitIDsInRange(Point midpoint, int radius){
        ArrayList<Point> coordinatesInRange = circleGenerator.getCoordinatesInRange(midpoint, radius);
        ArrayList<Integer> targetIDs = new ArrayList<>();


        for (Point coordinate : coordinatesInRange) {
            //checks if Base is in Range
            if (getBase().getPosition().equals(coordinate)) {
                targetIDs.add(getBase().getBaseID());
            }
            //checks if a Unit is in Range
            for (int unitID : units.keySet()) {
                if (getUnit(unitID).getPosition().equals(coordinate)) {
                    targetIDs.add(unitID);
                }
            }
        }
        return targetIDs;
    }

    public void setUnitsAvailable() {
        for (Unit unit : units.values()) {
            unit.setAvailability(true);
        }
    }

    public void setUnitUnavailable(int unitID) {
        getUnit(unitID).setAvailability(false);
    }

    public CircleGenerator getCircleGenerator() {
        return circleGenerator;
    }
}
