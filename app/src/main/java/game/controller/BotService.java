package game.controller;

import game.events.GameEvent;
import game.game.Player;
import game.game.UnitFactory;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.HealCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Infrastructure;
import game.units.SupportClass;
import game.units.Unit;
import game.enums.UnitEnum;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.infrastructures.AirForceBase;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredRecoveryVehicle;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enthält die Logik und Entscheidungsfindung für den KI-Spieler (Bot).
 * Diese Klasse verwendet die bestehenden Command-Klassen, um Aktionen auszuführen.
 */
public class BotService {

    private final Game model;
    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final UnitFactory unitFactory = new UnitFactory();

    private final Deque<Integer> pendingUnitIds = new ArrayDeque<>();
    private boolean purchasePending = false;

    public BotService(Game model, AddCommand add, MoveCommand move, AttackCommand attack, HealCommand heal) {
        this.model = model;
        this.addCommand = add;
        this.moveCommand = move;
        this.attackCommand = attack;
        this.healCommand = heal;
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
     * Starts a new bot turn that is then executed step by step with {@link #performNextStep()}.
     */
    public void startTurn() {
        pendingUnitIds.clear();
        pendingUnitIds.addAll(model.getPlayerTwo().getPlayerDeck().getUnits().keySet());
        purchasePending = true;
    }

    /**
     * Executes the next unit action (or the purchase) on the model and returns the events it caused.
     * Steps that cause no events are skipped. An empty result means the turn is finished.
     */
    public Optional<List<GameEvent>> performNextStep() {
        Player botPlayer = model.getPlayerTwo();
        Player humanPlayer = model.getPlayerOne();

        while (!pendingUnitIds.isEmpty()) {
            int unitId = pendingUnitIds.poll();
            Unit unit = botPlayer.getPlayerDeck().getUnit(unitId);

            if (unit == null || !unit.isAvailable()) {
                continue;
            }

            List<GameEvent> events = performActionForUnit(unit, unitId, botPlayer, humanPlayer);
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

    /**
     * Entscheidet, ob eine Einheit angreifen oder sich bewegen soll.
     */
    private List<GameEvent> performActionForUnit(Unit unit, int unitId, Player botPlayer, Player humanPlayer) {
        if (unit instanceof SupportClass support && support.getSupportPoints() > 0) {
            return supportOrFollow(support, unitId, botPlayer);
        }
        if (!(unit instanceof AssaultClass)) {
            return moveUnitTowardsClosestEnemy(unit, unitId, botPlayer, humanPlayer);
        }

        Optional<Unit> targetToAttack = findBestTargetInRange(unit, humanPlayer);

        if (targetToAttack.isPresent()) {
            int targetId = model.getUnitID(targetToAttack.get());
            System.out.println("Bot: Unit " + unitId + " attacks Unit " + targetId);
            return attackCommand.execute(botPlayer, unitId, targetId).getEvents();
        } else {
            return moveUnitTowardsClosestEnemy(unit, unitId, botPlayer, humanPlayer);
        }
    }

    /**
     * Bewegt eine Einheit zur bestmöglichen Position näher am nächsten Feind.
     */
    private List<GameEvent> moveUnitTowardsClosestEnemy(Unit unit, int unitId, Player botPlayer, Player humanPlayer) {
        return findClosestEnemy(unit, humanPlayer).flatMap(closestEnemy -> {
            Point goal = closestEnemy.getPosition();

            return findBestMovePosition(unit, goal).map(bestMove -> {
                System.out.println("Bot: Unit " + unitId + " moves to " + bestMove.x + ", " + bestMove.y);
                return moveCommand.execute(botPlayer, unitId, bestMove).getEvents();
            });
        }).orElse(List.of());
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
        if (patient.isEmpty() || patient.get().getPosition().distance(support.getPosition()) <= support.getSupportRange()) {
            return List.of();
        }
        return findBestMovePosition(support, patient.get().getPosition())
                .map(bestMove -> moveCommand.execute(botPlayer, unitId, bestMove).getEvents())
                .orElse(List.of());
    }

    private static double healthRatio(Unit unit) {
        return (double) unit.getHealthPoints() / unit.getMaxHealthPoints();
    }

    /**
     * Kauft höchstens eine neue Einheit pro Zug. Die erste passende Regel entscheidet; reicht das Geld dafür noch
     * nicht, spart der Bot darauf.
     */
    private List<GameEvent> purchaseNewUnits(Player botPlayer) {
        return choosePurchase(botPlayer)
                .filter(type -> botPlayer.getResourcePoints() >= cost(type))
                .flatMap(type -> findPlacementPosition(botPlayer, type).map(pos -> {
                    System.out.println("Bot: Adding new " + type + " at " + pos.x + ", " + pos.y);
                    return addCommand.execute(botPlayer, type.name(), pos).getEvents();
                }))
                .orElse(List.of());
    }

    private Optional<UnitEnum> choosePurchase(Player botPlayer) {
        List<Unit> own = List.copyOf(botPlayer.getPlayerDeck().getUnits().values());
        long enemyAircraft = model.getPlayerOne().getPlayerDeck().getUnits().values().stream()
                .filter(unit -> unit instanceof Aircraft).count();
        long antiAir = count(own, SelfPropelledAntiAir.class);
        long combat = count(own, AssaultClass.class);
        Unit base = botPlayer.getPlayerDeck().getBase();

        if (antiAir < enemyAircraft) {
            return firstDeployable(botPlayer, UnitEnum.TYPEEIGHTYONEC, UnitEnum.TYPENINETYTHREE, UnitEnum.TYPEEIGHTYSEVEN);
        }
        if (healthRatio(base) < 0.7 && count(own, ArmoredRecoveryVehicle.class) == 0) {
            return firstDeployable(botPlayer, UnitEnum.ARMOREDRECOVERYVEHICLE);
        }
        if (combat >= 4 && count(own, AirForceBase.class) == 0) {
            return firstDeployable(botPlayer, UnitEnum.AIRFORCEBASE);
        }
        if (count(own, AirForceBase.class) > 0 && count(own, Aircraft.class) * 3 < combat) {
            return firstDeployable(botPlayer, UnitEnum.FTWO);
        }
        if (combat >= 2 && count(own, SelfPropelledArtillery.class) == 0) {
            return firstDeployable(botPlayer, UnitEnum.TYPENINETYNINESPH);
        }
        if (combat >= 3 && count(own, ArmoredAmbulanceVehicle.class) == 0) {
            return firstDeployable(botPlayer, UnitEnum.ARMOREDAMBULANCE);
        }
        return firstDeployable(botPlayer, UnitEnum.TYPENINETY);
    }

    private static long count(List<Unit> units, Class<?> kind) {
        return units.stream().filter(kind::isInstance).count();
    }

    /** The first of the types that has somewhere to go, regardless of its price. */
    private Optional<UnitEnum> firstDeployable(Player botPlayer, UnitEnum... types) {
        for (UnitEnum type : types) {
            if (findPlacementPosition(botPlayer, type).isPresent()) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private int cost(UnitEnum type) {
        return unitFactory.createUnit(2, type, new Point()).getResourceCost();
    }

    private Optional<Unit> findBestTargetInRange(Unit attacker, Player opponent) {
        Point attackerPos = attacker.getPosition();
        Set<Integer> inRange = Set.copyOf(opponent.getPlayerDeck().getUnitIDsInRange(attackerPos,
                model.getShootingRange((AssaultClass) attacker)));

        return opponent.getPlayerDeck().getUnits().entrySet().stream()
                .filter(entry -> inRange.contains(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                // Einfache Priorität: Greife das Ziel mit den wenigsten HP an
                .min(Comparator.comparingInt(Unit::getHealthPoints));
    }

    private Optional<Unit> findClosestEnemy(Unit unit, Player opponent) {
        return opponent.getPlayerDeck().getUnits().values().stream()
                .min(Comparator.comparingDouble(enemy -> unit.getPosition().distance(enemy.getPosition())));
    }

    private Optional<Point> findBestMovePosition(Unit unit, Point goal) {
        ArrayList<Point> possibleMoves = unit.getPossibleMoves(model.getBattlefield().getSize());

        return possibleMoves.stream()
                .filter(p -> model.isPositionFree(p) && moveCommand.checkIfTerrainIsDeployable(unit, p))
                .min(Comparator.comparingDouble(p -> p.distance(goal)));
    }

    /** Buildings go close to home, everything else as close to the enemy base as possible. */
    private Optional<Point> findPlacementPosition(Player botPlayer, UnitEnum type) {
        Unit unit = unitFactory.createUnit(botPlayer.getPlayerID(), type, new Point());
        Point goal = unit instanceof Infrastructure
                ? botPlayer.getPlayerDeck().getBase().getPosition()
                : model.getPlayerOne().getPlayerDeck().getBase().getPosition();
        return addCommand.findDeployablePositions(botPlayer, unit).stream()
                .min(Comparator.comparingDouble(p -> p.distance(goal)));
    }
}
