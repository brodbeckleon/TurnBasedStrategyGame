package game.game;

import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.Unit;
import game.enums.UnitEnum;

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