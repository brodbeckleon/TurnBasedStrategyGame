package game.controller;

/**
 * How well the bot plays. It never cheats: every level plays by the same rules and the same fog of war. Lower
 * levels buy more randomly, sometimes take a worse shot or none at all, are careless under fire and slow to
 * upgrade; Hard makes no random choices and no blunders (in bot-vs-bot games it beats Normal about 57 % of the time,
 * Easy wins about a third against Normal).
 *
 * @param purchaseNoise how much chance goes into what the bot buys
 * @param blunderChance how often a unit takes a random worthwhile shot (or skips it) instead of the best one
 * @param caution       how seriously the bot takes enemy fire when it picks where to stand
 * @param upgradeSpareRp how many resource points the bot keeps back before it upgrades a base
 */
public enum Difficulty {
    EASY("Easy", 2.5, 0.35, 0.4, 8),
    NORMAL("Normal", 0.8, 0.05, 1, 4),
    HARD("Hard", 0.2, 0, 1, 4);

    private final String label;
    final double purchaseNoise;
    final double blunderChance;
    final double caution;
    final int upgradeSpareRp;

    Difficulty(String label, double purchaseNoise, double blunderChance, double caution, int upgradeSpareRp) {
        this.label = label;
        this.purchaseNoise = purchaseNoise;
        this.blunderChance = blunderChance;
        this.caution = caution;
        this.upgradeSpareRp = upgradeSpareRp;
    }

    @Override
    public String toString() {
        return label;
    }
}
