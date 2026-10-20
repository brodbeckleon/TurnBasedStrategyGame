package game.view;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.game.Player;
import game.game.map.Battlefield;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Unit;
import game.view.anim.AnimationDirector;
import game.view.anim.Cells;
import game.view.anim.EffectSprites;
import game.view.anim.IdleMotion;
import game.view.anim.UnitKind;
import game.view.anim.UnitNode;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.awt.Point;
import java.util.*;
import java.util.function.Supplier;

public class GameView {

    private static final double FAST_FORWARD_RATE = 8;

    private final BorderPane root;
    private final GridPane grid;
    private final StackPane board;
    private final Pane effectsLayer;
    private final Label turnLabel;
    private final Label player1Status;
    private final Label player2Status;
    private final TextArea infoArea;
    private final Button skipTurnButton;
    private final Button addUnitButton;
    private final Button moveButton;
    private final Button attackButton;
    private final CheckBox animationsBox;
    private final int gridWidth;
    private final int gridHeight;
    private static final int CELL_SIZE = Cells.SIZE;
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<Point, StackPane> cellPanes = new HashMap<>();
    private final CircleGenerator circleGenerator = new CircleGenerator();

    private final Map<Integer, UnitNode> unitNodes = new HashMap<>();
    private final Set<Integer> hiddenUnitIds = new HashSet<>();
    private final IdleMotion idleMotion = new IdleMotion();
    private final AnimationDirector director;
    private Playback current;
    private boolean inputLocked;
    private boolean fastForward;
    private boolean gameOver;

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

        // Animations are drawn on a transparent layer above the grid that never takes part in layout or clicks.
        effectsLayer = new Pane();
        effectsLayer.setManaged(false);
        effectsLayer.setMouseTransparent(true);
        effectsLayer.setClip(new Rectangle(gridWidth * CELL_SIZE, gridHeight * CELL_SIZE));
        board = new StackPane(grid, effectsLayer);
        board.setAlignment(Pos.TOP_LEFT);
        root.setCenter(board);

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
        animationsBox = new CheckBox("Animations");
        animationsBox.setSelected(true);
        VBox commandBox = new VBox(5, new Label("Commands:"), addUnitButton, moveButton, attackButton);
        controlPanel.getChildren().addAll(turnLabel, player1Status, player2Status, skipTurnButton, animationsBox, new Label("Info:"), infoArea);
        controlPanel.getChildren().add(3, commandBox);
        root.setRight(controlPanel);

        director = new AnimationDirector(effectsLayer, board, gridWidth * CELL_SIZE, gridHeight * CELL_SIZE,
                this::liftUnit, new EffectSprites());
        idleMotion.runWhileShowing(root);
        animationsBox.selectedProperty().addListener((observable, wasSelected, selected) -> idleMotion.setEnabled(selected));
        installFastForward();
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

        idleMotion.clear();
        unitNodes.clear();
        clearOverlaysAndUnits();
        drawTerrain(model.getBattlefield());
        drawPlayerUnits(model.getPlayerOne());
        drawPlayerUnits(model.getPlayerTwo());

