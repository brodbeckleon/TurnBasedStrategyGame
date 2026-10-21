package game.view;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Paints the whole battlefield as one pixel-art picture, so the terrain flows from tile to tile: coasts are
 * rounded and ringed with surf, grass varies in patches, forests grow together into woods and mountains into
 * ridges. Every tile still shows its own terrain in its middle, where the units stand.
 *
 * <p>The picture has {@link #TILE} pixels per tile in the palette of the unit and terrain art; the board shows it
 * at twice that size. It is plain ARGB data, so it can be painted (and tested) without a screen.
 */
public final class MapPainter {
    /** Pixels per tile. */
    public static final int TILE = 16;

    private static final int GRASS = 0, WATER = 1, FOREST = 2, MOUNTAIN = 3, BUILT = 4; // BUILT: roads and yards

    // the palette of the terrain tiles and unit sprites
    private static final int GRASS_BASE = 0xff3abe41, GRASS_LIGHT = 0xff3fc445, GRASS_DARK = 0xff36b53e;
    private static final int BLADE_LIGHT = 0xff6add4b, BLADE_DARK = 0xff29973b, SHORE_GRASS = 0xff328e41;
    private static final int WOODLAND = 0xff2f7a3a;
    private static final int ROCK_GROUND = 0xff866a4c, ROCK_SPECKLE = 0xff9c8062, ROCK_PIT = 0xff6b523f;
    private static final int SEA = 0xff1e7cb8, SEA_DEEP = 0xff1c77b2, SEA_LIGHT = 0xff5aaee4, SEA_SHADOW = 0xff165c88;
    private static final int SURF = 0xffffffff, SPARKLE = 0xff3b93d0;
    private static final int FLOWER_A = 0xfff2e86d, FLOWER_B = 0xffffffff;
    // packed earth and gravel of the yards around the bases, the tracks between them and the wooden jetties
    private static final int YARD = 0xffa08a63, YARD_LIGHT = 0xffb39c72, YARD_DARK = 0xff8a7553, YARD_EDGE = 0xff7d6a4a;
    private static final int YARD_SHADOW = 0xff665540, TRACK = 0xffa8916a, TRACK_EDGE = 0xff86714f;
    private static final int PLANK = 0xff8a6a44, PLANK_DARK = 0xff5e4630, PLANK_LIGHT = 0xffa7835a;

    private static final String[] PINE = {
            "...oo...",
            "..oLLo..",
            "..oLGo..",
            ".oLGGDo.",
            "..oGDo..",
            ".oLGGDo.",
            "oLLGGDDo",
            ".oGGDDo.",
            "oLGGGDDo",
            "ooGGDDDo",
            ".oootooo",
            "...tT...",
    };
    private static final String[] BROADLEAF = {
            "..oooo..",
            ".oLLGGo.",
            "oLLGGGDo",
            "oLGGGDDo",
            "oGGGDDDo",
            ".oGDDDo.",
            "..ootoo.",
            "...tT...",
    };
    private static final String[] BIG_PEAK = {
            ".........o.........",
            "........oSo........",
            ".......oSSDo.......",
            "......oSSSDDo......",
            ".....oLSSDDDDo.....",
            "....oLLLSDDDDDo....",
            "...oLLLLLDDDDDDo...",
            "..oLLLLlLDDDDDDDo..",
            ".oLLLLlllDDDDDDDDo.",
            "oLLLllllDDDDDDDDDDo",
            "oLLlllllDDDDDDDDDDo",
            "ooooooooooooooooooo",
    };
    private static final String[] PEAK = {
            "......o......",
            ".....oSo.....",
            "....oSSDo....",
            "...oLSDDDo...",
            "..oLLLDDDDo..",
            ".oLLLLDDDDDo.",
            "oLLLLLDDDDDDo",
            "oLLLLDDDDDDDo",
            "ooooooooooooo",
    };
    private static final String[] HILL = {
            "...oooo...",
            "..oLLLDo..",
            ".oLLLDDDo.",
            "oLLLDDDDDo",
            "oooooooooo",
    };
    private static final int[] TREE_COLORS = {0xff201829, 0xff4f9e44, 0xff2f7a3a, 0xff225c34, 0xff75553a, 0xff4b3626};
    private static final String TREE_KEYS = "oLGDtT";
    private static final int[] ROCK_COLORS = {0xff331f29, 0xffbc9a78, 0xff94785c, 0xff79584f, 0xffe8eef2};
    private static final String ROCK_KEYS = "oLlDS";

    private final Battlefield battlefield;
    private final int tilesX, tilesY, width, height;
    private final long seed;
    private final int[] kind;
    private final int[] pixels;

    private MapPainter(Battlefield battlefield, long seed) {
        this.battlefield = battlefield;
        this.seed = seed;
        Point size = battlefield.getSize();
        tilesX = size.x;
        tilesY = size.y;
        width = tilesX * TILE;
        height = tilesY * TILE;
        kind = new int[width * height];
        pixels = new int[width * height];
    }

    /** The battlefield as ARGB pixels, {@code TILE} per tile in both directions, row by row. */
    public static int[] paint(Battlefield battlefield, long seed) {
        MapPainter painter = new MapPainter(battlefield, seed);
        painter.shape();
        painter.colour();
        painter.settle();
        painter.decorate();
        return painter.pixels;
    }

    /**
     * The fog of war over the tiles that are not seen: a dark veil with a soft, dithered edge that follows the
     * same rounded shapes as the terrain. Seen tiles stay clear in their middle.
     */
    public static int[] paintFog(Point size, java.util.function.Predicate<Point> fogged, int argb) {
        int width = size.x * TILE, height = size.y * TILE;
        int[] pixels = new int[width * height];
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                double x = (px + 0.5) / TILE - 0.5, y = (py + 0.5) / TILE - 0.5;
                int i = (int) Math.floor(x), j = (int) Math.floor(y);
                double fx = x - i, fy = y - j;
                double top = lerp(fog(fogged, size, i, j), fog(fogged, size, i + 1, j), fx);
                double bottom = lerp(fog(fogged, size, i, j + 1), fog(fogged, size, i + 1, j + 1), fx);
                double amount = lerp(top, bottom, fy);
                // a band of checkerboard pixels softens the edge the pixel-art way
                boolean veiled = amount > 0.62 || amount > 0.38 && (px + py) % 2 == 0;
                pixels[py * width + px] = veiled ? argb : 0;
            }
        }
        return pixels;
    }

    private static double fog(java.util.function.Predicate<Point> fogged, Point size, int x, int y) {
        int cx = Math.max(0, Math.min(size.x - 1, x)), cy = Math.max(0, Math.min(size.y - 1, y));
        return fogged.test(new Point(cx, cy)) ? 1 : 0;
    }

    // --- Where each terrain is ---

    /**
     * Decides the terrain of every pixel: each terrain's share blends smoothly between the tile centres, and a
     * soft noise bends the borders, so coasts and forest edges are round and irregular instead of square.
     */
    private void shape() {
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                double u = (px + 0.5) / TILE, v = (py + 0.5) / TILE;
                int k;
                if (blend(u, v, TerrainEnum.WATER) + 0.22 * wobble(px, py, 7, 1) > 0.5) {
                    k = WATER;
                } else if (blend(u, v, TerrainEnum.MOUNTAIN) + 0.25 * wobble(px, py, 6, 2) > 0.5) {
                    k = MOUNTAIN;
                } else if (blend(u, v, TerrainEnum.FOREST) + 0.28 * wobble(px, py, 5, 3) > 0.5) {
                    k = FOREST;
                } else {
                    k = GRASS;
                }
                kind[py * width + px] = k;
            }
        }
    }

    /** How much of the terrain there is at the point, between 0 and 1, blended over the four nearest tile centres. */
    private double blend(double u, double v, TerrainEnum terrain) {
        double x = u - 0.5, y = v - 0.5;
        int i = (int) Math.floor(x), j = (int) Math.floor(y);
        double fx = x - i, fy = y - j;
        double top = lerp(is(i, j, terrain), is(i + 1, j, terrain), fx);
        double bottom = lerp(is(i, j + 1, terrain), is(i + 1, j + 1, terrain), fx);
        return lerp(top, bottom, fy);
    }

    private double is(int x, int y, TerrainEnum terrain) {
        int cx = Math.max(0, Math.min(tilesX - 1, x)), cy = Math.max(0, Math.min(tilesY - 1, y));
        return battlefield.getGeographyOfPoint(new Point(cx, cy)) == terrain ? 1 : 0;
    }

    // --- Colours ---

    private void colour() {
        int[] toWater = distance(WATER);
        int[] toLand = distanceToLand();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int i = py * width + px;
                pixels[i] = kind[i] == WATER ? water(px, py, toLand[i]) : land(px, py, kind[i], toWater[i]);
            }
        }
    }

    private int water(int px, int py, int toLand) {
        if (toLand == 1) return SURF;
        // the shore throws a shadow on the water south of it
        if (toLand <= 3 && isLandAt(px, py - toLand)) return SEA_SHADOW;
        if (toLand == 2 && hash(px, py, 11) % 3 != 0) return SEA_LIGHT;
        if (toLand >= 7 && wobble(px, py, 13, 4) > 0.35) return SEA_DEEP;
        // glints on the open water: short horizontal dashes
        int dash = hash(px / 3, py, 12);
        if (toLand > 3 && dash % 37 == 0) return SPARKLE;
        return SEA;
    }

    private int land(int px, int py, int k, int toWater) {
        if (toWater == 1) return SHORE_GRASS;
        if (k == FOREST) return WOODLAND;
        if (k == MOUNTAIN) {
            int roll = hash(px, py, 6) % 9;
            return roll == 0 ? ROCK_SPECKLE : roll == 1 ? ROCK_PIT : ROCK_GROUND;
        }
        double patch = wobble(px, py, 14, 5);
        return patch > 0.35 ? GRASS_LIGHT : patch < -0.35 ? GRASS_DARK : GRASS_BASE;
    }

    private boolean isLandAt(int px, int py) {
        return py >= 0 && kind[py * width + px] != WATER;
    }

    /** For every pixel, how many steps (in eight directions) it is to the nearest pixel of the terrain, up to 9. */
    private int[] distance(int target) {
        int[] distance = new int[width * height];
        Arrays.fill(distance, 9);
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < distance.length; i++) {
            if (kind[i] == target) {
                distance[i] = 0;
                queue.add(i);
            }
        }
        spread(distance, queue);
        return distance;
    }

    private int[] distanceToLand() {
        int[] distance = new int[width * height];
        Arrays.fill(distance, 9);
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < distance.length; i++) {
            if (kind[i] != WATER) {
                distance[i] = 0;
                queue.add(i);
            }
        }
        spread(distance, queue);
        return distance;
    }

    private void spread(int[] distance, Deque<Integer> queue) {
        while (!queue.isEmpty()) {
            int i = queue.poll();
            int x = i % width, y = i / width;
            if (distance[i] >= 8) continue;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                    int n = ny * width + nx;
                    if (distance[n] > distance[i] + 1) {
                        distance[n] = distance[i] + 1;
                        queue.add(n);
                    }
                }
            }
        }
    }

    // --- Yards, tracks and jetties: the bases belong to the land ---

    /**
     * Every headquarters and every set of base ruins stands in a yard of packed earth, so the buildings sit in
     * the landscape instead of on top of it. Dirt tracks join neighbouring yards across open ground (only over
     * plains, so they never suggest a way through woods or mountains), and ruins by the water get a jetty.
     */
    private void settle() {
        List<Point> places = new ArrayList<>(battlefield.getBasePositions());
        for (Point site : battlefield.getBaseSites()) {
            if (!places.contains(site)) places.add(site);
        }
        for (List<Point> road : roads(places)) {
            track(road);
        }
        for (Point place : places) {
            if (battlefield.getBaseSites().contains(place)) {
                jetty(place);
            }
            yard(place);
        }
    }

    /** A rounded yard of earth around the tile, a darker rim, a dithered edge and the building's shadow. */
    private void yard(Point tile) {
        int cx = tile.x * TILE + TILE / 2, cy = tile.y * TILE + TILE / 2 + 1;
        double rx = 10.5, ry = 8.5;
        for (int py = cy - 11; py <= cy + 11; py++) {
            for (int px = cx - 13; px <= cx + 13; px++) {
                if (!inside(px, py) || kind[py * width + px] == WATER) continue;
                double dx = (px - cx) / rx, dy = (py - cy) / ry;
                double r = Math.sqrt(dx * dx + dy * dy) + 0.08 * wobble(px, py, 4, 7);
                int i = py * width + px;
                if (r < 0.86) {
                    int roll = hash(px, py, 61) % 11;
                    pixels[i] = roll == 0 ? YARD_LIGHT : roll == 1 ? YARD_DARK : YARD;
                } else if (r < 1) {
                    pixels[i] = YARD_EDGE;
                } else if (r < 1.14 && (px + py) % 2 == 0) {
                    pixels[i] = YARD_EDGE; // blends into the grass
                } else {
                    continue;
                }
                kind[i] = BUILT;
            }
        }
        // the shadow the building casts to the lower right, peeking out under its sprite
        for (int py = cy + 5; py <= cy + 7; py++) {
            for (int px = cx - 6; px <= cx + 9; px++) {
                if (inside(px, py) && kind[py * width + px] == BUILT) pixels[py * width + px] = YARD_SHADOW;
            }
        }
    }

    /** A wooden jetty from ruins by the water out to the sea, so a harbour there reaches the water. */
    private void jetty(Point site) {
        int[][] directions = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
        for (int[] d : directions) {
            Point next = new Point(site.x + d[0], site.y + d[1]);
            if (battlefield.getGeographyOfPoint(next) != TerrainEnum.WATER) continue;
            int cx = site.x * TILE + TILE / 2, cy = site.y * TILE + TILE / 2;
            for (int step = 4; step <= TILE - 4; step++) { // stops short of the middle of the water tile
                for (int side = -2; side <= 2; side++) {
                    int px = cx + d[0] * step + (d[0] == 0 ? side : 0);
                    int py = cy + d[1] * step + (d[1] == 0 ? side : 0);
                    if (!inside(px, py)) continue;
                    int i = py * width + px;
                    if (kind[i] == WATER) {
                        boolean rim = Math.abs(side) == 2;
                        boolean seam = (d[0] == 0 ? py : px) % 3 == 0;
                        pixels[i] = rim ? PLANK_DARK : seam ? PLANK_LIGHT : PLANK;
                        kind[i] = BUILT;
                    } else if (kind[i] != BUILT || pixels[i] == YARD_EDGE) {
                        // on land up to the shore the jetty is a strip of trodden earth
                        pixels[i] = Math.abs(side) == 2 ? TRACK_EDGE : TRACK;
                        kind[i] = BUILT;
                    }
                }
            }
            return;
        }
    }

    /** A dirt track along the tiles, three pixels wide with darker verges, around water. */
    private void track(List<Point> road) {
        for (int k = 1; k < road.size(); k++) {
            Point a = road.get(k - 1), b = road.get(k);
            double ax = a.x * TILE + TILE / 2.0, ay = a.y * TILE + TILE / 2.0;
            double bx = b.x * TILE + TILE / 2.0, by = b.y * TILE + TILE / 2.0;
            int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
            for (int t = 0; t <= steps; t++) {
                double f = steps == 0 ? 0 : (double) t / steps;
                int x = (int) Math.round(ax + (bx - ax) * f + 1.2 * wobble((int) ax + t, (int) ay, 9, 8));
                int y = (int) Math.round(ay + (by - ay) * f + 1.2 * wobble((int) ax, (int) ay + t, 9, 9));
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int px = x + dx, py = y + dy;
                        if (!inside(px, py) || kind[py * width + px] == WATER) continue;
                        int i = py * width + px;
                        boolean core = Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
                        if (core) {
                            pixels[i] = hash(px, py, 62) % 9 == 0 ? YARD_DARK : TRACK;
                        } else if (kind[i] != BUILT) {
                            pixels[i] = TRACK_EDGE;
                        }
                        kind[i] = BUILT;
                    }
                }
            }
        }
    }

    /**
     * The tracks between the yards: every yard is joined to its nearest neighbour it can reach over plains (and
     * the groups to each other), like a spanning tree, so the network stays sparse.
     */
    private List<List<Point>> roads(List<Point> places) {
        List<List<Point>> roads = new ArrayList<>();
        List<Point> joined = new ArrayList<>();
        if (places.isEmpty()) return roads;
        joined.add(places.get(0));
        List<Point> waiting = new ArrayList<>(places.subList(1, places.size()));
        while (!waiting.isEmpty()) {
            List<Point> best = null;
            Point bestPlace = null;
            for (Point place : waiting) {
                List<Point> path = plainPath(place, joined);
                if (path != null && (best == null || path.size() < best.size())) {
                    best = path;
                    bestPlace = place;
                }
            }
            if (best == null) {
                // the rest cannot be reached over plains from here: start a new group
                joined.add(waiting.remove(0));
                continue;
            }
            if (best.size() <= MAX_ROAD_TILES) roads.add(best);
            joined.add(bestPlace);
            waiting.remove(bestPlace);
        }
        return roads;
    }

    private static final int MAX_ROAD_TILES = 12;

    /** The shortest way over plains (four directions) from the tile to one of the targets, or null. */
    private List<Point> plainPath(Point start, List<Point> targets) {
        java.util.Map<Point, Point> previous = new java.util.HashMap<>();
        Deque<Point> queue = new ArrayDeque<>(List.of(start));
        previous.put(start, start);
        while (!queue.isEmpty()) {
            Point point = queue.poll();
            if (!point.equals(start) && targets.contains(point)) {
                List<Point> path = new ArrayList<>();
                for (Point p = point; !p.equals(start); p = previous.get(p)) path.add(p);
                path.add(start);
                return path;
            }
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Point next = new Point(point.x + d[0], point.y + d[1]);
                if (previous.containsKey(next)) continue;
                if (battlefield.getGeographyOfPoint(next) != TerrainEnum.PLAIN) continue;
                previous.put(next, point);
                queue.add(next);
            }
        }
        return null;
    }

    // --- Grass tufts, flowers, trees and mountains ---

    private record Sprite(String[] rows, int[] colours, String keys, int x, int y) {
    }

    private void decorate() {
        int[] toWater = distance(WATER);
        for (int py = 1; py < height - 1; py++) {
            for (int px = 1; px < width - 1; px++) {
                int i = py * width + px;
                if (kind[i] != GRASS || toWater[i] < 3) continue;
                int roll = hash(px, py, 21) % 1000;
                if (roll < 14) {
                    tuft(px, py);
                } else if (roll < 16) {
                    pixels[i] = roll == 14 ? FLOWER_A : FLOWER_B;
                }
            }
        }

        List<Sprite> sprites = new ArrayList<>();
        // trees on a jittered lattice: neighbouring forest tiles grow into one wood
        for (int gy = 3; gy < height + 8; gy += 4) {
            for (int gx = (gy / 4) % 2 * 3; gx < width + 4; gx += 6) {
                int x = gx + hash(gx, gy, 31) % 3 - 1, y = gy + hash(gx, gy, 32) % 3 - 1;
                String[] tree = hash(gx, gy, 33) % 4 == 0 ? BROADLEAF : PINE;
                if (inside(x, y) && kind[y * width + x] == FOREST && overLand(x, y, tree.length)) {
                    sprites.add(new Sprite(tree, TREE_COLORS, TREE_KEYS, x - tree[0].length() / 2, y - tree.length + 1));
                }
            }
        }
        // a big peak on every mountain tile, smaller ones between neighbouring mountain tiles: ridges
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                if (!isMountain(tx, ty)) continue;
                int cx = tx * TILE + TILE / 2 + hash(tx, ty, 41) % 5 - 2;
                int foot = ty * TILE + TILE - 3 + hash(tx, ty, 42) % 3 - 1;
                sprites.add(sprite(BIG_PEAK, cx, foot));
                int[][] links = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
                for (int[] link : links) {
                    if (!isMountain(tx + link[0], ty + link[1])) continue;
                    int mx = cx + link[0] * TILE / 2 + hash(tx, ty, 45 + link[1]) % 3 - 1;
                    int my = foot + link[1] * TILE / 2 + 2;
                    String[] rock = hash(tx, ty, 50 + link[0] + 2 * link[1]) % 3 == 0 ? HILL : snowless(PEAK);
                    sprites.add(sprite(rock, mx, my));
                }
            }
        }
        sprites.sort(Comparator.comparingInt(sprite -> sprite.y() + sprite.rows().length));
        for (Sprite sprite : sprites) {
            draw(sprite);
        }
    }

    /** True if a sprite of the height standing on the point stays over land: trees never hide the water. */
    private boolean overLand(int x, int y, int spriteHeight) {
        for (int up = 0; up < spriteHeight; up++) {
            if (inside(x, y - up) && kind[(y - up) * width + x] == WATER) return false;
        }
        return true;
    }

    private boolean isMountain(int tx, int ty) {
        return battlefield.getGeographyOfPoint(new Point(tx, ty)) == TerrainEnum.MOUNTAIN;
    }

    /** A rock sprite standing with the middle of its foot on the point. */
    private static Sprite sprite(String[] rows, int x, int foot) {
        return new Sprite(rows, ROCK_COLORS, ROCK_KEYS, x - rows[0].length() / 2, foot - rows.length + 1);
    }

    private static String[] snowless(String[] rows) {
        String[] plain = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            plain[i] = rows[i].replace('S', 'L');
        }
        return plain;
    }

    /** A little tuft of grass: two light blades with a dark root. */
    private void tuft(int px, int py) {
        set(px - 1, py - 1, BLADE_LIGHT);
        set(px + 1, py - 1, BLADE_LIGHT);
        set(px, py, BLADE_LIGHT);
        set(px, py + 1, BLADE_DARK);
    }

    private void draw(Sprite sprite) {
        for (int row = 0; row < sprite.rows().length; row++) {
            String line = sprite.rows()[row];
            for (int col = 0; col < line.length(); col++) {
                int key = sprite.keys().indexOf(line.charAt(col));
                if (key < 0) continue;
                int colour = sprite.colours()[Math.min(key, sprite.colours().length - 1)];
                set(sprite.x() + col, sprite.y() + row, colour);
            }
        }
    }

    private void set(int x, int y, int colour) {
        if (inside(x, y)) pixels[y * width + x] = colour;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    // --- Noise ---

    /** Smooth value noise between -1 and 1 with features about {@code period} pixels across. */
    private double wobble(int px, int py, int period, int channel) {
        double x = (double) px / period, y = (double) py / period;
        int i = (int) Math.floor(x), j = (int) Math.floor(y);
        double fx = smooth(x - i), fy = smooth(y - j);
        double top = lerp(lattice(i, j, channel), lattice(i + 1, j, channel), fx);
        double bottom = lerp(lattice(i, j + 1, channel), lattice(i + 1, j + 1, channel), fx);
        return lerp(top, bottom, fy);
    }

    private double lattice(int i, int j, int channel) {
        return hash(i, j, channel) / (double) Integer.MAX_VALUE * 2 - 1;
    }

    /** A well mixed, non-negative pseudo-random number for the point; the same seed paints the same map. */
    private int hash(int x, int y, int channel) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xBF58476D1CE4E5B9L + y * 0x94D049BB133111EBL + channel * 0x2545F4914F6CDD1DL;
        h ^= h >>> 31;
        h *= 0x7FB5D329728EA185L;
        h ^= h >>> 27;
        h *= 0x81DADEF4BC2DD44DL;
        h ^= h >>> 33;
        return (int) (h & Integer.MAX_VALUE);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
