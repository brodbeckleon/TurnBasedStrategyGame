package game.controller;

import game.events.GameEvent;
import game.game.Command;
import game.game.Movement;
import game.game.Player;
import game.game.UnitFactory;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.HealCommand;
import game.game.commands.MoveCommand;
import game.game.commands.UpgradeCommand;
import game.interfaces.ActsAfterMoving;
import game.interfaces.AntiAir;
import game.interfaces.MovableUnit;
import game.interfaces.Spotter;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Base;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.SupportClass;
import game.units.Unit;
import game.enums.UnitEnum;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.Infantry;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.assaultClasses.Ship;
import game.units.assaultClasses.Submarine;
import game.units.infrastructures.AirForceBase;
import game.units.infrastructures.NavyBase;
import game.units.infrastructures.Radarstation;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredReconaissanceVehicle;
import game.units.supportClasses.ArmoredRecoveryVehicle;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Enthält die Logik und Entscheidungsfindung für den KI-Spieler (Bot).
 * Diese Klasse verwendet die bestehenden Command-Klassen, um Aktionen auszuführen.
 *
 * <p>Every unit weighs its options: attacks are scored by the damage they do and the value of the target (kills
 * count extra), moves by what the unit could hit from the new tile, how close it gets to its goal and how much
 * enemy fire it walks into. The economy claims base ruins with recovery vehicles, saves up for the base and
 * upgrades bases when there is money to spare. Purchases counter what the enemy fields, with a little randomness
 * so no two games play alike.
 */
public class BotService {

    private static final double KILL_BONUS = 1.5;
    private static final double HQ_VALUE = 40;
    private static final double NEXT_TURN_WEIGHT = 0.8;
    private static final double GOAL_WEIGHT = 0.3;
    private static final double THREAT_RADIUS = 6;
    private static final double ESCORT_DISTANCE = 2;
    private static final int UPGRADE_SPARE_RP = 4;
    private static final double PURCHASE_NOISE = 0.8;
    private static final double REPEAT_PENALTY = 0.35;
    private static final double SAVE_FOR_BETTER = 1.3;
    private static final double COUNTER_WEIGHT = 0.6;
    private static final double MIN_CAUTION = 0.3;
    private static final double MAX_CAUTION = 1.5;

    private final Game model;
    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final UpgradeCommand upgradeCommand;
    private final Random random;
    private final int playerId;
    private Predicate<UnitEnum> roster = type -> true;
    private final UnitFactory unitFactory = new UnitFactory();

    private final Deque<Integer> pendingUnitIds = new ArrayDeque<>();
    private boolean upgradePending = false;
    private double boldness = 1;
    private boolean purchasePending = false;

    /** A bot that always makes the same choices in the same situation. */
    public BotService(Game model, AddCommand add, MoveCommand move, AttackCommand attack, HealCommand heal) {
        this(model, add, move, attack, heal, new UpgradeCommand(model), new Random(0));
    }

    /** The bot playing player two, as in the game. */
    public BotService(Game model, AddCommand add, MoveCommand move, AttackCommand attack, HealCommand heal,
                      UpgradeCommand upgrade, Random random) {
        this(model, add, move, attack, heal, upgrade, random, 2);
    }

    /** A bot for either side, so two bots can play each other (balance simulations). */
    public BotService(Game model, AddCommand add, MoveCommand move, AttackCommand attack, HealCommand heal,
                      UpgradeCommand upgrade, Random random, int playerId) {
        this.playerId = playerId;
        this.model = model;
        this.addCommand = add;
        this.moveCommand = move;
        this.attackCommand = attack;
        this.healCommand = heal;
        this.upgradeCommand = upgrade;
        this.random = random;
    }

    /** Limits what the bot may buy; everything by default. */
    public void setRoster(Predicate<UnitEnum> roster) {
        this.roster = roster;
    }

    private Player me() {
        return playerId == 1 ? model.getPlayerOne() : model.getPlayerTwo();
    }

    private Player enemy() {
        return playerId == 1 ? model.getPlayerTwo() : model.getPlayerOne();
    }

    /**
     * Führt den gesamten Zug für den Bot aus.
     */
    public void performTurn() {
        startTurn();
        while (performNextStep().isPresent()) {
            // each step already changed the model, nothing to play back here
        }
    }

