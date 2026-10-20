package game.game.commands;

import game.events.GameEvent;
import game.events.Weapon;
import game.game.Command;
import game.game.Player;
import game.enums.TerrainEnum;
import game.interfaces.AntiAir;
import game.interfaces.TankHunter;
import game.units.assaultClasses.Helicopter;
import game.units.assaultClasses.Submarine;
import game.model.Game;
import game.units.assaultClasses.Ship;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Base;
import game.units.Infrastructure;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.Unit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AttackCommand extends Command {
    private static final CircleGenerator circleGenerator = new CircleGenerator();

    public AttackCommand(Game gameModel) {
        super(gameModel);
    }

    /**
     * Executes an attack with a given attacking unit to a target (unit or base).
     *
     * @param attackerPlayer   The player who attacks.
     * @param attackingUnitID  The unit ID that attacks.
     * @param targetID         The ID of the target. Use 1/2 for base, or >2 for units.
     * @return CommandResult   Success/failure message.
     */
    public CommandResult execute(Player attackerPlayer, int attackingUnitID, int targetID) {
        Player defenderPlayer = (attackerPlayer.getPlayerID() == 1) ? getGameModel().getPlayerTwo() : getGameModel().getPlayerOne();
        Unit attackingUnit = attackerPlayer.getPlayerDeck().getUnit(attackingUnitID);

        if (attackingUnit == null) {
            return new CommandResult(false, "Attacking unit does not exist.");
        }

        if (!attackingUnit.isAvailable()) {
            return new CommandResult(false, "This unit is not available for attacking.");
        }

        if (!(attackingUnit instanceof AssaultClass)) {
            return new CommandResult(false, "This unit cannot attack.");
        }

        // Determine possible targets in range (includes the enemy base)
        List<Integer> targetsInRange = scout(attackingUnitID, attackerPlayer, defenderPlayer);
        if (!targetsInRange.contains(targetID)) {
            return new CommandResult(false, "Target is not in range.");
        }

        List<GameEvent> events;
        // Attack base
        if (targetID == 1 || targetID == 2) {
            events = attackBase(attackingUnitID, attackingUnit, defenderPlayer);
        }
        // Attack unit
        else {
            events = attackUnit(attackingUnitID, attackingUnit, defenderPlayer, targetID);
        }

        // Mark attacker as unavailable
        attackingUnit.setIsAvailable(false);

        // A surviving defender that has the attacker in its own range strikes back at once
        Unit defender = defenderPlayer.getPlayerDeck().getUnit(targetID);
        if (defender != null && canStrikeBack(defender, attackingUnit)) {
            events = new ArrayList<>(events);
            events.addAll(strikeBack(attackerPlayer, attackingUnitID, attackingUnit, targetID, (AssaultClass) defender));
        }

        // An aircraft that attacks inside the enemy's air defence draws fire from every anti-air unit in range
        if (attackingUnit instanceof Aircraft) {
            for (int interceptorID : findInterceptors(defenderPlayer, attackingUnit, targetID)) {
                if (attackingUnit.getHealthPoints() <= 0) break;
                events = new ArrayList<>(events);
                events.addAll(strikeBack(attackerPlayer, attackingUnitID, attackingUnit, interceptorID,
                        (AssaultClass) defenderPlayer.getPlayerDeck().getUnit(interceptorID)));
            }
        }

        return new CommandResult(true, "Unit " + attackingUnitID + " attacked target " + targetID + ".", events);
    }

    /** True if the defender survived, can fight, has the attacker in range and would actually hurt it. */
    public boolean canStrikeBack(Unit defender, Unit attacker) {
        return defender instanceof AssaultClass fighter && defender.getHealthPoints() > 0 && attacker.getHealthPoints() > 0
                && circleGenerator.getCoordinatesInRange(defender.getPosition(), getGameModel().getShootingRange(fighter))
                        .contains(attacker.getPosition())
                && canTarget(defender, attacker) && counterDamage(fighter, attacker) > 0;
    }

    /**
     * What a defender (or covering anti-air) deals when it strikes back: its normal damage, scaled by the share of
     * health it has left, so a battered unit hits back weakly.
     */
    public int counterDamage(AssaultClass defender, Unit attacker) {
        double strength = Math.max(0, (double) defender.getHealthPoints() / defender.getMaxHealthPoints());
        return (int) Math.round(predictDamage(defender, attacker) * strength);
    }

    /** The ids of the defender's anti-air units, besides the attacked unit itself, that cover the aircraft. */
    public List<Integer> findInterceptors(Player defenderPlayer, Unit aircraft, int targetID) {
        List<Integer> interceptors = new ArrayList<>();
        for (var entry : defenderPlayer.getPlayerDeck().getUnits().entrySet()) {
            if (entry.getKey() != targetID && entry.getValue() instanceof AntiAir
                    && canStrikeBack(entry.getValue(), aircraft)) {
                interceptors.add(entry.getKey());
            }
        }
        return interceptors;
    }

    private List<GameEvent> strikeBack(Player attackerPlayer, int attackingUnitID, Unit attacker, int defenderID,
                                       AssaultClass defender) {
        int damage = counterDamage(defender, attacker);
        int hpBefore = attacker.getHealthPoints();
        attacker.setHealthPoints(-damage);
        if (attacker.getHealthPoints() <= 0) {
            attackerPlayer.getPlayerDeck().removeUnit(attackingUnitID);
        }
        return describeAttack(defenderID, defender, attackingUnitID, attacker, damage, hpBefore);
    }

    private List<Integer> scout(int attackingUnitID, Player attacker, Player defender) {
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        Point attackingPos = attackingUnit.getPosition();
        int range = getGameModel().getShootingRange((AssaultClass) attackingUnit);

        return defender.getPlayerDeck().getUnitIDsInRange(attackingPos, range).stream()
                .distinct()
                .filter(id -> canTarget(attackingUnit, defender.getPlayerDeck().getUnit(id)))
                .filter(id -> getGameModel().isVisibleTo(attacker, defender.getPlayerDeck().getUnit(id))) // fog of war
                .toList();
    }

    /** What attacking the target would do, or null if the attack is not possible. */
    public AttackPreview preview(Player attackerPlayer, int attackingUnitID, int targetID) {
        Unit attacker = attackerPlayer.getPlayerDeck().getUnit(attackingUnitID);
        if (!(attacker instanceof AssaultClass assault) || !findTargets(attackerPlayer, attackingUnitID).contains(targetID)) {
            return null;
        }
        Player defenderPlayer = attackerPlayer.getPlayerID() == 1 ? getGameModel().getPlayerTwo() : getGameModel().getPlayerOne();
        Unit target = defenderPlayer.getPlayerDeck().getUnit(targetID);
        int damage = predictDamage(assault, target);
        int targetHpAfter = target.getHealthPoints() - damage;
        boolean destroys = targetHpAfter <= 0;

        int counter = 0;
        if (!destroys && target instanceof AssaultClass defender && canTarget(defender, attacker)
                && circleGenerator.getCoordinatesInRange(target.getPosition(), getGameModel().getShootingRange(defender))
                        .contains(attacker.getPosition())) {
            counter = (int) Math.round(predictDamage(defender, attacker) * (double) targetHpAfter / target.getMaxHealthPoints());
        }
        int flak = 0;
        if (attacker instanceof Aircraft) {
            for (int interceptorID : findInterceptors(defenderPlayer, attacker, targetID)) {
                flak += counterDamage((AssaultClass) defenderPlayer.getPlayerDeck().getUnit(interceptorID), attacker);
            }
        }
        return new AttackPreview(damage, Math.max(0, targetHpAfter), destroys, counter, flak,
                counter + flak >= attacker.getHealthPoints());
    }

    /** The ids of the enemies the player's unit could attack right now. */
    public List<Integer> findTargets(Player attacker, int attackingUnitID) {
        Unit unit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        if (!(unit instanceof AssaultClass)) return List.of();
        Player defender = attacker.getPlayerID() == 1 ? getGameModel().getPlayerTwo() : getGameModel().getPlayerOne();
        return scout(attackingUnitID, attacker, defender);
    }

    private List<GameEvent> attackBase(int attackingUnitID, Unit attackingUnit, Player defender) {
        var base = defender.getPlayerDeck().getBase();
        int hpBefore = base.getHealthPoints();
        int damage = structureDamage((AssaultClass) attackingUnit, base);

        base.setHealthPoints(-damage);

        if (base.getHealthPoints() <= 0) {
            //Todo:
        }
        return describeAttack(attackingUnitID, attackingUnit, base.getBaseID(), base, damage, hpBefore);
    }

    private List<GameEvent> attackUnit(int attackingUnitID, Unit attackingUnit, Player defender, int defendingUnitID) {
        Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);
        if (defendingUnit == null) return List.of();

        AssaultClass attacker = (AssaultClass) attackingUnit;
        int damage = predictDamage(attacker, defendingUnit);

        int hpBefore = defendingUnit.getHealthPoints();
        defendingUnit.setHealthPoints(-damage);

        if (defendingUnit.getHealthPoints() <= 0) {
            defender.getPlayerDeck().removeUnit(defendingUnitID);
        }
        return describeAttack(attackingUnitID, attackingUnit, defendingUnitID, defendingUnit, damage, hpBefore);
    }

    /** The damage the attacker would deal to the target where it stands, without changing anything. */
    public int predictDamage(AssaultClass attacker, Unit target) {
        return predictDamage(attacker, target, target.getPosition());
    }

    /** The damage the attacker would deal to the target if the target stood on the given tile (cover included). */
    public int predictDamage(AssaultClass attacker, Unit target, Point targetPosition) {
        if (target instanceof Infrastructure building) {
            return structureDamage(attacker, building);
        }
        int damage = unitDamage(attacker, target);
        return (int) Math.round(damage * (100 - coverPercent(target, targetPosition)) / 100.0);
    }

    /** Forests and mountains shelter units on the ground; aircraft and ships get no cover. */
    public int coverPercent(Unit unit, Point position) {
        if (unit instanceof Aircraft || unit instanceof Ship || unit instanceof Infrastructure) return 0;
        TerrainEnum terrain = getGameModel().getBattlefield().getGeographyOfPoint(position);
        return terrain == null ? 0 : terrain.coverPercent();
    }

    // How hard each kind of unit hits each kind of target; armour then takes its percentage off the result.
    private static final double ARTILLERY_VS_STRUCTURE = 2;
    private static final double TANK_VS_STRUCTURE = 1.5;
    private static final double AIRCRAFT_VS_STRUCTURE = 1;
    private static final double OTHER_VS_STRUCTURE = 0.5;
    private static final double ANTI_AIR_VS_AIRCRAFT = 2;
    private static final double GROUND_VS_AIRCRAFT = 0.75;
    private static final double ANTI_AIR_VS_GROUND = 0.5;
    private static final double GROUND_VS_HELICOPTER = 1;
    private static final double TORPEDO_VS_SHIP = 2;

    /** Artillery, tanks and aircraft are made to break buildings, everything else barely scratches them. */
    private static int structureDamage(AssaultClass attacker, Infrastructure building) {
        double multiplier;
        if (attacker instanceof SelfPropelledArtillery) multiplier = ARTILLERY_VS_STRUCTURE;
        else if (attacker instanceof AssaultTank) multiplier = TANK_VS_STRUCTURE;
        else if (attacker instanceof Aircraft) multiplier = AIRCRAFT_VS_STRUCTURE;
        else multiplier = OTHER_VS_STRUCTURE;
        return afterArmor(attacker.getDamage() * multiplier, building.getArmorValue());
    }

    /**
     * Anti-air hits aircraft twice as hard but ground targets only half as hard; other ground and sea units only
     * graze aircraft. Tank armour takes its percentage off.
     */
    private static int unitDamage(AssaultClass attacker, Unit target) {
        double damage = attacker.getDamage();
        if (target instanceof Aircraft) {
            if (attacker instanceof AntiAir) damage *= ANTI_AIR_VS_AIRCRAFT;
            else if (target instanceof Helicopter) damage *= GROUND_VS_HELICOPTER;
            else if (!(attacker instanceof Aircraft)) damage *= GROUND_VS_AIRCRAFT;
        } else if (attacker instanceof SelfPropelledAntiAir) {
            damage *= ANTI_AIR_VS_GROUND;
        }
        if (attacker instanceof TankHunter hunter && target instanceof AssaultTank) {
            damage *= hunter.antiTankMultiplier();
        }
        if (attacker instanceof Submarine && target instanceof Ship) {
            damage *= TORPEDO_VS_SHIP;
        }
        return afterArmor(damage, target instanceof AssaultTank tank ? tank.getArmorValue() : 0);
    }

    /**
     * Whether the attacker's weapons can reach the target at all: a submarine only attacks ships, and only ships
     * and aircraft can find a submarine.
     */
    public static boolean canTarget(Unit attacker, Unit target) {
        if (attacker instanceof Submarine) return target instanceof Ship;
        if (target instanceof Submarine) return attacker instanceof Ship || attacker instanceof Aircraft;
        return true;
    }

    /** Armour is a percentage: 40 armour stops 40 % of the damage. */
    private static int afterArmor(double damage, int armorPercent) {
        int armor = Math.max(0, Math.min(100, armorPercent));
        return (int) Math.round(damage * (100 - armor) / 100.0);
    }

    /** Builds the events of an attack that has already been applied to the model, in causal order. */
    private List<GameEvent> describeAttack(int attackingUnitID, Unit attacker, int targetID, Unit target,
                                           int damage, int hpBefore) {
        boolean targetIsBase = target instanceof Base;
        List<GameEvent> events = new ArrayList<>();
        events.add(new GameEvent.UnitAttacked(attacker.getPlayerID(), attackingUnitID, attacker.getUnitName(),
                attacker.getPosition(), targetID, target.getPosition(), targetIsBase, Weapon.of(attacker)));
        events.add(new GameEvent.UnitDamaged(target.getPlayerID(), targetID, target.getUnitName(), target.getPosition(),
                damage, hpBefore, target.getHealthPoints(), target.getMaxHealthPoints(), targetIsBase));
        if (target.getHealthPoints() <= 0) {
            events.add(new GameEvent.UnitDestroyed(target.getPlayerID(), targetID, target.getUnitName(),
                    target.getPosition(), targetIsBase));
        }
        return events;
    }
}
