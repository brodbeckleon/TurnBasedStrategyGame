package game.game;

import game.enums.UnitEnum;

import java.util.Optional;

/**
 * The research tree: projects bought with resource points, at most one per turn. Each branch is a chain, so a
 * project needs the one before it (if any) first. Logistics raises the army limit and the repairs, Arsenal unlocks
 * the heaviest units, Command unlocks the abilities.
 */
public enum Research {
    SUPPLY_LINES(Branch.LOGISTICS, "Supply Lines", 6, null, "Army limit +2."),
    FIELD_WORKSHOPS(Branch.LOGISTICS, "Field Workshops", 8, SUPPLY_LINES,
            "Ambulances and recovery vehicles restore 50% more health."),
    MASS_MOBILISATION(Branch.LOGISTICS, "Mass Mobilisation", 12, FIELD_WORKSHOPS, "Army limit +3."),

    STEALTH_PROGRAM(Branch.ARSENAL, "Stealth Program", 10, null, "Unlocks the F-35 stealth fighter."),
    CARRIER_AVIATION(Branch.ARSENAL, "Carrier Aviation", 10, null, "Unlocks the Izumo aircraft carrier."),
    BATTLESHIPS(Branch.ARSENAL, "Battleships", 14, CARRIER_AVIATION, "Unlocks the Yamato battleship."),

    RADAR_SWEEP(Branch.COMMAND, "Radar Sweep", 5, null, "Unlocks the Radar Sweep ability."),
    ARTILLERY_BARRAGE(Branch.COMMAND, "Artillery Barrage", 8, RADAR_SWEEP, "Unlocks the Artillery Barrage ability."),
    AIRSTRIKE(Branch.COMMAND, "Airstrike", 12, ARTILLERY_BARRAGE, "Unlocks the Airstrike ability.");

    /** The columns of the research tree. */
    public enum Branch {
        LOGISTICS("Logistics"),
        ARSENAL("Arsenal"),
        COMMAND("Command");

        private final String label;

        Branch(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Branch branch;
    private final String label;
    private final int cost;
    private final Research prerequisite;
    private final String description;

    Research(Branch branch, String label, int cost, Research prerequisite, String description) {
        this.branch = branch;
        this.label = label;
        this.cost = cost;
        this.prerequisite = prerequisite;
        this.description = description;
    }

    public Branch branch() {
        return branch;
    }

    public String label() {
        return label;
    }

    public int cost() {
        return cost;
    }

    /** The project that has to be finished first, or null. */
    public Research prerequisite() {
        return prerequisite;
    }

    public String description() {
        return description;
    }

    /** The project that unlocks the unit type, if it is locked at the start. */
    public static Optional<Research> requiredFor(UnitEnum type) {
        return Optional.ofNullable(switch (type) {
            case FTHIRTYFIVE -> STEALTH_PROGRAM;
            case IZUMO -> CARRIER_AVIATION;
            case YAMATO -> BATTLESHIPS;
            default -> null;
        });
    }

    @Override
    public String toString() {
        return label;
    }
}
