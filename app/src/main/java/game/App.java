package game;

import game.controller.GameController;
import game.model.Game;
import game.model.GameSettings;
import game.view.GameView;
import game.view.MainMenu;
import game.view.PixelFonts;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class to launch the JavaFX game.
 * Sets up the Model, View, and Controller.
 */
public class App extends Application {

    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        PixelFonts.load();
        // 1. Set up the main stage (window); the game itself is put in by newGame()
        scene = new Scene(new javafx.scene.layout.Pane(), 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/styling/progress.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styling/game.css").toExternalForm());
        primaryStage.setTitle("Turn-Based Strategy");
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);

        // 2. The main menu sets up the game
        showMenu(GameSettings.standard());
        primaryStage.show();
    }

    /** The start screen, with the settings of the last game picked. */
    private void showMenu(GameSettings settings) {
        scene.setRoot(new MainMenu(settings, this::newGame).getRoot());
    }

    /** Starts a fresh game in the window; the game over screen calls it again for a rematch. */
    private void newGame(GameSettings settings) {
        Game model = new Game(settings);
        GameView view = new GameView(model.getBattlefield().getSize().x, model.getBattlefield().getSize().y);
        GameController controller = new GameController(model, view);
        view.setOnRestart(() -> {
            controller.stop();
            newGame(settings);
        });
        view.setOnMainMenu(() -> {
            controller.stop();
            showMenu(settings);
        });
        scene.setRoot(view.getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
