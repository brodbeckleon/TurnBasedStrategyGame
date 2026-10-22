package game.game;

import game.enums.UnitEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The research tree: projects bought with resource points, at most one per turn. Each project has at most one
 * prerequisite in its own branch, so every branch is a tree that grows from one root and forks: Logistics (army
 * size, repairs, bases), Ground (veterans and tanks), Air (air defence, bombs, stealth), Navy (sonar and the big
 * ships) and Command (the abilities).
 */
public enum Research {
    SUPPLY_LINES(Branch.LOGISTICS, "Supply Lines", 6, null, "Army limit +2."),
    FIELD_WORKSHOPS(Branch.LOGISTICS, "Field Workshops", 8, SUPPLY_LINES,
            "Ambulances and recovery vehicles restore 50% more health."),
    INDUSTRIAL_EXPANSION(Branch.LOGISTICS, "Industrial Expansion", 12, FIELD_WORKSHOPS,
            "Bases can be upgraded to a fourth level: +1 RP per round, tougher walls."),
    RAPID_DEPLOYMENT(Branch.LOGISTICS, "Rapid Deployment", 8, SUPPLY_LINES,
            "New units can move and act on the turn they are deployed."),
    MASS_MOBILISATION(Branch.LOGISTICS, "Mass Mobilisation", 12, RAPID_DEPLOYMENT, "Army limit +3."),
    FORWARD_COMMAND(Branch.LOGISTICS, "Forward Command", 8, RAPID_DEPLOYMENT,
            "Buildings can be placed 3 tiles further from your bases."),

    BOOT_CAMP(Branch.GROUND, "Boot Camp", 6, null, "New combat units start as Private First Class (+10% damage)."),
    COMPOSITE_ARMOUR(Branch.GROUND, "Composite Armour", 10, BOOT_CAMP, "Tanks get +10% armour."),
    THERMAL_SIGHTS(Branch.GROUND, "Thermal Sights", 8, BOOT_CAMP, "Ground units see 1 tile further."),

    AEGIS_UPGRADE(Branch.AIR, "Aegis Upgrade", 8, null, "Anti-air units and destroyers shoot 1 tile further."),
    PRECISION_MUNITIONS(Branch.AIR, "Precision Munitions", 10, AEGIS_UPGRADE,
            "Aircraft deal 25% more damage to buildings."),
    STEALTH_PROGRAM(Branch.AIR, "Stealth Program", 12, AEGIS_UPGRADE, "Unlocks the F-35 stealth fighter."),

    IMPROVED_SONAR(Branch.NAVY, "Improved Sonar", 6, null, "Destroyers find submarines 2 tiles further away."),
    CARRIER_AVIATION(Branch.NAVY, "Carrier Aviation", 10, IMPROVED_SONAR, "Unlocks the Izumo aircraft carrier."),
    BATTLESHIPS(Branch.NAVY, "Battleships", 14, CARRIER_AVIATION, "Unlocks the Yamato battleship."),
    AMPHIBIOUS_DOCTRINE(Branch.NAVY, "Amphibious Doctrine", 10, IMPROVED_SONAR, "The Osumi carries two units."),

    RADAR_SWEEP(Branch.COMMAND, "Radar Sweep", 5, null, "Unlocks the Radar Sweep ability."),
    ARTILLERY_BARRAGE(Branch.COMMAND, "Artillery Barrage", 8, RADAR_SWEEP, "Unlocks the Artillery Barrage ability."),
    AIRSTRIKE(Branch.COMMAND, "Airstrike", 12, ARTILLERY_BARRAGE, "Unlocks the Airstrike ability."),
    CRUISE_MISSILE(Branch.COMMAND, "Cruise Missile", 16, AIRSTRIKE, "Unlocks the Cruise Missile ability."),
    PARATROOPERS(Branch.COMMAND, "Paratroopers", 10, RADAR_SWEEP, "Unlocks the Paratroopers ability.");

    /** The branches of the research tree, each one tree of its own. */
    public enum Branch {
        LOGISTICS("Logistics"),
        GROUND("Ground"),
        AIR("Air"),
        NAVY("Navy"),
        COMMAND("Command");

        private final String label;

        Branch(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** The project the branch grows from: the only one without a prerequisite. */
        public Research root() {
            return Arrays.stream(Research.values())
                    .filter(research -> research.branch == this && research.prerequisite == null)
                    .findFirst().orElseThrow();
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

    /** The project that has to be finished first (its parent in the tree), or null for a branch's root. */
    public Research prerequisite() {
        return prerequisite;
    }

    /** The projects that need this one first (its children in the tree), in tree order. */
    public List<Research> followUps() {
        return Arrays.stream(values()).filter(research -> research.prerequisite == this).toList();
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
