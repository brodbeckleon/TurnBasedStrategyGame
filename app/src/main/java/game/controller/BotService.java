package game.controller;

import game.game.Player;
import game.game.commands.AddCommand;
import game.game.commands.AttackCommand;
import game.game.commands.MoveCommand;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Unit;
import game.enums.UnitEnum;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
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
        Player botPlayer = model.getPlayerTwo();
        Player humanPlayer = model.getPlayerOne();

        for (Integer unitId : new ArrayList<>(botPlayer.getPlayerDeck().getUnits().keySet())) {
            Unit unit = botPlayer.getPlayerDeck().getUnit(unitId);

            if (unit == null || !unit.isAvailable()) {
                continue;
            }

            performActionForUnit(unit, unitId, botPlayer, humanPlayer);
        }

        purchaseNewUnits(botPlayer);
    }

    /**
     * Entscheidet, ob eine Einheit angreifen oder sich bewegen soll.
     */
    private void performActionForUnit(Unit unit, int unitId, Player botPlayer, Player humanPlayer) {
        if (!(unit instanceof AssaultClass)) {
            moveUnitTowardsClosestEnemy(unit, unitId, botPlayer, humanPlayer);
            return;
        }

        Optional<Unit> targetToAttack = findBestTargetInRange(unit, humanPlayer);

        if (targetToAttack.isPresent()) {
            int targetId = model.getUnitID(targetToAttack.get());
            System.out.println("Bot: Unit " + unitId + " attacks Unit " + targetId);
            attackCommand.execute(botPlayer, unitId, targetId);
        } else {
            moveUnitTowardsClosestEnemy(unit, unitId, botPlayer, humanPlayer);
        }
    }

    /**
     * Bewegt eine Einheit zur bestmöglichen Position näher am nächsten Feind.
     */
    private void moveUnitTowardsClosestEnemy(Unit unit, int unitId, Player botPlayer, Player humanPlayer) {
        findClosestEnemy(unit, humanPlayer).ifPresent(closestEnemy -> {
            Point goal = closestEnemy.getPosition();

            findBestMovePosition(unit, goal).ifPresent(bestMove -> {
                System.out.println("Bot: Unit " + unitId + " moves to " + bestMove.x + ", " + bestMove.y);
                moveCommand.execute(botPlayer, unitId, bestMove);
            });
        });
    }

    /**
     * Kauft neue Einheiten, sobald Ressourcen verfügbar sind.
     */
    private void purchaseNewUnits(Player botPlayer) {
        if (botPlayer.getResourcePoints() >= 5) { // Annahme: Archer kostet 3
            findPlacementPosition(botPlayer).ifPresent(pos -> {
                System.out.println("Bot: Adding new Type 90 at " + pos.x + ", " + pos.y);
                addCommand.execute(botPlayer, UnitEnum.TYPENINETY.name(), pos);
            });
        }
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