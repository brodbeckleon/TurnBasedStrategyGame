package game.controller;

import game.events.GameEvent;
import game.game.Player;
import game.game.commands.*;
import game.model.Game;
import game.enums.UnitEnum;
import game.units.Unit;
import game.view.GameView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

public class GameController {

    private static final int CELL_SIZE = 32;
    private static final double BOT_THINK_SECONDS = 1;
    private static final double BOT_STEP_GAP_SECONDS = 0.15;

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


    private ControllerState currentState = ControllerState.NORMAL;
    private UnitEnum unitTypeToAdd = null;
    private int selectedUnitId = -1; // -1 means no unit is selected
    private boolean animating = false; // true while an animation or the bot turn blocks the player's input

    public GameController(Game model, GameView view) {
        this.model = model;
        this.view = view;

        this.addCommand = new AddCommand(model);
        this.moveCommand = new MoveCommand(model);
        this.attackCommand = new AttackCommand(model);
        this.healCommand = new HealCommand(model);
        this.botService = new BotService(model, addCommand, moveCommand, attackCommand);

        attachHandlers();
        view.update(model, selectedUnitId);
    }

    private void attachHandlers() {
        view.getSkipTurnButton().setOnAction(e -> {
            if (!animating) endTurn();
        });
        view.getGrid().setOnMouseClicked(this::handleGridClick);
        view.getAddUnitButton().setOnAction(e -> {
            if (!animating) prepareAddUnit();
        });
        // The move and attack buttons are no longer needed for the primary flow
        view.getMoveButton().setDisable(true);
        view.getAttackButton().setDisable(true);
    }

    private void endTurn() {
        resetSelection(); // Deselect unit when turn ends
        model.switchTurn();
        view.logInfo("Player " + model.getCurrentPlayer().getPlayerID() + "'s turn begins.");
        view.update(model, selectedUnitId);

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
                view.showBanner("PLAYER 2", () -> guarded(this::beginBotTurn));
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
        play(step.get(), -1, this::afterBotStep);
    }

    private void afterBotStep() {
        model.checkWinCondition();
        if (model.isGameOver()) {
            unlockInput();
            view.showWinner(model.getWinnerId());
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
    private void play(List<GameEvent> events, int selectedUnit, Runnable afterwards) {
        guarded(() -> {
            boolean[] finished = {false};
            view.playEvents(events, () -> guarded(() -> {
                finished[0] = true;
                view.update(model, selectedUnit);
                afterwards.run();
            }));
            if (!finished[0]) {
                view.update(model, selectedUnit); // clears the range overlays and updates the labels while the units move
            }
        });
    }

    /** Plays the events of a successful player command; returns false if there was nothing to play. */
    private boolean playPlayerAction(CommandResult result) {
        if (!result.wasSuccessful() || result.getEvents().isEmpty()) {
            return false;
        }
        lockInput();
        play(result.getEvents(), selectedUnitId, this::finishPlayerAction);
        return true;
    }

    private void finishPlayerAction() {
        unlockInput();
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
        }
    }

    private void lockInput() {
        animating = true;
        view.setInputLocked(true);
    }

    private void unlockInput() {
        animating = false;
        view.setInputLocked(false);
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

    private void prepareAddUnit() {
        Optional<UnitEnum> result = view.showAddUnitDialog();
        if (result.isPresent()) {
            unitTypeToAdd = result.get();
            resetSelection();
            currentState = ControllerState.AWAITING_PLACEMENT;
            view.logInfo("Select a tile to place your " + unitTypeToAdd.name());
        } else {
            view.logInfo("Add unit cancelled.");
        }
        view.update(model, selectedUnitId);
    }

    private void resetSelection() {
        selectedUnitId = -1;
        currentState = ControllerState.NORMAL;
    }

    private void handleGridClick(MouseEvent event) {
        if (!model.isPlayerOneTurn() || model.isGameOver() || animating) {
            return;
        }
        int col = (int) (event.getX() / CELL_SIZE);
        int row = (int) (event.getY() / CELL_SIZE);
        Point clickedPoint = new Point(col, row);

        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            CommandResult result = addCommand.execute(model.getCurrentPlayer(), unitTypeToAdd.name(), clickedPoint);
            view.logInfo(result.getMessage());
            currentState = ControllerState.NORMAL;
            if (!playPlayerAction(result)) {
                view.update(model, selectedUnitId);
            }
            return;
        }

        Optional<Unit> candidate = Optional.ofNullable(model.getUnitAt(clickedPoint));
        Unit clickedUnit = candidate.orElse(null);
        Player currentPlayer = model.getCurrentPlayer();
        boolean animated = false;

        if (currentState == ControllerState.NORMAL) {
            if (clickedUnit != null && model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                selectedUnitId = model.getUnitID(clickedUnit);
                currentState = ControllerState.UNIT_SELECTED;
                view.logInfo("Unit " + selectedUnitId + " selected. Click a target to move or attack.");
            }
        } else if (currentState == ControllerState.UNIT_SELECTED) {
            Unit selectedUnit = model.findUnitById(selectedUnitId);
            if (selectedUnit == null) {
                view.logInfo("Selected unit " + selectedUnitId + " not found.");
                resetSelection();
                return;
            }

            if (clickedUnit != null && model.getUnitID(clickedUnit) == selectedUnitId) {
                resetSelection();
                view.logInfo("Unit deselected.");
            } else if (clickedUnit != null && !model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                CommandResult result = attackCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                view.logInfo(result.getMessage());
                resetSelection();
                animated = playPlayerAction(result);
            } else if (clickedUnit == null) {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                view.logInfo(result.getMessage());
                resetSelection();
                animated = playPlayerAction(result);
            } else {
                selectedUnitId = model.getUnitID(clickedUnit);
                view.logInfo("Switched selection to unit " + selectedUnitId + ".");
            }
        }
        if (animated) {
            return;
        }
        view.update(model, selectedUnitId);
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
        }
    }
}