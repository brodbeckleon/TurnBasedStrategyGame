package game.view;

import game.game.Ability;
import game.game.Player;
import game.game.Research;
import game.model.Game;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The research tree, over the whole game: every branch a band that grows from its root on the left, each project
 * a card joined to the one it needs by a line. Done projects light up in their branch's colour, the ones that can
 * be researched right now pulse, locked ones stay dim. A click picks a card and shows it on the right, where the
 * research button is; a double click researches it straight away.
 */
final class ResearchScreen extends StackPane {
    private static final double NODE_W = 170;
    private static final double NODE_H = 40;
    private static final double COLUMN_GAP = 26;
    private static final double ROW_H = 48;
    private static final double BAND_GAP = 12;
    private static final double LABEL_W = 86;
    private static final double PAD = 10;

    private static final Map<Research.Branch, Color> BRANCH_COLORS = new EnumMap<>(Map.of(
            Research.Branch.LOGISTICS, Color.rgb(245, 185, 66),
            Research.Branch.GROUND, Color.rgb(111, 220, 140),
            Research.Branch.AIR, Color.rgb(121, 168, 255),
            Research.Branch.NAVY, Color.rgb(79, 209, 197),
            Research.Branch.COMMAND, Color.rgb(255, 123, 114)));
    private static final Color LINE_LOCKED = Color.rgb(74, 82, 114);
    private static final Color LINE_OPEN = Color.rgb(200, 205, 225);

    private final Function<String, Image> unitImage;
    private final Consumer<Research> onResearch;
    private final Function<Research, String> refusal;
    private final Runnable onClose;
    private final Map<Research, String> details;
    private final List<Timeline> pulses = new ArrayList<>();
    private Research selected;
    private Game lastModel;
    private boolean lastCanAct;

