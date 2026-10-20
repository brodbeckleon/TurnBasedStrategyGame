package game.game.commands;

/**
 * What an attack would do, worked out without changing anything: the damage dealt, whether that destroys the
 * target, what the target strikes back with, what covering anti-air fires at an attacking aircraft, and whether the
 * attacker would be lost.
 */
public record AttackPreview(int damage, int targetHpAfter, boolean destroysTarget, int counterDamage, int antiAirDamage,
                            boolean attackerLost) {
}
