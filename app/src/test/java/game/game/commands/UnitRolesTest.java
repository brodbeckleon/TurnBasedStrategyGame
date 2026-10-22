package game.game.commands;

import game.enums.TerrainEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitDamaged;
import game.game.Player;
import game.game.Research;
import game.game.Vision;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeSixteen;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.ATen;
import game.units.assaultClasses.aircraft.FFifteen;
import game.units.assaultClasses.aircraft.FFourEJKai;
import game.units.assaultClasses.aircraft.FThirtyFive;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightyOneC;
import game.units.assaultClasses.antiAir.TypeEightySeven;
import game.units.assaultClasses.antiAir.TypeNinetyThree;
import game.units.assaultClasses.artillery.TypeNinetyNineSPH;
import game.units.assaultClasses.infantry.AntiTankTeam;
import game.units.assaultClasses.infantry.RifleSquad;
import game.units.assaultClasses.infantryFightingVehicles.TypeEightyNine;
import game.units.infrastructures.Radarstation;
import game.units.assaultClasses.ships.Kongo;
import game.units.assaultClasses.ships.Soryu;
import game.units.assaultClasses.ships.Yamato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Every aircraft, anti-air vehicle, tank, the IFV and the howitzer have a rule of their own. */
class UnitRolesTest {

