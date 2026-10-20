package game.events;

import java.awt.Point;

/**
 * Something that happened in the model while a command was executed. Events carry everything the view
 * needs to replay the action, so it never has to query the model during an animation.
 */
public sealed interface GameEvent {

    record UnitDeployed(int playerId, int unitId, String unitName, Point pos) implements GameEvent {
        public UnitDeployed {
            pos = new Point(pos);
        }
    }

    record UnitMoved(int playerId, int unitId, String unitName, Point from, Point to) implements GameEvent {
        public UnitMoved {
            from = new Point(from);
            to = new Point(to);
        }
    }

    record UnitAttacked(int playerId, int attackerId, String attackerName, Point from,
                        int targetId, Point to, boolean targetIsBase, Weapon weapon) implements GameEvent {
        public UnitAttacked {
            from = new Point(from);
            to = new Point(to);
        }
    }

    record UnitDamaged(int ownerId, int unitId, String unitName, Point pos, int damage,
                       int hpBefore, int hpAfter, int maxHp, boolean isBase) implements GameEvent {
        public UnitDamaged {
            pos = new Point(pos);
        }
    }

    record UnitDestroyed(int ownerId, int unitId, String unitName, Point pos, boolean isBase) implements GameEvent {
        public UnitDestroyed {
            pos = new Point(pos);
        }
    }

    /** A support unit restored health to a friendly unit or building. */
    record UnitHealed(int playerId, int supporterId, String supporterName, Point from,
                      int targetId, String targetName, Point pos, int amount,
                      int hpBefore, int hpAfter, int maxHp) implements GameEvent {
        public UnitHealed {
            from = new Point(from);
            pos = new Point(pos);
        }
    }
}
