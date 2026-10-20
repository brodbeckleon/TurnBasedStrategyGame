package game.controller;

import game.events.GameEvent;
import game.game.Player;
import game.game.UnitFactory;
import game.game.commands.*;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.enums.UnitEnum;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Unit;
import game.view.BoardHighlights;
import game.view.DeployOption;
import game.view.GameView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GameController {

    private static final double BOT_THINK_SECONDS = 1;
    private static final double BOT_STEP_GAP_SECONDS = 0.15;
    private static final Point PROBE_POSITION = new Point(0, 0);

    private enum ControllerState {
        NORMAL,
        UNIT_SELECTED,
        AWAITING_PLACEMENT
    }

    private final Game model;
    private final GameView view;

    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final BotService botService;
    private final UnitFactory unitFactory = new UnitFactory();
    private final CircleGenerator circleGenerator = new CircleGenerator();
    private final List<DeployOption> deployOptions;


    private ControllerState currentState = ControllerState.NORMAL;
    private UnitEnum unitTypeToAdd = null;
    private int selectedUnitId = -1; // -1 means no unit is selected
    private boolean animating = false; // true while an animation or the bot turn blocks the player's input
    private int round = 1;

    public GameController(Game model, GameView view) {
        this.model = model;
        this.view = view;

        this.addCommand = new AddCommand(model);
        this.moveCommand = new MoveCommand(model);
        this.attackCommand = new AttackCommand(model);
        this.healCommand = new HealCommand(model);
        this.botService = new BotService(model, addCommand, moveCommand, attackCommand);
        this.deployOptions = buildDeployOptions();

        attachHandlers();
        view.setRound(round);
        refresh();
    }

    /** The unit types the factory can build, with their numbers for the deploy panel. */
    private List<DeployOption> buildDeployOptions() {
        List<DeployOption> options = new ArrayList<>();
        for (UnitEnum type : UnitEnum.values()) {
            Unit unit = unitFactory.createUnit(1, type.name(), PROBE_POSITION);
            if (unit instanceof AssaultClass assault) {
                options.add(new DeployOption(type, unit.getUnitName(), unit.getResourceCost(), unit.getMaxHealthPoints(),
                        assault.getDamage(), assault.getMovingRange(), assault.getShootingRange()));
            }
        }
        return options;
    }

    private void attachHandlers() {
        view.getSkipTurnButton().setOnAction(e -> {
            if (canAct()) endTurn();
        });
        view.getGrid().setOnMouseClicked(this::handleGridClick);
        view.setDeployOptions(deployOptions, this::prepareAddUnit);
        view.setKeyHandler(this::handleKey);
    }

    private boolean canAct() {
        return model.isPlayerOneTurn() && !model.isGameOver() && !animating;
    }

    private void handleKey(KeyEvent event) {
        if (!canAct()) return;
        switch (event.getCode()) {
            case E -> endTurn();
            case ESCAPE -> cancel();
            default -> {
                String key = event.getText();
                if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
                    int index = key.charAt(0) - '1';
                    if (index >= 0 && index < deployOptions.size()) {
                        prepareAddUnit(deployOptions.get(index).type());
                    }
                }
            }
        }
    }

    /** Drops the current selection or placement. */
    private void cancel() {
        if (currentState == ControllerState.NORMAL) return;
        resetSelection();
        refresh();
    }

    private void endTurn() {
        resetSelection(); // Deselect unit when turn ends
        model.switchTurn();
        if (model.isPlayerOneTurn()) {
            round++;
            view.setRound(round);
            view.logInfo("— Round " + round + ": your turn —");
        } else {
            view.logInfo("— Enemy turn —");
        }
        refresh();

        handleBotTurn();
    }

    /**
     * Starts the bot turn if it is the bot's turn: a banner (or a short pause without animations), then one
     * step after the other, each played before the next one is decided.
     */
    private void handleBotTurn() {
        if (!model.isPlayerOneTurn() && !model.isGameOver()) {
            lockInput(); // block the player while the bot "thinks" and acts

            if (view.isAnimationsEnabled()) {
                view.showBanner("ENEMY TURN", () -> guarded(this::beginBotTurn));
            } else {
                PauseTransition pause = new PauseTransition(Duration.seconds(BOT_THINK_SECONDS));
                pause.setOnFinished(event -> guarded(this::beginBotTurn));
                pause.play();
            }
        }
    }

    private void beginBotTurn() {
        botService.startTurn();
        botStep();
    }

    private void botStep() {
        Optional<List<GameEvent>> step = botService.performNextStep();
        if (step.isEmpty()) {
            finishBotTurn();
            return;
        }
        logEvents(step.get());
        play(step.get(), this::afterBotStep);
    }

    private void afterBotStep() {
        model.checkWinCondition();
        if (model.isGameOver()) {
            unlockInput();
            view.showWinner(model.getWinnerId());
            refreshStatus();
            return;
        }
        // Come back through the event loop so that a long bot turn without animations never recurses deeply.
        if (view.isAnimationsEnabled()) {
            PauseTransition gap = new PauseTransition(Duration.seconds(BOT_STEP_GAP_SECONDS));
            gap.setOnFinished(event -> guarded(this::botStep));
            gap.play();
        } else {
            Platform.runLater(() -> guarded(this::botStep));
        }
    }

    private void finishBotTurn() {
        endTurn();
        view.showBanner("YOUR TURN", this::unlockInput);
    }

    /**
     * Plays the events of one action. The board is refreshed right away (the units being animated stay hidden)
     * and again when the animation is over; then {@code afterwards} continues the game.
     */
    private void play(List<GameEvent> events, Runnable afterwards) {
        guarded(() -> {
            boolean[] finished = {false};
            view.playEvents(events, () -> guarded(() -> {
                finished[0] = true;
                refresh();
                afterwards.run();
            }));
            if (!finished[0]) {
                refresh(); // clears the range overlays and updates the labels while the units move
            }
        });
    }

    /** Plays the events of a successful player command; returns false if there was nothing to play. */
    private boolean playPlayerAction(CommandResult result) {
        if (!result.wasSuccessful() || result.getEvents().isEmpty()) {
            return false;
        }
        lockInput();
        play(result.getEvents(), this::finishPlayerAction);
        return true;
    }

    private void finishPlayerAction() {
        unlockInput();
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
            refreshStatus();
        }
    }

    /** Logs what a command did, or why it was refused. */
    private void logResult(CommandResult result) {
        if (result.wasSuccessful() && !result.getEvents().isEmpty()) {
            logEvents(result.getEvents());
        } else {
            view.logInfo(result.getMessage());
        }
    }

    private void logEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            String line = switch (event) {
                case GameEvent.UnitDeployed e -> owner(e.playerId()) + " " + e.unitName() + " deployed.";
                case GameEvent.UnitMoved e -> owner(e.playerId()) + " " + e.unitName() + " moved.";
                case GameEvent.UnitAttacked e -> null; // the damage line says it all
                case GameEvent.UnitDamaged e -> owner(e.ownerId()) + " " + (e.isBase() ? "base" : e.unitName())
                        + " took " + e.damage() + " damage (" + Math.max(0, e.hpAfter()) + " HP left).";
                case GameEvent.UnitDestroyed e -> owner(e.ownerId()) + " " + (e.isBase() ? "base" : e.unitName())
                        + " was destroyed!";
            };
            if (line != null) {
                view.logInfo(line);
            }
        }
    }

    private static String owner(int playerId) {
        return playerId == 1 ? "Your" : "Enemy";
    }

    private void lockInput() {
        animating = true;
        view.setInputLocked(true);
        refreshStatus();
    }

    private void unlockInput() {
        animating = false;
        view.setInputLocked(false);
        refreshStatus();
    }

    /** Runs the action and releases the input lock if it fails, so the game can never soft-lock. */
    private void guarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error e) {
            unlockInput();
            throw e;
        }
    }

    /** Arms a unit type for placement; picking the armed type again cancels it. */
    private void prepareAddUnit(UnitEnum type) {
        if (!canAct()) return;
        if (currentState == ControllerState.AWAITING_PLACEMENT && unitTypeToAdd == type) {
            cancel();
            return;
        }
        DeployOption option = optionFor(type);
        if (model.getCurrentPlayer().getResourcePoints() < option.cost()) {
            view.logInfo("Not enough resource points for a " + option.name() + " (" + option.cost() + " RP).");
            return;
        }
        resetSelection();
        unitTypeToAdd = type;
        currentState = ControllerState.AWAITING_PLACEMENT;
        refresh();
    }

    private DeployOption optionFor(UnitEnum type) {
        return deployOptions.stream().filter(option -> option.type() == type).findFirst().orElseThrow();
    }

    private void resetSelection() {
        selectedUnitId = -1;
        unitTypeToAdd = null;
        currentState = ControllerState.NORMAL;
    }

    private void handleGridClick(MouseEvent event) {
        if (!canAct()) {
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            cancel();
            return;
        }
        Point clickedPoint = view.cellAt(event.getX(), event.getY());
        if (clickedPoint == null) {
            return;
        }
        BoardHighlights highlights = computeHighlights();

        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            CommandResult result = addCommand.execute(model.getCurrentPlayer(), unitTypeToAdd.name(), clickedPoint);
            logResult(result);
            if (result.wasSuccessful()) {
                resetSelection();
            }
            if (!playPlayerAction(result)) {
                refresh();
            }
            return;
        }

        Unit clickedUnit = model.getUnitAt(clickedPoint);
        Player currentPlayer = model.getCurrentPlayer();
        boolean animated = false;

        if (currentState == ControllerState.NORMAL) {
            if (clickedUnit != null && model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                selectedUnitId = model.getUnitID(clickedUnit);
                currentState = ControllerState.UNIT_SELECTED;
            }
        } else if (currentState == ControllerState.UNIT_SELECTED) {
            Unit selectedUnit = model.findUnitById(selectedUnitId);
            if (selectedUnit == null) {
                resetSelection();
            } else if (clickedUnit != null && model.getUnitID(clickedUnit) == selectedUnitId) {
                resetSelection();
            } else if (clickedUnit != null && !model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                CommandResult result = attackCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                logResult(result);
                if (result.wasSuccessful()) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (clickedUnit == null && highlights.moveTiles().contains(clickedPoint)) {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                logResult(result);
                resetSelection();
                animated = playPlayerAction(result);
            } else if (clickedUnit == null) {
                resetSelection(); // a click on an unreachable tile just drops the selection
            } else {
                selectedUnitId = model.getUnitID(clickedUnit);
            }
        }
        if (animated) {
            return;
        }
        refresh();
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
            refreshStatus();
        }
    }

    // --- What the view shows ---

    private void refresh() {
        view.update(model, computeHighlights());
        refreshStatus();
    }

    /** The hint, the armed deploy card and the end turn suggestion, which change with the input lock too. */
    private void refreshStatus() {
        view.setArmedDeploy(currentState == ControllerState.AWAITING_PLACEMENT ? unitTypeToAdd : null);
        view.setEndTurnSuggested(canAct() && currentState == ControllerState.NORMAL && nothingLeftToDo());
        view.setHint(hint());
    }

    /** Where the selected unit may move and shoot, or where the armed unit type may be deployed. */
    private BoardHighlights computeHighlights() {
        if (!canAct()) {
            return BoardHighlights.NONE;
        }
        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            return new BoardHighlights(-1, Set.of(), Set.of(), Set.of(), deployTiles(unitTypeToAdd));
        }
        Unit selected = currentState == ControllerState.UNIT_SELECTED ? model.findUnitById(selectedUnitId) : null;
        if (selected == null || !selected.isAvailable()) {
            return new BoardHighlights(selectedUnitId, Set.of(), Set.of(), Set.of(), Set.of());
        }

        Set<Point> moves = new HashSet<>();
        if (selected instanceof MovableUnit) {
            for (Point point : selected.getPossibleMoves(model.getBattlefield().getSize())) {
                if (moveCommand.checkIfPositionIsFree(point) && moveCommand.checkIfTerrainIsDeployable(selected, point)) {
                    moves.add(point);
                }
            }
        }
        Set<Point> range = new HashSet<>();
        Set<Point> targets = new HashSet<>();
        if (selected instanceof AssaultClass assault) {
            Point origin = selected.getPosition();
            for (Point point : circleGenerator.getCoordinatesInRange(origin, assault.getShootingRange())) {
                if (isOnBoard(point) && !point.equals(origin)) {
                    range.add(point);
                }
            }
            for (int targetId : model.getOpponent().getPlayerDeck().getUnitIDsInRange(origin, assault.getShootingRange())) {
                targets.add(model.findUnitById(targetId).getPosition());
            }
        }
        return new BoardHighlights(selectedUnitId, moves, range, targets, Set.of());
    }

    private Set<Point> deployTiles(UnitEnum type) {
        Player player = model.getCurrentPlayer();
        Point base = player.getPlayerDeck().getBase().getPosition();
        Set<Point> tiles = new HashSet<>();
        for (Point point : circleGenerator.getCoordinatesInRange(base, player.getPlayerDeck().getBase().getDeploymentRadius())) {
            if (addCommand.checkIfPositionIsFree(point)
                    && addCommand.checkIfTerrainIsDeployable(unitFactory.createUnit(player.getPlayerID(), type.name(), point), point)) {
                tiles.add(point);
            }
        }
        return tiles;
    }

    private boolean isOnBoard(Point point) {
        Point size = model.getBattlefield().getSize();
        return point.x >= 0 && point.x < size.x && point.y >= 0 && point.y < size.y;
    }

    /** True when no unit can act any more and no unit can be bought: all that is left is ending the turn. */
    private boolean nothingLeftToDo() {
        for (Unit unit : model.getCurrentPlayer().getPlayerDeck().getUnits().values()) {
            if (unit instanceof MovableUnit && unit.isAvailable()) {
                return false;
            }
        }
        int resources = model.getCurrentPlayer().getResourcePoints();
        return deployOptions.stream().noneMatch(option -> option.cost() <= resources);
    }

    private String hint() {
        if (model.isGameOver()) {
            return "Game over.";
        }
        if (!model.isPlayerOneTurn()) {
            return "The enemy is making its move…  Click the board or press Space to fast-forward.";
        }
        if (animating) {
            return "Click the board or press Space to fast-forward.";
        }
        switch (currentState) {
            case AWAITING_PLACEMENT -> {
                DeployOption option = optionFor(unitTypeToAdd);
                if (deployTiles(unitTypeToAdd).isEmpty()) {
                    return "There is no free tile near your base for a " + option.name() + ".  Esc to cancel.";
                }
                return "Deploy " + option.name() + " (" + option.cost() + " RP): click a green tile near your base.  "
                        + "Right-click or Esc to cancel.";
            }
            case UNIT_SELECTED -> {
                Unit selected = model.findUnitById(selectedUnitId);
                if (selected == null || !selected.isAvailable()) {
                    return "This unit cannot act right now.  Select another unit or press E to end your turn.";
                }
                return "Blue tiles: move.  Red brackets: attack.  Right-click or Esc to deselect.";
            }
            default -> {
                if (nothingLeftToDo()) {
                    return "Nothing left to do this turn.  Press E to end your turn.";
                }
                return "Select one of your units, or deploy a new one with keys 1–" + deployOptions.size()
                        + ".  Press E to end your turn.";
            }
        }
    }
}
