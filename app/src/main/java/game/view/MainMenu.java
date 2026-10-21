package game.view;

import game.game.map.MapSize;
import game.game.map.MapType;
import game.model.GameSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import game.game.map.Battlefield;
import game.tools.CircleGenerator;
import java.util.Random;

import java.util.function.Consumer;
import java.util.function.Function;

/** The start screen: pick the kind of map, how many bots to fight (everyone against everyone) and the map size. */
public class MainMenu {
    private static final int BACKDROP_TILES_X = 60;
    private static final int BACKDROP_TILES_Y = 36;
    private final StackPane root = new StackPane();
    private MapType mapType;
    private MapSize mapSize;
    private int enemies;

    public MainMenu(GameSettings initial, Consumer<GameSettings> onStart) {
        PixelFonts.load();
        mapType = initial.mapType();
        mapSize = initial.mapSize();
        enemies = initial.enemies();

        Label title = new Label("TURN-BASED STRATEGY");
        title.getStyleClass().add("menu-title");
        Label subtitle = new Label("Destroy every enemy base to win. Every bot fights everyone.");
        subtitle.getStyleClass().add("muted");

        Label typeDescription = new Label(mapType.description());
        typeDescription.getStyleClass().add("muted");
        typeDescription.setWrapText(true);
        FlowPane types = options(MapType.values(), mapType, MapType::label, type -> {
            mapType = type;
            typeDescription.setText(type.description());
        });
        FlowPane counts = options(new Integer[]{1, 2, 3, 4}, enemies,
                count -> count == 1 ? "1 enemy" : count + " enemies", count -> enemies = count);
        FlowPane sizes = options(MapSize.values(), mapSize, MapSize::toString, size -> mapSize = size);

        Button start = new Button("Start game");
        start.getStyleClass().add("end-turn");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setDefaultButton(true);
        start.setOnAction(event -> onStart.accept(new GameSettings(mapType, mapSize, enemies)));

        VBox panel = new VBox(10, title, subtitle,
                section("MAP TYPE"), types, typeDescription,
                section("ENEMIES"), counts,
                section("MAP SIZE"), sizes,
                start);
        VBox.setMargin(start, new Insets(12, 0, 0, 0));
        panel.getStyleClass().add("game-over");
        panel.setMaxWidth(560);
        panel.setMaxHeight(VBox.USE_PREF_SIZE);

        ScrollPane scroll = new ScrollPane(new StackPane(panel));
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("menu-scroll");
        Region shade = new Region();
        shade.getStyleClass().add("menu-shade");
        root.getChildren().addAll(backdrop(), shade, scroll);
        root.getStyleClass().addAll("game-root", "menu-root");
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    /**
     * A battlefield painted like the board, larger than the window and centred in it, so the menu looks out over
     * a map of the game.
     */
    private Node backdrop() {
        Battlefield field = new Battlefield(new CircleGenerator(), BACKDROP_TILES_X, BACKDROP_TILES_Y,
                MapType.values()[new Random().nextInt(MapType.values().length)], 2);
        int width = BACKDROP_TILES_X * MapPainter.TILE, height = BACKDROP_TILES_Y * MapPainter.TILE;
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(),
                MapPainter.paint(field, new Random().nextLong()), 0, width);
        ImageView view = new ImageView(image);
        view.setSmooth(false);
        view.setFitWidth(width * 2);
        view.setFitHeight(height * 2);
        view.setManaged(false);
        // keep it centred as the window changes size
        root.widthProperty().addListener((observable, was, now) -> view.setLayoutX((now.doubleValue() - width * 2) / 2));
        root.heightProperty().addListener((observable, was, now) -> view.setLayoutY((now.doubleValue() - height * 2) / 2));
        Pane holder = new Pane(view);
        holder.setMinSize(0, 0);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        holder.setClip(clip);
        return holder;
    }

    public Parent getRoot() {
        return root;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        VBox.setMargin(label, new Insets(6, 0, 0, 0));
        return label;
    }

    /** A row of toggle buttons of which exactly one is selected. */
    private static <T> FlowPane options(T[] values, T selected, Function<T, String> label, Consumer<T> onPick) {
        FlowPane row = new FlowPane(6, 6);
        ToggleGroup group = new ToggleGroup();
        for (T value : values) {
            ToggleButton button = new ToggleButton(label.apply(value));
            button.getStyleClass().addAll("deploy-tab", "menu-option");
            button.setToggleGroup(group);
            button.setFocusTraversable(false);
            button.setSelected(value.equals(selected));
            button.setOnAction(event -> {
                if (!button.isSelected()) {
                    button.setSelected(true); // one option always stays picked
                }
                onPick.accept(value);
            });
            row.getChildren().add(button);
        }
        return row;
    }
}
