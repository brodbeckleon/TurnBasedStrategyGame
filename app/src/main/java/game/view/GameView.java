package game.view;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.game.Player;
import game.game.map.Battlefield;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Unit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GameView {

    // --- (Fields are the same) ---
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
    private final Map<String, Image> imageCache = new HashMap<>();


    // --- (Constructor is the same) ---
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

    private Image loadImage(String path) {
        if (imageCache.containsKey(path)) return imageCache.get(path);
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
            imageCache.put(path, image);
            return image;
        } catch (Exception e) {
            System.err.println("Could not load image: " + path);
            imageCache.put(path, null);
            return null;
        }
    }

    public void update(Game model, int selectedUnitId) {
        Player currentPlayer = model.getCurrentPlayer();
        turnLabel.setText("Current Turn: Player " + currentPlayer.getPlayerID());
        player1Status.setText(String.format("Player 1: %d/%d RP", model.getPlayerOne().getResourcePoints(), 10));
        player2Status.setText(String.format("Player 2: %d/%d RP", model.getPlayerTwo().getResourcePoints(), 10));

        grid.getChildren().clear();
        drawTerrain(model.getBattlefield());
        drawPlayerUnits(model.getPlayerOne());
        drawPlayerUnits(model.getPlayerTwo());

        // NEW: Draw range overlays for the selected unit
        if (selectedUnitId != -1) {
            drawRangeOverlays(model, selectedUnitId);
        }
    }

    private void drawTerrain(Battlefield battlefield) {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                TerrainEnum terrainType = battlefield.getGeographyOfPoint(new Point(x,y));
                Image terrainImage = loadImage("/images/terrain/" + terrainType + ".png");
                if (terrainImage != null) {
                    grid.add(new ImageView(terrainImage), x, y);
                }
            }
        }
    }

    private void drawPlayerUnits(Player player) {
        // Draw regular units
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            drawUnitWithHealthBar(entry.getValue(), player.getPlayerID(), entry.getKey());
        }
        // Draw the base
        drawUnitWithHealthBar(player.getPlayerDeck().getBase(), player.getPlayerID(), player.getPlayerDeck().getBase().getBaseID());
    }

    private void drawUnitWithHealthBar(Unit unit, int playerID, int unitId) {
        Point pos = unit.getPosition();

        String unitTypeName = unit.getUnitName();
        String imagePath = String.format("/images/units/player%d/%s.png", playerID, unitTypeName);
        Image unitImage = loadImage(imagePath);

        if (unitImage == null) return;

        ImageView unitImageView = new ImageView(unitImage);
        unitImageView.setFitWidth(CELL_SIZE);
        unitImageView.setFitHeight(CELL_SIZE);
        unitImageView.setPreserveRatio(true);

        if (playerID == 2) {
            unitImageView.setScaleX(-1); // Flip sprite for player 2
        }

        double healthPercentage = (double) unit.getHealthPoints() / unit.getMaxHealthPoints();
        ProgressBar hpBar = new ProgressBar(healthPercentage);
        hpBar.setPrefWidth(CELL_SIZE - 4);
        hpBar.setPrefHeight(5);
        if (healthPercentage > 0.6) {
            hpBar.setStyle("-fx-accent: green;");
        } else if (healthPercentage > 0.3) {
            hpBar.setStyle("-fx-accent: orange;");
        } else {
            hpBar.setStyle("-fx-accent: red;");
        }

        StackPane stack = new StackPane();
        stack.getChildren().addAll(unitImageView, hpBar);
        StackPane.setAlignment(hpBar, Pos.TOP_CENTER);

        stack.setUserData(unitId);
        grid.add(stack, pos.x, pos.y);
    }

    /**
     * NEW: Draws overlays on the grid to show movement and attack ranges.
     */
    private void drawRangeOverlays(Game model, int selectedUnitId) {

        Unit selectedUnit = model.findUnitById(selectedUnitId); // Assumes this method exists in Game
        if (selectedUnit == null) return;

        if (selectedUnit instanceof MovableUnit movableUnit) {
            Point startPos = ((Unit)movableUnit).getPosition();

            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Point targetPos = new Point(x, y);
                    int distance = Math.abs(startPos.x - x) + Math.abs(startPos.y - y);

                    // Check attack range first, as it can overlap with move range
                    if (movableUnit instanceof AssaultClass assaultingUnit) {
                        if (assaultingUnit.isAvailable() && distance > 0 && distance <= assaultingUnit.getShootingRange()) {
                            Rectangle overlay = createOverlay(Color.RED);
                            grid.add(overlay, x, y);
                        }
                    }
                    // Check move range
                    else if (((Unit)movableUnit).isAvailable() && distance > 0 && distance <= movableUnit.getMovingRange()) {
                        if (model.getUnitAt(targetPos) == null) { // Can only move to empty tiles
                            Rectangle overlay = createOverlay(Color.BLACK);
                            grid.add(overlay, x, y);
                        }
                    }
                }
            }
        }
    }

    private Rectangle createOverlay(Color strokeColor) {
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE, Color.TRANSPARENT);
        rect.setStroke(strokeColor);
        rect.setStrokeWidth(3);
        rect.setMouseTransparent(true); // Allows clicks to pass through to the node below
        return rect;
    }

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