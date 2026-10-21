package game.game.map;

/** The five map sizes of the main menu; medium is the classic 16 x 16 board. */
public enum MapSize {
    TINY("Tiny", 10),
    SMALL("Small", 13),
    MEDIUM("Medium", 16),
    LARGE("Large", 20),
    HUGE("Huge", 24);

    private final String label;
    private final int tiles;

    MapSize(String label, int tiles) {
        this.label = label;
        this.tiles = tiles;
    }

    public String label() {
        return label;
    }

    /** The width and height of the board in tiles. */
    public int tiles() {
        return tiles;
    }

    @Override
    public String toString() {
        return label + " (" + tiles + "×" + tiles + ")";
    }
}
