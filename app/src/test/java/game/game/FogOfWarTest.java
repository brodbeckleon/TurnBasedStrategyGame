package game.game;

import game.controller.BotService;
import game.enums.TerrainEnum;
import game.events.GameEvent;
import game.events.GameEvent.UnitAttacked;
import game.events.GameEvent.UnitMoved;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.CommandResult;
import game.game.commands.HealCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FThirtyFive;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.artillery.TypeNinetyNineSPH;
import game.units.assaultClasses.ships.Kongo;
import game.units.assaultClasses.ships.Soryu;
import game.units.infrastructures.ArmyBase;
import game.units.supportClasses.ArmoredReconaissanceVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FogOfWarTest {

    private Game game;
    private Player player;
    private AttackCommand attack;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        game.setFogOfWar(true);
        player = game.getPlayerOne();
        attack = new AttackCommand(game);
    }

    @Test
    void unitsOnlySeeTheTilesAroundThem() {
        Fixtures.place(game, 1, new TypeTen(1, new Point(5, 5))); // sees 3 tiles
        var near = new TypeNinety(2, new Point(7, 5));
        var far = new TypeNinety(2, new Point(10, 5));
        Fixtures.place(game, 2, near);
        Fixtures.place(game, 2, far);

        List<game.units.Unit> visible = game.getVisibleEnemies(player);

        assertTrue(visible.contains(near));
        assertFalse(visible.contains(far));
        assertTrue(visible.contains(game.getPlayerTwo().getPlayerDeck().getBase()), "buildings never hide");
    }

    @Test
    void artilleryNeedsSomeoneToSeeItsTarget() {
        int artillery = Fixtures.place(game, 1, new TypeNinetyNineSPH(1, new Point(2, 5))); // range 6, sees 2
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));

        assertFalse(attack.execute(player, artillery, target).wasSuccessful(), "in range but unseen");

        Fixtures.place(game, 1, new ArmoredReconaissanceVehicle(1, new Point(4, 7)));
        assertTrue(attack.execute(player, artillery, target).wasSuccessful(), "the scout sees it");
    }

    @Test
    void submarinesOnlyShowUpNextToAUnitOrOnSonar() {
        game.getBattlefield().getGeography().replaceAll((point, terrain) -> TerrainEnum.WATER);
        Soryu submarine = new Soryu(2, new Point(8, 8));
        Fixtures.place(game, 2, submarine);
        Fixtures.place(game, 1, new TypeTen(1, new Point(6, 8))); // two tiles away: it would see a tank there

        assertFalse(game.isVisibleTo(player, submarine));

        Fixtures.place(game, 1, new Kongo(1, new Point(4, 8)));
        assertTrue(game.isVisibleTo(player, submarine), "within the destroyer's sonar");
    }

    @Test
    void walkingIntoAHiddenEnemyIsAnAmbush() {
        TypeTen tank = new TypeTen(1, new Point(2, 2)); // moves 3 (diagonals too), sees 3
        int id = Fixtures.place(game, 1, tank);
        Fixtures.place(game, 2, new TypeNinety(2, new Point(5, 5))); // 4.2 tiles away: hidden, but within reach
        assertTrue(game.getVisibleEnemies(player).stream().noneMatch(unit -> unit.getUnitName().equals("Type 90")));

        CommandResult result = new MoveCommand(game).execute(player, id, new Point(5, 5)); // looks free

        assertTrue(result.wasSuccessful());
        assertTrue(result.getMessage().startsWith("Ambush!"), result.getMessage());
        UnitMoved moved = assertInstanceOf(UnitMoved.class, result.getEvents().get(0));
        assertEquals(new Point(4, 4), moved.to(), "stopped in front of the hidden tank");
        assertEquals(new Point(4, 4), tank.getPosition());
        assertTrue(tank.hasMoved());
    }

    @Test
    void theBotDoesNotShootAtWhatItCannotSee() {
        game.switchTurn(); // the bot's turn
        game.getPlayerTwo().removeResourcePoints(game.getPlayerTwo().getResourcePoints());
        Fixtures.place(game, 2, new TypeNinetyNineSPH(2, new Point(12, 12))); // range 6, sees 2
        Fixtures.place(game, 1, new TypeNinety(1, new Point(7, 12)));
        BotService bot = new BotService(game, new AddCommand(game), new MoveCommand(game), attack, new HealCommand(game));

        bot.startTurn();
        Optional<List<GameEvent>> step;
        while ((step = bot.performNextStep()).isPresent()) {
            assertTrue(step.get().stream().noneMatch(event -> event instanceof UnitAttacked), "no blind shots");
        }
    }

    @Test
    void withoutFogEverythingIsVisible() {
        game.setFogOfWar(false);
        var far = new TypeNinety(2, new Point(10, 5));
        Fixtures.place(game, 2, far);

        assertTrue(game.isVisibleTo(player, far));
        assertEquals(256, game.getVisibleTiles(player).size());
    }

    @Test
    void buildingsAlwaysShowAndLetTheirOwnerSeeAround() {
        ArmyBase base = new ArmyBase(2, new Point(9, 9));
        Fixtures.place(game, 2, base);
        assertTrue(game.isVisibleTo(player, base));
        assertTrue(game.getVisibleTiles(game.getPlayerTwo()).contains(new Point(10, 10)));
    }

    @Test
    void groundUnitsDriveUnderAHiddenAircraftWithoutAnAmbush() {
        MoveCommand move = new MoveCommand(game);
        TypeTen tank = new TypeTen(1, new Point(2, 2));
        int id = Fixtures.place(game, 1, tank);
        FThirtyFive stealth = new FThirtyFive(2, new Point(4, 4));
        Fixtures.place(game, 2, stealth);
        assertFalse(game.isVisibleTo(player, stealth));

        CommandResult result = move.execute(player, id, new Point(5, 5));

        assertTrue(result.wasSuccessful());
        assertFalse(result.getMessage().startsWith("Ambush!"), "a visible aircraft would not block it either");
        assertEquals(new Point(5, 5), tank.getPosition());
    }

    @Test
    void aircraftFlyOverHiddenGroundUnitsWithoutAnAmbush() {
        MoveCommand move = new MoveCommand(game);
        FTwo jet = new FTwo(1, new Point(2, 2));
        int id = Fixtures.place(game, 1, jet);
        TypeNinety hidden = new TypeNinety(2, new Point(6, 6));
        Fixtures.place(game, 2, hidden);
        assertFalse(game.isVisibleTo(player, hidden));

        CommandResult result = move.execute(player, id, new Point(7, 7));

        assertTrue(result.wasSuccessful());
        assertFalse(result.getMessage().startsWith("Ambush!"));
        assertEquals(new Point(7, 7), jet.getPosition());
    }

    @Test
    void aHiddenEnemyOnTheDestinationStillAmbushes() {
        MoveCommand move = new MoveCommand(game);
        FTwo jet = new FTwo(1, new Point(2, 2));
        int id = Fixtures.place(game, 1, jet);
        Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 7)));

        CommandResult result = move.execute(player, id, new Point(7, 7));

        assertTrue(result.getMessage().startsWith("Ambush!"));
        assertEquals(new Point(6, 6), jet.getPosition());
    }

    @Test
    void deployTilesDoNotGiveAHiddenEnemyAway() {
        AddCommand add = new AddCommand(game);
        var base = player.getPlayerDeck().getBase();
        base.upgrade();
        base.upgrade(); // deploys 4 tiles out, but only sees 3
        TypeNinety hidden = new TypeNinety(2, new Point(4, 0));
        Fixtures.place(game, 2, hidden);
        assertFalse(game.isVisibleTo(player, hidden));

        assertTrue(add.findDeployablePositions(player, new TypeTen(1, new Point())).contains(new Point(4, 0)));
        player.addResourcePoints(20);
        CommandResult result = add.execute(player, "TYPETEN", new Point(4, 0));
        assertFalse(result.wasSuccessful());
        assertEquals(new Point(4, 0), hidden.getPosition());
    }

    @Test
    void theForecastOnlyCountsAntiAirTheAttackerCanSee() {
        var helicopter = new game.units.assaultClasses.helicopters.AHSixtyFourD(1, new Point(5, 5)); // sees 3 tiles
        int id = Fixtures.place(game, 1, helicopter);
        int target = Fixtures.place(game, 2, new TypeNinety(2, new Point(7, 5)));
        var antiAir = new game.units.assaultClasses.antiAir.TypeEightySeven(2, new Point(9, 5)); // reaches 4
        Fixtures.place(game, 2, antiAir);
        assertFalse(game.isVisibleTo(player, antiAir));

        var preview = attack.preview(player, id, target);

        assertNotNull(preview);
        assertEquals(0, preview.antiAirDamage());
        assertFalse(attack.findInterceptors(game.getPlayerTwo(), helicopter, target).isEmpty(), "it does fire, though");
    }
}
