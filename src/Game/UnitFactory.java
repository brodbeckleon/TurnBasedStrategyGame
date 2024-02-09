package Game;

import Units.*;
import Units.AssaultClasses.AssaultTanks.TypeNinety;
import Units.AssaultClasses.AssaultTanks.TypeTen;
import Units.Unit;
import Enums.UnitEnum;

import java.awt.*;


public class UnitFactory {
    public Unit createUnit(String unitName, Point position) {

        switch (UnitEnum.valueOf(unitName)) {
            case TYPENINETY:
                return new TypeNinety(position);
            case TYPETEN:
                return new TypeTen(position);
            default:
                return null;
        }
    }
}