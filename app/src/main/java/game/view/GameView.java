package game.view;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.game.Player;
import game.game.map.Battlefield;
import game.model.Game;
import game.units.Unit;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * View: Renders the game state and provides UI controls.
 * Contains no game logic.
 */
public class GameView {

    // --- Fields ---
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

    // --- NEW: Image cache to improve performance ---
    private final Map<String, Image> imageCache = new HashMap<>();

    public GameView(int width, int height) {
        this.gridWidth = width;
        this.gridHeight = height;

        root = new BorderPane();
        grid = new GridPane();
        grid.setGridLinesVisible(true);

        for (int i = 0; i < gridWidth; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
        }
        for (int i = 0; i < gridHeight; i++) {
            grid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }
        root.setCenter(grid);

        // --- Control Panel (No changes here) ---
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

    /**
     * NEW: Helper method to load images and cache them.
     * This is more efficient than loading from disk every time.
     * @param path The path to the image within the resources folder.
     * @return The loaded Image object, or null if not found.
     */
    private Image loadImage(String path) {
        // Check if the image is already in our cache
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }
        // If not, try to load it
        try {
            Image image = new Image(getClass().getResourceAsStream(path));
            if (image.isError()) {
                throw new Exception("Image failed to load: " + path);
            }
            imageCache.put(path, image); // Add the loaded image to the cache
            return image;
        } catch (Exception e) {
            System.err.println("Could not load image: " + path);
            // Put a null or a placeholder in the cache to avoid trying again
            imageCache.put(path, null);
            return null;
        }
    }

    /**
     * UPDATED: The main update method now controls the drawing order.
     * 1. Clear the grid.
     * 2. Draw the terrain background.
     * 3. Draw the units on top.
     */
    public void update(Game model) {
        // Update turn label and player status (no changes)
        Player currentPlayer = model.getCurrentPlayer();
        turnLabel.setText("Current Turn: Player " + currentPlayer.getPlayerID());
        player1Status.setText(String.format("Player 1: %d/%d RP", model.getPlayerOne().getResourcePoints(), 10));
        player2Status.setText(String.format("Player 2: %d/%d RP", model.getPlayerTwo().getResourcePoints(), 10));

        // Redraw the entire grid
        grid.getChildren().clear(); // Clear everything once
        drawTerrain(model.getBattlefield()); // Draw the background terrain first
        drawPlayerUnits(model.getPlayerOne()); // Draw Player 1's units on top
        drawPlayerUnits(model.getPlayerTwo()); // Draw Player 2's units on top
    }

    /**
     * NEW: Draws the terrain tiles for the entire map.
     * This must be called BEFORE drawing units.
     */
    private void drawTerrain(Battlefield battlefield) {
        // Note: This assumes your Battlefield class can provide terrain info.
        // You might need to implement a method like 'getTerrainType(x, y)'.
        // For this example, we'll just draw a "grass" tile everywhere.
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // String terrainType = battlefield.getTerrainType(x, y).toLowerCase(); // e.g., "grass"
                TerrainEnum terrainType = battlefield.getGeographyOfPoint(new Point(x,y));
                Image terrainImage = loadImage("/images/terrain/" + terrainType + ".png");
                if (terrainImage != null) {
                    ImageView terrainView = new ImageView(terrainImage);
                    terrainView.setFitWidth(CELL_SIZE);
                    terrainView.setFitHeight(CELL_SIZE);
                    grid.add(terrainView, x, y);
                }
            }
        }
    }

    /**
     * UPDATED: Draws units using PNGs instead of rectangles.
     * Also flips the image for player 2.
     */
    private void drawPlayerUnits(Player player) {
        int playerID = player.getPlayerID();

        // Draw all units in the player's deck
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            Integer unitId = entry.getKey();
            Unit unit = entry.getValue();
            Point pos = unit.getPosition();

            // Construct the path to the unit's image
            // This assumes unit.getUnitType().name() returns something like "TYPENINETY"
            String unitTypeName = unit.getUnitName();
            String imagePath = String.format("/images/units/player%d/%s.png", playerID, unitTypeName);
            Image unitImage = loadImage(imagePath);

            if (unitImage != null) {
                ImageView unitImageView = new ImageView(unitImage);
                unitImageView.setFitWidth(CELL_SIZE);
                unitImageView.setFitHeight(CELL_SIZE);
                unitImageView.setPreserveRatio(true); // Keep aspect ratio

                // Store the unit's ID for the controller to identify it on click
                unitImageView.setUserData(unitId);

                // --- NEW: Flip the image for Player 2 ---
                if (playerID == 2) {
                    unitImageView.setScaleX(-1);
                }

                grid.add(unitImageView, pos.x, pos.y);
            }
        }

        // Draw the player's base using a PNG
        Point basePos = player.getPlayerDeck().getBase().getPosition();
        String baseImagePath = String.format("/images/units/player%d/Base.png", playerID);
        Image baseImage = loadImage(baseImagePath);
        if (baseImage != null) {
            ImageView baseImageView = new ImageView(baseImage);
            baseImageView.setFitWidth(CELL_SIZE);
            baseImageView.setFitHeight(CELL_SIZE);
            baseImageView.setUserData(player.getPlayerDeck().getBase().getBaseID());
            grid.add(baseImageView, basePos.x, basePos.y);
        }
    }

    // --- Other methods (no changes) ---
    public Pane getRoot() { return root; }

    public Optional<UnitEnum> showAddUnitDialog() {
        ChoiceDialog<UnitEnum> dialog = new ChoiceDialog<>(UnitEnum.TYPENINETY, UnitEnum.values());
        dialog.setTitle("Add Unit");
        dialog.setHeaderText("Choose a unit to deploy");
        dialog.setContentText("Unit type:");
        return dialog.showAndWait();
    }

    public void showWinner(int winnerId) {
        turnLabel.setText("Player " + winnerId + " has won!");
        skipTurnButton.setDisable(true);
        infoArea.appendText("\nGame Over!");
    }

    public void logInfo(String message) { infoArea.appendText(message + "\n"); }
    public GridPane getGrid() { return grid; }
    public Button getSkipTurnButton() { return skipTurnButton; }
    public Button getAddUnitButton() { return addUnitButton; }
    public Button getMoveButton() { return moveButton; }
    public Button getAttackButton() { return attackButton; }
}