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
}
