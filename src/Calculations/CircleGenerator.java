package Calculations;

import java.awt.Point;
import java.util.HashSet;

public class CircleGenerator {
    HashSet<Point> circlePoints = new HashSet<>();
    // Methode zum Zeichnen eines Kreises mit dem Midpoint Circle Algorithm
    public void drawThinCircle(Point centerPoint, int radius) {
        int centerX = centerPoint.x;
        int centerY = centerPoint.y;
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x; // Entscheidungsvariable über 2 berechnet

        // Wenn Radius 0 ist, einfach nur einen Punkt zeichnen
        if (radius > 0) {
            circlePoints.add(new Point((centerX - radius), centerY));
            circlePoints.add(new Point((centerX + radius), centerY));
            circlePoints.add(new Point(centerX, (centerY - radius)));
            circlePoints.add(new Point(centerX, (centerY + radius)));
        }

        // Zeichnen von Punkten aufgrund von Symmetrie in 8 Oktanten
        while (x > y) {
            y++;

            // Mid-Point ist innerhalb oder auf dem Kreis
            if (decisionOver2 <= 0) {
                decisionOver2 += 2 * y + 1;
            } else { // Mid-Point ist außerhalb des Kreises
                x--;
                decisionOver2 += 2 * (y - x) + 1;
            }

            // Alle Punkte haben Symmetrie in 8 Oktanten
            if (x < y) break; // Alle Oktanten sind gezeichnet
            circlePoints.add(new Point((centerX + x), (centerY - y)));
            circlePoints.add(new Point((centerX - x), (centerY - y)));
            circlePoints.add(new Point((centerX + x), (centerY + y)));
            circlePoints.add(new Point((centerX - x), (centerY + y)));

            // Zeichnen der Ecken
            if (x != y) {
                circlePoints.add(new Point((centerX + y), (centerY - x)));
                circlePoints.add(new Point((centerX - y), (centerY - x)));
                circlePoints.add(new Point((centerX + y), (centerY + x)));
                circlePoints.add(new Point((centerX - y), (centerY + x)));
            }
        }

    }
    private void fillCircle(HashSet<Point> circlePoints) {
        for (Point point : circlePoints) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    this.circlePoints.add(new Point(point.x + i, point.y + j));
                }
            }
        }
    }

    public HashSet<Point> getCirclePoints() {
        return circlePoints;
    }
}