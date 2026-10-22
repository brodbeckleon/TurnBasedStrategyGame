package game.game;

import game.enums.UnitEnum;
import game.tools.CircleGenerator;
import java.awt.*;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Player {
    private final int playerID;
    private final PlayerDeck playerDeck;
    private int resourcePoints = 6;
    private final Set<Research> researched = EnumSet.noneOf(Research.class);
    private boolean researchedThisTurn = false;
    private final Map<Ability, Integer> cooldowns = new EnumMap<>(Ability.class);
    private final Set<Point> revealedTiles = new HashSet<>(); // by a radar sweep, until the player's next turn

    public Player(CircleGenerator circleGenerator, int playerID, Point positionBase) {
        this.playerID = playerID;
        // *** THE FIX IS ON THIS LINE ***
        // We now pass the playerID as the third argument for the baseID.
        playerDeck = new PlayerDeck(circleGenerator, playerID, playerID, positionBase);
    }

    public void addResourcePoints() {
        addResourcePoints(2);
    }

    public void addResourcePoints(int amount) {
        resourcePoints += amount;
    }

    public void removeResourcePoints(int resourceCost) {
        resourcePoints -= resourceCost;
    }

    public int getResourcePoints() {
        return resourcePoints;
    }

    public PlayerDeck getPlayerDeck() {
        return playerDeck;
    }

    public int getPlayerID() {
        return playerID;
    }

    /** At the start of each of the player's turns: research is open again, abilities cool down, sweeps end. */
    public void startTurn() {
        researchedThisTurn = false;
        cooldowns.replaceAll((ability, turns) -> Math.max(0, turns - 1));
        revealedTiles.clear();
        playerDeck.setUnitsAvailable();
    }

    // --- Research ---

    public boolean hasResearched(Research research) {
        return researched.contains(research);
    }

    /** True if the project's prerequisite is done (or it has none) and the project itself is not. */
    public boolean canResearch(Research research) {
        return !hasResearched(research) && (research.prerequisite() == null || hasResearched(research.prerequisite()));
    }

    public boolean hasResearchedThisTurn() {
        return researchedThisTurn;
    }

    public void completeResearch(Research research) {
        researched.add(research);
        researchedThisTurn = true;
    }

    /** True if the unit type needs no research, or its research is done. */
    public boolean hasUnlocked(UnitEnum type) {
        return Research.requiredFor(type).map(this::hasResearched).orElse(true);
    }

    // --- Abilities ---

    public boolean hasUnlocked(Ability ability) {
        return hasResearched(ability.research());
    }

    /** How many of the player's turns pass before the ability can be used again; 0 if it is ready. */
    public int getCooldown(Ability ability) {
        return cooldowns.getOrDefault(ability, 0);
    }

    public void startCooldown(Ability ability) {
        cooldowns.put(ability, ability.cooldown());
    }

    public Set<Point> getRevealedTiles() {
        return revealedTiles;
    }

    public void reveal(Collection<Point> tiles) {
        revealedTiles.addAll(tiles);
    }
}
