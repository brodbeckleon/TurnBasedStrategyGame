package game.controller;

import game.game.Player;
import game.game.commands.*;
import game.model.Game;
import game.enums.UnitEnum;
import game.units.Unit;
import game.view.GameView;
import javafx.animation.PauseTransition;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.Point;
import java.util.Optional;

public class GameController {

    private static final int CELL_SIZE = 32;

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
        view.getSkipTurnButton().setOnAction(e -> endTurn());
        view.getGrid().setOnMouseClicked(this::handleGridClick);
        view.getAddUnitButton().setOnAction(e -> prepareAddUnit());
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
     * Prüft, ob der Bot am Zug ist und führt dessen Aktionen aus.
     * Diese Methode ist im Vergleich zum vorherigen Vorschlag unverändert.
     */
    private void handleBotTurn() {
        if (!model.isPlayerOneTurn() && !model.isGameOver()) {
            view.getGrid().setDisable(true); // UI sperren, während der Bot "denkt"

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> {
                botService.performTurn();

                view.update(model, -1);
                model.checkWinCondition();

                if (model.isGameOver()) {
                    view.showWinner(model.getWinnerId());
                } else {
                    endTurn();
                }

                if (model.isPlayerOneTurn()) {
                    view.getGrid().setDisable(false);
                }
            });
            pause.play();
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
        if (!model.isPlayerOneTurn() || model.isGameOver()) {
            return;
        }
        // ... Der Rest deiner handleGridClick Methode bleibt exakt gleich
        int col = (int) (event.getX() / CELL_SIZE);
        int row = (int) (event.getY() / CELL_SIZE);
        Point clickedPoint = new Point(col, row);

        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            CommandResult result = addCommand.execute(model.getCurrentPlayer(), unitTypeToAdd.name(), clickedPoint);
            view.logInfo(result.getMessage());
            currentState = ControllerState.NORMAL;
            view.update(model, selectedUnitId);
            return;
        }

        Optional<Unit> candidate = Optional.ofNullable(model.getUnitAt(clickedPoint));
        Unit clickedUnit = candidate.orElse(null);
        Player currentPlayer = model.getCurrentPlayer();

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
            } else if (clickedUnit == null) {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                view.logInfo(result.getMessage());
                resetSelection();
            } else {
                selectedUnitId = model.getUnitID(clickedUnit);
                view.logInfo("Switched selection to unit " + selectedUnitId + ".");
            }
        }
        view.update(model, selectedUnitId);
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
        }
    }
}