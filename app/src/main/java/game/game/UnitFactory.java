package game.game;

import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeSixteen;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.Unit;
import game.enums.UnitEnum;
import game.units.assaultClasses.aircraft.ATen;
import game.units.assaultClasses.aircraft.FFifteen;
import game.units.assaultClasses.aircraft.FFourEJKai;
import game.units.assaultClasses.aircraft.FThirtyFive;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightyOneC;
import game.units.assaultClasses.antiAir.TypeEightySeven;
import game.units.assaultClasses.antiAir.TypeNinetyThree;
import game.units.assaultClasses.artillery.TypeNinetyNineSPH;
import game.units.assaultClasses.infantryFightingVehicles.TypeEightyNine;
import game.units.assaultClasses.helicopters.AHSixtyFourD;
import game.units.assaultClasses.infantry.AntiTankTeam;
import game.units.assaultClasses.infantry.RifleSquad;
import game.units.assaultClasses.ships.Kongo;
import game.units.assaultClasses.ships.Soryu;
import game.units.assaultClasses.ships.Yamato;
import game.units.infrastructures.AirForceBase;
import game.units.infrastructures.ArmyBase;
import game.units.infrastructures.NavyBase;
import game.units.infrastructures.Radarstation;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredReconaissanceVehicle;
import game.units.supportClasses.ArmoredRecoveryVehicle;

import java.awt.*;


public class UnitFactory {
    public Unit createUnit(int playerID, String unitName, Point position) {
        return createUnit(playerID, UnitEnum.valueOf(unitName), position);
    }

    public Unit createUnit(int playerID, UnitEnum type, Point position) {
        return switch (type) {
            case TYPETEN -> new TypeTen(playerID, position);
            case TYPENINETY -> new TypeNinety(playerID, position);
            case TYPESIXTEEN -> new TypeSixteen(playerID, position);
            case TYPEEIGHTYNINE -> new TypeEightyNine(playerID, position);
            case TYPENINETYNINESPH -> new TypeNinetyNineSPH(playerID, position);
            case TYPEEIGHTYSEVEN -> new TypeEightySeven(playerID, position);
            case TYPEEIGHTYONEC -> new TypeEightyOneC(playerID, position);
            case TYPENINETYTHREE -> new TypeNinetyThree(playerID, position);
            case ARMOREDAMBULANCE -> new ArmoredAmbulanceVehicle(playerID, position);
            case ARMOREDRECOVERYVEHICLE -> new ArmoredRecoveryVehicle(playerID, position);
            case ARMOREDRECONNAISSANCE -> new ArmoredReconaissanceVehicle(playerID, position);
            case FFOUREJKAI -> new FFourEJKai(playerID, position);
            case FFIFTEEN -> new FFifteen(playerID, position);
            case FTWO -> new FTwo(playerID, position);
            case FTHIRTYFIVE -> new FThirtyFive(playerID, position);
            case ATEN -> new ATen(playerID, position);
            case YAMATO -> new Yamato(playerID, position);
            case KONGO -> new Kongo(playerID, position);
            case SORYU -> new Soryu(playerID, position);
            case INFANTRY -> new RifleSquad(playerID, position);
            case ANTITANKTEAM -> new AntiTankTeam(playerID, position);
            case AHSIXTYFOURD -> new AHSixtyFourD(playerID, position);
            case ARMYBASE -> new ArmyBase(playerID, position);
            case AIRFORCEBASE -> new AirForceBase(playerID, position);
            case NAVYBASE -> new NavyBase(playerID, position);
            case RADARSTATION -> new Radarstation(playerID, position);
        };
    }
}
