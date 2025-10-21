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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<Point, StackPane> cellPanes = new HashMap<>();

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

        initializeCellPanes();

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

    private void initializeCellPanes() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);
                grid.add(cellPane, x, y);
                cellPanes.put(new Point(x, y), cellPane);
            }
        }
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

        clearOverlaysAndUnits();
        drawTerrain(model.getBattlefield());
        drawPlayerUnits(model.getPlayerOne());
        drawPlayerUnits(model.getPlayerTwo());

        if (selectedUnitId != -1) {
            drawRangeOverlays(model, selectedUnitId);
        }
    }

    private void clearOverlaysAndUnits() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Point pos = new Point(x, y);
                StackPane cellPane = cellPanes.get(pos);
                if (cellPane != null) {
                    // Remove all children except the first one (terrain)
                    if (cellPane.getChildren().size() > 1) {
                        cellPane.getChildren().remove(1, cellPane.getChildren().size());
                    }
                }
            }
        }
    }

    private void drawTerrain(Battlefield battlefield) {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Point pos = new Point(x, y);
                StackPane cellPane = cellPanes.get(pos);
                if (cellPane == null) continue;

                cellPane.getChildren().clear();

                TerrainEnum terrainType = battlefield.getGeographyOfPoint(pos);
                Image terrainImage = loadImage("/images/terrain/" + terrainType + ".png");
                if (terrainImage != null) {
                    ImageView terrainView = new ImageView(terrainImage);
                    terrainView.setFitWidth(CELL_SIZE);
                    terrainView.setFitHeight(CELL_SIZE);
                    cellPane.getChildren().add(terrainView);
                }
            }
        }
    }

    private void drawPlayerUnits(Player player) {
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            drawUnitWithHealthBar(entry.getValue(), player.getPlayerID(), entry.getKey());
        }
        drawUnitWithHealthBar(player.getPlayerDeck().getBase(), player.getPlayerID(), player.getPlayerDeck().getBase().getBaseID());
    }

    private void drawUnitWithHealthBar(Unit unit, int playerID, int unitId) {
        Point pos = unit.getPosition();
        StackPane cellPane = cellPanes.get(pos);
        if (cellPane == null) return;

        String unitTypeName = unit.getUnitName();
        String imagePath = String.format("/images/units/player%d/%s.png", playerID, unitTypeName);
        Image unitImage = loadImage(imagePath);

        if (unitImage == null) return;

        ImageView unitImageView = new ImageView(unitImage);
        unitImageView.setFitWidth(CELL_SIZE);
        unitImageView.setFitHeight(CELL_SIZE);
        unitImageView.setPreserveRatio(true);

        if (playerID == 2) {
            unitImageView.setScaleX(-1);
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

        StackPane unitStack = new StackPane();
        unitStack.getChildren().addAll(unitImageView, hpBar);
        StackPane.setAlignment(hpBar, Pos.TOP_CENTER);
        unitStack.setUserData(unitId);

        cellPane.getChildren().add(unitStack);
    }

    private void drawRangeOverlays(Game model, int selectedUnitId) {
        Unit selectedUnit = model.findUnitById(selectedUnitId);
        if (selectedUnit == null) return;

        if (selectedUnit instanceof MovableUnit movableUnit) {
            Point startPos = ((Unit)movableUnit).getPosition();

            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Point targetPos = new Point(x, y);
                    StackPane cellPane = cellPanes.get(targetPos);
                    if (cellPane == null) continue;

                    int distance = Math.abs(startPos.x - x) + Math.abs(startPos.y - y);

                    boolean canMove = false;
                    boolean canAttack = false;

                    // Check move range
                    if (((Unit)movableUnit).isAvailable() && distance > 0 && distance <= movableUnit.getMovingRange()) {
                        if (model.getUnitAt(targetPos) == null) {
                            canMove = true;
                        }
                    }

                    // Check attack range
                    if (movableUnit instanceof AssaultClass assaultingUnit) {
                        if (assaultingUnit.isAvailable() && distance > 0 && distance <= assaultingUnit.getShootingRange()) {
                            canAttack = true;
                        }
                    }

                    // Create appropriate overlay
                    if (canMove && canAttack) {
                        Pane crossHatched = createCrossHatchedOverlay();
                        cellPane.getChildren().add(crossHatched);
                    } else if (canMove) {
                        Pane overlay = createDiagonalHatchOverlay(Color.rgb(0, 100, 255, 0.6), false);
                        cellPane.getChildren().add(overlay);
                    } else if (canAttack) {
                        Pane overlay = createDiagonalHatchOverlay(Color.rgb(255, 50, 50, 0.6), true);
                        cellPane.getChildren().add(overlay);
                    }
                }
            }
        }
    }

    private Pane createDiagonalHatchOverlay(Color color, boolean leftDown) {
        Pane pane = new Pane();
        pane.setPrefSize(CELL_SIZE, CELL_SIZE);
        pane.setClip(new javafx.scene.shape.Rectangle(CELL_SIZE, CELL_SIZE)); // Ensures lines don't draw outside the bounds

        double strokeWidth = 1.5; // You can adjust the line thickness
        int spacing = 6;          // You can adjust the spacing between lines

        if (leftDown) { // from top-left to bottom-right (like a backslash \)
            for (int i = -CELL_SIZE; i < CELL_SIZE * 2; i += spacing) {
                Line line = new Line(i - strokeWidth, -strokeWidth, i + CELL_SIZE + strokeWidth, CELL_SIZE + strokeWidth);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
                pane.getChildren().add(line);
            }
        } else { // from top-right to bottom-left (like a forward slash /)
            for (int i = -CELL_SIZE; i < CELL_SIZE * 2; i += spacing) {
                Line line = new Line(CELL_SIZE - (i - strokeWidth), -strokeWidth, CELL_SIZE - (i + CELL_SIZE + strokeWidth), CELL_SIZE + strokeWidth);
                line.setStroke(color);
                line.setStrokeWidth(strokeWidth);
                pane.getChildren().add(line);
            }
        }

        pane.setMouseTransparent(true);
        return pane;
    }

    private Pane createCrossHatchedOverlay() {
        // This now simply combines the two diagonal overlays
        Color blue = Color.rgb(0, 100, 255, 0.6);
        Color red = Color.rgb(255, 50, 50, 0.6);

        // Create the red diagonal overlay (top-left to bottom-right)
        Pane redHatch = createDiagonalHatchOverlay(red, true);

        // Create the blue diagonal overlay (top-right to bottom-left)
        Pane blueHatch = createDiagonalHatchOverlay(blue, false);

        // Stack them on top of each other
        StackPane stack = new StackPane(redHatch, blueHatch);
        stack.setPrefSize(CELL_SIZE, CELL_SIZE);
        stack.setMouseTransparent(true);

        return stack;
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