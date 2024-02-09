package Game;

import Units.AssaultClasses.GroundForces.Vehicles.AssaultTanks.TypeNinety;
import Units.AssaultClasses.GroundForces.Vehicles.AssaultTanks.TypeTen;
import Units.Unit;
import java.awt.*;


public class UnitFactory {
    public Unit createUnit(String unitName, Point position) {
        switch (unitName.toLowerCase()) {
            case "type 10":
                return new TypeTen(position);
            case "type 90":
                return new TypeNinety(position);
            default:
                return null;
        }
    }
}