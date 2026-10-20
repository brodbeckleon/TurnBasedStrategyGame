package game.interfaces;

/**
 * A unit that spots targets for its side: friendly attackers standing within its spotting range shoot one tile
 * further.
 */
public interface Spotter {
    int SPOTTING_BONUS = 1;

    int getSpottingRange();
}
