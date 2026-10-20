package game;

import game.controller.GameController;
import game.model.Game;
import game.view.GameView;
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
        // 1. Set up the main stage (window); the game itself is put in by newGame()
        scene = new Scene(new javafx.scene.layout.Pane(), 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/styling/progress.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styling/game.css").toExternalForm());
        primaryStage.setTitle("Turn-Based Strategy");
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);

        // 2. Initialize the Model, View and Controller
        newGame();
        primaryStage.show();
    }

    /** Starts a fresh game in the window; the game over screen calls it again for a rematch. */
    private void newGame() {
        Game model = new Game();
        GameView view = new GameView(model.getBattlefield().getSize().x, model.getBattlefield().getSize().y);
        view.setOnRestart(this::newGame);
        new GameController(model, view);
        scene.setRoot(view.getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
