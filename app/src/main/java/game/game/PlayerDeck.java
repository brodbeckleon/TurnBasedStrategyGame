package game.game;

import game.tools.CircleGenerator;
import game.units.Unit;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerDeck {
    private final int playerID; // Store player ID for convenience
    private final int baseID;   // Store the base's ID for easy retrieval
    private final HashMap<Integer, Unit> units = new HashMap<>();
    private final CircleGenerator circleGenerator;
    private static int nextUnitID = 3; // Start unit IDs after bases

    public PlayerDeck(CircleGenerator circleGenerator, int playerID, int baseID, Point positionBase) {
        this.circleGenerator = circleGenerator;
        this.playerID = playerID;
        this.baseID = baseID;

        // The Base is now just another Unit in the map, with a special ID
        Base base = new Base(playerID, baseID, positionBase);
        units.put(baseID, base);
    }

    public void addUnit(Unit unit) {
        units.put(nextUnitID, unit);
        nextUnitID++;
    }

    public void removeUnit(int unitID) {
        units.remove(unitID);
    }

    public void moveUnit(int unitID, Point newPosition) {
        getUnit(unitID).setPosition(newPosition);
    }

    // Now returns the Base object by retrieving it from the units map
    public Base getBase() {
        return (Base) units.get(this.baseID);
    }

    public Unit getUnit(int unitID) {
        return units.get(unitID);
    }

    // Simplified: No longer needs a special check for the base's position
    public boolean checkIfPositionIsAvailable(Point position, Point battlefieldSize) {
        if (position.x < 0 || position.x >= battlefieldSize.x || position.y < 0 || position.y >= battlefieldSize.y) {
            return false;
        }
        for (Unit unit : units.values()) {
            if (unit.getPosition().equals(position)) {
                return false;
            }
        }
        return true;
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
            // The base's isAvailable() is overridden to always be false, so this is safe
            unit.setIsAvailable(true);
        }
    }

    public void setUnitUnavailable(int unitID) {
        Unit unit = getUnit(unitID);
        if (unit != null) {
            unit.setIsAvailable(false);
        }
    }

    // --- Other methods (getters, etc.) remain mostly the same ---
    public HashMap<Integer, Unit> getUnits() { return units; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBase().toString()).append("\n");
        sb.append("Units:\n");
        for (Integer id : units.keySet()) {
            if (id != baseID) { // Don't print the base twice
                sb.append(singleUnitToString(id)).append("\n");
            }
        }
        return sb.toString();
    }

    public String singleUnitToString(int unitID) {
        Unit unit = getUnit(unitID);
        return String.format("%d\t%s: \t(%d, %d)\n\t Health: \t%d/%d",
                unitID, unit.getUnitName(), unit.getPosition().x, unit.getPosition().y, unit.getHealthPoints(), unit.getMaxHealthPoints());
    }
}