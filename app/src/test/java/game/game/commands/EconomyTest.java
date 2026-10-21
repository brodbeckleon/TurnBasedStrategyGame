package game.game.commands;

import game.controller.BotService;
import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.events.GameEvent.BaseUpgraded;
import game.events.GameEvent.UnitDeployed;
import game.game.Player;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Base;
import game.units.MilitaryBase;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.infrastructures.ArmyBase;
import game.units.supportClasses.ArmoredRecoveryVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EconomyTest {

    private Game game;
    private Player player;
    private AddCommand add;
    private MoveCommand move;
    private UpgradeCommand upgrade;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        player = game.getPlayerOne();
        add = new AddCommand(game);
        move = new MoveCommand(game);
        upgrade = new UpgradeCommand(game);
    }

    private void giveResources(int amount) {
        while (player.getResourcePoints() < amount) player.addResourcePoints();
    }

    @Test
    void generatedMapsHaveMirroredRuinsOnOpenGround() {
        for (int i = 0; i < 20; i++) {
            Game generated = new Game();
            var battlefield = generated.getBattlefield();
            assertFalse(battlefield.getBaseSites().isEmpty(), "every map has ruins");
            for (Point site : battlefield.getBaseSites()) {
                assertEquals(TerrainEnum.PLAIN, battlefield.getGeographyOfPoint(site));
                assertTrue(battlefield.isBaseSite(new Point(15 - site.x, 15 - site.y)), "mirror of " + site);
                assertTrue(site.distance(0, 0) >= 6 && site.distance(15, 15) >= 6, site + " is away from both bases");
            }
        }
    }

    @Test
    void ruinsBlockUnitsUntilABaseIsBuiltOnThem() {
        Point site = new Point(5, 5);
        Fixtures.addBaseSite(game, site);
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(4, 4)));

        assertFalse(game.isPositionFree(site));
        assertFalse(move.execute(player, tank, site).wasSuccessful());
        assertFalse(add.findDeployablePositions(player, new TypeTen(1, new Point())).contains(site));
    }

    @Test
    void aBaseNeedsAReadyRecoveryVehicleNextToTheRuinsAndUsesItsTurn() {
        Point site = new Point(6, 6);
        Fixtures.addBaseSite(game, site);
        giveResources(20);

        assertFalse(add.execute(player, UnitEnum.ARMYBASE.name(), site).wasSuccessful(), "no recovery vehicle");
        ArmoredRecoveryVehicle far = new ArmoredRecoveryVehicle(1, new Point(8, 6));
        Fixtures.place(game, 1, far);
        assertFalse(add.execute(player, UnitEnum.ARMYBASE.name(), site).wasSuccessful(), "too far away");
        assertFalse(add.execute(player, UnitEnum.ARMYBASE.name(), new Point(9, 6)).wasSuccessful(), "not on ruins");

        ArmoredRecoveryVehicle builder = new ArmoredRecoveryVehicle(1, new Point(7, 7));
        Fixtures.place(game, 1, builder);
        CommandResult result = add.execute(player, UnitEnum.ARMYBASE.name(), site);

        assertTrue(result.wasSuccessful(), result.getMessage());
        assertInstanceOf(ArmyBase.class, game.getUnitAt(site));
        assertFalse(builder.isAvailable(), "building took the vehicle's turn");
        assertEquals(10, player.getResourcePoints());
        assertTrue(far.isAvailable());
    }

    @Test
    void aDestroyedBaseLeavesItsRuinsForTheNextBuilder() {
        Point site = new Point(6, 6);
        Fixtures.addBaseSite(game, site);
        ArmyBase base = new ArmyBase(2, site);
        int baseId = Fixtures.place(game, 2, base);
        base.setHealthPoints(-2400);
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(4, 6)));

        new AttackCommand(game).execute(player, tank, baseId);

        assertNull(game.getUnitAt(site));
        assertTrue(add.isFreeSite(site));
    }

    @Test
    void basesProduceIncomeWithoutALimit() {
        Player bot = game.getPlayerTwo();
        int botStart = bot.getResourcePoints();
        assertEquals(2, game.getIncome(player), "the headquarters alone");

        Fixtures.place(game, 1, new ArmyBase(1, new Point(6, 6)));
        assertEquals(4, game.getIncome(player));
        int before = player.getResourcePoints();
        game.switchTurn();
        assertEquals(before + 4, player.getResourcePoints());

        game.switchTurn();
        while (player.getResourcePoints() < 40) player.addResourcePoints(1);
        game.switchTurn();
        assertEquals(44, player.getResourcePoints(), "no cap: savings keep growing");
        assertEquals(botStart + 2, bot.getResourcePoints(), "the bot earned only its own income, once");
    }

    @Test
    void upgradingRaisesIncomeHealthAndArmourUpToLevelThree() {
        Base hq = player.getPlayerDeck().getBase();
        giveResources(20);

        CommandResult first = upgrade.execute(player, hq.getBaseID());

        assertTrue(first.wasSuccessful());
        int hp = new Base(1, 1, new Point()).getMaxHealthPoints();
        int raised = hp + hp / 10;
        assertEquals(List.of(new BaseUpgraded(1, hq.getBaseID(), "Base", new Point(0, 0), 2, hp, hp, raised, raised)),
                first.getEvents());
        assertEquals(3, hq.getIncome());
        assertEquals(new Base(1, 1, new Point()).getArmorValue() + 5, hq.getArmorValue());
        assertEquals(20 - 6, player.getResourcePoints());

        assertTrue(upgrade.execute(player, hq.getBaseID()).wasSuccessful());
        assertEquals(3, hq.getLevel());
        assertEquals(4, player.getResourcePoints());
        assertFalse(upgrade.execute(player, hq.getBaseID()).wasSuccessful(), "already at the highest level");
    }

    @Test
    void upgradesNeedResourcesAndABaseOfYourOwn() {
        player.removeResourcePoints(player.getResourcePoints());
        int enemyHq = game.getPlayerTwo().getPlayerDeck().getBase().getBaseID();
        int tank = Fixtures.place(game, 1, new TypeTen(1, new Point(3, 3)));

        assertFalse(upgrade.execute(player, player.getPlayerDeck().getBase().getBaseID()).wasSuccessful(), "no RP");
        giveResources(20);
        assertFalse(upgrade.execute(player, enemyHq).wasSuccessful(), "not yours");
        assertFalse(upgrade.execute(player, tank).wasSuccessful(), "not a base");
    }

    // --- The bot ---

    private List<List<GameEvent>> botTurn(BotService bot) {
        List<List<GameEvent>> steps = new ArrayList<>();
        bot.startTurn();
        for (int i = 0; i < 50; i++) {
            Optional<List<GameEvent>> step = bot.performNextStep();
            if (step.isEmpty()) return steps;
            steps.add(step.get());
        }
        fail("the bot turn did not terminate");
        return steps;
    }

    private BotService newBot() {
        game.switchTurn(); // the bot is player two
        return new BotService(game, add, move, new AttackCommand(game), new HealCommand(game));
    }

    @Test
    void botRecoveryVehicleBuildsABaseOnRuinsItReaches() {
        Player bot = game.getPlayerTwo();
        Point site = new Point(10, 10);
        Fixtures.addBaseSite(game, site);
        ArmoredRecoveryVehicle recovery = new ArmoredRecoveryVehicle(2, new Point(13, 13));
        Fixtures.place(game, 2, recovery);
        BotService service = newBot();
        while (bot.getResourcePoints() < 20) bot.addResourcePoints(1);

        botTurn(service);
        assertTrue(recovery.getPosition().distance(site) < 1.5, "drove next to the ruins: " + recovery.getPosition());

        game.switchTurn();
        game.switchTurn();
        botTurn(service);

        Unit built = game.getUnitAt(site);
        assertInstanceOf(MilitaryBase.class, built);
        assertEquals(2, built.getPlayerID());
    }

    @Test
    void botSavesForTheBaseWhileItsRecoveryVehicleIsOnTheWay() {
        Player bot = game.getPlayerTwo();
        Fixtures.addBaseSite(game, new Point(10, 10));
        Fixtures.place(game, 2, new ArmoredRecoveryVehicle(2, new Point(11, 11)));
        BotService service = newBot();
        while (bot.getResourcePoints() < 10) bot.addResourcePoints(1);

        List<List<GameEvent>> steps = botTurn(service);

        assertTrue(steps.stream().noneMatch(step -> step.get(0) instanceof UnitDeployed), "saved instead of buying");
        assertEquals(10, bot.getResourcePoints());
    }

    @Test
    void botAnswersAircraftWithAntiAir() {
        Player bot = game.getPlayerTwo();
        Fixtures.place(game, 1, new FTwo(1, new Point(2, 2)));
        Fixtures.place(game, 1, new FTwo(1, new Point(3, 2)));
        BotService service = newBot();
        while (bot.getResourcePoints() < 8) bot.addResourcePoints(1);

        botTurn(service);

        assertTrue(bot.getPlayerDeck().getUnits().values().stream().anyMatch(unit -> unit instanceof SelfPropelledAntiAir));
    }

    @Test
    void botUpgradesRatherThanWastingResources() {
        Player bot = game.getPlayerTwo();
        BotService service = newBot();
        while (bot.getResourcePoints() < 20) bot.addResourcePoints(1);

        List<List<GameEvent>> steps = botTurn(service);

        assertTrue(steps.stream().anyMatch(step -> step.get(0) instanceof BaseUpgraded));
        assertEquals(2, bot.getPlayerDeck().getBase().getLevel());
    }
}
