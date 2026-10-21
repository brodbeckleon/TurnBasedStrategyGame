package game.game.map;

/** The kinds of map the main menu offers: how much of it is land, water and mountains, and whether all land joins up. */
public enum MapType {
    CONTINENTS("Continents", "Land and sea in equal measure; every base can be reached by land.", -0.18, 0.5, 2.0, true, 4),
    BIG_CONTINENT("Big Continent", "One great landmass with a few lakes, ringed by the sea.", -0.5, 0.55, 1.4, true, 4),
    HIGHLANDS("Highlands", "Rugged mountains and narrow passes; infantry rules the heights.", -0.4, 0.2, 2.0, true, 3),
    ARCHIPELAGO("Archipelago", "Scattered islands: ferry your army over with amphibious ships.", 0.12, 0.62, 3.2, false, 3),
    WATERWORLD("Waterworld", "Open ocean with tiny islands: a war of fleets, carriers and aircraft.", 0.28, 0.8, 2.6, false, 2);

    private final String label;
    private final String description;
    private final double waterBelow;
    private final double mountainAbove;
    private final double scale;
    private final boolean landBridges;
    private final int homeRadius;

    MapType(String label, String description, double waterBelow, double mountainAbove, double scale, boolean landBridges,
            int homeRadius) {
        this.label = label;
        this.description = description;
        this.waterBelow = waterBelow;
        this.mountainAbove = mountainAbove;
        this.scale = scale;
        this.landBridges = landBridges;
        this.homeRadius = homeRadius;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Noise below this value is water. */
    double waterBelow() {
        return waterBelow;
    }

    /** Noise above this value is mountains. */
    double mountainAbove() {
        return mountainAbove;
    }

    /** How finely the terrain noise is sampled: bigger values give smaller islands and lakes. */
    double scale() {
        return scale;
    }

    /** True if land bridges join every base (and all ruins) by land; island maps leave the sea in between. */
    public boolean landBridges() {
        return landBridges;
    }

    /** The radius of open ground around each headquarters. */
    int homeRadius() {
        return homeRadius;
    }

    @Override
    public String toString() {
        return label;
    }
}
