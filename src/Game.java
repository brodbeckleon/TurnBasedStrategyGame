import java.awt.*;
import java.util.Scanner;


public class Game {
    private boolean isRunning = true;
    public Game() {
        Battlefield battlefield = new Battlefield(10000, 10000,
                new Base(new Point(0, 0)),
                new Base(new Point(10000, 10000)));
    }
    public void run() {
        while (isRunning) {


        }
    }
    public void stop() {
        isRunning = false;
    }

}
