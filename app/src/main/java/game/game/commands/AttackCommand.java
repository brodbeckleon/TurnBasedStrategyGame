package game.game.commands;

import game.events.GameEvent;
import game.events.Weapon;
import game.game.Command;
import game.game.Player;
import game.game.Research;
import game.game.Vision;
import game.interfaces.Host;
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
import game.units.TransportShip;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.Infantry;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.Unit;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeSixteen;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import game.units.assaultClasses.aircraft.ATen;
import game.units.assaultClasses.aircraft.FFifteen;
import game.units.assaultClasses.aircraft.FThirtyFive;
import game.units.assaultClasses.aircraft.FTwo;
import game.units.assaultClasses.antiAir.TypeEightyOneC;
import game.units.assaultClasses.antiAir.TypeEightySeven;
import game.units.assaultClasses.infantryFightingVehicles.TypeEightyNine;
import game.units.assaultClasses.ships.Yamato;

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
     * @param targetID         The ID of the target: a base has its owner's player id, every other unit a larger one.
     * @return CommandResult   Success/failure message.
     */
    public CommandResult execute(Player attackerPlayer, int attackingUnitID, int targetID) {
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

        // Determine possible targets in range (includes the enemy bases)
        List<Integer> targetsInRange = findTargets(attackerPlayer, attackingUnitID);
        if (!targetsInRange.contains(targetID)) {
            Unit target = getGameModel().findUnitById(targetID);
            if (target != null && isShielded(target, attackingUnit)) {
                return new CommandResult(false, "A unit on top of the " + target.getUnitName()
                        + " takes the attacks first: attack that unit (you need to see it).");
            }
            return new CommandResult(false, "Target is not in range.");
        }
        Player defenderPlayer = getGameModel().getOwner(getGameModel().findUnitById(targetID));

        List<GameEvent> events;
        // Attack base
        if (defenderPlayer.getPlayerDeck().getUnit(targetID) instanceof Base) {
            events = attackBase(attackingUnitID, attackingUnit, defenderPlayer);
        }
        // Attack unit
        else {
            events = attackUnit(attackingUnitID, attackingUnit, defenderPlayer, targetID);
        }

        // Mark attacker as unavailable
        attackingUnit.setIsAvailable(false);

        // A surviving defender that has the attacker in its own range strikes back at once (not at standoff missiles)
        Unit defender = defenderPlayer.getPlayerDeck().getUnit(targetID);
        if (defender != null && !isStandoff(attackingUnit, defender) && canStrikeBack(defender, attackingUnit)) {
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

    /** True if the defender survived, can fight, has the attacker in range, sees it and would actually hurt it. */
    public boolean canStrikeBack(Unit defender, Unit attacker) {
        return defender instanceof AssaultClass fighter && attacker.getHealthPoints() > 0
                && couldStrikeBack(fighter, attacker, attacker.getPosition()) && counterDamage(fighter, attacker) > 0;
    }

    /**
     * True if the defender could fire at the attacker standing on the tile: it is alive, not artillery (which never
     * strikes back), has the tile in range, its weapons reach the attacker, and it sees it (a stealth fighter its side
     * cannot see strikes unanswered).
     */
    public boolean couldStrikeBack(AssaultClass defender, Unit attacker, Point from) {
        return inReach(defender, attacker, from) && !(attacker instanceof FThirtyFive
                && !getGameModel().isVisibleTo(getGameModel().getOwner(defender), attacker, from));
    }

    private boolean inReach(AssaultClass defender, Unit attacker, Point from) {
        return defender.getHealthPoints() > 0 && !(defender instanceof SelfPropelledArtillery) && canTarget(defender, attacker)
                && !isTooClose(defender, defender.getPosition(), from)
                && circleGenerator.getCoordinatesInRange(defender.getPosition(), getGameModel().getShootingRange(defender))
                        .contains(from);
    }

    /**
     * True if the attacker hits the target from beyond the reach of its answer: artillery's indirect fire, and the
     * F-2's missiles against a ship.
     */
    public static boolean isStandoff(Unit attacker, Unit target) {
        return attacker instanceof SelfPropelledArtillery || attacker instanceof FTwo && isVessel(target);
    }

    /**
     * True if the target tile is too close for the attacker standing on the tile: artillery and the Yamato's big guns
     * cannot fire point-blank.
     */
    public static boolean isTooClose(Unit attacker, Point from, Point target) {
        return (attacker instanceof SelfPropelledArtillery || attacker instanceof Yamato) && from.distance(target) < 1.5;
    }

    /**
     * What a defender (or covering anti-air) deals when it strikes back: its normal damage, scaled by the share of
     * health it has left, so a battered unit hits back weakly (except the Type 90).
     */
    public int counterDamage(AssaultClass defender, Unit attacker) {
        return (int) Math.round(predictDamage(defender, attacker) * counterStrength(defender, defender.getHealthPoints()));
    }

    /** The share of its damage the defender strikes back with at the given health: the heavy Type 90 always all of it. */
    public static double counterStrength(AssaultClass defender, int healthLeft) {
        if (defender instanceof TypeNinety) return 1;
        return Math.max(0, (double) healthLeft / defender.getMaxHealthPoints());
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
        List<GameEvent> events = describeAttack(defenderID, defender, attackingUnitID, attacker, damage, hpBefore);
        if (attacker.getHealthPoints() <= 0) {
            attackerPlayer.getPlayerDeck().removeUnit(attackingUnitID);
            events.addAll(creditKill(defenderID, defender));
        }
        return events;
    }

    /** Counts a kill for the unit; returns its promotion if the kill raised its rank. */
    private static List<GameEvent> creditKill(int unitId, Unit unit) {
        if (unit instanceof AssaultClass veteran && unit.getHealthPoints() > 0 && veteran.addKill()) {
            return List.of(new GameEvent.UnitPromoted(unit.getPlayerID(), unitId, unit.getUnitName(), unit.getPosition(),
                    veteran.getRank()));
        }
        return List.of();
    }

    /**
     * The enemies of every other player the unit could hit from the tile (its own: right now). The one place that
     * decides what can be attacked: the player's clicks, the board's markers and the bot all go through it.
     */
    private List<Integer> scout(Player attacker, Unit attackingUnit, Point from) {
        int range = getGameModel().getShootingRange((AssaultClass) attackingUnit, from);

        List<Integer> targets = new ArrayList<>();
        for (Player defender : getGameModel().getEnemies(attacker)) {
            defender.getPlayerDeck().getUnitIDsInRange(from, range).stream()
                    .distinct()
                    .filter(id -> canTarget(attackingUnit, defender.getPlayerDeck().getUnit(id)))
                    .filter(id -> !isTooClose(attackingUnit, from, defender.getPlayerDeck().getUnit(id).getPosition()))
                    .filter(id -> !isShielded(defender.getPlayerDeck().getUnit(id), attackingUnit))
                    .filter(id -> getGameModel().isVisibleTo(attacker, defender.getPlayerDeck().getUnit(id))) // fog of war
                    .forEach(targets::add);
        }
        return targets;
    }

    /**
     * True if a unit the attacker could hit stands above the target, on top of the building or ship (or before it on
     * the same deck): it takes the attacks first, seen or not (an attacker that cannot see it cannot attack the tile
     * until it does). Only a stealth fighter the attacker's side cannot see shields nothing: parked on its air base,
     * the base is a fair target.
     */
    public boolean isShielded(Unit target, Unit attacker) {
        // the units above the target: everyone on deck for a building or ship, those before it for a unit on deck
        Unit host = target instanceof Host ? target : getGameModel().getHostOf(target);
        if (host == null) return false;
        for (Unit guest : getGameModel().getGuestsOf(host)) {
            if (guest == target) return false;
            if (canTarget(attacker, guest) && (!(guest instanceof FThirtyFive)
                    || getGameModel().isVisibleTo(getGameModel().getOwner(attacker), guest))) {
                return true;
            }
        }
        return false;
    }

    /** What attacking the target would do, or null if the attack is not possible. */
    public AttackPreview preview(Player attackerPlayer, int attackingUnitID, int targetID) {
        Unit attacker = attackerPlayer.getPlayerDeck().getUnit(attackingUnitID);
        if (!(attacker instanceof AssaultClass assault) || !findTargets(attackerPlayer, attackingUnitID).contains(targetID)) {
            return null;
        }
        Unit target = getGameModel().findUnitById(targetID);
        Player defenderPlayer = getGameModel().getOwner(target);
        int damage = predictDamage(assault, target);
        int targetHpAfter = target.getHealthPoints() - damage;
        boolean destroys = targetHpAfter <= 0;

        // a stealth fighter is only answered by a side that spots it: judged by the enemies the attacker can see, so the
        // forecast does not give away what hides in the fog
        boolean answered = !(attacker instanceof FThirtyFive) || !getGameModel().isFogOfWar()
                || Vision.spotsStealth(getGameModel().getVisibleEnemies(attackerPlayer).stream()
                        .filter(unit -> unit.getPlayerID() == defenderPlayer.getPlayerID()).toList(), attacker.getPosition());
        int counter = 0;
        if (!destroys && answered && target instanceof AssaultClass defender && !isStandoff(attacker, target)
                && inReach(defender, attacker, attacker.getPosition())) {
            counter = (int) Math.round(predictDamage(defender, attacker) * counterStrength(defender, targetHpAfter));
        }
        // only the anti-air the attacker can see: the forecast must not give away what hides in the fog
        int flak = 0;
        if (attacker instanceof Aircraft && answered) {
            for (int interceptorID : findInterceptors(defenderPlayer, attacker, targetID)) {
                if (counter + flak >= attacker.getHealthPoints()) break; // shot down: the rest hold their fire
                Unit interceptor = defenderPlayer.getPlayerDeck().getUnit(interceptorID);
                if (getGameModel().isVisibleTo(attackerPlayer, interceptor)) {
                    flak += counterDamage((AssaultClass) interceptor, attacker);
                }
            }
        }
        return new AttackPreview(damage, Math.max(0, targetHpAfter), destroys, counter, flak,
                counter + flak >= attacker.getHealthPoints());
    }

    /** The ids of the enemies the player's unit could attack right now. */
    public List<Integer> findTargets(Player attacker, int attackingUnitID) {
        Unit unit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        return unit == null ? List.of() : findTargetsFrom(attacker, attackingUnitID, unit.getPosition());
    }

    /** The ids of the enemies the player's unit could attack if it stood on the tile (for the bot's planning). */
    public List<Integer> findTargetsFrom(Player attacker, int attackingUnitID, Point from) {
        Unit unit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        if (!(unit instanceof AssaultClass)) return List.of();
        return scout(attacker, unit, from);
    }

    private List<GameEvent> attackBase(int attackingUnitID, Unit attackingUnit, Player defender) {
        var base = defender.getPlayerDeck().getBase();
        int hpBefore = base.getHealthPoints();
        int damage = structureDamage((AssaultClass) attackingUnit, base);

        base.setHealthPoints(-damage);
        base.markUnderFire();

        List<GameEvent> events = describeAttack(attackingUnitID, attackingUnit, base.getBaseID(), base, damage, hpBefore);
        if (base.getHealthPoints() <= 0) {
            events.addAll(creditKill(attackingUnitID, attackingUnit));
        }
        return events;
    }

    private List<GameEvent> attackUnit(int attackingUnitID, Unit attackingUnit, Player defender, int defendingUnitID) {
        Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);
        if (defendingUnit == null) return List.of();

        AssaultClass attacker = (AssaultClass) attackingUnit;
        int damage = predictDamage(attacker, defendingUnit);

        int hpBefore = defendingUnit.getHealthPoints();
        List<Unit> passengers = getGameModel().getGuestsOf(defendingUnit);
        defendingUnit.setHealthPoints(-damage);
        if (defendingUnit instanceof Infrastructure building) {
            building.markUnderFire();
        }

        if (defendingUnit.getHealthPoints() <= 0) {
            defender.getPlayerDeck().removeUnit(defendingUnitID);
        }
        List<GameEvent> events = describeAttack(attackingUnitID, attackingUnit, defendingUnitID, defendingUnit, damage, hpBefore);
        if (defendingUnit.getHealthPoints() <= 0 && defendingUnit instanceof TransportShip) {
            // a sunk transport takes the units on its deck down with it
            events = new ArrayList<>(events);
            events.addAll(sinkPassengers(defender, passengers));
        }
        if (defendingUnit.getHealthPoints() <= 0) {
            events = new ArrayList<>(events);
            events.addAll(creditKill(attackingUnitID, attackingUnit));
        }
        return events;
    }

    /** Removes the units that went down with their ship; returns their destruction. */
    List<GameEvent> sinkPassengers(Player owner, List<Unit> passengers) {
        List<GameEvent> events = new ArrayList<>();
        for (Unit passenger : passengers) {
            int passengerId = getGameModel().getUnitID(passenger);
            if (passengerId < 0) continue; // already gone
            owner.getPlayerDeck().removeUnit(passengerId);
            events.add(new GameEvent.UnitDestroyed(passenger.getPlayerID(), passengerId, passenger.getUnitName(),
                    passenger.getPosition(), false));
        }
        return events;
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
        int cover = attacker instanceof TypeTen ? 0 : coverPercent(target, targetPosition); // its fire control sees through
        return (int) Math.round(damage * (100 - cover) / 100.0);
    }

    /**
     * What a strike of the given strength that comes from no unit (an ability) does to the target: armour takes its
     * share off, and on the ground cover does too.
     */
    public int strikeDamage(double damage, Unit target) {
        if (target instanceof Infrastructure) {
            return afterArmor(damage, getGameModel().getArmorValue(target));
        }
        int dealt = afterArmor(damage, getGameModel().getArmorValue(target));
        return (int) Math.round(dealt * (100 - coverPercent(target, target.getPosition())) / 100.0);
    }

    /** Forests and mountains shelter units on the ground; aircraft and ships get no cover. */
    public int coverPercent(Unit unit, Point position) {
        if (unit instanceof Aircraft || isVessel(unit) || unit instanceof Infrastructure) return 0;
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
    private static final double ARMOURED_COCKPIT = 0.5;
    private static final double RAIDER_VS_LIGHT_VEHICLE = 1.5;
    private static final double AUTOCANNON_VS_INFANTRY = 1.75;
    private static final double MISSILE_VS_SHIP = 2;
    private static final double TORPEDO_VS_SHIP = 2;
    private static final double PRECISION_MUNITIONS_BONUS = 1.25;

    /**
     * Artillery, tanks, the Yamato's shore bombardment and aircraft are made to break buildings (aircraft better with
     * Precision Munitions, except the F-15, which is built to fight in the air), everything else barely scratches them.
     */
    private int structureDamage(AssaultClass attacker, Infrastructure building) {
        double multiplier;
        if (attacker instanceof SelfPropelledArtillery) multiplier = ARTILLERY_VS_STRUCTURE;
        else if (attacker instanceof AssaultTank || attacker instanceof Yamato) multiplier = TANK_VS_STRUCTURE;
        else if (attacker instanceof Aircraft && !(attacker instanceof FFifteen)) multiplier = AIRCRAFT_VS_STRUCTURE
                * (getGameModel().getOwner(attacker).hasResearched(Research.PRECISION_MUNITIONS) ? PRECISION_MUNITIONS_BONUS : 1);
        else multiplier = OTHER_VS_STRUCTURE;
        return afterArmor(attacker.getDamage() * multiplier, getGameModel().getArmorValue(building));
    }

    /**
     * Anti-air hits aircraft twice as hard (the A-10's armoured cockpit halves that) but what does not fly only half as
     * hard; other ground and sea units only graze aircraft. The specialists hit their prey harder: tank hunters tanks,
     * the Type 16 light vehicles, the Type 89 infantry, the F-2 and the submarine ships. Tank armour takes its percentage off, except
     * against the A-10's cannon.
     */
    private int unitDamage(AssaultClass attacker, Unit target) {
        double damage = attacker.getDamage();
        if (target instanceof Aircraft) {
            if (attacker instanceof AntiAir) damage *= ANTI_AIR_VS_AIRCRAFT * (target instanceof ATen ? ARMOURED_COCKPIT : 1);
            else if (target instanceof Helicopter) damage *= GROUND_VS_HELICOPTER;
            else if (!(attacker instanceof Aircraft)) damage *= GROUND_VS_AIRCRAFT;
        } else if (weakAgainstGround(attacker, target)) {
            damage *= ANTI_AIR_VS_GROUND;
        }
        if (attacker instanceof TankHunter hunter && target instanceof AssaultTank) {
            damage *= hunter.antiTankMultiplier();
        }
        if (attacker instanceof TypeSixteen && Vision.isGround(target) && !(target instanceof AssaultTank)
                && !(target instanceof Infantry)) {
            damage *= RAIDER_VS_LIGHT_VEHICLE;
        }
        if (attacker instanceof TypeEightyNine && target instanceof Infantry) {
            damage *= AUTOCANNON_VS_INFANTRY;
        }
        if (attacker instanceof FTwo && isVessel(target)) {
            damage *= MISSILE_VS_SHIP;
        }
        if (attacker instanceof Submarine && isVessel(target)) {
            damage *= TORPEDO_VS_SHIP;
        }
        return afterArmor(damage, attacker instanceof ATen ? 0 : getGameModel().getArmorValue(target));
    }

    /**
     * True if the attacker is made for aircraft and loses its punch against the target on the ground or at sea: anti-air
     * vehicles and the F-15. The Type 87's guns only bounce off tanks.
     */
    private static boolean weakAgainstGround(AssaultClass attacker, Unit target) {
        if (attacker instanceof TypeEightySeven) return target instanceof AssaultTank;
        return attacker instanceof SelfPropelledAntiAir || attacker instanceof FFifteen;
    }

    /**
     * Whether the attacker's weapons can reach the target at all: a submarine only attacks ships, and only ships
     * and aircraft can find a submarine. The Type 81's missiles only hit aircraft, the A-10's cannon never does.
     */
    public static boolean canTarget(Unit attacker, Unit target) {
        if (attacker instanceof Submarine) return isVessel(target);
        if (attacker instanceof TypeEightyOneC) return target instanceof Aircraft;
        if (attacker instanceof ATen && target instanceof Aircraft) return false;
        if (target instanceof Submarine) return attacker instanceof Ship || attacker instanceof Aircraft;
        return true;
    }

    /** Warships and transports alike: what torpedoes hunt. */
    public static boolean isVessel(Unit unit) {
        return unit instanceof Ship || unit instanceof TransportShip;
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
