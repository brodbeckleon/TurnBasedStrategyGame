package game.game;

/**
 * Commander abilities: researched once, then called in for resource points on a tile the player can see (the
 * radar sweep anywhere, the cruise missile on any enemy building). Each one then needs a few of the player's turns
 * to be ready again.
 */
public enum Ability {
    RADAR_SWEEP("Radar Sweep", Research.RADAR_SWEEP, 2, 2, 4, 0,
            "Reveals every tile within 4 of any point on the map until your next turn."),
    ARTILLERY_BARRAGE("Artillery Barrage", Research.ARTILLERY_BARRAGE, 6, 3, 1, 350,
            "Shells land on a tile you can see and every tile around it: 350 damage to each enemy there."),
    AIRSTRIKE("Airstrike", Research.AIRSTRIKE, 10, 4, 0, 800,
            "800 damage to one enemy unit or building you can see. Nothing strikes back."),
    CRUISE_MISSILE("Cruise Missile", Research.CRUISE_MISSILE, 12, 5, 0, 1200,
            "1200 damage to any enemy building on the map, seen or not. It hits the building itself, even with a unit on top."),
    PARATROOPERS("Paratroopers", Research.PARATROOPERS, 6, 3, 0, 0,
            "Drops a rifle squad on any free tile you can see that infantry can stand on. It counts towards your army limit.");

    private final String label;
    private final Research research;
    private final int cost;
    private final int cooldown;
    private final int radius;
    private final int damage;
    private final String description;

    Ability(String label, Research research, int cost, int cooldown, int radius, int damage, String description) {
        this.label = label;
        this.research = research;
        this.cost = cost;
        this.cooldown = cooldown;
        this.radius = radius;
        this.damage = damage;
        this.description = description;
    }

    public String label() {
        return label;
    }

    /** The project that unlocks it. */
    public Research research() {
        return research;
    }

    public int cost() {
        return cost;
    }

    /** How many of the player's turns pass before it can be used again (1: next turn). */
    public int cooldown() {
        return cooldown;
    }

    /** How far around the target tile it reaches. */
    public int radius() {
        return radius;
    }

    /** The damage it deals to each enemy it hits, before armour and cover. */
    public int damage() {
        return damage;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }
}
