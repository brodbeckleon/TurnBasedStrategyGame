package game.game.commands;

import game.enums.TerrainEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDestroyed;
import game.game.Player;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FFourEJKai;
import game.units.assaultClasses.antiAir.TypeEightySeven;
import game.units.assaultClasses.antiAir.TypeNinetyThree;
import game.units.assaultClasses.infantry.AntiTankTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The forecast shown on hover must be exactly what the attack then does. */
class AttackPreviewTest {

    private Game game;
    private Player player;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        attack = new AttackCommand(game);
    }

    private void assertPreviewMatches(int attackerId, int targetId) {
        AttackPreview preview = attack.preview(player, attackerId, targetId);
        assertNotNull(preview);
        Unit attacker = game.findUnitById(attackerId);

        List<GameEvent> events = attack.execute(player, attackerId, targetId).getEvents();

        UnitDamaged hit = assertInstanceOf(UnitDamaged.class, events.get(1));
        assertEquals(preview.damage(), hit.damage());
        assertEquals(preview.destroysTarget(), events.get(2) instanceof UnitDestroyed d && d.unitId() == targetId);
        int taken = events.stream().filter(e -> e instanceof UnitDamaged d && d.unitId() == attackerId)
                .mapToInt(e -> ((UnitDamaged) e).damage()).sum();
        assertEquals(preview.counterDamage() + preview.antiAirDamage(), taken);
        assertEquals(preview.attackerLost(), attacker.getHealthPoints() <= 0);
    }

    @Test
    void aPlainExchangeOfFire() {
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 5)));
        assertPreviewMatches(tank, target);
    }

    @Test
    void aKillDrawsNoCounterattack() {
        int team = Fixtures.place(game, 1, new AntiTankTeam(1, new Point(5, 5)));
        TypeNinety tank = new TypeNinety(2, new Point(7, 5));
        int target = Fixtures.place(game, 2, tank);
        tank.setHealthPoints(-(tank.getMaxHealthPoints() - 50));

        assertTrue(attack.preview(player, team, target).destroysTarget());
        assertEquals(0, attack.preview(player, team, target).counterDamage());
        assertPreviewMatches(team, target);
    }

    @Test
    void anAircraftUnderAnAntiAirUmbrella() {
        int jet = Fixtures.place(game, 1, new FFourEJKai(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeEightySeven(2, new Point(7, 5))); // survives the jet, strikes back
        Fixtures.place(game, 2, new TypeNinetyThree(2, new Point(7, 7)));

        AttackPreview preview = attack.preview(player, jet, target);
        assertTrue(preview.antiAirDamage() > 0);
        assertPreviewMatches(jet, target);
    }

    @Test
    void coverIsPartOfTheForecast() {
        int tank = Fixtures.place(game, 1, new TypeNinety(1, new Point(5, 5)));
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 5)));
        int open = attack.preview(player, tank, target).damage();
        Fixtures.setTerrain(game, new Point(8, 5), TerrainEnum.FOREST);

        assertTrue(attack.preview(player, tank, target).damage() < open);
        assertPreviewMatches(tank, target);
    }

    @Test
    void noForecastForImpossibleAttacks() {
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(2, 2)));
        int far = Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 12)));
        assertNull(attack.preview(player, tank, far));
    }
}
