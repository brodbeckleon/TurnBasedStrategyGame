package game.controller;

import game.events.GameEvent;
import game.game.Ability;
import game.game.Command;
import game.game.Movement;
import game.enums.TerrainEnum;
import game.game.Player;
import game.game.Research;
import game.game.UnitFactory;
import game.game.commands.AbilityCommand;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.CommandResult;
import game.game.commands.HealCommand;
import game.game.commands.MoveCommand;
import game.game.commands.ResearchCommand;
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
import game.units.TransportShip;
import game.units.transports.Izumo;
import game.units.transports.Osumi;

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
    private static final double REPEAT_PENALTY = 0.35;
    private static final double SAVE_FOR_BETTER = 1.3;
    private static final double FLEET_REACH = 7;
    private static final double COUNTER_WEIGHT = 0.6;
    private static final double MIN_CAUTION = 0.3;
    private static final double MAX_CAUTION = 1.5;
    private static final double FERRY_BONUS = 6;
    private static final double ABILITY_WORTH = 0.8; // what an ability's score must reach per resource point it costs
    private static final double MISSILE_WORTH = 0.5;
    private static final double PICK_UP_DETOUR = 4; // how far an amphibious ship with a unit aboard goes for another
    private static final int MAX_PURCHASES = 4; // units bought per turn at most
    private static final int RICH = 20; // resource points beyond which the bot upgrades even with little to spare
    private static final int EXTRA_PURCHASE_BUDGET = 12; // savings needed to buy more than one unit a turn
    private static final double CUT_OFF_GROUND = 0.35; // ground units wanted on an island without a ferry
    private static final double FERRIED_GROUND = 0.75; // ... and with one

    private final Game model;
    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final UpgradeCommand upgradeCommand;
    private final ResearchCommand researchCommand;
    private final AbilityCommand abilityCommand;
    private final Random random;
    private final int playerId;
    private Predicate<UnitEnum> roster = type -> true;
    private Difficulty difficulty = Difficulty.NORMAL;
    private final UnitFactory unitFactory = new UnitFactory();

    private final Deque<Integer> pendingUnitIds = new ArrayDeque<>();
    private boolean upgradePending = false;
    private boolean researchPending = false;
    private boolean abilitiesPending = false;
    private double boldness = 1;
    private boolean purchasePending = false;
    private int purchases = 0;

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
        this.researchCommand = new ResearchCommand(model);
        this.abilityCommand = new AbilityCommand(model);
        this.random = random;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** Limits what the bot may buy; everything by default. */
    public void setRoster(Predicate<UnitEnum> roster) {
        this.roster = roster;
    }

    private Player me() {
        return model.getPlayer(playerId);
    }

    /** The enemies the bot can see: it plays by the fog of war like the player. The ruins of the knocked out don't count. */
    private List<Unit> visibleEnemies() {
        return model.getVisibleEnemies(me()).stream()
                .filter(enemy -> !model.isEliminated(model.getOwner(enemy)))
                .toList();
    }

    /**
     * The headquarters the bot marches on: in a free-for-all the nearest enemy base still standing (bases never
     * hide, so this is no cheating).
     */
    private Point enemyBase() {
        Point home = me().getPlayerDeck().getBase().getPosition();
        return model.getEnemies(me()).stream()
                .map(enemy -> enemy.getPlayerDeck().getBase().getPosition())
                .min(Comparator.comparingDouble(base -> base.distance(home)))
                .orElse(home);
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
     * (after the abilities, long-range fire first, the builders last), then at most one upgrade, one research
     * project and a few purchases.
     */
    public void startTurn() {
        pendingUnitIds.clear();
        me().getPlayerDeck().getUnits().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Integer, Unit> entry) -> actionOrder(entry.getValue()))
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> pendingUnitIds.add(entry.getKey()));
        upgradePending = true;
        researchPending = true;
        abilitiesPending = true;
        purchasePending = true;
        purchases = 0;
        boldness = boldness();
        landmasses.clear(); // mapped afresh when first needed
    }

    /**
     * How much the bot may ignore enemy fire this turn: cautious while outgunned, bold with the stronger army or a
     * full one it can afford to replace, so evenly matched armies do not just wait for each other forever.
     */
    private double boldness() {
        if (addCommand.isArmyFull(me()) && me().getResourcePoints() > RICH) {
            return MIN_CAUTION; // a full army with money in the bank replaces its losses at once: nothing to save
        }
        double own = strength(me().getPlayerDeck().getUnits().values());
        double enemy = strength(visibleEnemies());
        if (own + enemy == 0) return 1;
        return Math.max(MIN_CAUTION, Math.min(MAX_CAUTION, 2 * enemy / (own + enemy)));
    }

    private static double strength(java.util.Collection<Unit> units) {
        return units.stream()
                .filter(unit -> unit instanceof AssaultClass)
                .mapToDouble(unit -> valueOf(unit) * healthRatio(unit))
                .sum();
    }

    private static int actionOrder(Unit unit) {
        if (unit instanceof TransportShip) return -1; // sail first, so the units on deck can go ashore this turn
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

        // shells and bombs first, so the units can finish off what they leave
        if (abilitiesPending) {
            List<GameEvent> events = useAbility(botPlayer);
            if (!events.isEmpty()) {
                return Optional.of(events);
            }
            abilitiesPending = false;
        }

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

        if (researchPending) {
            researchPending = false;
            List<GameEvent> events = research(botPlayer);
            if (!events.isEmpty()) {
                return Optional.of(events);
            }
        }

        // with money saved up it buys several units, one step each, as long as it finds something worth it
        if (purchasePending) {
            purchases++;
            purchasePending = purchases < MAX_PURCHASES;
            List<GameEvent> events = purchaseNewUnits(botPlayer, purchases > 1);
            if (!events.isEmpty()) {
                actAtOnce(events);
                return Optional.of(events);
            }
            purchasePending = false;
        }
        return Optional.empty();
    }

    /** With Rapid Deployment a unit just deployed can act this turn: it joins the queue of units still to act. */
    private void actAtOnce(List<GameEvent> events) {
        for (GameEvent event : events) {
            if (event instanceof GameEvent.UnitDeployed deployed) {
                Unit unit = me().getPlayerDeck().getUnit(deployed.unitId());
                if (unit != null && unit.isAvailable() && !(unit instanceof Infrastructure)) {
                    pendingUnitIds.add(deployed.unitId());
                }
            }
        }
    }

    // --- Unit actions ---

    private List<GameEvent> performActionForUnit(Unit unit, int unitId, Player botPlayer) {
        if (unit instanceof TransportShip ship) {
            return sail(ship, unitId, botPlayer);
        }
        if (model.getHostOf(unit) instanceof Osumi && !(unit instanceof ArmoredRecoveryVehicle)) {
            return disembark(unit, unitId, botPlayer);
        }
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
        Optional<Strike> strike = bestStrike(attacker, unitId);
        if (strike.isPresent() && strike.get().tile().equals(attacker.getPosition())) {
            System.out.println("Bot: Unit " + unitId + " attacks Unit " + strike.get().targetId());
            return attackCommand.execute(botPlayer, unitId, strike.get().targetId()).getEvents();
        }
        if (strike.isPresent()) {
            return moveTo(botPlayer, unitId, attacker, strike.get().tile()); // and shoot on its next step
        }
        Point goal = goalFor(attacker);
        if (isGround(attacker) && !reachableOverLand(attacker, attacker.getPosition(), goal)) {
            List<GameEvent> boarding = ferry(attacker, unitId, botPlayer);
            if (!boarding.isEmpty()) return boarding;
        }
        return moveTo(botPlayer, unitId, attacker, bestTile(attacker, goal, tile -> 0));
    }

    /** An attack on a target from a tile (the unit's own, or one it moves to first), with how good a trade it is. */
    private record Strike(Point tile, int targetId, double score) {
    }

    /**
     * The best worthwhile attack: from where the unit stands, or, for units that can act after moving, from any
     * tile it can reach. A strike is worthwhile if the damage dealt outweighs the counterattack. The targets come
     * from the attack command, so the bot plays by exactly the rules the player's clicks do.
     */
    private Optional<Strike> bestStrike(AssaultClass attacker, int unitId) {
        boolean mayMoveFirst = attacker instanceof ActsAfterMoving && !attacker.hasMoved();
        List<Point> tiles = mayMoveFirst ? candidateTiles(attacker) : List.of(attacker.getPosition());
        Strike best = null;
        List<Strike> worthwhile = new ArrayList<>();
        for (Point tile : tiles) {
            boolean here = tile.equals(attacker.getPosition());
            double exposure = here ? 0 : danger(attacker, tile) - danger(attacker, attacker.getPosition());
            for (int targetId : attackCommand.findTargetsFrom(me(), unitId, tile)) {
                Unit target = model.findUnitById(targetId);
                double trade = tradeScore(attacker, tile, target);
                if (trade <= 0) continue;
                // on a tie: the weaker target, then the tile the unit already stands on
                double score = trade - Math.max(0, exposure) - target.getHealthPoints() * 1e-6 + (here ? 1e-3 : 0);
                Strike strike = new Strike(tile, targetId, score);
                worthwhile.add(strike);
                if (best == null || score > best.score()) {
                    best = strike;
                }
            }
        }
        if (best != null && random.nextDouble() < difficulty.blunderChance) {
            // a weaker bot misjudges: half the time it takes some other shot, half the time none at all
            return random.nextBoolean() ? Optional.of(worthwhile.get(random.nextInt(worthwhile.size()))) : Optional.empty();
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
            for (Unit enemy : visibleEnemies()) {
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
     * target, everything else for the nearest enemy unit close by it can fight, or else the enemy base.
     */
    private Point goalFor(AssaultClass unit) {
        Point position = unit.getPosition();
        List<Unit> enemies = visibleEnemies();
        Point enemyBase = enemyBase();

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
        // a ground unit only chases what it can fight from land: not a ship out at sea, nor what its guns cannot hit
        return enemies.stream()
                .filter(enemy -> !(enemy instanceof Base) && enemy.getPosition().distance(position) <= 8)
                .filter(enemy -> !isGround(unit) || AttackCommand.canTarget(unit, enemy) && canLandAt(unit, enemy.getPosition()))
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
        Point enemyBase = enemyBase();
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
     * A carrier or amphibious ship heads for the enemy coast, keeping out of harm's way: new units are deployed
     * onto its deck there (the purchase picks the spot closest to the enemy), and they go ashore from it.
     */
    private List<GameEvent> sail(TransportShip ship, int unitId, Player botPlayer) {
        if (ship instanceof Osumi) {
            Unit passenger = model.getGuestOf(ship);
            Optional<Unit> waiting = strandedUnits(botPlayer).stream()
                    .min(Comparator.comparingDouble(unit -> unit.getPosition().distance(ship.getPosition())));
            // with room on deck it picks up a unit waiting close by first (any, while the deck is empty)
            if (waiting.isPresent() && model.hasRoomOnTop(ship)
                    && (passenger == null || waiting.get().getPosition().distance(ship.getPosition()) <= PICK_UP_DETOUR)) {
                Point at = waiting.get().getPosition();
                return moveTo(botPlayer, unitId, ship, bestTile(ship, at,
                        tile -> tile.distance(at) < 1.5 ? FERRY_BONUS : 0));
            }
            if (passenger != null) {
                // dock at a shore from which the unit on deck can get where it wants to go
                Point target = destinationOf(passenger);
                if (target != null) {
                    return moveTo(botPlayer, unitId, ship, bestTile(ship, target,
                            tile -> landsFrom(passenger, tile, target) ? FERRY_BONUS : 0));
                }
            }
        }
        Point enemyBase = enemyBase();
        double standOff = ship instanceof Osumi ? 1.5 : 5;
        Point best = bestTile(ship, null, tile -> -Math.abs(tile.distance(enemyBase) - standOff)
                + (ship instanceof Osumi && addCommand.isDocked(tile) ? 1 : 0));
        return moveTo(botPlayer, unitId, ship, best);
    }

    // --- Ferrying ---

    /**
     * Which landmass each tile belongs to, per way of getting around: a map for every set of terrains a unit can
     * cross (the eight directions units move in), so a ridge cuts a vehicle off where infantry walks over it. Tiles
     * the unit cannot enter have none. Built when first needed each turn.
     */
    private final Map<List<Boolean>, Map<Point, Integer>> landmasses = new java.util.HashMap<>();

    private Map<Point, Integer> landIds(Unit unit) {
        List<Boolean> passable = java.util.Arrays.stream(TerrainEnum.values())
                .map(terrain -> terrain != TerrainEnum.WATER && Movement.cost(unit, terrain) != Movement.IMPASSABLE)
                .toList();
        return landmasses.computeIfAbsent(passable, key -> floodFill(terrain -> key.get(terrain.ordinal())));
    }

    private Map<Point, Integer> floodFill(Predicate<TerrainEnum> passable) {
        Map<Point, Integer> ids = new java.util.HashMap<>();
        int next = 0;
        for (Map.Entry<Point, TerrainEnum> entry : model.getBattlefield().getGeography().entrySet()) {
            if (!passable.test(entry.getValue()) || ids.containsKey(entry.getKey())) continue;
            Deque<Point> queue = new ArrayDeque<>(List.of(entry.getKey()));
            ids.put(entry.getKey(), next);
            while (!queue.isEmpty()) {
                Point point = queue.poll();
                for (Point around : around(point)) {
                    TerrainEnum terrain = model.getBattlefield().getGeographyOfPoint(around);
                    if (terrain != null && passable.test(terrain) && !ids.containsKey(around)) {
                        ids.put(around, next);
                        queue.add(around);
                    }
                }
            }
            next++;
        }
        return ids;
    }

    /** The landmass the unit would be on at the point, or -1 if it cannot stand there on its own (water, a ridge). */
    private int landOf(Unit unit, Point point) {
        return landIds(unit).getOrDefault(point, -1);
    }

    private static List<Point> around(Point point) {
        List<Point> around = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) around.add(new Point(point.x + dx, point.y + dy));
            }
        }
        return around;
    }

    /**
     * True if the ground unit, standing on the land tile, can get onto or next to the target without crossing water
     * or ground it cannot enter.
     */
    private boolean reachableOverLand(Unit unit, Point from, Point target) {
        int land = landOf(unit, from);
        if (land < 0) return false;
        if (landOf(unit, target) == land) return true;
        return around(target).stream().anyMatch(point -> landOf(unit, point) == land);
    }

    /** True if the land route from the bot's headquarters to the enemy's is open, at least on foot. */
    private boolean headquartersConnected(Player botPlayer) {
        Unit walker = unitFactory.createUnit(playerId, UnitEnum.INFANTRY, new Point());
        return reachableOverLand(walker, botPlayer.getPlayerDeck().getBase().getPosition(), enemyBase());
    }

    /** True if the unit, on a ship at the tile, could go ashore somewhere from which it reaches the target over land. */
    private boolean landsFrom(Unit unit, Point tile, Point target) {
        return around(tile).stream().anyMatch(shore -> reachableOverLand(unit, shore, target));
    }

    /** True if the unit on deck can go ashore this turn onto land from which it reaches the target. */
    private boolean landsWithin(Unit unit, Point target) {
        return isNextTo(unit.getPosition(), target) || Movement.reachable(model, unit).keySet().stream()
                .anyMatch(tile -> reachableOverLand(unit, tile, target));
    }

    /** True if the ground unit could stand on or next to the point, wherever it is (it may need a ship to get there). */
    private boolean canLandAt(Unit unit, Point point) {
        return landOf(unit, point) >= 0 || around(point).stream().anyMatch(next -> landOf(unit, next) >= 0);
    }

    /** Ground units: what amphibious ships carry. */
    private static boolean isGround(Unit unit) {
        return unit instanceof game.interfaces.militaryBranches.Army && !(unit instanceof Aircraft)
                && !(unit instanceof Infrastructure);
    }

    /** Where a ground unit is heading: a recovery vehicle to its ruins, a fighting unit to the enemy. */
    private Point destinationOf(Unit unit) {
        if (unit instanceof ArmoredRecoveryVehicle) {
            return targetSite(me(), unit).orElse(null);
        }
        return unit instanceof AssaultClass attacker ? goalFor(attacker) : enemyBase();
    }

    /** The bot's ground units on land that cannot get where they are heading without a ship. */
    private List<Unit> strandedUnits(Player botPlayer) {
        return botPlayer.getPlayerDeck().getUnits().values().stream()
                .filter(unit -> isGround(unit) && (unit instanceof AssaultClass || unit instanceof ArmoredRecoveryVehicle))
                .filter(unit -> model.getHostOf(unit) == null && landOf(unit, unit.getPosition()) >= 0)
                .filter(unit -> {
                    Point target = destinationOf(unit);
                    return target != null && !reachableOverLand(unit, unit.getPosition(), target);
                })
                .toList();
    }

    /** A unit that cannot get there over land boards the nearest free amphibious ship, or heads for it. */
    private List<GameEvent> ferry(Unit unit, int unitId, Player botPlayer) {
        Optional<Unit> ship = freeFerry(botPlayer, unit);
        if (ship.isEmpty()) {
            return List.of();
        }
        Point deck = ship.get().getPosition();
        if (Movement.reachable(model, unit).containsKey(deck)) {
            System.out.println("Bot: Unit " + unitId + " boards the " + ship.get().getUnitName());
            return moveTo(botPlayer, unitId, unit, deck);
        }
        return moveTo(botPlayer, unitId, unit, bestTile(unit, deck, tile -> 0));
    }

    /** The bot's amphibious ship with room on deck closest to the unit, if it has one. */
    private Optional<Unit> freeFerry(Player botPlayer, Unit unit) {
        return botPlayer.getPlayerDeck().getUnits().values().stream()
                .filter(own -> own instanceof Osumi && own.getHealthPoints() > 0 && model.hasRoomOnTop(own))
                .min(Comparator.comparingDouble(own -> own.getPosition().distance(unit.getPosition())));
    }

    /**
     * A unit on deck shoots if it can, goes ashore once the ship lies at a shore from which it reaches its goal,
     * and otherwise stays aboard for the crossing.
     */
    private List<GameEvent> disembark(Unit unit, int unitId, Player botPlayer) {
        if (unit instanceof AssaultClass attacker) {
            Optional<Strike> strike = bestStrike(attacker, unitId);
            if (strike.isPresent() && strike.get().tile().equals(attacker.getPosition())) {
                return attackCommand.execute(botPlayer, unitId, strike.get().targetId()).getEvents();
            }
        }
        Point target = destinationOf(unit);
        if (target == null) {
            target = enemyBase();
        }
        Point goal = target;
        Optional<Point> ashore = Movement.reachable(model, unit).keySet().stream()
                .filter(tile -> reachableOverLand(unit, tile, goal))
                .min(Comparator.comparingDouble((Point tile) -> danger(unit, tile) + GOAL_WEIGHT * tile.distance(goal)));
        if (ashore.isEmpty()) {
            return List.of();
        }
        System.out.println("Bot: Unit " + unitId + " goes ashore at " + ashore.get().x + ", " + ashore.get().y);
        return moveTo(botPlayer, unitId, unit, ashore.get());
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
            boolean aboard = model.getHostOf(recovery) instanceof Osumi;
            if (aboard && !landsWithin(recovery, goal)) {
                return List.of(); // the ship takes it there
            }
            List<GameEvent> events = !aboard && !reachableOverLand(recovery, recovery.getPosition(), goal)
                    ? ferry(recovery, unitId, botPlayer) // ruins on another island
                    : moveTo(botPlayer, unitId, recovery, bestTile(recovery, null,
                            tile -> isNextTo(tile, goal) ? 10 : -tile.distance(goal)));
            if (!events.isEmpty()) {
                return events;
            }
            // stuck on the way: it may as well see to the buildings
        }
        Optional<Unit> damaged = botPlayer.getPlayerDeck().getBuildings().stream()
                .filter(building -> building.getHealthPoints() < building.getMaxHealthPoints())
                .min(Comparator.comparingDouble(BotService::healthRatio)).map(building -> (Unit) building);
        if (damaged.isPresent() && !isNextTo(recovery.getPosition(), damaged.get().getPosition())) {
            return moveTo(botPlayer, unitId, recovery, bestTile(recovery, damaged.get().getPosition(), tile -> 0));
        }
        return List.of();
    }

    /**
     * The free, safe ruins worth claiming that the vehicle can get to: over land if any, else across the water when
     * it is aboard an amphibious ship or one has room for it. Close to the vehicle and not too far from home.
     */
    private Optional<Point> targetSite(Player botPlayer, Unit recovery) {
        Point home = botPlayer.getPlayerDeck().getBase().getPosition();
        Point position = recovery.getPosition();
        boolean shipping = model.getHostOf(recovery) instanceof Osumi || freeFerry(botPlayer, recovery).isPresent();
        return model.getBattlefield().getBaseSites().stream()
                .filter(site -> addCommand.isFreeSite(site) && !isThreatened(site))
                .filter(site -> reachableOverLand(recovery, position, site) || shipping && canLandAt(recovery, site))
                .min(Comparator.comparing((Point site) -> !reachableOverLand(recovery, position, site))
                        .thenComparingDouble(site -> site.distance(position) + 0.5 * site.distance(home)));
    }

    /**
     * First an air base (it unlocks aircraft), a naval base on the coast, otherwise an army base. On island maps
     * the harbour comes first: without ships the army never leaves home.
     */
    private UnitEnum chooseBaseType(Player botPlayer, Point site) {
        List<Unit> own = List.copyOf(botPlayer.getPlayerDeck().getUnits().values());
        boolean islands = !model.getBattlefield().getType().landBridges();
        if (islands && count(own, NavyBase.class) == 0 && addCommand.bordersWater(site)) return UnitEnum.NAVYBASE;
        if (count(own, AirForceBase.class) == 0) return UnitEnum.AIRFORCEBASE;
        if (count(own, NavyBase.class) == 0 && addCommand.bordersWater(site) && waterReachesEnemy(site)) return UnitEnum.NAVYBASE;
        return UnitEnum.ARMYBASE;
    }

    /**
     * True if the water next to the point is part of a sea that comes close to the enemy base, so ships launched
     * there can join the fight (a fleet in a land-locked lake is wasted).
     */
    private boolean waterReachesEnemy(Point shore) {
        Point enemyBase = enemyBase();
        java.util.Set<Point> seen = new java.util.HashSet<>();
        Deque<Point> queue = new ArrayDeque<>();
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Point next = new Point(shore.x + d[0], shore.y + d[1]);
            if (model.getBattlefield().getGeographyOfPoint(next) == TerrainEnum.WATER && seen.add(next)) queue.add(next);
        }
        while (!queue.isEmpty()) {
            Point water = queue.poll();
            if (water.distance(enemyBase) <= FLEET_REACH) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                Point next = new Point(water.x + d[0], water.y + d[1]);
                if (model.getBattlefield().getGeographyOfPoint(next) == TerrainEnum.WATER && seen.add(next)) queue.add(next);
            }
        }
        return false;
    }

    // --- Tile choice ---

    /**
     * The best tile for the unit to end its turn on, its current tile included: what it could shoot from there,
     * how close it gets to the goal (if any), plus the extra score, minus the enemy fire it would face.
     */
    private Point bestTile(Unit unit, Point goal, java.util.function.ToDoubleFunction<Point> extra) {
        int unitId = model.getUnitID(unit);
        Map<Point, Double> scores = new java.util.HashMap<>(); // each tile is scored once
        for (Point tile : candidateTiles(unit)) {
            double score = extra.applyAsDouble(tile) - danger(unit, tile);
            if (unit instanceof AssaultClass attacker) score += NEXT_TURN_WEIGHT * bestShotFrom(attacker, unitId, tile);
            if (goal != null) score -= GOAL_WEIGHT * tile.distance(goal);
            scores.putIfAbsent(tile, score);
        }
        return scores.keySet().stream()
                .max(Comparator.comparingDouble((Point tile) -> scores.get(tile))
                        .thenComparing(tile -> tile.equals(unit.getPosition()) ? 0 : 1)) // move rather than idle on a tie
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

    /** The best shot the unit would have from the tile, by the same rules as an actual attack. */
    private double bestShotFrom(AssaultClass attacker, int unitId, Point tile) {
        double best = 0;
        for (int targetId : attackCommand.findTargetsFrom(me(), unitId, tile)) {
            best = Math.max(best, attackScore(attacker, model.findUnitById(targetId)));
        }
        return best;
    }

    /** How much the unit stands to lose on the tile next turn, weighted by how fragile its role is. */
    private double danger(Unit unit, Point tile) {
        double expected = 0;
        for (Unit enemy : visibleEnemies()) {
            if (enemy instanceof AssaultClass attacker && AttackCommand.canTarget(attacker, unit)) {
                // only units that can act after moving threaten tiles beyond their shooting range
                int reach = attacker.getShootingRange()
                        + (enemy instanceof ActsAfterMoving && enemy instanceof MovableUnit movable ? movable.getMovingRange() : 0);
                if (tile.distance(enemy.getPosition()) <= reach + 0.4) {
                    expected += Math.min(1, (double) attackCommand.predictDamage(attacker, unit, tile) / unit.getHealthPoints());
                }
            }
        }
        return Math.min(expected, 1.5) * valueOf(unit) * caution(unit) * boldness * difficulty.caution;
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
        boolean overflowing = rp + model.getIncome(botPlayer) > RICH;
        boolean threatened = isThreatened(botPlayer.getPlayerDeck().getBase().getPosition());
        return botPlayer.getPlayerDeck().getUnits().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof MilitaryBase base && base.canUpgrade(model.getMaxBaseLevel(botPlayer))
                        && base.getHealthPoints() > 0)
                .min(Comparator.comparingInt((Map.Entry<Integer, Unit> entry) -> ((MilitaryBase) entry.getValue()).getUpgradeCost())
                        .thenComparing(entry -> entry.getValue() instanceof Base ? 0 : 1))
                .filter(entry -> {
                    int cost = ((MilitaryBase) entry.getValue()).getUpgradeCost();
                    return overflowing ? rp >= cost : !threatened && rp - cost >= difficulty.upgradeSpareRp + reserve(botPlayer);
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
    private List<GameEvent> purchaseNewUnits(Player botPlayer, boolean another) {
        boolean threatened = isThreatened(botPlayer.getPlayerDeck().getBase().getPosition());
        int budget = botPlayer.getResourcePoints() - (threatened ? 0 : reserve(botPlayer));
        if (another && budget < EXTRA_PURCHASE_BUDGET) {
            return List.of(); // a second unit this turn only out of savings
        }
        Army army = new Army(botPlayer);

        // what next turn's money could buy, without the noise: if that is clearly better, save up for it
        int nextBudget = budget + model.getIncome(botPlayer);
        double bestNow = 0;
        double bestNext = 0;
        UnitEnum best = null;
        double bestScore = 0;
        boolean armyFull = addCommand.isArmyFull(botPlayer);
        for (UnitEnum type : UnitEnum.values()) {
            int cost = cost(type);
            if (cost > nextBudget || !roster.test(type) || !botPlayer.hasUnlocked(type)) continue;
            if (armyFull && !(unitFactory.createUnit(playerId, type, new Point()) instanceof Infrastructure)) continue;
            // every copy the bot already has makes another one less attractive, so the army stays mixed
            double desire = desire(type, army, threatened) / (1 + REPEAT_PENALTY * army.owned(unitName(type)));
            if (army.cutOff && isGround(unitFactory.createUnit(playerId, type, new Point())) && type != UnitEnum.ARMOREDRECOVERYVEHICLE) {
                // an army that cannot reach the enemy over land needs ships to get anywhere
                desire *= army.amphibious > 0 ? FERRIED_GROUND : CUT_OFF_GROUND;
            }
            if (desire <= 0 || findPlacementPosition(botPlayer, type).isEmpty()) continue;
            double worth = desire + 0.15 * cost;
            bestNext = Math.max(bestNext, worth);
            if (cost > budget) continue;
            bestNow = Math.max(bestNow, worth);
            double score = worth + difficulty.purchaseNoise * random.nextDouble();
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

    /**
     * Researches the next project of its plan, but only once the army is (nearly) at its limit or the money piles
     * up anyway: until then units are worth more.
     */
    private List<GameEvent> research(Player botPlayer) {
        int rp = botPlayer.getResourcePoints();
        boolean crowded = model.getArmySize(botPlayer) >= model.getArmyLimit(botPlayer) - 1;
        boolean rich = rp + model.getIncome(botPlayer) > RICH;
        if (!crowded && !rich) {
            return List.of();
        }
        int keep = reserve(botPlayer) + (crowded ? 0 : difficulty.upgradeSpareRp);
        for (Research project : researchPlan(botPlayer)) {
            if (researchCommand.refusal(botPlayer, project) == null && rp - project.cost() >= keep) {
                System.out.println("Bot: Researching " + project.label());
                return researchCommand.execute(botPlayer, project).getEvents();
            }
        }
        return List.of();
    }

    /**
     * The order the bot researches in: more room for units, the abilities, better troops, then the rest; the
     * fleet only with a harbour on a sea that reaches the enemy, more ferrying only on islands.
     */
    private List<Research> researchPlan(Player botPlayer) {
        List<Research> plan = new ArrayList<>(List.of(Research.SUPPLY_LINES, Research.RADAR_SWEEP,
                Research.ARTILLERY_BARRAGE, Research.BOOT_CAMP, Research.AIRSTRIKE, Research.RAPID_DEPLOYMENT,
                Research.COMPOSITE_ARMOUR, Research.AEGIS_UPGRADE, Research.CRUISE_MISSILE, Research.MASS_MOBILISATION,
                Research.FIELD_WORKSHOPS, Research.PRECISION_MUNITIONS, Research.STEALTH_PROGRAM,
                Research.THERMAL_SIGHTS, Research.INDUSTRIAL_EXPANSION, Research.PARATROOPERS, Research.FORWARD_COMMAND));
        boolean usefulFleet = botPlayer.getPlayerDeck().getBuildings().stream()
                .anyMatch(building -> building instanceof NavyBase && waterReachesEnemy(building.getPosition()));
        if (usefulFleet) {
            plan.addAll(List.of(Research.IMPROVED_SONAR, Research.CARRIER_AVIATION, Research.BATTLESHIPS));
        }
        if (!headquartersConnected(botPlayer)) {
            plan.addAll(List.of(Research.IMPROVED_SONAR, Research.AMPHIBIOUS_DOCTRINE)); // islands: ferry more
        }
        return plan;
    }

    /**
     * Calls in the airstrike or the barrage where it pays: on a target worth at least what it costs, judged by
     * what the bot can see (it never aims at units hidden from it). The radar sweep it leaves to the player.
     */
    private List<GameEvent> useAbility(Player botPlayer) {
        int spare = botPlayer.getResourcePoints() - reserve(botPlayer);
        List<Unit> enemies = visibleEnemies().stream()
                .filter(enemy -> enemy.getHealthPoints() > 0 && !model.isEliminated(model.getOwner(enemy)))
                .toList();
        for (Ability ability : List.of(Ability.CRUISE_MISSILE, Ability.AIRSTRIKE, Ability.ARTILLERY_BARRAGE)) {
            if (abilityCommand.notReady(botPlayer, ability) != null || spare < ability.cost()) continue;
            Point bestTile = null;
            // the missile is the siege weapon: worth it on a building even if it only takes a share off
            double bestScore = ability.cost() * (ability == Ability.CRUISE_MISSILE ? MISSILE_WORTH : ABILITY_WORTH);
            for (Unit enemy : enemies) {
                Point tile = enemy.getPosition();
                if (abilityCommand.refusal(botPlayer, ability, tile) != null) continue;
                double score = 0;
                List<Unit> hits = switch (ability) {
                    case AIRSTRIKE -> List.of(abilityCommand.airstrikeTarget(botPlayer, tile));
                    case CRUISE_MISSILE -> List.of(abilityCommand.cruiseTarget(botPlayer, tile));
                    default -> enemiesSeenIn(abilityCommand.area(ability, tile), enemies);
                };
                for (Unit hit : hits) {
                    score += strikeScore(ability, hit);
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestTile = tile;
                }
            }
            if (bestTile != null) {
                System.out.println("Bot: " + ability.label() + " on " + bestTile.x + ", " + bestTile.y);
                return abilityCommand.execute(botPlayer, ability, bestTile).getEvents();
            }
        }
        return paradrop(botPlayer, spare);
    }

    /**
     * With money piling up and room in the army, drops a rifle squad where it gets closest to the enemy base
     * without landing in a storm of fire.
     */
    private List<GameEvent> paradrop(Player botPlayer, int spare) {
        Ability drop = Ability.PARATROOPERS;
        if (abilityCommand.notReady(botPlayer, drop) != null || spare < drop.cost() || botPlayer.getResourcePoints() <= RICH
                || addCommand.isArmyFull(botPlayer)) {
            return List.of();
        }
        Point goal = enemyBase();
        Unit squad = unitFactory.createUnit(playerId, UnitEnum.INFANTRY, new Point());
        // the tiles that look free to it: an enemy hidden in the fog only turns up when the drop is called in
        Optional<Point> best = abilityCommand.targetTiles(botPlayer, drop).stream()
                .max(Comparator.comparingDouble(tile -> -GOAL_WEIGHT * tile.distance(goal) - danger(squad, tile)));
        if (best.isEmpty()) {
            return List.of();
        }
        System.out.println("Bot: Paratroopers on " + best.get().x + ", " + best.get().y);
        CommandResult result = abilityCommand.execute(botPlayer, drop, best.get());
        if (!result.wasSuccessful()) {
            System.out.println("Bot: " + result.getMessage()); // refused, nothing spent: no drop this turn
            return List.of();
        }
        actAtOnce(result.getEvents());
        return result.getEvents();
    }

    private static List<Unit> enemiesSeenIn(List<Point> area, List<Unit> enemies) {
        return enemies.stream().filter(enemy -> !(enemy instanceof Submarine) && area.contains(enemy.getPosition())).toList();
    }

    /** What an ability's hit on the unit is worth, scored like an attack. */
    private double strikeScore(Ability ability, Unit target) {
        if (target.getHealthPoints() <= 0) return 0;
        int damage = abilityCommand.predictDamage(ability, target);
        if (damage <= 0) return 0;
        if (damage >= target.getHealthPoints()) {
            return valueOf(target) * KILL_BONUS + 2;
        }
        return valueOf(target) * damage / target.getMaxHealthPoints();
    }

    /** How much the bot wants another one of these, given both armies; 0 means not at all. */
    private double desire(UnitEnum type, Army army, boolean threatened) {
        double defence = threatened ? 1 : 0;
        boolean airHeavy = army.aircraft * 2 > army.combat;
        return switch (type) {
            case TYPETEN -> 2 + 0.6 * army.enemyTanks + defence;
            case TYPENINETY -> 2 + 0.5 * army.enemyTanks + 1.5 * defence;
            case TYPESIXTEEN -> 1.5 + 0.4 * army.enemySoft;
            case TYPEEIGHTYNINE -> 1.5 + 0.4 * army.enemySoft;
            // artillery needs a front line to hide behind
            case TYPENINETYNINESPH -> army.combat - army.artillery < 3 ? 0 : army.artillery * 5 < army.combat ? 2 + defence : 0;
            case TYPEEIGHTYONEC, TYPENINETYTHREE, TYPEEIGHTYSEVEN -> {
                // one anti-air unit covers about two aircraft
                double base = army.enemyAircraft > 2 * army.antiAir ? 3 + army.enemyAircraft - 2 * army.antiAir
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
            case FTHIRTYFIVE -> airHeavy ? 0.5 : 1.8;
            case ATEN -> airHeavy ? 0.6 : 1.5 + 0.6 * army.enemyTanks;
            case YAMATO -> army.usefulFleet ? 2.5 : 0;
            case KONGO -> army.usefulFleet ? 2 + 0.5 * army.enemyAircraft + army.enemySubmarines : 0;
            case SORYU -> army.enemyShips > 0 ? 2 + army.enemyShips : 0; // only worth it against a fleet
            // a carrier brings the air force within reach of a distant enemy; the amphibious ship lands troops
            case IZUMO -> army.usefulFleet && army.carriers == 0 && army.combat >= 3 ? 1.8 : 0;
            // one ship per few units that cannot reach the enemy over land
            case OSUMI -> army.stranded > 0 && army.amphibious < 1 + army.stranded / 4
                    ? (army.amphibious == 0 ? 4 : 2.5) + 0.3 * army.stranded
                    : army.usefulFleet && army.amphibious == 0 && army.combat >= 3 ? 1.2 : 0;
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
        final long combat, antiAir, artillery, infantry, aircraft, ambulances, recon, recovery, radar, carriers, amphibious;
        final long stranded;
        final boolean cutOff;
        final long enemyAircraft, enemyTanks, enemySoft, enemyShips, enemySubmarines;
        final boolean claimableSite, hqDamaged, usefulFleet;
        private final List<Unit> own;

        Army(Player botPlayer) {
            own = List.copyOf(botPlayer.getPlayerDeck().getUnits().values());
            List<Unit> enemy = visibleEnemies();
            combat = count(own, AssaultClass.class);
            antiAir = count(own, AntiAir.class);
            artillery = count(own, SelfPropelledArtillery.class);
            infantry = count(own, Infantry.class);
            aircraft = count(own, Aircraft.class);
            ambulances = count(own, ArmoredAmbulanceVehicle.class);
            recon = count(own, ArmoredReconaissanceVehicle.class);
            recovery = count(own, ArmoredRecoveryVehicle.class);
            radar = count(own, Radarstation.class);
            carriers = count(own, Izumo.class);
            amphibious = count(own, Osumi.class);
            stranded = strandedUnits(botPlayer).size();
            cutOff = !headquartersConnected(botPlayer);
            enemyAircraft = count(enemy, Aircraft.class);
            enemyTanks = count(enemy, AssaultTank.class);
            enemyShips = count(enemy, Ship.class);
            enemySubmarines = count(enemy, Submarine.class);
            enemySoft = enemy.stream().filter(unit -> unit instanceof AssaultClass
                    && !(unit instanceof AssaultTank) && !(unit instanceof Aircraft)).count();
            claimableSite = model.getBattlefield().getBaseSites().stream()
                    .anyMatch(site -> addCommand.isFreeSite(site) && !isThreatened(site));
            hqDamaged = healthRatio(botPlayer.getPlayerDeck().getBase()) < 0.7;
            usefulFleet = botPlayer.getPlayerDeck().getBuildings().stream()
                    .anyMatch(building -> building instanceof NavyBase && waterReachesEnemy(building.getPosition()));
        }

        long owned(String unitName) {
            return own.stream().filter(unit -> unit.getUnitName().equals(unitName)).count();
        }
    }

    // --- Helpers ---

    /** True if an enemy combat unit stands close to the point. */
    private boolean isThreatened(Point point) {
        return visibleEnemies().stream()
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
                : enemyBase();
        return addCommand.findDeployablePositions(botPlayer, unit).stream()
                .min(Comparator.comparingDouble(p -> p.distance(goal)));
    }
}
