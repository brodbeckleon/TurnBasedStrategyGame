package game.controller;

import game.game.Player;
import game.game.commands.*;
import game.model.Game;
import game.enums.UnitEnum;
import game.view.GameView;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import java.awt.Point;
import java.util.Optional;

public class GameController {

    private static final int CELL_SIZE = 40; // Use the same constant here

    private enum ControllerState {
        NORMAL,
        AWAITING_PLACEMENT,
        AWAITING_UNIT_SELECTION_FOR_MOVE,
        AWAITING_ATTACKER_SELECTION,
        AWAITING_ATTACK_TARGET, AWAITING_MOVE_TARGET
    }

    private final Game model;
    private final GameView view;

    // --- Instantiate your command classes ---
    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;

    private ControllerState currentState = ControllerState.NORMAL;
    private UnitEnum unitTypeToAdd = null;
    private int selectedUnitId = -1;
    private int selectedAttackerID = -1;


    public GameController(Game model, GameView view) {
        this.model = model;
        this.view = view;

        // Initialize commands with the game model
        this.addCommand = new AddCommand(model);
        this.moveCommand = new MoveCommand(model);
        this.attackCommand = new AttackCommand(model);
        this.healCommand = new HealCommand(model);

        // Attach event handlers
        attachHandlers();

        this.view.update(model);
    }

    private void attachHandlers() {
        view.getSkipTurnButton().setOnAction(e -> endTurn());
        view.getGrid().setOnMouseClicked(this::handleGridClick);
        view.getAddUnitButton().setOnAction(e -> prepareAddUnit());
        view.getMoveButton().setOnAction(e -> prepareMoveUnit());
        view.getAttackButton().setOnAction(e -> prepareAttack());
    }

    private void endTurn() {
        currentState = ControllerState.NORMAL; // Reset state on turn end
        model.switchTurn();
        view.logInfo("Player " + model.getCurrentPlayer().getPlayerID() + "'s turn begins.");
        view.update(model);
    }

    private void prepareAddUnit() {
        // Use the view to get the unit type from the user
        Optional<UnitEnum> result = view.showAddUnitDialog(); // Assumes this method exists in your view

        if (result.isPresent()) {
            unitTypeToAdd = result.get();
            currentState = ControllerState.AWAITING_PLACEMENT;
            view.logInfo("Select a tile to place your " + unitTypeToAdd.name());
        } else {
            view.logInfo("Add unit cancelled.");
            currentState = ControllerState.NORMAL;
        }
    }

    private void prepareMoveUnit() {
        // Ask the user to click a unit on the grid
        currentState = ControllerState.AWAITING_UNIT_SELECTION_FOR_MOVE;
        view.logInfo("Select a unit to move.");
    }

    private void prepareAttack() {
        currentState = ControllerState.AWAITING_ATTACKER_SELECTION;
        view.logInfo("Select a unit to attack with.");
    }

    private void handleGridClick(MouseEvent event) {
        if (model.isGameOver()) return;
        // Use the CELL_SIZE for accurate coordinate calculation
        int col = (int) (event.getX() / CELL_SIZE);
        int row = (int) (event.getY() / CELL_SIZE);
        Point clickedPoint = new Point(col, row);

        Player currentPlayer = model.getCurrentPlayer();

        switch (currentState) {
            case AWAITING_PLACEMENT -> {
                CommandResult result = addCommand.execute(currentPlayer, unitTypeToAdd.name(), clickedPoint);
                view.logInfo(result.getMessage());
                currentState = ControllerState.NORMAL;
            }
            case AWAITING_UNIT_SELECTION_FOR_MOVE -> {
                // Identify the unit ID clicked
                Node clickedNode = event.getPickResult().getIntersectedNode();
                if (clickedNode != null && clickedNode.getUserData() instanceof Integer) {
                    selectedUnitId = (Integer) clickedNode.getUserData();
                    view.logInfo("Selected unit " + selectedUnitId + ". Click target tile to move.");
                    currentState = ControllerState.AWAITING_MOVE_TARGET;
                }
            }
            case AWAITING_MOVE_TARGET -> {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                view.logInfo(result.getMessage());
                currentState = ControllerState.NORMAL;
                selectedUnitId = -1;
            }
            case AWAITING_ATTACKER_SELECTION -> {
                Node clickedNode = event.getPickResult().getIntersectedNode();
                if (clickedNode != null && clickedNode.getUserData() instanceof Integer) {
                    selectedAttackerID = (Integer) clickedNode.getUserData();
                    view.logInfo("Selected attacker " + selectedAttackerID + ". Now select target.");
                    currentState = ControllerState.AWAITING_ATTACK_TARGET;
                }
            }
            case AWAITING_ATTACK_TARGET -> {
                Node clickedNode = event.getPickResult().getIntersectedNode();
                if (clickedNode != null && clickedNode.getUserData() instanceof Integer) {
                    int targetID = (Integer) clickedNode.getUserData();
                    CommandResult result = attackCommand.execute(model.getCurrentPlayer(), selectedAttackerID, targetID);
                    view.logInfo(result.getMessage());
                    selectedAttackerID = -1;
                    currentState = ControllerState.NORMAL;
                }
            }
            default -> {
                // Normal click behavior
            }
        }

        view.update(model);

        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
        }
    }

}