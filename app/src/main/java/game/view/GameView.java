package game.view;

import game.enums.TerrainEnum;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.game.Player;
import game.game.map.Battlefield;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.interfaces.Spotter;
import game.units.AssaultClass;
import game.units.Base;
import game.units.Infrastructure;
import game.units.SupportClass;
import game.units.Unit;
import game.units.assaultClasses.AssaultTank;
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
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.awt.Point;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameView {

    private static final double FAST_FORWARD_RATE = 8;
    private static final int MAX_RESOURCE_POINTS = 10;
    private static final double BOARD_MARGIN = 16;
    private static final double SIDE_PANEL_WIDTH = 330;

    private static final Color MOVE_FILL = Color.rgb(70, 140, 255, 0.38);
    private static final Color MOVE_EDGE = Color.rgb(160, 200, 255, 0.9);
    private static final Color RANGE_HATCH = Color.rgb(255, 70, 70, 0.32);
    private static final Color TARGET_COLOR = Color.rgb(255, 70, 60);
    private static final Color SUPPORT_COLOR = Color.rgb(90, 235, 130);
    private static final Color DEPLOY_FILL = Color.rgb(60, 230, 120, 0.42);
    private static final Color DEPLOY_EDGE = Color.rgb(170, 255, 190, 0.9);
    private static final Color SELECTION_COLOR = Color.rgb(255, 245, 170);

    private final BorderPane root;
    private final GridPane grid;
    private final StackPane board;
    private final StackPane boardArea;
    private final Pane boardHolder;
    private final Scale boardScale = new Scale(1, 1, 0, 0);
    private final Pane markerLayer;
    private final Rectangle hoverFrame;
    private final Pane effectsLayer;
    private final Label hintLabel;
    private final Label roundLabel;
    private final Label turnLabel;
    private final PlayerCard player1Card;
    private final PlayerCard player2Card;
    private final VBox deployBox;
    private final HBox deployTabs;
    private final GridPane deployCards;
    private final VBox inspector;
    private final TextArea infoArea;
    private final Button skipTurnButton;
    private final CheckBox animationsBox;
    private final int gridWidth;
    private final int gridHeight;
    private static final int CELL_SIZE = Cells.SIZE;
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<Point, StackPane> cellPanes = new HashMap<>();

    private final Map<Integer, UnitNode> unitNodes = new HashMap<>();
    private final Set<Integer> hiddenUnitIds = new HashSet<>();
    private final IdleMotion idleMotion = new IdleMotion();
    private final AnimationDirector director;
    private final Map<UnitEnum, Button> deployButtons = new EnumMap<>(UnitEnum.class);
    private final Map<UnitEnum, DeployOption> deployOptions = new EnumMap<>(UnitEnum.class);
    private final Map<DeployOption.Category, ToggleButton> deployTabButtons = new EnumMap<>(DeployOption.Category.class);
    private Map<UnitEnum, String> blockedDeploys = Map.of();
    private DeployOption.Category deployCategory = DeployOption.Category.ARMY;
    private Consumer<UnitEnum> onDeployPick = type -> { };
    private Playback current;
    private boolean inputLocked;
    private boolean fastForward;
    private boolean gameOver;
    private Game model;
    private BoardHighlights highlights = BoardHighlights.NONE;
    private Point hoveredCell;
    private UnitEnum armedDeploy;
    private Consumer<KeyEvent> keyHandler = event -> { };
    private Runnable onRestart;

    public GameView(int width, int height) {
        this.gridWidth = width;
        this.gridHeight = height;

        root = new BorderPane();
        root.getStyleClass().add("game-root");
        grid = new GridPane();

        for (int i = 0; i < gridWidth; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
        }
        for (int i = 0; i < gridHeight; i++) {
            grid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }

        // Grid lines and the hover frame sit above the tiles and never take part in layout or clicks.
        markerLayer = new Pane();
        markerLayer.setManaged(false);
        markerLayer.setMouseTransparent(true);
        hoverFrame = new Rectangle(CELL_SIZE, CELL_SIZE);
        hoverFrame.setFill(Color.rgb(255, 255, 255, 0.14));
        hoverFrame.setStroke(Color.rgb(255, 255, 255, 0.55));
        hoverFrame.setStrokeWidth(1);
        hoverFrame.setStrokeType(StrokeType.INSIDE);
        hoverFrame.setVisible(false);
        markerLayer.getChildren().addAll(createGridLines(), hoverFrame);

        // Animations are drawn on a transparent layer above the grid that never takes part in layout or clicks.
        effectsLayer = new Pane();
        effectsLayer.setManaged(false);
        effectsLayer.setMouseTransparent(true);
        effectsLayer.setClip(new Rectangle(gridWidth * CELL_SIZE, gridHeight * CELL_SIZE));
        board = new StackPane(grid, markerLayer, effectsLayer);
        board.setAlignment(Pos.TOP_LEFT);
        board.getStyleClass().add("board");
        board.getTransforms().add(boardScale);

        // The board is scaled to fill the space it gets; the holder reserves the scaled size and never moves the
        // board itself, so the shake of an exploding base is not undone by the layout.
        boardHolder = new Pane(board);
        boardHolder.setMinSize(0, 0);
        boardHolder.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        boardArea = new StackPane(boardHolder);
        boardArea.getStyleClass().add("board-area");
        boardArea.setMinSize(0, 0);
        boardArea.setPrefSize(gridWidth * CELL_SIZE + 2 * BOARD_MARGIN, gridHeight * CELL_SIZE + 2 * BOARD_MARGIN);
        boardArea.widthProperty().addListener((observable, oldValue, newValue) -> fitBoard());
        boardArea.heightProperty().addListener((observable, oldValue, newValue) -> fitBoard());

        hintLabel = new Label();
        hintLabel.getStyleClass().add("hint-bar");
        hintLabel.setMaxWidth(Double.MAX_VALUE);
        hintLabel.setWrapText(true);
        BorderPane centerPane = new BorderPane(boardArea);
        centerPane.setBottom(hintLabel);
        root.setCenter(centerPane);

        initializeCellPanes();

        roundLabel = new Label();
        roundLabel.getStyleClass().add("round-label");
        turnLabel = new Label();
        turnLabel.getStyleClass().add("turn-label");
        VBox header = new VBox(2, roundLabel, turnLabel);

        player1Card = new PlayerCard("PLAYER 1", "You", 1);
        player2Card = new PlayerCard("PLAYER 2", "Bot", 2);
        HBox playerCards = new HBox(8, player1Card, player2Card);
        HBox.setHgrow(player1Card, Priority.ALWAYS);
        HBox.setHgrow(player2Card, Priority.ALWAYS);

        skipTurnButton = new Button("End Turn  [E]");
        skipTurnButton.getStyleClass().add("end-turn");
        skipTurnButton.setMaxWidth(Double.MAX_VALUE);
        skipTurnButton.setFocusTraversable(false);
        animationsBox = new CheckBox("Animations");
        animationsBox.setSelected(true);
        animationsBox.setFocusTraversable(false);

        deployTabs = new HBox(4);
        deployCards = new GridPane();
        deployCards.setHgap(6);
        deployCards.setVgap(6);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            deployCards.getColumnConstraints().add(half);
        }
        deployBox = new VBox(8, deployTabs, deployCards);
        inspector = new VBox(6);
        inspector.getStyleClass().add("card");

        infoArea = new TextArea("Welcome, commander! Destroy the enemy base to win.\n");
        infoArea.setEditable(false);
        infoArea.setWrapText(true);
        infoArea.setFocusTraversable(false);
        infoArea.getStyleClass().add("log");
        infoArea.setPrefHeight(120);
        infoArea.setMinHeight(70);
        VBox.setVgrow(infoArea, Priority.ALWAYS);

        VBox controlPanel = new VBox(10,
                header,
                playerCards,
                skipTurnButton,
                sectionTitle("DEPLOY"), deployBox,
                sectionTitle("INSPECT"), inspector,
                sectionTitle("LOG"), infoArea,
                animationsBox);
        controlPanel.getStyleClass().add("side-panel");
        controlPanel.setPadding(new Insets(14));
        controlPanel.setPrefWidth(SIDE_PANEL_WIDTH);
        controlPanel.setMinWidth(SIDE_PANEL_WIDTH);
        root.setRight(controlPanel);

        director = new AnimationDirector(effectsLayer, board, gridWidth * CELL_SIZE, gridHeight * CELL_SIZE,
                this::liftUnit, new EffectSprites());
        idleMotion.runWhileShowing(root);
        animationsBox.selectedProperty().addListener((observable, wasSelected, selected) -> idleMotion.setEnabled(selected));
        installHover();
        installKeys();
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Path createGridLines() {
        Path lines = new Path();
        for (int x = 1; x < gridWidth; x++) {
            lines.getElements().addAll(new MoveTo(x * CELL_SIZE, 0), new LineTo(x * CELL_SIZE, gridHeight * CELL_SIZE));
        }
        for (int y = 1; y < gridHeight; y++) {
            lines.getElements().addAll(new MoveTo(0, y * CELL_SIZE), new LineTo(gridWidth * CELL_SIZE, y * CELL_SIZE));
        }
        lines.setStroke(Color.rgb(0, 0, 0, 0.16));
        lines.setStrokeWidth(1);
        return lines;
    }

    /** Scales the board to the largest size that fits, in whole screen pixels per cell so the pixel art stays crisp. */
    private void fitBoard() {
        double boardWidth = gridWidth * CELL_SIZE;
        double boardHeight = gridHeight * CELL_SIZE;
        double fit = Math.min((boardArea.getWidth() - 2 * BOARD_MARGIN) / boardWidth,
                (boardArea.getHeight() - 2 * BOARD_MARGIN) / boardHeight);
        double scale = Math.max(0.5, Math.floor(fit * CELL_SIZE) / CELL_SIZE);
        boardScale.setX(scale);
        boardScale.setY(scale);
        boardHolder.setPrefSize(boardWidth * scale, boardHeight * scale);
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

    private Image unitImage(int playerID, String unitName) {
        return loadImage(String.format("/images/units/player%d/%s.png", playerID, unitName));
    }

    public void update(Game model, BoardHighlights highlights) {
        this.model = model;
        this.highlights = highlights;
        Player currentPlayer = model.getCurrentPlayer();
        if (!gameOver) {
            boolean yourTurn = currentPlayer.getPlayerID() == 1;
            turnLabel.setText(yourTurn ? "Your turn" : "Enemy turn");
            turnLabel.getStyleClass().removeAll("turn-p1", "turn-p2");
            turnLabel.getStyleClass().add(yourTurn ? "turn-p1" : "turn-p2");
        }
        player1Card.update(model.getPlayerOne(), currentPlayer == model.getPlayerOne());
        player2Card.update(model.getPlayerTwo(), currentPlayer == model.getPlayerTwo());
        refreshDeployButtons();

        idleMotion.clear();
        unitNodes.clear();
        drawTerrain(model.getBattlefield());
        drawRangeOverlays();
        drawPlayerUnits(model, model.getPlayerOne());
        drawPlayerUnits(model, model.getPlayerTwo());
        drawTargetMarkers();

        if (highlights.selectedUnitId() != -1) {
            drawSelectionFrame(highlights.selectedUnitId());
        }
        refreshHover();
    }

    public void setRound(int round) {
        roundLabel.setText("ROUND " + round);
    }

    /** The one-line instruction under the board that tells the player what they can do right now. */
    public void setHint(String hint) {
        hintLabel.setText(hint);
    }

    /** Makes the end turn button glow when there is nothing useful left to do this turn. */
    public void setEndTurnSuggested(boolean suggested) {
        skipTurnButton.getStyleClass().remove("suggested");
        if (suggested) {
            skipTurnButton.getStyleClass().add("suggested");
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


    private void drawPlayerUnits(Game model, Player player) {
        boolean playersTurn = model.getCurrentPlayer() == player;
        // the base is one of the deck's units and is drawn with them
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            Unit unit = entry.getValue();
            boolean spent = playersTurn && !(unit instanceof Infrastructure) && !unit.isAvailable();
            drawUnitWithHealthBar(unit, player.getPlayerID(), entry.getKey(), spent);
        }
    }

    private void drawUnitWithHealthBar(Unit unit, int playerID, int unitId, boolean spent) {
        if (hiddenUnitIds.contains(unitId)) return;

        Point pos = unit.getPosition();
        StackPane cellPane = cellPanes.get(pos);
        if (cellPane == null) return;

        double healthPercentage = (double) unit.getHealthPoints() / unit.getMaxHealthPoints();
        UnitNode unitStack = createUnitNode(playerID, unit.getUnitName(), healthPercentage, UnitKind.of(unit));
        if (!unitStack.hasArt()) return;

        if (unit.getHealthPoints() <= 0) {
            unitStack.showWreck();
        } else if (spent) {
            unitStack.showSpent();
        }
        unitStack.setUserData(unitId);

        cellPane.getChildren().add(unitStack);
        unitNodes.put(unitId, unitStack);
        addIdleMotion(unitStack, unitId);
    }

    /** The one place that builds a unit sprite with its health bar, for the board and for animation ghosts. */
    private UnitNode createUnitNode(int playerID, String unitName, double healthRatio, UnitKind kind) {
        return new UnitNode(unitImage(playerID, unitName), playerID == 2, healthRatio, kind);
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
        frame.setStroke(SELECTION_COLOR);
        frame.setStrokeWidth(2.5);
        frame.setStrokeType(StrokeType.INSIDE);
        frame.setMouseTransparent(true);
        ((StackPane) selected.getParent()).getChildren().add(frame);
        idleMotion.pulse(frame);
    }

    /** Move and deploy tiles are filled, the shooting range is hatched underneath the units. */
    private void drawRangeOverlays() {
        for (Point cell : highlights.attackRange()) {
            addOverlay(cell, createDiagonalHatchOverlay(RANGE_HATCH));
        }
        for (Point cell : highlights.moveTiles()) {
            addOverlay(cell, createTileOverlay(MOVE_FILL, MOVE_EDGE));
        }
        for (Point cell : highlights.deployTiles()) {
            addOverlay(cell, createTileOverlay(DEPLOY_FILL, DEPLOY_EDGE));
        }
    }

    private void drawTargetMarkers() {
        for (Point cell : highlights.targets()) {
            Rectangle glow = new Rectangle(CELL_SIZE - 2, CELL_SIZE - 2, Color.rgb(255, 60, 60, 0.3));
            glow.setMouseTransparent(true);
            addOverlay(cell, glow);
            idleMotion.pulse(glow);
            addOverlay(cell, createTargetMarker(TARGET_COLOR));
        }
        for (Point cell : highlights.supportTargets()) {
            Rectangle glow = new Rectangle(CELL_SIZE - 2, CELL_SIZE - 2, Color.rgb(90, 235, 130, 0.28));
            glow.setMouseTransparent(true);
            addOverlay(cell, glow);
            idleMotion.pulse(glow);
            addOverlay(cell, createTargetMarker(SUPPORT_COLOR));
        }
    }

    private void addOverlay(Point cell, Node overlay) {
        StackPane cellPane = cellPanes.get(cell);
        if (cellPane != null) {
            cellPane.getChildren().add(overlay);
        }
    }

    private Node createTileOverlay(Color fill, Color edge) {
        Rectangle tile = new Rectangle(CELL_SIZE - 2, CELL_SIZE - 2);
        tile.setFill(fill);
        tile.setStroke(edge);
        tile.setStrokeWidth(1);
        tile.setStrokeType(StrokeType.INSIDE);
        tile.setArcWidth(6);
        tile.setArcHeight(6);
        tile.setMouseTransparent(true);
        return tile;
    }

    private Pane createDiagonalHatchOverlay(Color color) {
        Pane pane = new Pane();
        pane.setPrefSize(CELL_SIZE, CELL_SIZE);
        pane.setClip(new Rectangle(CELL_SIZE, CELL_SIZE)); // Ensures lines don't draw outside the bounds

        double strokeWidth = 2;
        int spacing = 8;

        for (int i = -CELL_SIZE; i < CELL_SIZE * 2; i += spacing) {
            Line line = new Line(i - strokeWidth, -strokeWidth, i + CELL_SIZE + strokeWidth, CELL_SIZE + strokeWidth);
            line.setStroke(color);
            line.setStrokeWidth(strokeWidth);
            pane.getChildren().add(line);
        }

        pane.setMouseTransparent(true);
        return pane;
    }

    /** Four corner brackets around a unit that the selected unit can attack (or heal). */
    private Node createTargetMarker(Color color) {
        double inset = 2.5;
        double arm = 9;
        double far = CELL_SIZE - inset;
        Path brackets = new Path(
                new MoveTo(inset, inset + arm), new LineTo(inset, inset), new LineTo(inset + arm, inset),
                new MoveTo(far - arm, inset), new LineTo(far, inset), new LineTo(far, inset + arm),
                new MoveTo(far, far - arm), new LineTo(far, far), new LineTo(far - arm, far),
                new MoveTo(inset + arm, far), new LineTo(inset, far), new LineTo(inset, far - arm));
        brackets.setStroke(color);
        brackets.setStrokeWidth(3);
        brackets.setMouseTransparent(true);
        return brackets;
    }

    // --- Side panel ---

    /** Takes everything that can be deployed; the panel shows one tab per category, a click (or number key) picks a card. */
    public void setDeployOptions(List<DeployOption> options, Consumer<UnitEnum> onPick) {
        onDeployPick = onPick;
        deployOptions.clear();
        for (DeployOption option : options) {
            deployOptions.put(option.type(), option);
        }

        deployTabs.getChildren().clear();
        deployTabButtons.clear();
        ToggleGroup group = new ToggleGroup();
        for (DeployOption.Category category : DeployOption.Category.values()) {
            if (options.stream().noneMatch(option -> option.category() == category)) continue;
            ToggleButton tab = new ToggleButton(category.label());
            tab.getStyleClass().add("deploy-tab");
            tab.setToggleGroup(group);
            tab.setFocusTraversable(false);
            tab.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tab, Priority.ALWAYS);
            tab.setOnAction(event -> showDeployCategory(category));
            deployTabs.getChildren().add(tab);
            deployTabButtons.put(category, tab);
        }
        showDeployCategory(deployTabButtons.containsKey(deployCategory) ? deployCategory
                : deployTabButtons.keySet().iterator().next());
    }

    /** The cards of the open tab, in the order of their number keys. */
    public List<DeployOption> getShownDeployOptions() {
        return deployOptions.values().stream().filter(option -> option.category() == deployCategory).toList();
    }

    private void showDeployCategory(DeployOption.Category category) {
        deployCategory = category;
        deployTabButtons.forEach((tabCategory, tab) -> tab.setSelected(tabCategory == category));

        deployCards.getChildren().clear();
        deployButtons.clear();
        int hotkey = 1;
        for (DeployOption option : getShownDeployOptions()) {
            Button button = createDeployCard(option, hotkey);
            deployCards.add(button, (hotkey - 1) % 2, (hotkey - 1) / 2);
            deployButtons.put(option.type(), button);
            hotkey++;
        }
        refreshDeployButtons();
    }

    /** Opens the next deploy tab (Tab key). */
    private void nextDeployCategory() {
        List<DeployOption.Category> categories = List.copyOf(deployTabButtons.keySet());
        if (categories.isEmpty()) return;
        showDeployCategory(categories.get((categories.indexOf(deployCategory) + 1) % categories.size()));
    }

    private Button createDeployCard(DeployOption option, int hotkey) {
        ImageView icon = new ImageView(unitImage(1, option.name()));
        icon.setFitWidth(28);
        icon.setFitHeight(28);
        icon.setPreserveRatio(true);
        icon.setSmooth(false);

        Label name = new Label(option.name());
        name.getStyleClass().add("deploy-name");
        name.setWrapText(true);
        Label cost = new Label(option.cost() + " RP");
        cost.getStyleClass().add("deploy-cost");
        cost.setMinWidth(Region.USE_PREF_SIZE);
        Label key = new Label(hotkey <= 9 ? String.valueOf(hotkey) : "");
        key.getStyleClass().add("deploy-key");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(4, cost, spacer, key);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(2, name, bottom);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox content = new HBox(6, icon, text);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.getStyleClass().add("deploy-card");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> onDeployPick.accept(option.type()));
        return button;
    }

    /** Marks the card of the unit type that is waiting to be placed, or none. */
    public void setArmedDeploy(UnitEnum type) {
        armedDeploy = type;
        refreshDeployButtons();
    }

    /** The types that cannot be deployed right now, with the reason; their cards are dimmed. */
    public void setBlockedDeploys(Map<UnitEnum, String> blocked) {
        blockedDeploys = Map.copyOf(blocked);
        refreshDeployButtons();
    }

    private void refreshDeployButtons() {
        boolean canAct = model != null && model.isPlayerOneTurn() && !inputLocked && !gameOver;
        for (Map.Entry<UnitEnum, Button> entry : deployButtons.entrySet()) {
            Button button = entry.getValue();
            DeployOption option = deployOptions.get(entry.getKey());
            boolean affordable = model != null && model.getPlayerOne().getResourcePoints() >= option.cost();
            String blocked = blockedDeploys.get(entry.getKey());
            // Locked cards stay enabled so their tooltip can say why; a click on one just logs the reason.
            button.setDisable(!canAct);
            button.getStyleClass().removeAll("armed-card", "locked-card");
            if (entry.getKey() == armedDeploy) {
                button.getStyleClass().add("armed-card");
            } else if (!affordable || blocked != null) {
                button.getStyleClass().add("locked-card");
            }
            StringBuilder tip = new StringBuilder(option.name()).append("  ·  ").append(option.cost()).append(" RP\n")
                    .append(option.stats());
            if (!option.description().isEmpty()) {
                tip.append("\n").append(option.description());
            }
            if (!affordable) {
                tip.append("\n\nNeeds ").append(option.cost()).append(" RP.");
            } else if (blocked != null) {
                tip.append("\n\nCannot deploy: ").append(blocked);
            }
            Tooltip tooltip = new Tooltip(tip.toString());
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(280);
            tooltip.setShowDelay(Duration.millis(250));
            button.setTooltip(tooltip);
        }
    }

    private void installHover() {
        grid.addEventHandler(MouseEvent.MOUSE_MOVED, event -> setHoveredCell(cellAt(event.getX(), event.getY())));
        grid.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setHoveredCell(cellAt(event.getX(), event.getY())));
        grid.addEventHandler(MouseEvent.MOUSE_EXITED, event -> setHoveredCell(null));
    }

    /** The cell under a point in the grid's own coordinates, or null outside the board. */
    public Point cellAt(double x, double y) {
        int col = (int) Math.floor(x / CELL_SIZE);
        int row = (int) Math.floor(y / CELL_SIZE);
        if (col < 0 || col >= gridWidth || row < 0 || row >= gridHeight) {
            return null;
        }
        return new Point(col, row);
    }

    private void setHoveredCell(Point cell) {
        if (Objects.equals(cell, hoveredCell)) return;
        hoveredCell = cell;
        refreshHover();
    }

    private void refreshHover() {
        hoverFrame.setVisible(hoveredCell != null);
        if (hoveredCell != null) {
            hoverFrame.setX(hoveredCell.x * CELL_SIZE);
            hoverFrame.setY(hoveredCell.y * CELL_SIZE);
        }
        boolean actionable = hoveredCell != null && !inputLocked && !gameOver && highlights.isActionTile(hoveredCell);
        grid.setCursor(actionable ? Cursor.HAND : Cursor.DEFAULT);
        refreshInspector();
    }

    /** Shows the hovered tile, or else the selected unit. */
    private void refreshInspector() {
        inspector.getChildren().clear();
        if (model == null) return;

        Unit unit = null;
        Point cell = hoveredCell;
        if (cell != null) {
            unit = model.getUnitAt(cell);
        } else if (highlights.selectedUnitId() != -1) {
            unit = model.findUnitById(highlights.selectedUnitId());
            cell = unit == null ? null : unit.getPosition();
        }

        if (unit != null) {
            inspectUnit(unit);
        } else if (cell != null) {
            inspectTerrain(model.getBattlefield().getGeographyOfPoint(cell));
        } else {
            Label placeholder = new Label("Hover over a tile or unit to see its details.");
            placeholder.getStyleClass().add("muted");
            placeholder.setWrapText(true);
            inspector.getChildren().add(placeholder);
        }

        if (cell != null && !inputLocked) {
            String action = null;
            if (highlights.targets().contains(cell)) {
                action = "Click to attack";
            } else if (highlights.supportTargets().contains(cell)) {
                action = "Click to heal / repair";
            } else if (highlights.moveTiles().contains(cell)) {
                action = "Click to move here";
            } else if (highlights.deployTiles().contains(cell)) {
                action = "Click to deploy here";
            }
            if (action != null) {
                Label actionLabel = new Label(action);
                actionLabel.getStyleClass().add("inspect-action");
                inspector.getChildren().add(actionLabel);
            }
        }
    }

    private void inspectUnit(Unit unit) {
        ImageView icon = new ImageView(unitImage(unit.getPlayerID(), unit.getUnitName()));
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);
        icon.setSmooth(false);
        if (unit.getPlayerID() == 2) {
            icon.setScaleX(-1);
        }

        Label name = new Label(unit.getUnitName());
        name.getStyleClass().add("inspect-name");
        boolean own = unit.getPlayerID() == 1;
        Label owner = new Label(own ? "Your unit" : "Enemy unit");
        owner.getStyleClass().add(own ? "owner-p1" : "owner-p2");
        VBox title = new VBox(1, name, owner);
        HBox top = new HBox(10, icon, title);
        top.setAlignment(Pos.CENTER_LEFT);

        int hp = Math.max(0, unit.getHealthPoints());
        ProgressBar hpBar = new ProgressBar((double) hp / unit.getMaxHealthPoints());
        hpBar.getStyleClass().addAll("stat-bar", "hp-bar");
        hpBar.setMaxWidth(Double.MAX_VALUE);
        Label hpText = new Label("HP " + hp + " / " + unit.getMaxHealthPoints());
        hpText.getStyleClass().add("stat-text");

        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.setVgap(2);
        int column = 0;
        if (unit instanceof AssaultClass assault) {
            addStat(stats, column++, "DMG", assault.getDamage());
            addStat(stats, column++, "RNG", model.getShootingRange(assault));
        }
        if (unit instanceof MovableUnit movable) {
            addStat(stats, column++, "MOV", movable.getMovingRange());
        }
        if (unit instanceof SupportClass support && support.getSupportPoints() > 0) {
            addStat(stats, column++, support.canSupport(model.getPlayerOne().getPlayerDeck().getBase()) ? "REPAIR" : "HEAL",
                    support.getSupportPoints());
        }
        if (unit instanceof Spotter spotter) {
            addStat(stats, column++, "SPOT", spotter.getSpottingRange());
        }
        if (unit instanceof AssaultTank tank) {
            addStat(stats, column++, "ARM", tank.getArmorValue());
        } else if (unit instanceof Infrastructure building) {
            addStat(stats, column++, "ARM", building.getArmorValue());
            if (building.getDeploymentRadius() > 0) {
                addStat(stats, column++, "DEPLOY", building.getDeploymentRadius());
            }
            if (building.getConstructionRadius() > 0) {
                addStat(stats, column, "BUILD", building.getConstructionRadius());
            }
        }

        inspector.getChildren().addAll(top, hpBar, hpText, stats);

        if (!(unit instanceof Infrastructure) && model.getCurrentPlayer().getPlayerID() == unit.getPlayerID()) {
            Label status = new Label(unit.isAvailable() ? "Ready to act" : "Already acted this turn");
            status.getStyleClass().add(unit.isAvailable() ? "status-ready" : "status-spent");
            inspector.getChildren().add(status);
        }
    }

    private static void addStat(GridPane stats, int column, String key, int value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("stat-key");
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("stat-value");
        stats.add(keyLabel, column, 0);
        stats.add(valueLabel, column, 1);
    }

    private void inspectTerrain(TerrainEnum terrain) {
        String name = switch (terrain) {
            case PLAIN -> "Plains";
            case WATER -> "Water";
            case MOUNTAIN -> "Mountains";
        };
        String access = switch (terrain) {
            case PLAIN -> "Open ground: vehicles and aircraft can enter.";
            case WATER -> "Only ships and aircraft can enter.";
            case MOUNTAIN -> "Impassable for vehicles; aircraft can fly over.";
        };
        Label title = new Label(name);
        title.getStyleClass().add("inspect-name");
        Label text = new Label(access);
        text.getStyleClass().add("muted");
        text.setWrapText(true);
        inspector.getChildren().addAll(title, text);
    }

    public Pane getRoot() { return root; }

    /** Called with every key the player presses, unless it fast-forwards an animation. */
    public void setKeyHandler(Consumer<KeyEvent> keyHandler) {
        this.keyHandler = keyHandler;
    }

    /** What the "Play again" button on the game over screen does. */
    public void setOnRestart(Runnable onRestart) {
        this.onRestart = onRestart;
    }

    public void showWinner(int winnerId) {
        gameOver = true;
        boolean won = winnerId == 1;
        turnLabel.setText(won ? "Victory" : "Defeat");
        turnLabel.getStyleClass().removeAll("turn-p1", "turn-p2");
        turnLabel.getStyleClass().add(won ? "turn-p1" : "turn-p2");
        skipTurnButton.setDisable(true);
        setEndTurnSuggested(false);
        refreshDeployButtons();
        refreshHover();
        infoArea.appendText("\nGame Over! Player " + winnerId + " has won.\n");

        Label title = new Label(won ? "VICTORY" : "DEFEAT");
        title.getStyleClass().addAll("game-over-title", won ? "victory" : "defeat");
        Label subtitle = new Label(won ? "The enemy base has been destroyed." : "Your base has fallen.");
        subtitle.getStyleClass().add("game-over-subtitle");
        Button again = new Button("Play again");
        again.getStyleClass().add("end-turn");
        again.setDisable(onRestart == null);
        again.setOnAction(event -> onRestart.run());
        VBox box = new VBox(12, title, subtitle, again);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("game-over");
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane overlay = new StackPane(box);
        overlay.getStyleClass().add("game-over-backdrop");
        boardArea.getChildren().add(overlay);
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
        refreshDeployButtons();
        refreshHover();
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

    private void installKeys() {
        EventHandler<KeyEvent> keyFilter = event -> {
            if (inputLocked && (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER)) {
                speedUp();
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                nextDeployCategory();
                event.consume();
            }
        };
        EventHandler<KeyEvent> keyForwarder = event -> keyHandler.accept(event);
        board.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (inputLocked && event.getClickCount() == 1) { // the second click of a double-click is not a skip
                speedUp();
                event.consume();
            }
        });
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, keyForwarder);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, keyForwarder);
            }
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

    /** A player's resources, base health and army size; the card of the player whose turn it is is highlighted. */
    private static final class PlayerCard extends VBox {
        private final Label units = new Label();
        private final ProgressBar resources = new ProgressBar();
        private final Label resourcesText = new Label();
        private final ProgressBar baseHealth = new ProgressBar();
        private final Label baseHealthText = new Label();
        private final String activeStyle;

        private PlayerCard(String name, String role, int playerId) {
            super(4);
            getStyleClass().addAll("card", "player-card");
            activeStyle = "active-p" + playerId;

            Label title = new Label(name);
            title.getStyleClass().addAll("player-name", "owner-p" + playerId);
            Label roleLabel = new Label(role);
            roleLabel.getStyleClass().add("muted");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox head = new HBox(4, title, spacer, roleLabel);

            resources.getStyleClass().addAll("stat-bar", "rp-bar");
            resources.setMaxWidth(Double.MAX_VALUE);
            resourcesText.getStyleClass().add("stat-text");
            baseHealth.getStyleClass().addAll("stat-bar", "hp-bar");
            baseHealth.setMaxWidth(Double.MAX_VALUE);
            baseHealthText.getStyleClass().add("stat-text");
            units.getStyleClass().add("stat-text");

            getChildren().addAll(head, resourcesText, resources, baseHealthText, baseHealth, units);
        }

        private void update(Player player, boolean active) {
            int rp = player.getResourcePoints();
            resources.setProgress(Math.min(1, (double) rp / MAX_RESOURCE_POINTS));
            resourcesText.setText("Resources  " + rp + " / " + MAX_RESOURCE_POINTS);
            Base base = player.getPlayerDeck().getBase();
            int hp = Math.max(0, base.getHealthPoints());
            baseHealth.setProgress((double) hp / base.getMaxHealthPoints());
            baseHealthText.setText("Base  " + hp + " HP");
            int buildings = (int) player.getPlayerDeck().getUnits().values().stream()
                    .filter(unit -> unit instanceof Infrastructure).count() - 1; // without the base
            int count = player.getPlayerDeck().getUnits().size() - 1 - buildings;
            units.setText((count == 1 ? "1 unit" : count + " units")
                    + (buildings == 0 ? "" : buildings == 1 ? ", 1 building" : ", " + buildings + " buildings"));
            getStyleClass().remove(activeStyle);
            if (active) {
                getStyleClass().add(activeStyle);
            }
        }
    }

    public void logInfo(String message) { infoArea.appendText(message + "\n"); }
    public GridPane getGrid() { return grid; }
    public Button getSkipTurnButton() { return skipTurnButton; }
}