    private Game game;
    private Player player;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        attack = new AttackCommand(game);
    }

    /** The damage the unit took in the attack's events. */
    private static int damageTaken(List<GameEvent> events, int unitId) {
        return events.stream().filter(e -> e instanceof UnitDamaged d && d.unitId() == unitId)
                .mapToInt(e -> ((UnitDamaged) e).damage()).sum();
    }

    // --- Aircraft ---

    @Test
    void theF15HuntsAircraftButBarelyHurtsTheGround() {
        FFifteen fighter = new FFifteen(1, new Point(5, 5));
        assertEquals(2 * fighter.getDamage(), attack.predictDamage(fighter, new FTwo(2, new Point(7, 5))));
        assertEquals(fighter.getDamage() / 2, attack.predictDamage(fighter, new TypeEightyNine(2, new Point(7, 5))));
    }

    @Test
    void theF15CoversItsSideFromAirAttacks() {
        int jet = Fixtures.place(game, 1, new FTwo(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeEightyNine(2, new Point(7, 5)));
        int fighter = Fixtures.place(game, 2, new FFifteen(2, new Point(7, 7)));

        assertEquals(List.of(fighter), attack.findInterceptors(game.getPlayerTwo(), game.findUnitById(jet), target));
    }

    @Test
    void theAegisUpgradeIsForAntiAirVehiclesNotTheF15() {
        FFifteen fighter = new FFifteen(2, new Point(5, 5));
        TypeEightySeven guns = new TypeEightySeven(2, new Point(9, 9));
        Fixtures.place(game, 2, fighter);
        Fixtures.place(game, 2, guns);
        game.getPlayerTwo().completeResearch(Research.AEGIS_UPGRADE);

        assertEquals(fighter.getShootingRange(), game.getShootingRange(fighter));
        assertEquals(guns.getShootingRange() + 1, game.getShootingRange(guns));
    }

    @Test
    void theF2HitsShipsHardAndTheShipCannotAnswerItsMissiles() {
        FTwo strike = new FTwo(1, new Point(5, 5));
        int jet = Fixtures.place(game, 1, strike);
        Yamato battleship = new Yamato(2, new Point(8, 5));
        int target = Fixtures.place(game, 2, battleship);

        assertEquals(2 * strike.getDamage(), attack.predictDamage(strike, battleship));
        assertEquals(0, attack.preview(player, jet, target).counterDamage());
        List<GameEvent> events = attack.execute(player, jet, target).getEvents();

        assertEquals(2 * strike.getDamage(), damageTaken(events, target));
        assertEquals(strike.getMaxHealthPoints(), strike.getHealthPoints(), "the battleship's guns stayed silent");
    }

    @Test
    void anEscortingDestroyerStillFiresAtTheF2() {
        FTwo strike = new FTwo(1, new Point(5, 5));
        int jet = Fixtures.place(game, 1, strike);
        int target = Fixtures.place(game, 2, new Yamato(2, new Point(8, 5)));
        int escort = Fixtures.place(game, 2, new Kongo(2, new Point(8, 7)));

        AttackPreview preview = attack.preview(player, jet, target);
        List<GameEvent> events = attack.execute(player, jet, target).getEvents();

        assertTrue(preview.antiAirDamage() > 0);
        assertEquals(preview.antiAirDamage(), damageTaken(events, jet));
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.UnitAttacked a && a.attackerId() == escort));
    }

    @Test
    void theF4SeesFarAndSpotsForTheUnitsBelowIt() {
        FFourEJKai recon = new FFourEJKai(1, new Point(5, 6));
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, recon);
        Fixtures.place(game, 1, tank);

        assertEquals(6, Vision.range(recon));
        assertEquals(tank.getShootingRange() + 1, game.getShootingRange(tank));
    }

    @Test
    void theA10GoesThroughTankArmourAndShrugsOffHalfTheFlak() {
        ATen hog = new ATen(1, new Point(5, 5));
        TypeEightySeven guns = new TypeEightySeven(2, new Point(7, 5));

        assertEquals(hog.getDamage(), attack.predictDamage(hog, new TypeNinety(2, new Point(7, 5))));
        assertEquals(guns.getDamage(), attack.predictDamage(guns, hog), "double damage, halved by the armoured cockpit");
        assertEquals(2 * guns.getDamage(), attack.predictDamage(guns, new FTwo(1, new Point(5, 5))));
    }

    @Test
    void theA10CannotAttackAircraft() {
        int hog = Fixtures.place(game, 1, new ATen(1, new Point(5, 5)));
        Fixtures.place(game, 2, new FTwo(2, new Point(6, 5)));

        assertTrue(attack.findTargets(player, hog).isEmpty());
    }

    @Test
    void anF35TheEnemyCannotSeeStrikesUnanswered() {
        game.setFogOfWar(true);
        FThirtyFive stealth = new FThirtyFive(1, new Point(5, 5));
        int jet = Fixtures.place(game, 1, stealth);
        int target = Fixtures.place(game, 2, new Kongo(2, new Point(9, 5))); // anti-air that reaches 5
        Fixtures.place(game, 2, new TypeEightyOneC(2, new Point(9, 6))); // reaches 6

        assertEquals(0, attack.preview(player, jet, target).counterDamage());
        assertEquals(0, attack.preview(player, jet, target).antiAirDamage());
        attack.execute(player, jet, target);
        assertEquals(stealth.getMaxHealthPoints(), stealth.getHealthPoints());
    }

    @Test
    void anF35TheEnemySpotsDrawsFire() {
        game.setFogOfWar(true);
        FThirtyFive stealth = new FThirtyFive(1, new Point(5, 5));
        int jet = Fixtures.place(game, 1, stealth);
        int target = Fixtures.place(game, 2, new Kongo(2, new Point(9, 5)));
        Fixtures.place(game, 2, new RifleSquad(2, new Point(6, 6))); // right next to the jet: it spots it

        AttackPreview preview = attack.preview(player, jet, target);
        attack.execute(player, jet, target);

        assertTrue(preview.counterDamage() > 0);
        assertEquals(stealth.getMaxHealthPoints() - preview.counterDamage(), stealth.getHealthPoints());
    }

    // --- Anti-air ---

    @Test
    void theType87GunsHitSoftTargetsFullyButTanksOnlyHalf() {
        TypeEightySeven guns = new TypeEightySeven(1, new Point(5, 5));
        TypeNinetyThree light = new TypeNinetyThree(1, new Point(5, 5));
        TypeEightyNine ifv = new TypeEightyNine(2, new Point(7, 5));
        TypeNinety tank = new TypeNinety(2, new Point(7, 5));

        assertEquals(guns.getDamage(), attack.predictDamage(guns, ifv));
        assertEquals(guns.getDamage(), attack.predictDamage(guns, new RifleSquad(2, new Point(7, 5))));
        assertEquals(light.getDamage() / 2, attack.predictDamage(light, ifv));
        assertEquals(Math.round(guns.getDamage() / 2.0 * (100 - tank.getArmorValue()) / 100.0),
                attack.predictDamage(guns, tank));
    }

    @Test
    void theType81OnlyFiresAtAircraft() {
        int missiles = Fixtures.place(game, 1, new TypeEightyOneC(1, new Point(5, 5)));
        Fixtures.place(game, 2, new TypeTen(2, new Point(6, 5)));
        int jet = Fixtures.place(game, 2, new FTwo(2, new Point(9, 5)));

        assertEquals(List.of(jet), attack.findTargets(player, missiles));
    }

    @Test
    void aTankAttackingTheType81DrawsNoAnswer() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        int tankId = Fixtures.place(game, 1, tank);
        int target = Fixtures.place(game, 2, new TypeEightyOneC(2, new Point(7, 5)));

        attack.execute(player, tankId, target);
        assertEquals(tank.getMaxHealthPoints(), tank.getHealthPoints());
    }

    @Test
    void theType93LiesInAmbushInAForest() {
        game.setFogOfWar(true);
        Point woods = new Point(7, 5);
        Fixtures.setTerrain(game, woods, TerrainEnum.FOREST);
        TypeNinetyThree ambush = new TypeNinetyThree(2, woods);
        Fixtures.place(game, 2, ambush);
        Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5))); // sees 3 tiles

        assertFalse(game.isVisibleTo(player, ambush), "hidden under the trees");
        Fixtures.setTerrain(game, woods, TerrainEnum.PLAIN);
        assertTrue(game.isVisibleTo(player, ambush), "out in the open it shows up");
        Fixtures.setTerrain(game, woods, TerrainEnum.FOREST);
        Fixtures.place(game, 1, new RifleSquad(1, new Point(6, 5)));
        assertTrue(game.isVisibleTo(player, ambush), "a unit right next to it finds it");
    }

    // --- Tanks ---

    @Test
    void aBatteredType90StillStrikesBackAtFullStrength() {
        TypeSixteen raider = new TypeSixteen(1, new Point(5, 5));
        int raiderId = Fixtures.place(game, 1, raider);
        TypeNinety anchor = new TypeNinety(2, new Point(8, 5));
        int anchorId = Fixtures.place(game, 2, anchor);
        anchor.setHealthPoints(-anchor.getMaxHealthPoints() / 2);

        int counter = attack.preview(player, raiderId, anchorId).counterDamage();
        attack.execute(player, raiderId, anchorId);

        assertEquals(Math.min(raider.getMaxHealthPoints(), attack.predictDamage(anchor, raider)), counter);
        assertEquals(raider.getMaxHealthPoints() - counter, Math.max(0, raider.getHealthPoints()));
    }

    @Test
    void theType10SeesThroughCover() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        TypeNinety other = new TypeNinety(1, new Point(5, 5));
        Unit target = new TypeEightyNine(2, new Point(7, 5));
        int open = attack.predictDamage(tank, target);
        int openOther = attack.predictDamage(other, target);
        Fixtures.setTerrain(game, new Point(7, 5), TerrainEnum.FOREST);

        assertEquals(open, attack.predictDamage(tank, target));
        assertTrue(attack.predictDamage(other, target) < openOther, "other units' shots lose to the trees");
    }

    @Test
    void theType16RaidsLightVehiclesButNotTanksOrInfantry() {
        TypeSixteen raider = new TypeSixteen(1, new Point(5, 5));
        TypeTen tank = new TypeTen(2, new Point(7, 5));

        assertEquals(Math.round(raider.getDamage() * 1.5), attack.predictDamage(raider, new TypeEightyNine(2, new Point(7, 5))));
        assertEquals(Math.round(raider.getDamage() * (100 - tank.getArmorValue()) / 100.0), attack.predictDamage(raider, tank));
        assertEquals(raider.getDamage(), attack.predictDamage(raider, new RifleSquad(2, new Point(7, 5))));
    }

    @Test
    void theType89TearsThroughInfantry() {
        TypeEightyNine ifv = new TypeEightyNine(1, new Point(5, 5));
        RifleSquad squad = new RifleSquad(2, new Point(7, 5));

        assertEquals(Math.round(ifv.getDamage() * 1.75), attack.predictDamage(ifv, squad));
        assertEquals(Math.round(ifv.getDamage() * 1.75), attack.predictDamage(ifv, new AntiTankTeam(2, new Point(7, 5))));
        assertEquals(ifv.getDamage(), attack.predictDamage(ifv, new TypeEightySeven(2, new Point(7, 5))));
        Fixtures.setTerrain(game, new Point(7, 5), TerrainEnum.FOREST);
        assertTrue(attack.predictDamage(ifv, squad) < squad.getHealthPoints(), "infantry in a forest survives a burst");
    }

    // --- Artillery ---

    @Test
    void artilleryCannotFireAtAdjacentUnits() {
        int howitzer = Fixtures.place(game, 1, new TypeNinetyNineSPH(1, new Point(5, 5)));
        Fixtures.place(game, 2, new TypeTen(2, new Point(6, 6)));
        int far = Fixtures.place(game, 2, new TypeTen(2, new Point(5, 7)));

        assertEquals(List.of(far), attack.findTargets(player, howitzer));
    }

    @Test
    void artilleryFireIsNeverAnswered() {
        TypeNinetyNineSPH howitzer = new TypeNinetyNineSPH(1, new Point(5, 5));
        int howitzerId = Fixtures.place(game, 1, howitzer);
        int target = Fixtures.place(game, 2, new TypeNinetyNineSPH(2, new Point(9, 5))); // could reach it back

        assertEquals(0, attack.preview(player, howitzerId, target).counterDamage());
        attack.execute(player, howitzerId, target);
        assertEquals(howitzer.getMaxHealthPoints(), howitzer.getHealthPoints());
    }

    @Test
    void artilleryNeverStrikesBack() {
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        int tankId = Fixtures.place(game, 1, tank);
        int howitzer = Fixtures.place(game, 2, new TypeNinetyNineSPH(2, new Point(8, 5)));

        assertEquals(0, attack.preview(player, tankId, howitzer).counterDamage());
        attack.execute(player, tankId, howitzer);
        assertEquals(tank.getMaxHealthPoints(), tank.getHealthPoints());
    }

    // --- Ships ---

    @Test
    void theYamatoBombardsTheShore() {
        Yamato battleship = new Yamato(1, new Point(5, 5));
        Fixtures.place(game, 1, battleship);
        int radar = Fixtures.place(game, 2, new Radarstation(2, new Point(8, 5)));
        Radarstation building = (Radarstation) game.findUnitById(radar);

        assertEquals(Math.round(battleship.getDamage() * 1.5 * (100 - building.getArmorValue()) / 100.0),
                attack.predictDamage(battleship, building));
    }

    @Test
    void theYamatosBigGunsCannotFireAtAdjacentUnits() {
        int battleship = Fixtures.place(game, 1, new Yamato(1, new Point(5, 5)));
        Fixtures.place(game, 2, new Kongo(2, new Point(6, 5)));
        int far = Fixtures.place(game, 2, new Kongo(2, new Point(8, 5)));

        assertEquals(List.of(far), attack.findTargets(player, battleship));
    }

    @Test
    void theYamatoCannotStrikeBackAtAnAdjacentAttacker() {
        Soryu submarine = new Soryu(1, new Point(5, 5));
        int sub = Fixtures.place(game, 1, submarine);
        int close = Fixtures.place(game, 2, new Yamato(2, new Point(6, 5)));

        assertEquals(0, attack.preview(player, sub, close).counterDamage());
        attack.execute(player, sub, close);
        assertEquals(submarine.getMaxHealthPoints(), submarine.getHealthPoints());

        Soryu other = new Soryu(1, new Point(9, 9));
        int otherSub = Fixtures.place(game, 1, other);
        int further = Fixtures.place(game, 2, new Yamato(2, new Point(9, 11))); // two tiles off: in reach of its guns
        attack.execute(player, otherSub, further);
        assertTrue(other.getHealthPoints() < other.getMaxHealthPoints());
    }
}
