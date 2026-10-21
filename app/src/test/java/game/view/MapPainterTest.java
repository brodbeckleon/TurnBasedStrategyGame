package game.view;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.game.map.MapType;
import game.tools.CircleGenerator;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapPainterTest {

    private static final Set<Integer> WATER_COLOURS = Set.of(0xff1e7cb8, 0xff1c77b2, 0xff5aaee4, 0xff165c88, 0xffffffff, 0xff3b93d0);

    @Test
    void theMapIsOnePictureWithSixteenPixelsPerTile() {
        Battlefield field = new Battlefield(new CircleGenerator(), 13, 13, MapType.ARCHIPELAGO, 2);
        int[] pixels = MapPainter.paint(field, 7);

        assertEquals(13 * 16 * 13 * 16, pixels.length);
        assertTrue(Arrays.stream(pixels).allMatch(argb -> argb >>> 24 == 0xff), "fully opaque");
        assertArrayEquals(pixels, MapPainter.paint(field, 7), "the same seed paints the same map");
    }

    @Test
    void everyTileShowsItsOwnTerrainInItsMiddle() {
        for (MapType type : MapType.values()) {
            Battlefield field = new Battlefield(new CircleGenerator(), 16, 16, type, 2);
            int[] pixels = MapPainter.paint(field, 3);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int centre = pixels[(y * 16 + 8) * 16 * 16 + x * 16 + 8];
                    boolean water = field.getGeographyOfPoint(new Point(x, y)) == TerrainEnum.WATER;
                    if (water) {
                        assertTrue(WATER_COLOURS.contains(centre), type + ": water at " + x + ", " + y);
                    } else {
                        assertNotEquals(0xff1e7cb8, centre, type + ": land at " + x + ", " + y);
                    }
                }
            }
        }
    }

    @Test
    void theFogVeilsWhatIsNotSeenAndLeavesTheSeenTilesClear() {
        Point size = new Point(4, 4);
        int[] fog = MapPainter.paintFog(size, point -> point.x >= 2, 0x8c0c101e);

        assertEquals(0, fog[8 * 64 + 8], "a seen tile is clear in its middle");
        assertEquals(0x8c0c101e, fog[8 * 64 + 3 * 16 + 8], "a fogged tile is veiled");
        long edge = Arrays.stream(fog, 8 * 64 + 28, 8 * 64 + 36).filter(argb -> argb != 0).count();
        assertTrue(edge > 0 && edge < 8, "the edge between them is dithered");
    }

    @Test
    void everyHeadquartersAndRuinsStandInAYardOfEarth() {
        Set<Integer> yard = Set.of(0xffa08a63, 0xffb39c72, 0xff8a7553, 0xff7d6a4a, 0xff665540);
        Battlefield field = new Battlefield(new CircleGenerator(), 16, 16, MapType.CONTINENTS, 4);
        int[] pixels = MapPainter.paint(field, 5);
        java.util.List<Point> places = new java.util.ArrayList<>(field.getBasePositions());
        places.addAll(field.getBaseSites());
        for (Point place : places) {
            int x = place.x * 16 + 8, y = place.y * 16 + 4;
            assertTrue(yard.contains(pixels[y * 16 * 16 + x]), "a yard at " + place);
        }
    }

    @Test
    void aHarbourGetsACanalToTheSeaWhicheverSideTheSeaIsOn() {
        int[][] sides = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
        for (int[] side : sides) {
            Battlefield field = new Battlefield(new CircleGenerator(), 10, 10, MapType.CONTINENTS, 2);
            field.getGeography().replaceAll((point, terrain) -> TerrainEnum.PLAIN);
            field.getBaseSites().clear();
            Point harbour = new Point(5, 5);
            field.getBaseSites().add(harbour);
            for (int i = -5; i <= 5; i++) {
                for (int j = 1; j <= 4; j++) {
                    Point water = new Point(harbour.x + side[0] * j + (side[0] == 0 ? i : 0), harbour.y + side[1] * j + (side[1] == 0 ? i : 0));
                    if (field.getGeography().containsKey(water)) field.getGeography().put(water, TerrainEnum.WATER);
                }
            }
            int[] without = MapPainter.paint(field, 1);
            int[] with = MapPainter.paint(field, 1, Set.of(harbour));
            // under the quay of the harbour (the bottom of its tile) there is water now
            int quay = (harbour.y * 16 + 16) * 160 + harbour.x * 16 + 8;
            assertFalse(WATER_COLOURS.contains(without[quay]), "dry land before");
            assertTrue(WATER_COLOURS.contains(with[quay]), "a basin under the quay, sea to " + Arrays.toString(side));
        }
    }
}
