package game.tools;

import java.awt.*;
import java.util.ArrayList;

public class CircleGenerator {
    public ArrayList<Point> getCoordinatesInRange(Point midpoint, int radius){
        ArrayList<Point> coordinatesInRange = new ArrayList<>();
        int midX = midpoint.x;
        int midY = midpoint.y;

        for (int y = 0; y <= radius * 2; y++) {
            for (int x = 0; x <= radius * 2; x++) {
                int deltaX = radius - x;
                int deltaY = radius - y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                // Point lies outside of the circle
                if (distance - radius > 1) {
                    continue;
                }

                // Edge threshold
                if ((double) radius / distance < 0.9) {
                    continue;
                }

                coordinatesInRange.add(new Point(midX - radius + x, midY - radius + y));
            }
        }
        return coordinatesInRange;
    }
}