        if (selectedUnitId != -1) {
            drawRangeOverlays(model, selectedUnitId);
            drawSelectionFrame(selectedUnitId);
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

                Image terrainImage = loadImage(getTerrainImagePath(battlefield, pos));
                if (terrainImage != null) {
                    ImageView terrainView = new ImageView(terrainImage);
                    terrainView.setFitWidth(CELL_SIZE);
                    terrainView.setFitHeight(CELL_SIZE);
                    terrainView.setSmooth(false);
                    cellPane.getChildren().add(terrainView);
                }
            }
        }
    }

    private String getTerrainImagePath(Battlefield battlefield, Point pos) {
        StringBuilder path = new StringBuilder("/images/terrain/");

        TerrainEnum centerTerrain = battlefield.getGeographyOfPoint(pos);
        path.append(centerTerrain);

        if (!isInsideBattlefield(battlefield, pos)) {
            return path.append(".png").toString();
        }

        StringBuilder surrounding = new StringBuilder("_");

        if (centerTerrain == TerrainEnum.MOUNTAIN) {
            int seed = Objects.hash(pos.x, pos.y);
            Random rand = new Random(seed);
            int randomNum = rand.nextInt(5);
            path.append(surrounding).append(randomNum);
        }

        if (centerTerrain == TerrainEnum.PLAIN) {
            Point[] directions = {
                    new Point(0, -1),
                    new Point(1, 0),
                    new Point(0, 1),
                    new Point(-1, 0)
            };

            for (Point dir : directions) {
                Point neighbor = new Point(pos.x + dir.x, pos.y + dir.y);
                surrounding.append(getTerrainInitial(battlefield, neighbor));
            }

            String surrStr = surrounding.substring(1);
            if (surrStr.chars().distinct().count() == 1) {
                surrounding.append("_");
                Point[] diagonals = {
                        new Point(1, -1),
                        new Point(1, 1),
                        new Point(-1, 1),
                        new Point(-1, -1)
                };
                for (Point diag : diagonals) {
                    Point neighbor = new Point(pos.x + diag.x, pos.y + diag.y);
                    surrounding.append(getTerrainInitial(battlefield, neighbor));
                }
            }
            String s = surrounding.toString();

            if (s.contains("X")) {
                s = s.replace("X", getTerrainInitial(battlefield, pos));
            }
            if (s.contains("M")) {
                s = s.replace("M", "P");
            }

            path.append(s);


        }

        path.append(".png");
        return path.toString();
    }

    private boolean isInsideBattlefield(Battlefield battlefield, Point pos) {
        return pos.x >= 0 && pos.x < battlefield.getSize().x &&
                pos.y >= 0 && pos.y < battlefield.getSize().y;
    }

    private String getTerrainInitial(Battlefield battlefield, Point pos) {
        if (!isInsideBattlefield(battlefield, pos)) return "X"; // oder "_"
        TerrainEnum terrain = battlefield.getGeographyOfPoint(pos);
        return terrain.toString().substring(0, 1).toUpperCase();
    }


    private void drawPlayerUnits(Player player) {
        // the base is one of the deck's units and is drawn with them
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            drawUnitWithHealthBar(entry.getValue(), player.getPlayerID(), entry.getKey());
        }
    }

    private void drawUnitWithHealthBar(Unit unit, int playerID, int unitId) {
        if (hiddenUnitIds.contains(unitId)) return;

        Point pos = unit.getPosition();
        StackPane cellPane = cellPanes.get(pos);
        if (cellPane == null) return;

        double healthPercentage = (double) unit.getHealthPoints() / unit.getMaxHealthPoints();
        UnitNode unitStack = createUnitNode(playerID, unit.getUnitName(), healthPercentage, UnitKind.of(unit));
        if (!unitStack.hasArt()) return;

        if (unit.getHealthPoints() <= 0) {
            unitStack.showWreck();
        }
        unitStack.setUserData(unitId);

        cellPane.getChildren().add(unitStack);
        unitNodes.put(unitId, unitStack);
        addIdleMotion(unitStack, unitId);
    }

    /** The one place that builds a unit sprite with its health bar, for the board and for animation ghosts. */
    private UnitNode createUnitNode(int playerID, String unitName, double healthRatio, UnitKind kind) {
        Image unitImage = loadImage(String.format("/images/units/player%d/%s.png", playerID, unitName));
        return new UnitNode(unitImage, playerID == 2, healthRatio, kind);
    }

    private void addIdleMotion(UnitNode unitNode, int unitId) {
        double phase = unitId * 0.618 % 1;
        if (unitNode.getKind() == UnitKind.AIR) {
            idleMotion.bob(unitNode, 1.5, 1.6, phase);
        } else if (unitNode.getKind() == UnitKind.SEA) {
            idleMotion.bob(unitNode, 1, 2.4, phase);
        }
    }

    private void drawSelectionFrame(int selectedUnitId) {
        UnitNode selected = unitNodes.get(selectedUnitId);
        if (selected == null) return;

        Rectangle frame = new Rectangle(CELL_SIZE - 2, CELL_SIZE - 2);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Color.rgb(255, 245, 170));
        frame.setStrokeWidth(2);
        frame.setStrokeType(StrokeType.INSIDE);
        frame.setMouseTransparent(true);
        ((StackPane) selected.getParent()).getChildren().add(frame);
        idleMotion.pulse(frame);
    }

    private void drawRangeOverlays(Game model, int selectedUnitId) {
        Unit selectedUnit = model.findUnitById(selectedUnitId);
        if (selectedUnit == null) return;

        if (selectedUnit instanceof MovableUnit movableUnit) {
            Point startPos = ((Unit)movableUnit).getPosition();

            // Generate ranges
            java.util.List<Point> moveRange = ((Unit)movableUnit).isAvailable() ?
                    circleGenerator.getCoordinatesInRange(startPos, movableUnit.getMovingRange()) :
                    new ArrayList<>();

            java.util.List<Point> attackRange = (movableUnit instanceof AssaultClass assaultingUnit && assaultingUnit.isAvailable()) ?
                    circleGenerator.getCoordinatesInRange(startPos, assaultingUnit.getShootingRange()) :
                    new ArrayList<>();

            Set<Point> allPoints = new HashSet<>();
            allPoints.addAll(moveRange);
            allPoints.addAll(attackRange);

            for (Point targetPos : allPoints) {
                if (targetPos.x < 0 || targetPos.x >= gridWidth ||
                        targetPos.y < 0 || targetPos.y >= gridHeight ||
                        targetPos.equals(startPos)) {
                    continue;
                }

                StackPane cellPane = cellPanes.get(targetPos);
                if (cellPane == null) continue;

                boolean canMove = moveRange.contains(targetPos) && model.getUnitAt(targetPos) == null;
                boolean canAttack = attackRange.contains(targetPos);

                if (canMove && canAttack) {
                    Pane crossHatched = createCrossHatchedOverlay();
                    cellPane.getChildren().add(crossHatched);
                } else if (canMove) {
                    Pane overlay = createDiagonalHatchOverlay(Color.rgb(0, 100, 255, 0.4), false);
                    cellPane.getChildren().add(overlay);
                } else if (canAttack) {
                    Pane overlay = createDiagonalHatchOverlay(Color.rgb(255, 50, 50, 0.4), true);
                    cellPane.getChildren().add(overlay);
                }
            }
        }
    }

    private Pane createDiagonalHatchOverlay(Color color, boolean leftDown) {
        Pane pane = new Pane();
        pane.setPrefSize(CELL_SIZE, CELL_SIZE);
        pane.setClip(new javafx.scene.shape.Rectangle(CELL_SIZE, CELL_SIZE)); // Ensures lines don't draw outside the bounds

        double strokeWidth = 2;
        int spacing = 10;

        if (leftDown) {
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
        Color blue = Color.rgb(0, 100, 255, 0.4);
        Color red = Color.rgb(255, 50, 50, 0.4);

        Pane redHatch = createDiagonalHatchOverlay(red, true);

        Pane blueHatch = createDiagonalHatchOverlay(blue, false);

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
        gameOver = true;
        turnLabel.setText("Player " + winnerId + " has won!");
        skipTurnButton.setDisable(true);
        addUnitButton.setDisable(true);
        infoArea.appendText("\nGame Over!");
    }

    public boolean isAnimationsEnabled() {
        return animationsBox.isSelected();
    }

    /**
     * Locks the controls that would interfere with a running animation. While locked, a click on the board or
     * Space/Enter fast-forwards the animations; the fast-forward ends when the controls are locked or unlocked.
     */
    public void setInputLocked(boolean locked) {
        inputLocked = locked;
        fastForward = false;
        skipTurnButton.setDisable(locked || gameOver);
        addUnitButton.setDisable(locked || gameOver);
    }

    /**
     * Plays what happened in the model. The units involved are replaced by animated ghosts, so {@link #update}
     * may be called while it plays: it skips those units. {@code onDone} runs exactly once on the FX thread when
     * the animation is over (immediately if there is nothing to play or animations are off); by then all ghosts
     * and effects are gone, and the caller is expected to {@link #update} the board.
     */
    public void playEvents(List<GameEvent> events, Runnable onDone) {
        finishPlayback();
        if (events.isEmpty() || !isAnimationsEnabled()) {
            onDone.run();
            return;
        }
        play(() -> director.direct(events), onDone);
    }

    /** Shows a banner across the board; {@code onDone} runs exactly once, immediately if animations are off. */
    public void showBanner(String text, Runnable onDone) {
        finishPlayback();
        if (!isAnimationsEnabled()) {
            onDone.run();
            return;
        }
        play(() -> director.banner(text), onDone);
    }

    private void play(Supplier<Transition> builder, Runnable onDone) {
        Transition animation;
        try {
            animation = builder.get();
        } catch (RuntimeException e) {
            // a broken animation must never block the game: report it and carry on without it
            e.printStackTrace();
            endPlayback();
            onDone.run();
            return;
        }
        Playback playback = new Playback(animation, onDone);
        current = playback;
        animation.setRate(fastForward ? FAST_FORWARD_RATE : 1);
        animation.setOnFinished(event -> playback.finish());
        animation.play();
    }

    private void finishPlayback() {
        if (current != null) {
            current.finish();
        }
    }

    private void endPlayback() {
        director.reset();
        hiddenUnitIds.clear();
    }

    /** Replaces a unit's static drawing by a ghost that the animation can move around. */
    private UnitNode liftUnit(int unitId, int playerId, String unitName) {
        hiddenUnitIds.add(unitId);
        double healthRatio = 1;
        UnitKind kind = UnitKind.GROUND;
        UnitNode drawn = unitNodes.get(unitId);
        if (drawn != null) {
            drawn.setVisible(false);
            healthRatio = drawn.getHealthRatio();
            kind = drawn.getKind();
        }
        UnitNode ghost = createUnitNode(playerId, unitName, healthRatio, kind);
        ghost.setVisible(ghost.hasArt());
        return ghost;
    }

    private void installFastForward() {
        EventHandler<KeyEvent> keyFilter = event -> {
            if (inputLocked && (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER)) {
                speedUp();
                event.consume();
            }
        };
        board.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (inputLocked && event.getClickCount() == 1) { // the second click of a double-click is not a skip
                speedUp();
                event.consume();
            }
        });
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
        });
    }

    private void speedUp() {
        fastForward = true;
        if (current != null) {
            current.animation.setRate(FAST_FORWARD_RATE);
        }
    }

    /** A running animation; makes sure its cleanup and {@code onDone} happen exactly once. */
    private final class Playback {
        private final Animation animation;
        private final Runnable onDone;
        private boolean finished;

        private Playback(Animation animation, Runnable onDone) {
            this.animation = animation;
            this.onDone = onDone;
        }

        private void finish() {
            if (finished) return;
            finished = true;
            animation.stop();
            if (current == this) {
                current = null;
            }
            endPlayback();
            onDone.run();
        }
    }

    public void logInfo(String message) { infoArea.appendText(message + "\n"); }
    public GridPane getGrid() { return grid; }
    public Button getSkipTurnButton() { return skipTurnButton; }
    public Button getAddUnitButton() { return addUnitButton; }
    public Button getMoveButton() { return moveButton; }
    public Button getAttackButton() { return attackButton; }
}