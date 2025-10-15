package main.java.game.game;

import main.java.game.units.assaultClasses.AssaultTanks.TypeNinety;
import main.java.game.units.assaultClasses.AssaultTanks.TypeTen;
import main.java.game.units.Unit;
import main.java.game.enums.UnitEnum;

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