package game.tools;

import java.awt.*;
import java.util.ArrayList;

public class CircleGenerator {
    public ArrayList<Point> getCoordinatesInRange(Point midpoint, int radius) {
        ArrayList<Point> coordinatesInRange = new ArrayList<>();
        int midX = midpoint.x;
        int midY = midpoint.y;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {

                double distanceSquared = x * x + y * y;

                if (distanceSquared <= (radius + 0.4) * (radius + 0.4)) {
                    coordinatesInRange.add(new Point(midX + x, midY + y));
                }
            }
        }

        return coordinatesInRange;
    }
}
