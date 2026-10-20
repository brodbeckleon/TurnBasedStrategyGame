package game.controller;

import game.events.GameEvent;
import game.game.Player;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Unit;
import game.enums.UnitEnum;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Enthält die Logik und Entscheidungsfindung für den KI-Spieler (Bot).
 * Diese Klasse verwendet die bestehenden Command-Klassen, um Aktionen auszuführen.
 */
public class BotService {

    private final Game model;
    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;

    private final Deque<Integer> pendingUnitIds = new ArrayDeque<>();
    private boolean purchasePending = false;

    public BotService(Game model, AddCommand add, MoveCommand move, AttackCommand attack) {
        this.model = model;
        this.addCommand = add;
        this.moveCommand = move;
        this.attackCommand = attack;
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
     * Kauft neue Einheiten, sobald Ressourcen verfügbar sind.
     */
    private List<GameEvent> purchaseNewUnits(Player botPlayer) {
        if (botPlayer.getResourcePoints() >= 5) { // Annahme: Archer kostet 3
            return findPlacementPosition(botPlayer).map(pos -> {
                System.out.println("Bot: Adding new Type 90 at " + pos.x + ", " + pos.y);
                return addCommand.execute(botPlayer, UnitEnum.TYPENINETY.name(), pos).getEvents();
            }).orElse(List.of());
        }
        return List.of();
    }


    private Optional<Unit> findBestTargetInRange(Unit attacker, Player opponent) {
        Point attackerPos = attacker.getPosition();
        int range = ((AssaultClass) attacker).getShootingRange();

        return opponent.getPlayerDeck().getUnits().values().stream()
                .filter(target -> attackerPos.distance(target.getPosition()) <= range)
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

    private Optional<Point> findPlacementPosition(Player botPlayer) {
        Point basePos = botPlayer.getPlayerDeck().getBase().getPosition();
        for (int r = 1; r < 5; r++) {
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    if (Math.abs(i) != r && Math.abs(j) != r) continue; // Nur der äußere Ring der Spirale
                    Point candidate = new Point(basePos.x + i, basePos.y + j);
                    if (addCommand.checkIfPositionIsFree(candidate) && addCommand.checkIfPositionIsDeployable(botPlayer, candidate)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }
}