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

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize the Model and View
        Game model = new Game();
        GameView view = new GameView(model.getBattlefield().getSize().x, model.getBattlefield().getSize().y);

        // 2. Initialize the Controller with references to the Model and View
        new GameController(model, view);

        // 3. Set up the main stage (window)
        Scene scene = new Scene(view.getRoot(), 950, 700);
        scene.getStylesheets().add(getClass().getResource("/styling/progress.css").toExternalForm());
        primaryStage.setTitle("Battlefield Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}