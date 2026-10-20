package game.controller;

import game.events.GameEvent;
import game.events.GameEvent.UnitAttacked;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDeployed;
import game.events.GameEvent.UnitDestroyed;
import game.events.GameEvent.UnitMoved;
import game.game.Player;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.testing.Fixtures;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.ships.Yamato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class BotServiceTest {

    private static final int MAX_STEPS = 50;

    private Game game;
    private BotService bot;

    @BeforeEach
    void setUp() {
        game = Fixtures.plainGame();
        bot = newBot(game);
        game.switchTurn(); // the bot is player two
    }

    private static BotService newBot(Game game) {
        return new BotService(game, new AddCommand(game), new MoveCommand(game), new AttackCommand(game));
    }

    private List<List<GameEvent>> runSteps(BotService service) {
        List<List<GameEvent>> steps = new ArrayList<>();
        service.startTurn();
        for (int i = 0; i < MAX_STEPS; i++) {
            Optional<List<GameEvent>> step = service.performNextStep();
            if (step.isEmpty()) {
                return steps;
            }
            steps.add(step.get());
        }
        fail("the bot turn did not terminate");
        return steps;
    }

    @Test
    void attackStepIsFollowedByThePurchaseStep() {
        Fixtures.place(game, 1, new TypeNinety(1, new Point(6, 8)));
        Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 8)));

        List<List<GameEvent>> steps = runSteps(bot);

        assertEquals(2, steps.size());
        assertInstanceOf(UnitAttacked.class, steps.get(0).get(0));
        assertInstanceOf(UnitDamaged.class, steps.get(0).get(1));
        UnitDeployed deployed = assertInstanceOf(UnitDeployed.class, steps.get(1).get(0));
        assertEquals(2, deployed.playerId());
        assertTrue(deployed.pos().distance(new Point(15, 15)) <= 3);
    }

    @Test
    void unitsFarFromTheEnemyMoveCloser() {
        int id = Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 12)));

        List<List<GameEvent>> steps = runSteps(bot);

        UnitMoved moved = assertInstanceOf(UnitMoved.class, steps.get(0).get(0));
        assertEquals(id, moved.unitId());
        assertEquals(new Point(12, 12), moved.from());
        assertTrue(moved.to().distance(new Point(0, 0)) < moved.from().distance(new Point(0, 0)));
        assertEquals(moved.to(), game.findUnitById(id).getPosition());
    }

    @Test
    void botAttacksTheTargetWithTheLowestHealthInRange() {
        int healthy = Fixtures.place(game, 1, new TypeNinety(1, new Point(6, 8)));
        int weak = Fixtures.place(game, 1, new TypeNinety(1, new Point(7, 9)));
        game.findUnitById(weak).setHealthPoints(-20);
        Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 8)));

        List<List<GameEvent>> steps = runSteps(bot);

        UnitAttacked attacked = assertInstanceOf(UnitAttacked.class, steps.get(0).get(0));
        assertEquals(weak, attacked.targetId());
        assertNotEquals(healthy, attacked.targetId());
    }

    @Test
    void botOnlyBuysOneUnitPerTurn() {
        for (int i = 0; i < 5; i++) game.getPlayerTwo().addResourcePoints();

        List<List<GameEvent>> steps = runSteps(bot);

        assertEquals(1, steps.size());
        assertInstanceOf(UnitDeployed.class, steps.get(0).get(0));
    }

    @Test
    void noPurchaseWithoutResources() {
        game.getPlayerTwo().removeResourcePoints(game.getPlayerTwo().getResourcePoints());
        Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 12)));

        List<List<GameEvent>> steps = runSteps(bot);

        assertEquals(1, steps.size());
        assertInstanceOf(UnitMoved.class, steps.get(0).get(0));
    }

    @Test
    void stepsWithoutEventsAreSkipped() {
        game.getPlayerTwo().removeResourcePoints(game.getPlayerTwo().getResourcePoints());
        Fixtures.place(game, 2, new Yamato(2, new Point(10, 10))); // stranded on land, cannot do anything
        int tank = Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 12)));
        TypeTen exhausted = new TypeTen(2, new Point(12, 6));
        Fixtures.place(game, 2, exhausted);
        exhausted.setIsAvailable(false);

        List<List<GameEvent>> steps = runSteps(bot);

        assertEquals(1, steps.size());
        assertEquals(tank, assertInstanceOf(UnitMoved.class, steps.get(0).get(0)).unitId());
    }

    @Test
    void finishedTurnKeepsReturningEmptyUntilTheNextTurnStarts() {
        runSteps(bot);

        assertTrue(bot.performNextStep().isEmpty());
        assertTrue(bot.performNextStep().isEmpty());

        game.switchTurn();
        game.switchTurn();
        bot.startTurn();
        assertTrue(bot.performNextStep().isPresent(), "the purchase of the new turn");
    }

    @Test
    void stepBeforeStartTurnDoesNothing() {
        Fixtures.place(game, 2, new TypeNinety(2, new Point(12, 12)));

        assertTrue(bot.performNextStep().isEmpty());
        assertEquals(new Point(12, 12), game.getUnitAt(new Point(12, 12)).getPosition());
    }

    @Test
    void eventsDescribeTheResultingModelState() {
        TypeNinety doomed = new TypeNinety(1, new Point(6, 8));
        int doomedId = Fixtures.place(game, 1, doomed);
        doomed.setHealthPoints(-550);
        Fixtures.place(game, 1, new TypeTen(1, new Point(2, 2)));
        Fixtures.place(game, 2, new TypeTen(2, new Point(8, 8)));
        Fixtures.place(game, 2, new TypeNinety(2, new Point(13, 4)));

        Map<Integer, Point> positions = new HashMap<>();
        for (Player player : List.of(game.getPlayerOne(), game.getPlayerTwo())) {
            player.getPlayerDeck().getUnits().forEach((id, unit) -> positions.put(id, new Point(unit.getPosition())));
        }
        Map<Integer, Integer> health = new HashMap<>();
        TreeSet<Integer> destroyed = new TreeSet<>();

        for (List<GameEvent> step : runSteps(bot)) {
            for (GameEvent event : step) {
                switch (event) {
                    case UnitDeployed e -> positions.put(e.unitId(), e.pos());
                    case UnitMoved e -> {
                        assertEquals(positions.get(e.unitId()), e.from());
                        positions.put(e.unitId(), e.to());
                    }
                    case UnitAttacked e -> assertEquals(positions.get(e.attackerId()), e.from());
                    case UnitDamaged e -> {
                        assertEquals(positions.get(e.unitId()), e.pos());
                        health.put(e.unitId(), e.hpAfter());
                    }
                    case UnitDestroyed e -> destroyed.add(e.unitId());
                }
            }
        }

        assertEquals(new TreeSet<>(List.of(doomedId)), destroyed);
        for (Player player : List.of(game.getPlayerOne(), game.getPlayerTwo())) {
            player.getPlayerDeck().getUnits().forEach((id, unit) -> {
                assertEquals(positions.get(id), unit.getPosition(), "position of unit " + id);
                if (health.containsKey(id)) {
                    assertEquals(health.get(id), unit.getHealthPoints(), "health of unit " + id);
                }
            });
        }
        assertNull(game.findUnitById(doomedId));
    }

    @Test
    void performTurnLeadsToTheSameStateAsSteppingThroughTheTurn() {
        List<String> viaPerformTurn = playScenario(false);
        List<String> viaSteps = playScenario(true);

        assertEquals(viaPerformTurn, viaSteps);
        assertTrue(viaPerformTurn.stream().anyMatch(s -> s.startsWith("2,Type 90")), "the bot bought a unit");
    }

    private List<String> playScenario(boolean stepwise) {
        Game scenario = Fixtures.plainGame();
        BotService scenarioBot = newBot(scenario);
        scenario.switchTurn();
        Fixtures.place(scenario, 1, new TypeNinety(1, new Point(5, 3)));
        Fixtures.place(scenario, 2, new TypeTen(2, new Point(8, 3)));
        Fixtures.place(scenario, 2, new TypeNinety(2, new Point(14, 9)));

        if (stepwise) {
            runSteps(scenarioBot);
        } else {
            scenarioBot.performTurn();
        }

        List<String> state = new ArrayList<>();
        for (Player player : List.of(scenario.getPlayerOne(), scenario.getPlayerTwo())) {
            for (Unit unit : player.getPlayerDeck().getUnits().values()) {
                state.add(String.format("%d,%s,%d,%d,%d,%b", player.getPlayerID(), unit.getUnitName(),
                        unit.getPosition().x, unit.getPosition().y, unit.getHealthPoints(), unit.isAvailable()));
            }
            state.add("rp" + player.getResourcePoints());
        }
        state.sort(null);
        return state;
    }

    @Test
    void performTurnStillRunsTheWholeTurn() {
        Fixtures.place(game, 1, new TypeNinety(1, new Point(6, 8)));
        int attacker = Fixtures.place(game, 2, new TypeNinety(2, new Point(8, 8)));

        bot.performTurn();

        assertFalse(game.findUnitById(attacker).isAvailable(), "the unit acted");
        assertEquals(1, game.getPlayerTwo().getPlayerDeck().getUnits().values().stream()
                .filter(u -> u.getUnitName().equals("Type 90") && u != game.findUnitById(attacker)).count(), "and a unit was bought");
    }
}
