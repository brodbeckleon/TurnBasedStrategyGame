package game.units.assaultClasses.antiAir;

import game.units.assaultClasses.SelfPropelledAntiAir;

import java.awt.*;

/** Twin 35 mm guns: short-ranged, but they shred infantry and light vehicles too; only tanks shrug them off. */
public class TypeEightySeven extends SelfPropelledAntiAir {
    public TypeEightySeven(int playerID, Point position) {
        super(playerID, "Type 87", position, 3, 550, 3, 3, 300);
    }
}
