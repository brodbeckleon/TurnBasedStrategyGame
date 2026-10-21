package game.game.map;

import game.enums.TerrainEnum;
import game.tools.CircleGenerator;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapGeneratorTest {

    @Test
    void everyBaseRuinCanBeReachedByARecoveryVehicle() {
        for (int i = 0; i < 300; i++) {
            MapGenerator generator = new MapGenerator(new CircleGenerator(), 16, 16);
            HashMap<Point, TerrainEnum> map = generator.generate();
            Set<Point> sites = generator.generateBaseSites(map);
            Set<Point> land = landReachableFromHq(map);
            for (Point site : sites) {
                boolean reachable = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        reachable |= (dx != 0 || dy != 0) && land.contains(new Point(site.x + dx, site.y + dy));
                    }
                }
                assertTrue(reachable, "ruins at " + site + " cannot be reached on map " + i);
            }
        }
    }

    private static Set<Point> landReachableFromHq(HashMap<Point, TerrainEnum> map) {
        Set<Point> reached = new HashSet<>(Set.of(new Point(0, 0)));
        Deque<Point> queue = new ArrayDeque<>(reached);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Point next = new Point(point.x + dx, point.y + dy);
                    TerrainEnum terrain = map.get(next);
                    if ((terrain == TerrainEnum.PLAIN || terrain == TerrainEnum.FOREST) && reached.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return reached;
    }
}
