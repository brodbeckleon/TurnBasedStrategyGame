package game.controller;

import game.game.Player;
import game.game.commands.*;
import game.model.Game;
import game.enums.UnitEnum;
import game.units.Unit;
import game.view.GameView;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import java.awt.Point;
import java.util.Optional;

public class GameController {

    private static final int CELL_SIZE = 40;

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

        attachHandlers();
        // Initial view update
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
    }

    // This method is largely the same
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

    // NEW: Helper to deselect units and reset state
    private void resetSelection() {
        selectedUnitId = -1;
        currentState = ControllerState.NORMAL;
    }

    /**
     * REWRITTEN: This method now contains the core logic for selecting, moving, and attacking.
     */
    private void handleGridClick(MouseEvent event) {
        if (model.isGameOver()) return;

        int col = (int) (event.getX() / CELL_SIZE);
        int row = (int) (event.getY() / CELL_SIZE);
        Point clickedPoint = new Point(col, row);

        // This part is for adding new units
        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            CommandResult result = addCommand.execute(model.getCurrentPlayer(), unitTypeToAdd.name(), clickedPoint);
            view.logInfo(result.getMessage());
            currentState = ControllerState.NORMAL;
            view.update(model, selectedUnitId);
            return;
        }

        Optional<Unit> candidate = Optional.ofNullable(model.getUnitAt(clickedPoint));
        Unit clickedUnit = null;
        clickedUnit = candidate.orElse(null);
        Player currentPlayer = model.getCurrentPlayer();

        if (currentState == ControllerState.NORMAL) {
            // --- SELECT A UNIT ---
            if (clickedUnit != null && model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                selectedUnitId = model.getUnitID(clickedUnit);
                currentState = ControllerState.UNIT_SELECTED;
                view.logInfo("Unit " + selectedUnitId + " selected. Click a target to move or attack.");
            }
        } else if (currentState == ControllerState.UNIT_SELECTED) {
            Unit selectedUnit = model.findUnitById(selectedUnitId);
            if (selectedUnit == null) { // Should not happen
                view.logInfo("Selected unit " + selectedUnitId + " not found.");
                resetSelection();
                return;
            }

            // --- DESELECT OR RESELECT ---
            if (clickedUnit != null && model.getUnitID(clickedUnit) == selectedUnitId) {
                resetSelection();
                view.logInfo("Unit deselected.");
            }
            // --- ATTACK AN ENEMY ---
            else if (clickedUnit != null && !model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                CommandResult result = attackCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                view.logInfo(result.getMessage());
                // After an action, always deselect
                resetSelection();
            }
            // --- MOVE TO AN EMPTY TILE ---
            else if (clickedUnit == null) {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                view.logInfo(result.getMessage());
                // After an action, always deselect
                resetSelection();
            } else {
                // If another friendly unit is clicked, switch selection to it
                selectedUnitId = model.getUnitID(clickedUnit);
                view.logInfo("Switched selection to unit " + selectedUnitId + ".");
            }
        }

        // Update the view after any action
        view.update(model, selectedUnitId);
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
        }
    }
}