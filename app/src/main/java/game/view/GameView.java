package game.view;

import game.enums.UnitEnum;
import game.game.Player;
import game.model.Game;
import game.units.Unit;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.Point;
import java.util.Map;
import java.util.Optional;

/**
 * View: Renders the game state and provides UI controls.
 * Contains no game logic.
 */
public class GameView {

    private final BorderPane root;
    private final GridPane grid;
    private final Label turnLabel;
    private final Label player1Status;
    private final Label player2Status;
    private final TextArea infoArea;
    private final Button skipTurnButton;
    private final Button addUnitButton;
    private final Button moveButton;
    private final Button attackButton;

    private final int gridWidth;
    private final int gridHeight;
    private static final int CELL_SIZE = 40;

    public GameView(int width, int height) {
        this.gridWidth = width;
        this.gridHeight = height;

        root = new BorderPane();
        grid = new GridPane();
        grid.setGridLinesVisible(true);

        // --- Game Board ---
        // Use the CELL_SIZE constant to define the grid
        for (int i = 0; i < gridWidth; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
        }
        for (int i = 0; i < gridHeight; i++) {
            grid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }
        root.setCenter(grid);

        // --- Control Panel ---
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        turnLabel = new Label();
        player1Status = new Label();
        player2Status = new Label();
        skipTurnButton = new Button("End Turn");
        infoArea = new TextArea("Welcome to the game!\n");
        infoArea.setEditable(false);
        infoArea.setPrefHeight(200);

        addUnitButton = new Button("Add Unit");
        moveButton = new Button("Move");
        attackButton = new Button("Attack");

        VBox commandBox = new VBox(5, new Label("Commands:"), addUnitButton, moveButton, attackButton);


        controlPanel.getChildren().addAll(turnLabel, player1Status, player2Status, skipTurnButton, new Label("Info:"), infoArea);
        controlPanel.getChildren().add(3, commandBox);
        root.setRight(controlPanel);
    }

    public Pane getRoot() {
        return root;
    }

    public Optional<UnitEnum> showAddUnitDialog() {
        ChoiceDialog<UnitEnum> dialog = new ChoiceDialog<>(UnitEnum.TYPENINETY, UnitEnum.values());
        dialog.setTitle("Add Unit");
        dialog.setHeaderText("Choose a unit to deploy");
        dialog.setContentText("Unit type:");
        return dialog.showAndWait();
    }

    /**
     * Updates the entire UI based on the current state of the game model.
     * @param model The game model.
     */
    public void update(Game model) {
        // Update turn label
        Player currentPlayer = model.getCurrentPlayer();
        turnLabel.setText("Current Turn: Player " + currentPlayer.getPlayerID());
        turnLabel.setStyle("-fx-font-weight: bold;");

        // Update player status
        player1Status.setText(String.format("Player 1: %d/%d RP", model.getPlayerOne().getResourcePoints(), 10));
        player2Status.setText(String.format("Player 2: %d/%d RP", model.getPlayerTwo().getResourcePoints(), 10));

        // Redraw the grid
        grid.getChildren().clear();
        drawPlayerUnits(model.getPlayerOne());
        drawPlayerUnits(model.getPlayerTwo());
    }

    private void drawPlayerUnits(Player player) {
        Color playerColor = player.getPlayerID() == 1 ? Color.BLUE : Color.RED;

        // Iterate over the entry set to get both the ID and the Unit object
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            Integer unitId = entry.getKey();
            Unit unit = entry.getValue();

            Point pos = unit.getPosition();
            Rectangle unitRect = new Rectangle(38, 38, playerColor);
            unitRect.setStroke(Color.BLACK);

            // *** CRITICAL CHANGE HERE ***
            // Store the unit's ID for later identification, not the unit object itself.
            unitRect.setUserData(unitId);

            grid.add(unitRect, pos.x, pos.y);
        }

        // Draw base
        Point basePos = player.getPlayerDeck().getBase().getPosition();
        Rectangle baseRect = new Rectangle(38, 38, playerColor.desaturate());
        baseRect.setStroke(Color.GOLD);
        baseRect.setStrokeWidth(3);

        // Store the base's ID as well
        baseRect.setUserData(player.getPlayerDeck().getBase().getBaseID());
        grid.add(baseRect, basePos.x, basePos.y);
    }

    public void showWinner(int winnerId) {
        turnLabel.setText("Player " + winnerId + " has won!");
        skipTurnButton.setDisable(true);
        infoArea.appendText("\nGame Over!");
    }

    public void logInfo(String message) {
        infoArea.appendText(message + "\n");
    }

    // --- Getters for UI elements the Controller needs to access ---
    public GridPane getGrid() { return grid; }
    public Button getSkipTurnButton() { return skipTurnButton; }
    public Button getAddUnitButton() { return addUnitButton; }
    public Button getMoveButton() { return moveButton; }
    public Button getAttackButton() { return attackButton; }
}