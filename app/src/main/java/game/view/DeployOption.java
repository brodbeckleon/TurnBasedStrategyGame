package game.view;

import game.enums.UnitEnum;

/**
 * A unit type or building the player can deploy, with what the deploy panel shows about it.
 *
 * @param stats       the numbers in one short line, e.g. "HP 600  DMG 480  MOV 2  RNG 4"
 * @param description what the unit is good for and where it can be deployed
 */
public record DeployOption(UnitEnum type, Category category, String name, int cost, String stats,
                           String description) {

    /** The tabs of the deploy panel. */
    public enum Category {
        ARMY("Army"),
        SUPPORT("Support"),
        AIR("Air"),
        NAVY("Navy"),
        BUILDINGS("Build");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