    /**
     * Starts a new bot turn that is then executed step by step with {@link #performNextStep()}: first the units
     * (long-range fire first, the builders last), then at most one upgrade and one purchase.
     */
    public void startTurn() {
        pendingUnitIds.clear();
        me().getPlayerDeck().getUnits().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Integer, Unit> entry) -> actionOrder(entry.getValue()))
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> pendingUnitIds.add(entry.getKey()));
        upgradePending = true;
        purchasePending = true;
        boldness = boldness();
    }

    /**
     * How much the bot may ignore enemy fire this turn: cautious while outgunned, bold with the stronger army,
     * so evenly matched armies do not just wait for each other forever.
     */
    private double boldness() {
        double own = strength(me());
        double enemy = strength(enemy());
        if (own + enemy == 0) return 1;
        return Math.max(MIN_CAUTION, Math.min(MAX_CAUTION, 2 * enemy / (own + enemy)));
    }

    private static double strength(Player player) {
        return player.getPlayerDeck().getUnits().values().stream()
                .filter(unit -> unit instanceof AssaultClass)
                .mapToDouble(unit -> valueOf(unit) * healthRatio(unit))
                .sum();
    }

    private static int actionOrder(Unit unit) {
        if (unit instanceof SelfPropelledArtillery) return 0;
        if (unit instanceof Ship) return 1;
        if (unit instanceof Aircraft) return 2;
        if (unit instanceof AssaultTank) return 3;
        if (unit instanceof AssaultClass) return 4;
        if (unit instanceof ArmoredRecoveryVehicle) return 7;
        if (unit instanceof SupportClass support && support.getSupportPoints() > 0) return 6;
        return 5;
    }

    /**
     * Executes the next unit action (or the upgrade, or the purchase) on the model and returns the events it
     * caused. Steps that cause no events are skipped. An empty result means the turn is finished.
     */
    public Optional<List<GameEvent>> performNextStep() {
        Player botPlayer = me();

        while (!pendingUnitIds.isEmpty()) {
            int unitId = pendingUnitIds.poll();
            Unit unit = botPlayer.getPlayerDeck().getUnit(unitId);

            if (unit == null || !unit.isAvailable()) {
                continue;
            }

            List<GameEvent> events = performActionForUnit(unit, unitId, botPlayer);
            if (!events.isEmpty()) {
                if (unit.isAvailable() && unit.hasMoved() && botPlayer.getPlayerDeck().getUnit(unitId) != null) {
                    pendingUnitIds.addFirst(unitId); // moved but may still act: its next step is the attack
                }
                return Optional.of(events);
            }
        }

        if (upgradePending) {
            upgradePending = false;
            List<GameEvent> events = upgradeBase(botPlayer);
            if (!events.isEmpty()) {
                return Optional.of(events);
            }
        }

        if (purchasePending) {
            purchasePending = false;
            List<GameEvent> events = purchaseNewUnits(botPlayer);
            if (!events.isEmpty()) {
                return Optional.of(events);
            }
        }
        return Optional.empty();
    }

    // --- Unit actions ---

    private List<GameEvent> performActionForUnit(Unit unit, int unitId, Player botPlayer) {
        if (unit instanceof ArmoredRecoveryVehicle recovery) {
            return recover(recovery, unitId, botPlayer);
        }
        if (unit instanceof SupportClass support && support.getSupportPoints() > 0) {
            return supportOrFollow(support, unitId, botPlayer);
        }
        if (unit instanceof SupportClass scout) {
            return escort(scout, unitId, botPlayer);
        }
        if (unit instanceof AssaultClass attacker) {
            return attackOrReposition(attacker, unitId, botPlayer);
        }
        return List.of();
    }

    /**
     * Greift das lohnendste Ziel an. Leichte Einheiten dürfen dafür erst ziehen: dann wird das Feld gewählt, von dem
     * aus sich der Schuss am meisten lohnt. Ohne lohnendes Ziel zieht die Einheit auf das beste Feld.
     */
    private List<GameEvent> attackOrReposition(AssaultClass attacker, int unitId, Player botPlayer) {
        Optional<Strike> strike = bestStrike(attacker);
        if (strike.isPresent() && strike.get().tile().equals(attacker.getPosition())) {
            System.out.println("Bot: Unit " + unitId + " attacks Unit " + strike.get().targetId());
            return attackCommand.execute(botPlayer, unitId, strike.get().targetId()).getEvents();
        }
        if (strike.isPresent()) {
            return moveTo(botPlayer, unitId, attacker, strike.get().tile()); // and shoot on its next step
        }
        return moveTo(botPlayer, unitId, attacker, bestTile(attacker, goalFor(attacker), tile -> 0));
    }

    /** An attack on a target from a tile (the unit's own, or one it moves to first), with how good a trade it is. */
    private record Strike(Point tile, int targetId, double score) {
    }

    /**
     * The best worthwhile attack: from where the unit stands, or, for units that can act after moving, from any
     * tile it can reach. A strike is worthwhile if the damage dealt outweighs the counterattack.
     */
    private Optional<Strike> bestStrike(AssaultClass attacker) {
        boolean mayMoveFirst = attacker instanceof ActsAfterMoving && !attacker.hasMoved();
        List<Point> tiles = mayMoveFirst ? candidateTiles(attacker) : List.of(attacker.getPosition());
        Strike best = null;
        for (Point tile : tiles) {
            boolean here = tile.equals(attacker.getPosition());
            double range = (here ? model.getShootingRange(attacker) : attacker.getShootingRange()) + 0.4;
            double exposure = here ? 0 : danger(attacker, tile) - danger(attacker, attacker.getPosition());
            for (Map.Entry<Integer, Unit> entry : enemy().getPlayerDeck().getUnits().entrySet()) {
                Unit target = entry.getValue();
                if (target.getPosition().distance(tile) > range || !AttackCommand.canTarget(attacker, target)) continue;
                double trade = tradeScore(attacker, tile, target);
                if (trade <= 0) continue;
                // on a tie: the weaker target, then the tile the unit already stands on
                double score = trade - Math.max(0, exposure) - target.getHealthPoints() * 1e-6 + (here ? 1e-3 : 0);
                if (best == null || score > best.score()) {
                    best = new Strike(tile, entry.getKey(), score);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** The value of an attack from the tile, minus the counterattack it would draw (weighted, we shoot first). */
    private double tradeScore(AssaultClass attacker, Point from, Unit target) {
        double gain = attackScore(attacker, target);
        if (gain <= 0) return 0;
        int damage = attackCommand.predictDamage(attacker, target);
        boolean survives = damage < target.getHealthPoints();
        if (survives && target instanceof AssaultClass defender
                && from.distance(target.getPosition()) <= model.getShootingRange(defender) + 0.4) {
            // it strikes back with the health it has left after our hit
            double left = (double) (target.getHealthPoints() - damage) / target.getMaxHealthPoints();
            int counter = (int) Math.round(attackCommand.predictDamage(defender, attacker, from) * left);
            gain -= COUNTER_WEIGHT * valueOf(attacker) * Math.min(1, (double) counter / attacker.getHealthPoints());
        }
        if (attacker instanceof Aircraft) {
            // the enemy's anti-air umbrella fires at an aircraft that attacks under it
            int flak = 0;
            for (Unit enemy : enemy().getPlayerDeck().getUnits().values()) {
                if (enemy != target && enemy instanceof AntiAir && enemy instanceof AssaultClass antiAir
                        && from.distance(enemy.getPosition()) <= model.getShootingRange(antiAir) + 0.4) {
                    flak += attackCommand.counterDamage(antiAir, attacker);
                }
            }
            gain -= valueOf(attacker) * Math.min(1.5, (double) flak / attacker.getHealthPoints());
        }
        return gain;
    }

    private double attackScore(AssaultClass attacker, Unit target) {
        if (target.getHealthPoints() <= 0) return 0;
        int damage = attackCommand.predictDamage(attacker, target);
        if (damage <= 0) return 0;
        double value = valueOf(target);
        if (damage >= target.getHealthPoints()) {
            return value * KILL_BONUS + 2;
        }
        return value * damage / target.getMaxHealthPoints();
    }

    /** What a unit is worth to its owner: its price, plus its punch, or for a base what it produces. */
    private static double valueOf(Unit unit) {
        if (unit instanceof Base) return HQ_VALUE;
        if (unit instanceof MilitaryBase base) return base.getResourceCost() + base.getIncome() * 3;
        double value = Math.max(1, unit.getResourceCost());
        if (unit instanceof AssaultClass assault) value += assault.getDamage() / 200.0;
        return value;
    }

    /**
     * Where the unit is heading: anti-air hunts aircraft (or escorts the army), aircraft go for the most valuable
     * target, everything else for the nearest enemy unit close by, or else the enemy base.
     */
    private Point goalFor(AssaultClass unit) {
        Point position = unit.getPosition();
        List<Unit> enemies = List.copyOf(enemy().getPlayerDeck().getUnits().values());
        Point enemyBase = enemy().getPlayerDeck().getBase().getPosition();

        if (unit instanceof SelfPropelledAntiAir) {
            // anti-air covers the army; enemy aircraft that come close are shot by its strike planning
            return me().getPlayerDeck().getUnits().values().stream()
                    .filter(friend -> friend instanceof AssaultClass && !(friend instanceof SelfPropelledAntiAir))
                    .min(Comparator.comparingDouble(friend -> friend.getPosition().distance(enemyBase)))
                    .map(Unit::getPosition).orElse(enemyBase);
        }
        if (unit instanceof Aircraft) {
            return enemies.stream()
                    .max(Comparator.comparingDouble(enemy -> valueOf(enemy) / (1 + enemy.getPosition().distance(position) / 6)))
                    .map(Unit::getPosition).orElse(enemyBase);
        }
        return enemies.stream()
                .filter(enemy -> !(enemy instanceof Base) && enemy.getPosition().distance(position) <= 8)
                .min(Comparator.comparingDouble(enemy -> enemy.getPosition().distance(position)))
                .map(Unit::getPosition).orElse(enemyBase);
    }

    /**
     * Heilt bzw. repariert den am stärksten beschädigten Freund in Reichweite, sonst folgt die Einheit ihm.
     */
    private List<GameEvent> supportOrFollow(SupportClass support, int unitId, Player botPlayer) {
        Optional<Integer> target = healCommand.findTargets(botPlayer, unitId).stream()
                .min(Comparator.comparingDouble(id -> healthRatio(botPlayer.getPlayerDeck().getUnit(id))));
        if (target.isPresent()) {
            System.out.println("Bot: Unit " + unitId + " supports Unit " + target.get());
            return healCommand.execute(botPlayer, unitId, target.get()).getEvents();
        }
        Optional<Unit> patient = botPlayer.getPlayerDeck().getUnits().values().stream()
                .filter(friend -> friend != support && support.canSupport(friend) && friend.getHealthPoints() > 0
                        && friend.getHealthPoints() < friend.getMaxHealthPoints())
                .min(Comparator.comparingDouble(BotService::healthRatio));
        if (patient.isEmpty() || isNextTo(support.getPosition(), patient.get().getPosition())) {
            return List.of();
        }
        return moveTo(botPlayer, unitId, support, bestTile(support, patient.get().getPosition(), tile -> 0));
    }

    /** A scout keeps a couple of tiles behind the unit closest to the enemy base, so its spotting covers it. */
    private List<GameEvent> escort(SupportClass scout, int unitId, Player botPlayer) {
        Point enemyBase = enemy().getPlayerDeck().getBase().getPosition();
        Optional<Unit> spearhead = botPlayer.getPlayerDeck().getUnits().values().stream()
                .filter(friend -> friend instanceof AssaultClass)
                .min(Comparator.comparingDouble(friend -> friend.getPosition().distance(enemyBase)));
        if (spearhead.isEmpty()) {
            return List.of();
        }
        Point goal = spearhead.get().getPosition();
        Point best = bestTile(scout, null, tile -> -2 * Math.abs(tile.distance(goal) - ESCORT_DISTANCE));
        return moveTo(botPlayer, unitId, scout, best);
    }

    /**
     * Baut auf angrenzenden Ruinen eine Basis, sobald das Geld reicht, repariert Gebäude oder fährt zur nächsten
     * freien Ruine.
     */
    private List<GameEvent> recover(ArmoredRecoveryVehicle recovery, int unitId, Player botPlayer) {
        for (Point site : model.getBattlefield().getBaseSites()) {
            if (addCommand.isFreeSite(site) && isNextTo(recovery.getPosition(), site)) {
                UnitEnum type = chooseBaseType(botPlayer, site);
                if (botPlayer.getResourcePoints() >= cost(type)) {
                    System.out.println("Bot: Building " + type + " at " + site.x + ", " + site.y);
                    return addCommand.execute(botPlayer, type.name(), site).getEvents();
                }
                if (!isThreatened(site)) {
                    return List.of(); // wait here and save up for it
                }
            }
        }
        List<Integer> repairs = healCommand.findTargets(botPlayer, unitId);
        if (!repairs.isEmpty()) {
            return healCommand.execute(botPlayer, unitId, repairs.get(0)).getEvents();
        }
        Optional<Point> site = targetSite(botPlayer, recovery);
        if (site.isPresent()) {
            Point goal = site.get();
            return moveTo(botPlayer, unitId, recovery, bestTile(recovery, null,
                    tile -> isNextTo(tile, goal) ? 10 : -tile.distance(goal)));
        }
        Optional<Unit> damaged = botPlayer.getPlayerDeck().getBuildings().stream()
                .filter(building -> building.getHealthPoints() < building.getMaxHealthPoints())
                .min(Comparator.comparingDouble(BotService::healthRatio)).map(building -> (Unit) building);
        if (damaged.isPresent() && !isNextTo(recovery.getPosition(), damaged.get().getPosition())) {
            return moveTo(botPlayer, unitId, recovery, bestTile(recovery, damaged.get().getPosition(), tile -> 0));
        }
        return List.of();
    }

    /** The free, safe ruins worth claiming: close to the vehicle and not too far from home. */
    private Optional<Point> targetSite(Player botPlayer, Unit recovery) {
        Point home = botPlayer.getPlayerDeck().getBase().getPosition();
        return model.getBattlefield().getBaseSites().stream()
                .filter(site -> addCommand.isFreeSite(site) && !isThreatened(site))
                .min(Comparator.comparingDouble(site -> site.distance(recovery.getPosition()) + 0.5 * site.distance(home)));
    }

    /** First an air base (it unlocks aircraft), a naval base on the coast, otherwise an army base. */
    private UnitEnum chooseBaseType(Player botPlayer, Point site) {
        List<Unit> own = List.copyOf(botPlayer.getPlayerDeck().getUnits().values());
        if (count(own, AirForceBase.class) == 0) return UnitEnum.AIRFORCEBASE;
        if (count(own, NavyBase.class) == 0 && addCommand.bordersWater(site)) return UnitEnum.NAVYBASE;
        return UnitEnum.ARMYBASE;
    }

    // --- Tile choice ---

    /**
     * The best tile for the unit to end its turn on, its current tile included: what it could shoot from there,
     * how close it gets to the goal (if any), plus the extra score, minus the enemy fire it would face.
     */
    private Point bestTile(Unit unit, Point goal, java.util.function.ToDoubleFunction<Point> extra) {
        return candidateTiles(unit).stream()
                .max(Comparator.comparingDouble((Point tile) -> {
                    double score = extra.applyAsDouble(tile) - danger(unit, tile);
                    if (unit instanceof AssaultClass attacker) score += NEXT_TURN_WEIGHT * bestShotFrom(attacker, tile);
                    if (goal != null) score -= GOAL_WEIGHT * tile.distance(goal);
                    return score;
                }).thenComparing(tile -> tile.equals(unit.getPosition()) ? 0 : 1)) // move rather than idle on a tie
                .orElse(unit.getPosition());
    }

    /** The unit's own tile and, unless it has moved already, every tile it could move to. */
    private List<Point> candidateTiles(Unit unit) {
        List<Point> tiles = new ArrayList<>();
        tiles.add(unit.getPosition());
        if (unit.hasMoved()) {
            return tiles;
        }
        tiles.addAll(Movement.reachable(model, unit).keySet());
        return tiles;
    }

    private double bestShotFrom(AssaultClass attacker, Point tile) {
        double range = attacker.getShootingRange() + 0.4;
        double best = 0;
        for (Unit enemy : enemy().getPlayerDeck().getUnits().values()) {
            if (enemy.getPosition().distance(tile) <= range && AttackCommand.canTarget(attacker, enemy)) {
                best = Math.max(best, attackScore(attacker, enemy));
            }
        }
        return best;
    }

    /** How much the unit stands to lose on the tile next turn, weighted by how fragile its role is. */
    private double danger(Unit unit, Point tile) {
        double expected = 0;
        for (Unit enemy : enemy().getPlayerDeck().getUnits().values()) {
            if (enemy instanceof AssaultClass attacker && AttackCommand.canTarget(attacker, unit)) {
                // only units that can act after moving threaten tiles beyond their shooting range
                int reach = attacker.getShootingRange()
                        + (enemy instanceof ActsAfterMoving && enemy instanceof MovableUnit movable ? movable.getMovingRange() : 0);
                if (tile.distance(enemy.getPosition()) <= reach + 0.4) {
                    expected += Math.min(1, (double) attackCommand.predictDamage(attacker, unit, tile) / unit.getHealthPoints());
                }
            }
        }
        return Math.min(expected, 1.5) * valueOf(unit) * caution(unit) * boldness;
    }

    private static double caution(Unit unit) {
        double caution;
        if (unit instanceof SelfPropelledArtillery) caution = 1.2;
        else if (unit instanceof ArmoredRecoveryVehicle) caution = 3; // the economy depends on it
        else if (unit instanceof SupportClass) caution = 1.5;
        else if (unit instanceof Aircraft) caution = 0.7;
        else caution = 0.5;
        return healthRatio(unit) < 0.35 ? caution * 2 : caution;
    }

    private List<GameEvent> moveTo(Player botPlayer, int unitId, Unit unit, Point tile) {
        if (tile.equals(unit.getPosition())) {
            return List.of();
        }
        System.out.println("Bot: Unit " + unitId + " moves to " + tile.x + ", " + tile.y);
        return moveCommand.execute(botPlayer, unitId, tile).getEvents();
    }

    // --- Economy ---

    /** Upgrades the cheapest base (the headquarters first) when there is money to spare, or it would be wasted. */
    private List<GameEvent> upgradeBase(Player botPlayer) {
        int rp = botPlayer.getResourcePoints();
        boolean overflowing = rp + model.getIncome(botPlayer) > model.getMaxResourcePoints();
        boolean threatened = isThreatened(botPlayer.getPlayerDeck().getBase().getPosition());
        return botPlayer.getPlayerDeck().getUnits().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof MilitaryBase base && base.canUpgrade()
                        && base.getHealthPoints() > 0)
                .min(Comparator.comparingInt((Map.Entry<Integer, Unit> entry) -> ((MilitaryBase) entry.getValue()).getUpgradeCost())
                        .thenComparing(entry -> entry.getValue() instanceof Base ? 0 : 1))
                .filter(entry -> {
                    int cost = ((MilitaryBase) entry.getValue()).getUpgradeCost();
                    return overflowing ? rp >= cost : !threatened && rp - cost >= UPGRADE_SPARE_RP + reserve(botPlayer);
                })
                .map(entry -> {
                    System.out.println("Bot: Upgrading " + entry.getValue().getUnitName());
                    return upgradeCommand.execute(botPlayer, entry.getKey()).getEvents();
                })
                .orElse(List.of());
    }

    /** The money held back for a base the recovery vehicle is about to build. */
    private int reserve(Player botPlayer) {
        for (Unit unit : botPlayer.getPlayerDeck().getUnits().values()) {
            if (unit instanceof ArmoredRecoveryVehicle) {
                Optional<Point> site = targetSite(botPlayer, unit);
                if (site.isPresent() && site.get().distance(unit.getPosition()) <= 6) {
                    return cost(chooseBaseType(botPlayer, site.get()));
                }
            }
        }
        return 0;
    }

    /**
     * Kauft höchstens eine neue Einheit pro Zug: die, die am besten zur Lage passt, mit etwas Zufall. Unter
     * Beschuss der eigenen Basis wird nicht gespart.
     */
    private List<GameEvent> purchaseNewUnits(Player botPlayer) {
        boolean threatened = isThreatened(botPlayer.getPlayerDeck().getBase().getPosition());
        int budget = botPlayer.getResourcePoints() - (threatened ? 0 : reserve(botPlayer));
        Army army = new Army(botPlayer);

        // what next turn's money could buy, without the noise: if that is clearly better, save up for it
        int nextBudget = budget + model.getIncome(botPlayer);
        double bestNow = 0;
        double bestNext = 0;
        UnitEnum best = null;
        double bestScore = 0;
        for (UnitEnum type : UnitEnum.values()) {
            int cost = cost(type);
            if (cost > nextBudget || !roster.test(type)) continue;
            // every copy the bot already has makes another one less attractive, so the army stays mixed
            double desire = desire(type, army, threatened) / (1 + REPEAT_PENALTY * army.owned(unitName(type)));
            if (desire <= 0 || findPlacementPosition(botPlayer, type).isEmpty()) continue;
            double worth = desire + 0.15 * cost;
            bestNext = Math.max(bestNext, worth);
            if (cost > budget) continue;
            bestNow = Math.max(bestNow, worth);
            double score = worth + PURCHASE_NOISE * random.nextDouble();
            if (score > bestScore) {
                best = type;
                bestScore = score;
            }
        }
        if (!threatened && bestNext > bestNow * SAVE_FOR_BETTER) {
            return List.of();
        }
        if (best == null) {
            return List.of();
        }
        UnitEnum type = best;
        return findPlacementPosition(botPlayer, type).map(pos -> {
            System.out.println("Bot: Adding new " + type + " at " + pos.x + ", " + pos.y);
            return addCommand.execute(botPlayer, type.name(), pos).getEvents();
        }).orElse(List.of());
    }

    /** How much the bot wants another one of these, given both armies; 0 means not at all. */
    private double desire(UnitEnum type, Army army, boolean threatened) {
        double defence = threatened ? 1 : 0;
        boolean airHeavy = army.aircraft * 2 > army.combat;
        return switch (type) {
            case TYPETEN -> 2 + 0.6 * army.enemyTanks + defence;
            case TYPENINETY -> 2 + 0.5 * army.enemyTanks + 1.5 * defence;
            case TYPESIXTEEN -> 1.5 + 0.4 * army.enemySoft;
            case TYPEEIGHTYNINE -> 1 + 0.5 * army.enemySoft;
            // artillery needs a front line to hide behind
            case TYPENINETYNINESPH -> army.combat - army.artillery < 3 ? 0 : army.artillery * 5 < army.combat ? 2 + defence : 0;
            case TYPEEIGHTYONEC, TYPENINETYTHREE, TYPEEIGHTYSEVEN -> {
                double base = army.enemyAircraft > army.antiAir ? 3 + army.enemyAircraft - army.antiAir
                        : army.antiAir == 0 && army.combat >= 4 ? 0.8 : 0;
                yield base == 0 ? 0 : base + (type == UnitEnum.TYPEEIGHTYONEC ? 0.3 : 0);
            }
            case ARMOREDAMBULANCE -> army.combat >= 3 && army.ambulances == 0 ? 2.2 : 0;
            case ARMOREDRECOVERYVEHICLE -> army.recovery > 0 ? 0
                    : army.claimableSite && army.combat >= 1 ? 3.5 : army.hqDamaged ? 2.5 : 0;
            case ARMOREDRECONNAISSANCE -> army.combat >= 4 && army.recon == 0 ? 1.2 : 0;
            case FFOUREJKAI -> airHeavy ? 0.4 : 1.3 + 0.3 * army.enemySoft;
            case FFIFTEEN -> airHeavy ? 0.5 : 1.8 + 0.5 * army.enemyAircraft;
            case FTWO -> airHeavy ? 0.6 : 2 + 0.3 * army.enemyTanks;
            case FTHIRTYFIVE -> airHeavy ? 0.7 : 2.2;
            case ATEN -> airHeavy ? 0.6 : 1.5 + 0.6 * army.enemyTanks;
            case YAMATO -> 2.5;
            case KONGO -> 2 + 0.5 * army.enemyAircraft + army.enemySubmarines;
            case SORYU -> army.enemyShips > 0 ? 2 + army.enemyShips : 0; // only worth it against a fleet
            // filler: a few riflemen help, a horde of them does not
            case INFANTRY -> army.infantry * 3 > army.combat ? 0.2 : 0.8 + 0.15 * army.enemySoft;
            case ANTITANKTEAM -> 0.8 + 0.7 * army.enemyTanks;
            case AHSIXTYFOURD -> airHeavy ? 0.5 : 1.6 + 0.6 * army.enemyTanks;
            case RADARSTATION -> army.combat >= 5 && army.radar == 0 ? 1 : 0;
            // bases are built on ruins by the recovery vehicles, never bought here
            case ARMYBASE, AIRFORCEBASE, NAVYBASE -> 0;
        };
    }

    /** What both sides field, counted once per purchase decision. */
    private final class Army {
        final long combat, antiAir, artillery, infantry, aircraft, ambulances, recon, recovery, radar;
        final long enemyAircraft, enemyTanks, enemySoft, enemyShips, enemySubmarines;
        final boolean claimableSite, hqDamaged;
        private final List<Unit> own;

        Army(Player botPlayer) {
            own = List.copyOf(botPlayer.getPlayerDeck().getUnits().values());
            List<Unit> enemy = List.copyOf(enemy().getPlayerDeck().getUnits().values());
            combat = count(own, AssaultClass.class);
            antiAir = count(own, AntiAir.class);
            artillery = count(own, SelfPropelledArtillery.class);
            infantry = count(own, Infantry.class);
            aircraft = count(own, Aircraft.class);
            ambulances = count(own, ArmoredAmbulanceVehicle.class);
            recon = count(own, ArmoredReconaissanceVehicle.class);
            recovery = count(own, ArmoredRecoveryVehicle.class);
            radar = count(own, Radarstation.class);
            enemyAircraft = count(enemy, Aircraft.class);
            enemyTanks = count(enemy, AssaultTank.class);
            enemyShips = count(enemy, Ship.class);
            enemySubmarines = count(enemy, Submarine.class);
            enemySoft = enemy.stream().filter(unit -> unit instanceof AssaultClass
                    && !(unit instanceof AssaultTank) && !(unit instanceof Aircraft)).count();
            claimableSite = model.getBattlefield().getBaseSites().stream()
                    .anyMatch(site -> addCommand.isFreeSite(site) && !isThreatened(site));
            hqDamaged = healthRatio(botPlayer.getPlayerDeck().getBase()) < 0.7;
        }

        long owned(String unitName) {
            return own.stream().filter(unit -> unit.getUnitName().equals(unitName)).count();
        }
    }

    // --- Helpers ---

    /** True if an enemy combat unit stands close to the point. */
    private boolean isThreatened(Point point) {
        return enemy().getPlayerDeck().getUnits().values().stream()
                .anyMatch(enemy -> enemy instanceof AssaultClass && enemy.getPosition().distance(point) <= THREAT_RADIUS);
    }

    private static boolean isNextTo(Point a, Point b) {
        return Command.isNextTo(a, b);
    }

    private static double healthRatio(Unit unit) {
        return (double) unit.getHealthPoints() / unit.getMaxHealthPoints();
    }

    private static long count(List<Unit> units, Class<?> kind) {
        return units.stream().filter(kind::isInstance).count();
    }

    private int cost(UnitEnum type) {
        return unitFactory.createUnit(playerId, type, new Point()).getResourceCost();
    }

    private String unitName(UnitEnum type) {
        return unitFactory.createUnit(playerId, type, new Point()).getUnitName();
    }

    /** Buildings go close to home, everything else as close to the enemy base as possible. */
    private Optional<Point> findPlacementPosition(Player botPlayer, UnitEnum type) {
        Unit unit = unitFactory.createUnit(botPlayer.getPlayerID(), type, new Point());
        Point goal = unit instanceof Infrastructure
                ? botPlayer.getPlayerDeck().getBase().getPosition()
                : enemy().getPlayerDeck().getBase().getPosition();
        return addCommand.findDeployablePositions(botPlayer, unit).stream()
                .min(Comparator.comparingDouble(p -> p.distance(goal)));
    }
}
