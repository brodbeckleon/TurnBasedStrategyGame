import Units.Unit;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Game {
    private final int maxResourcePoints = 10;
    private boolean isRunning = true;
    private final ConsoleIO consoleIO = new ConsoleIO();
    private final UnitFactory unitFactory = new UnitFactory();
    private Battlefield battlefield;
    private Player playerOne;
    private Player playerTwo;

    public Game() {
        battlefield = new Battlefield(new Point(16,16));
        playerOne = new Player(1, new Point(0,0));
        playerTwo = new Player(2, new Point(16,16));
    }

    public void run() {
        while (isRunning) {
            gameCycle(playerOne);
            attack(playerOne, playerTwo);
            gameCycle(playerTwo);
            attack(playerTwo, playerOne);
        }
    }

    private void stop() {
        isRunning = false;
    }
    public void gameCycle(Player player) {
        consoleIO.println("Player " + player.getPlayerID() + ": " + player.getResourcePoints() + "/" + maxResourcePoints);
        while (player.getResourcePoints() > 0) {
            consoleIO.print("Do you want to add a unit? (y/n): ");
            String answer = consoleIO.readString();

            if (answer.equals("y")) {
                addUnitToTable(player);
            } else if (answer.equals("n")) {
                break;
            } else {
                consoleIO.printError("Invalid input!");
            }
            consoleIO.println(player.getPlayerDeck().toString());
        }
        if (player.getResourcePoints() < maxResourcePoints) {
            player.addResourcePoints();
        }
    }

    private void addUnitToTable(Player player) {
        consoleIO.print("What unit do you want to add?");
        String unitName = consoleIO.readString();

        consoleIO.print("What position do you want to add the unit?");
        String positionString = consoleIO.readString();
        String[] positionStringArray = positionString.split(",");
        Point position = new Point(Integer.parseInt(positionStringArray[0]), Integer.parseInt(positionStringArray[1]));

        if (player.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize())) {

            Unit unit = unitFactory.createUnit(unitName, position);

            if (unit != null) {

                if (player.getResourcePoints() < unit.getResourceCost()) {
                    consoleIO.printError("Not enough resource points!");
                } else {
                    player.getPlayerDeck().addUnit(unit, position);
                    player.removeResourcePoints(unit.getResourceCost());
                }
            } else {
                consoleIO.printError("Invalid unit type!");
            }
        } else {
            consoleIO.printError("Position is not free!");
        }
    }

    private void attack(Player attacker, Player defender) {

    }

    private void scout(Unit unit) {

    }


    public HashMap<Point, Boolean> midpointCircleAlgorithm(Point center, int radius) {
        HashMap<Point, Boolean> circlePoints = new HashMap<>();
        int x = radius;
        int y = 0;
        int error = 0;

        while (x >= y) {
            circlePoints.put(new Point(center.x + x, center.y + y), true);
            circlePoints.put(new Point(center.x + y, center.y + x), true);
            circlePoints.put(new Point(center.x - y, center.y + x), true);
            circlePoints.put(new Point(center.x - x, center.y + y), true);
            circlePoints.put(new Point(center.x - x, center.y - y), true);
            circlePoints.put(new Point(center.x - y, center.y - x), true);
            circlePoints.put(new Point(center.x + y, center.y - x), true);
            circlePoints.put(new Point(center.x + x, center.y - y), true);

            if (error <= 0) {
                y += 1;
                error += 2*y + 1;
            }

            if (error > 0) {
                x -= 1;
                error -= 2*x + 1;
            }
        }

        return circlePoints;
    }
    private List<Point> getPointInRadius(Point center, int radius) {
        List<Point> points = new ArrayList<>();
        int startX = center.x - radius;
        int startY = center.y - radius;
        int endX = center.x + radius;
        int endY = center.y + radius;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (Math.sqrt(Math.pow(x - center.x, 2) + Math.pow(y - center.y, 2)) <= radius) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

}