    /**
     * @param unitImage  the picture of a unit by name, for the icons
     * @param onResearch starts a project
     * @param refusal    why a project cannot be started now, or null if it can
     * @param details    an extra line per project (the stats of a unit it unlocks), may be missing
     */
    ResearchScreen(Function<String, Image> unitImage, Consumer<Research> onResearch, Function<Research, String> refusal,
                   Map<Research, String> details, Runnable onClose) {
        this.unitImage = unitImage;
        this.onResearch = onResearch;
        this.refusal = refusal;
        this.details = details;
        this.onClose = onClose;
        getStyleClass().addAll("game-over-backdrop", "research-screen");
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) onClose.run(); // a click beside the panel closes it
        });
    }

    /** Builds the screen (again) for the player's state; {@code canAct} says whether it may research right now. */
    void show(Game model, boolean canAct) {
        stop();
        lastModel = model;
        lastCanAct = canAct;
        Player you = model.getPlayerOne();
        if (selected == null || you.hasResearched(selected)) {
            selected = firstSuggestion(you);
        }
        BorderPane panel = new BorderPane();
        panel.getStyleClass().add("research-panel");
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setTop(header(model, you));
        ScrollPane scroll = new ScrollPane(tree(you, canAct));
        scroll.getStyleClass().add("research-scroll");
        scroll.setFitToHeight(true);
        scroll.setPrefViewportWidth(treeWidth());
        scroll.setPrefViewportHeight(Math.min(treeHeight(), 610));
        scroll.setFocusTraversable(false);
        panel.setCenter(scroll);
        panel.setRight(detailPanel(you, canAct));
        getChildren().setAll(panel);
    }

    /** Stops the pulsing of the cards (the screen is closed or rebuilt). */
    void stop() {
        pulses.forEach(Timeline::stop);
        pulses.clear();
    }

    private Research firstSuggestion(Player you) {
        for (Research research : Research.values()) {
            if (you.canResearch(research)) return research;
        }
        return Research.values()[0];
    }

    // --- Header ---

    private Node header(Game model, Player you) {
        Label title = new Label("RESEARCH");
        title.getStyleClass().addAll("game-over-title", "dialog-title");
        long done = java.util.Arrays.stream(Research.values()).filter(you::hasResearched).count();
        ProgressBar progress = new ProgressBar(done / (double) Research.values().length);
        progress.getStyleClass().addAll("stat-bar", "rp-bar");
        progress.setPrefWidth(160);
        Label count = new Label(done + " / " + Research.values().length + " projects");
        count.getStyleClass().add("stat-text");
        Label rp = new Label("RP " + you.getResourcePoints() + "  (+" + model.getIncome(you) + "/round)");
        rp.getStyleClass().add("research-rp");
        Label turn = new Label(you.hasResearchedThisTurn() ? "This turn's project is done" : "One project per turn");
        turn.getStyleClass().add(you.hasResearchedThisTurn() ? "status-spent" : "status-ready");
        Button close = new Button("Close  [R]");
        close.getStyleClass().add("small-button");
        close.setFocusTraversable(false);
        close.setOnAction(event -> onClose.run());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox status = new HBox(14, rp, turn);
        status.setAlignment(Pos.CENTER_LEFT);
        HBox bar = new HBox(8, progress, count);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox head = new HBox(18, title, new VBox(4, status, bar), spacer, close);
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(0, 0, 12, 0));
        return head;
    }

    // --- The tree ---

    /** Where each project sits: its depth in its branch (column) and its row on the whole screen. */
    private record Place(int column, double row) {
    }

    private Map<Research, Place> treeLayout() {
        Map<Research, Place> places = new EnumMap<>(Research.class);
        double[] nextRow = {0};
        for (Research.Branch branch : Research.Branch.values()) {
            placeSubtree(branch.root(), 0, nextRow, places);
            nextRow[0] += BAND_GAP / ROW_H;
        }
        return places;
    }

    /** Leaves get the next free row, a parent sits level with its first child, so its line runs straight on. */
    private double placeSubtree(Research research, int depth, double[] nextRow, Map<Research, Place> places) {
        List<Research> children = research.followUps();
        double row;
        if (children.isEmpty()) {
            row = nextRow[0]++;
        } else {
            row = placeSubtree(children.get(0), depth + 1, nextRow, places);
            for (Research child : children.subList(1, children.size())) {
                placeSubtree(child, depth + 1, nextRow, places);
            }
        }
        places.put(research, new Place(depth, row));
        return row;
    }

    private double treeWidth() {
        int columns = 0;
        for (Place place : treeLayout().values()) columns = Math.max(columns, place.column() + 1);
        return LABEL_W + columns * NODE_W + (columns - 1) * COLUMN_GAP + 2 * PAD;
    }

    private double treeHeight() {
        double rows = 0;
        for (Place place : treeLayout().values()) rows = Math.max(rows, place.row() + 1);
        return rows * ROW_H + 2 * PAD;
    }

    private Pane tree(Player you, boolean canAct) {
        Map<Research, Place> places = treeLayout();
        Pane pane = new Pane();
        pane.setPrefSize(treeWidth(), treeHeight());
        pane.getStyleClass().add("research-tree");

        // the bands behind each branch, with its name and progress
        for (Research.Branch branch : Research.Branch.values()) {
            double top = Double.MAX_VALUE, bottom = 0;
            int done = 0, total = 0;
            for (Research research : Research.values()) {
                if (research.branch() != branch) continue;
                Place place = places.get(research);
                top = Math.min(top, y(place));
                bottom = Math.max(bottom, y(place) + NODE_H);
                total++;
                if (you.hasResearched(research)) done++;
            }
            Color color = BRANCH_COLORS.get(branch);
            Rectangle band = new Rectangle(PAD, top - 5, treeWidth() - 2 * PAD, bottom - top + 10);
            band.setFill(color.deriveColor(0, 1, 0.35, 0.18));
            band.setStroke(color.deriveColor(0, 1, 1, 0.35));
            band.setArcWidth(4);
            band.setArcHeight(4);
            Label name = new Label(branch.label().toUpperCase());
            name.getStyleClass().add("research-branch");
            name.setTextFill(color);
            Label progress = new Label(done + "/" + total);
            progress.getStyleClass().add("stat-text");
            VBox label = new VBox(2, name, progress);
            label.setLayoutX(PAD + 8);
            label.setLayoutY(top + 2);
            pane.getChildren().addAll(band, label);
        }

        // the lines first, so the cards lie on top of them
        for (Research research : Research.values()) {
            Research parent = research.prerequisite();
            if (parent != null) {
                pane.getChildren().add(connector(places.get(parent), places.get(research), research, you));
            }
        }
        for (Research research : Research.values()) {
            Node card = card(research, you, canAct);
            card.setLayoutX(x(places.get(research)));
            card.setLayoutY(y(places.get(research)));
            pane.getChildren().add(card);
        }
        return pane;
    }

    private static double x(Place place) {
        return PAD + LABEL_W + place.column() * (NODE_W + COLUMN_GAP);
    }

    private static double y(Place place) {
        return PAD + place.row() * ROW_H;
    }

    /** An elbow from the right edge of the parent to the left edge of the child, coloured by how far research got. */
    private Path connector(Place from, Place to, Research child, Player you) {
        double startX = x(from) + NODE_W, startY = y(from) + NODE_H / 2;
        double endX = x(to), endY = y(to) + NODE_H / 2;
        double bend = startX + COLUMN_GAP / 2;
        Path path = new Path(new MoveTo(startX, startY), new LineTo(bend, startY), new LineTo(bend, endY),
                new LineTo(endX, endY));
        if (you.hasResearched(child)) {
            path.setStroke(BRANCH_COLORS.get(child.branch()));
            path.setStrokeWidth(3);
        } else if (you.canResearch(child)) {
            path.setStroke(LINE_OPEN);
            path.setStrokeWidth(2);
        } else {
            path.setStroke(LINE_LOCKED);
            path.setStrokeWidth(2);
            path.getStrokeDashArray().addAll(4.0, 4.0);
        }
        path.setMouseTransparent(true);
        return path;
    }

    private Node card(Research research, Player you, boolean canAct) {
        boolean done = you.hasResearched(research);
        boolean open = you.canResearch(research);
        boolean ready = open && canAct && refusal.apply(research) == null;
        Color color = BRANCH_COLORS.get(research.branch());

        Label name = new Label(research.label());
        name.getStyleClass().add("research-node-name");
        Label cost = new Label(done ? "DONE" : research.cost() + " RP");
        cost.getStyleClass().add(done ? "research-done-tag"
                : open && you.getResourcePoints() < research.cost() ? "research-cost-short" : "deploy-cost");
        VBox text = new VBox(1, name, cost);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox content = new HBox(5, icon(research, 24), text);
        content.setAlignment(Pos.CENTER_LEFT);

        StackPane card = new StackPane(content);
        card.setPrefSize(NODE_W, NODE_H);
        card.setMaxSize(NODE_W, NODE_H);
        card.setPadding(new Insets(3, 4, 3, 4));
        card.getStyleClass().add("research-node");
        String border = done ? toCss(color) : open ? "#c8cde1" : "#3a4058";
        String fill = done ? toCss(color.deriveColor(0, 1, 0.3, 1)) : "#23283a";
        // the picked card gets a thick light frame around its own
        card.setStyle(research == selected
                ? "-fx-background-color: #fff0b8, #0e0b16, " + border + ", " + fill + ";"
                  + "-fx-background-insets: -3, -1, 0, 2;"
                : "-fx-background-color: #0e0b16, " + border + ", " + fill + ";"
                  + "-fx-background-insets: 0, 1, 2;");
        if (!done && !open) {
            card.setOpacity(0.5);
        }
        if (research == selected) {
            card.getStyleClass().add("research-selected");
        }
        if (ready) {
            DropShadow glow = new DropShadow(6, color);
            glow.setSpread(0.35);
            card.setEffect(glow);
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 4)),
                    new KeyFrame(Duration.millis(700), new KeyValue(glow.radiusProperty(), 14)));
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
            pulses.add(pulse);
        }
        card.setOnMouseClicked(event -> {
            selected = research;
            if (event.getClickCount() >= 2 && ready) {
                onResearch.accept(research);
            } else {
                rebuild();
            }
            event.consume();
        });
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    /** The picture of what the project is about: a unit it unlocks or improves, or the missile for the strikes. */
    private Node icon(Research research, double size) {
        Image image = research == Research.CRUISE_MISSILE ? new game.view.anim.EffectSprites().missile()
                : unitImage.apply(iconUnit(research));
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        StackPane box = new StackPane(view);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    private static String iconUnit(Research research) {
        return switch (research) {
            case SUPPLY_LINES -> "Armored Recovery Vehicle";
            case FIELD_WORKSHOPS -> "Armored Ambulance";
            case INDUSTRIAL_EXPANSION -> "Base";
            case RAPID_DEPLOYMENT -> "Type 16";
            case MASS_MOBILISATION -> "Type 89";
            case FORWARD_COMMAND -> "Army Base";
            case BOOT_CAMP -> "Anti-Tank Team";
            case COMPOSITE_ARMOUR -> "Type 90";
            case THERMAL_SIGHTS -> "Armored Reconnaissance Vehicle";
            case AEGIS_UPGRADE -> "Type 81 (C)";
            case PRECISION_MUNITIONS -> "F-2";
            case STEALTH_PROGRAM -> "F-35";
            case IMPROVED_SONAR -> "Kongo";
            case CARRIER_AVIATION -> "Izumo";
            case BATTLESHIPS -> "Yamato";
            case AMPHIBIOUS_DOCTRINE -> "Osumi";
            case RADAR_SWEEP -> "Radar Station";
            case ARTILLERY_BARRAGE -> "Type 99 SPH";
            case AIRSTRIKE -> "A-10";
            case CRUISE_MISSILE -> "Kongo";
            case PARATROOPERS -> "Infantry";
        };
    }

    // --- The picked project ---

    private Node detailPanel(Player you, boolean canAct) {
        Research research = selected;
        Color color = BRANCH_COLORS.get(research.branch());
        Label branch = new Label(research.branch().label().toUpperCase());
        branch.getStyleClass().add("research-branch");
        branch.setTextFill(color);
        Label name = new Label(research.label());
        name.getStyleClass().add("inspect-name");
        name.setWrapText(true);
        Node icon = icon(research, 56);
        HBox top = new HBox(10, icon, new VBox(2, branch, name));
        top.setAlignment(Pos.CENTER_LEFT);

        Label description = new Label(research.description());
        description.getStyleClass().add("research-description");
        description.setWrapText(true);
        description.setMinHeight(Region.USE_PREF_SIZE);
        VBox box = new VBox(8, top, description);

        String extra = extraLine(research);
        if (extra != null) {
            Label more = new Label(extra);
            more.getStyleClass().add("muted");
            more.setWrapText(true);
            more.setMinHeight(Region.USE_PREF_SIZE);
            box.getChildren().add(more);
        }
        Research before = research.prerequisite();
        Label needs = new Label("Needs: " + (before == null ? "nothing, a branch root" : before.label()
                + (you.hasResearched(before) ? "  (done)" : "")));
        needs.getStyleClass().add(before == null || you.hasResearched(before) ? "muted" : "preview-bad");
        needs.setWrapText(true);
        needs.setMinHeight(Region.USE_PREF_SIZE);
        List<Research> next = research.followUps();
        Label leadsTo = new Label("Leads to: " + (next.isEmpty() ? "the end of the branch"
                : String.join(", ", next.stream().map(Research::label).toList())));
        leadsTo.getStyleClass().add("muted");
        leadsTo.setWrapText(true);
        leadsTo.setMinHeight(Region.USE_PREF_SIZE);
        box.getChildren().addAll(needs, leadsTo);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);
        boolean done = you.hasResearched(research);
        String why = done ? null : refusal.apply(research);
        if (done) {
            Label doneLabel = new Label("Researched");
            doneLabel.getStyleClass().add("status-ready");
            box.getChildren().add(doneLabel);
        } else {
            if (why != null) {
                Label reason = new Label(why);
                reason.getStyleClass().add("status-spent");
                reason.setWrapText(true);
                reason.setMinHeight(Region.USE_PREF_SIZE);
                box.getChildren().add(reason);
            }
            Button go = new Button("Research " + research.cost() + " RP");
            go.getStyleClass().add("end-turn");
            go.setMaxWidth(Double.MAX_VALUE);
            go.setFocusTraversable(false);
            go.setDisable(!canAct || why != null);
            go.setOnAction(event -> onResearch.accept(research));
            box.getChildren().add(go);
        }
        box.getStyleClass().add("research-details");
        box.setPrefWidth(250);
        box.setMinWidth(250);
        box.setMaxWidth(250);
        BorderPane.setMargin(box, new Insets(0, 0, 0, 12));
        return box;
    }

    /** What else to know: the ability a Command project unlocks, or the stats of a unit an Arsenal project unlocks. */
    private String extraLine(Research research) {
        for (Ability ability : Ability.values()) {
            if (ability.research() == research) {
                return ability.description() + "  Costs " + ability.cost() + " RP, then ready again after "
                        + ability.cooldown() + (ability.cooldown() == 1 ? " turn." : " turns.");
            }
        }
        return details.get(research);
    }

    /** Draws the screen again for the state it was last shown for (a card was picked). */
    private void rebuild() {
        show(lastModel, lastCanAct);
    }

    private static String toCss(Color color) {
        return String.format("#%02x%02x%02x", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }
}
