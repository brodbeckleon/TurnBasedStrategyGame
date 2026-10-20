package game.view;

import game.enums.UnitEnum;

/** A unit type the player can deploy, with the numbers the deploy panel shows. */
public record DeployOption(UnitEnum type, String name, int cost, int health, int damage, int moveRange,
                           int shootingRange) {
}
