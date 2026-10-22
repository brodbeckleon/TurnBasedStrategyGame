package game.view;

import game.game.Player;
import game.game.Research;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.infrastructures.ArmyBase;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredReconaissanceVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The stats the inspector shows: the player's own research and spotters count, the enemy's stay secret. */
class InspectorStatsTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        game.setFogOfWar(true);
    }

    private static void researchAll(Player who) {
        for (Research project : new Research[]{Research.COMPOSITE_ARMOUR, Research.FIELD_WORKSHOPS, Research.FORWARD_COMMAND}) {
            who.completeResearch(project);
        }
    }

    @Test
    void anEnemyShowsItsStatsWithoutItsResearchAndHiddenSpotters() {
        researchAll(game.getPlayerTwo());
        TypeTen tank = new TypeTen(2, new Point(8, 5));
        Fixtures.place(game, 2, tank);
        ArmoredReconaissanceVehicle spotter = new ArmoredReconaissanceVehicle(2, new Point(10, 5));
        Fixtures.place(game, 2, spotter);
        ArmoredAmbulanceVehicle ambulance = new ArmoredAmbulanceVehicle(2, new Point(8, 6));
        Fixtures.place(game, 2, ambulance);
        ArmyBase armyBase = new ArmyBase(2, new Point(9, 7));
        Fixtures.place(game, 2, armyBase);
        Fixtures.place(game, 1, new ArmoredReconaissanceVehicle(1, new Point(3, 5))); // sees the tank, not its spotter
        Player you = game.getPlayerOne();
        assertTrue(game.isVisibleTo(you, tank));
        assertFalse(game.isVisibleTo(you, spotter));
        assertTrue(game.getShootingRange(tank) > tank.getShootingRange(), "the model knows about the spotter");

        Map<String, Integer> tankStats = GameView.inspectorStats(game, tank);
        assertEquals(tank.getShootingRange(), tankStats.get("RNG"));
        assertEquals(tank.getArmorValue(), tankStats.get("ARM %"));
        assertEquals(ambulance.getSupportPoints(), GameView.inspectorStats(game, ambulance).get("HEAL"));
        assertEquals(armyBase.getConstructionRadius(), GameView.inspectorStats(game, armyBase).get("BUILD"));
    }

    @Test
    void yourOwnUnitsShowWhatTheyAreWorthRightNow() {
        researchAll(game.getPlayerOne());
        TypeTen tank = new TypeTen(1, new Point(5, 5));
        Fixtures.place(game, 1, tank);
        Fixtures.place(game, 1, new ArmoredReconaissanceVehicle(1, new Point(6, 5)));
        ArmoredAmbulanceVehicle ambulance = new ArmoredAmbulanceVehicle(1, new Point(5, 6));
        Fixtures.place(game, 1, ambulance);
        ArmyBase armyBase = new ArmyBase(1, new Point(4, 7));
        Fixtures.place(game, 1, armyBase);

        Map<String, Integer> tankStats = GameView.inspectorStats(game, tank);
        assertEquals(tank.getShootingRange() + 1, tankStats.get("RNG"));
        assertEquals(game.getArmorValue(tank), tankStats.get("ARM %"));
        assertTrue(tankStats.get("ARM %") > tank.getArmorValue());
        assertEquals(ambulance.getSupportPoints() * 3 / 2, GameView.inspectorStats(game, ambulance).get("HEAL"));
        assertEquals(game.getConstructionRadius(armyBase), GameView.inspectorStats(game, armyBase).get("BUILD"));
        assertTrue(game.getConstructionRadius(armyBase) > armyBase.getConstructionRadius());
    }
}
