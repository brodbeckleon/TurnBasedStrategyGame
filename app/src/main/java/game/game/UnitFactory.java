package game.game;

import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.Unit;
import game.enums.UnitEnum;
import game.units.assaultClasses.ships.Yamato;

import java.awt.*;


public class UnitFactory {
    public Unit createUnit(int playerID, String unitName, Point position) {

        switch (UnitEnum.valueOf(unitName)) {
            case TYPENINETY:
                return new TypeNinety(playerID, position);
            case TYPETEN:
                return new TypeTen(playerID, position);
            case YAMATO:
                return new Yamato(playerID, position);
            default:
                return null;
        }
    }
}