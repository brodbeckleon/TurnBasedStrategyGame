package game.controller;

import game.events.GameEvent;
import game.game.Command;
import game.game.Movement;
import game.game.Player;
import game.game.UnitFactory;
import game.game.commands.*;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.enums.UnitEnum;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.SupportClass;
import game.units.Unit;
import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.Ship;
import game.units.infrastructures.NavyBase;
import game.units.supportClasses.ArmoredRecoveryVehicle;
import game.view.BoardHighlights;
import game.view.DeployOption;
import game.view.GameView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GameController {

    private static final double BOT_THINK_SECONDS = 1;
    private static final double BOT_STEP_GAP_SECONDS = 0.15;
    private static final Point PROBE_POSITION = new Point(0, 0);

    private enum ControllerState {
        NORMAL,
        UNIT_SELECTED,
        AWAITING_PLACEMENT
    }

    private final Game model;
    private final GameView view;

    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final UpgradeCommand upgradeCommand;
    private final BotService botService;
    private final UnitFactory unitFactory = new UnitFactory();
    private final CircleGenerator circleGenerator = new CircleGenerator();
    private final List<DeployOption> deployOptions;


    private ControllerState currentState = ControllerState.NORMAL;
    private UnitEnum unitTypeToAdd = null;
    private int selectedUnitId = -1; // -1 means no unit is selected
    private boolean animating = false; // true while an animation or the bot turn blocks the player's input
    private int round = 1;

    public GameController(Game model, GameView view) {
        this.model = model;
        this.view = view;

        this.addCommand = new AddCommand(model);
        this.moveCommand = new MoveCommand(model);
        this.attackCommand = new AttackCommand(model);
        this.healCommand = new HealCommand(model);
        this.upgradeCommand = new UpgradeCommand(model);
        this.botService = new BotService(model, addCommand, moveCommand, attackCommand, healCommand, upgradeCommand,
                new java.util.Random());
        this.deployOptions = buildDeployOptions();

        attachHandlers();
        view.setRound(round);
        refresh();
    }

    /** Everything the factory can build, with what the deploy panel shows about it. */
    private List<DeployOption> buildDeployOptions() {
        List<DeployOption> options = new ArrayList<>();
        for (UnitEnum type : UnitEnum.values()) {
            Unit unit = unitFactory.createUnit(1, type, PROBE_POSITION);
            options.add(new DeployOption(type, categoryOf(unit), unit.getUnitName(), unit.getResourceCost(),
                    statsOf(unit), describe(unit)));
        }
        return options;
    }

    private static DeployOption.Category categoryOf(Unit unit) {
        if (unit instanceof Infrastructure) return DeployOption.Category.BUILDINGS;
        if (unit instanceof SupportClass) return DeployOption.Category.SUPPORT;
        if (unit instanceof Aircraft) return DeployOption.Category.AIR;
        if (unit instanceof Ship) return DeployOption.Category.NAVY;
        return DeployOption.Category.ARMY;
    }

    private static String statsOf(Unit unit) {
        StringBuilder stats = new StringBuilder("HP " + unit.getMaxHealthPoints());
        if (unit instanceof AssaultClass assault) {
            stats.append("  DMG ").append(assault.getDamage());
        }
        if (unit instanceof MovableUnit movable) {
            stats.append("  MOV ").append(movable.getMovingRange());
        }
        if (unit instanceof AssaultClass assault) {
            stats.append("  RNG ").append(assault.getShootingRange());
        }
        if (unit instanceof SupportClass support && support.getSupportPoints() > 0) {
            stats.append("  +").append(support.getSupportPoints()).append(" HP");
        }
        if (unit instanceof AssaultTank tank) {
            stats.append("  ARM ").append(tank.getArmorValue()).append('%');
        } else if (unit instanceof Infrastructure building) {
            stats.append("  ARM ").append(building.getArmorValue()).append('%');
        }
        return stats.toString();
    }

    /** One or two sentences on what the unit does and where it can be deployed. */
    private static String describe(Unit unit) {
        String text = switch (unit.getUnitName()) {
            case "Type 10" -> "Medium main battle tank: hard-hitting and quick.";
            case "Type 90" -> "Heavy main battle tank: the toughest armour, an anchor for your line.";
            case "Type 16" -> "Wheeled tank destroyer: fast and cheap, lightly armoured.";
            case "Type 89" -> "Infantry fighting vehicle: tough and cheap.";
            case "Type 99 SPH" -> "Self-propelled howitzer: shoots very far and wrecks buildings, but is fragile.";
            case "Type 87", "Type 81 (C)" -> "Heavy anti-air: double damage to aircraft, half to ground targets. Fires at every enemy aircraft that attacks within its range.";
            case "Type 93" -> "Light anti-air: double damage to aircraft, half to ground targets. Fires at every enemy aircraft that attacks within its range.";
            case "Armored Ambulance" -> "Heals an adjacent friendly unit by 300 HP.";
            case "Armored Recovery Vehicle" -> "Builds new bases on base ruins it stands next to, and repairs an adjacent friendly building (your base too) by 600 HP.";
            case "Armored Reconnaissance Vehicle" -> "Unarmed scout: friendly units within 3 tiles shoot 1 tile further.";
            case "Yamato" -> "Battleship with a huge punch and a thick hull.";
            case "Army Base" -> "Forward base: +2 RP per round, deploys army units around it, and a Radar Station can be built near it. Built on base ruins by a Recovery Vehicle.";
            case "Air Base" -> "Airfield: +2 RP per round, required to deploy aircraft. Built on base ruins by a Recovery Vehicle.";
            case "Naval Base" -> "Harbour: +2 RP per round, required to deploy ships. Built on base ruins next to water by a Recovery Vehicle.";
            case "Radar Station" -> "Friendly units within 4 tiles shoot 1 tile further.";
            default -> unit instanceof Aircraft ? "Flies over any terrain; ground fire only deals three quarters of its damage to it." : "";
        };
        if (unit instanceof Infrastructure) {
            return text;
        }
        String orders = unit instanceof AssaultClass
                ? unit instanceof ActsAfterMoving ? " Can move and attack in the same turn." : " Moves or attacks each turn, not both."
                : "";
        String where = unit instanceof Aircraft ? " Deploys at an air base."
                : unit instanceof Ship ? " Deploys on the water around a naval base."
                : " Deploys at your base or an army base.";
        return text + orders + where;
    }

    private void attachHandlers() {
        view.getSkipTurnButton().setOnAction(e -> {
            if (canAct()) endTurn();
        });
        view.getGrid().setOnMouseClicked(this::handleGridClick);
        view.setDeployOptions(deployOptions, this::prepareAddUnit);
        view.setKeyHandler(this::handleKey);
        view.setOnUpgrade(this::upgradeSelected);
    }

    private boolean canAct() {
        return model.isPlayerOneTurn() && !model.isGameOver() && !animating;
    }

    private void handleKey(KeyEvent event) {
        if (!canAct()) return;
        switch (event.getCode()) {
            case E -> endTurn();
            case U -> upgradeSelected();
            case ESCAPE -> cancel();
            default -> {
                String key = event.getText();
                if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
                    // the number keys pick from the deploy tab that is open
                    List<DeployOption> shown = view.getShownDeployOptions();
                    int index = key.charAt(0) - '1';
                    if (index >= 0 && index < shown.size()) {
                        prepareAddUnit(shown.get(index).type());
                    }
                }
            }
        }
    }

    /** Drops the current selection or placement. */
    private void cancel() {
        if (currentState == ControllerState.NORMAL) return;
        resetSelection();
        refresh();
    }

    private void endTurn() {
        resetSelection(); // Deselect unit when turn ends
        model.switchTurn();
        if (model.isPlayerOneTurn()) {
            round++;
            view.setRound(round);
            view.logInfo("— Round " + round + ": your turn —");
        } else {
            view.logInfo("— Enemy turn —");
        }
        refresh();

        handleBotTurn();
    }

    /**
     * Starts the bot turn if it is the bot's turn: a banner (or a short pause without animations), then one
     * step after the other, each played before the next one is decided.
     */
    private void handleBotTurn() {
        if (!model.isPlayerOneTurn() && !model.isGameOver()) {
            lockInput(); // block the player while the bot "thinks" and acts

            if (view.isAnimationsEnabled()) {
                view.showBanner("ENEMY TURN", () -> guarded(this::beginBotTurn));
            } else {
                PauseTransition pause = new PauseTransition(Duration.seconds(BOT_THINK_SECONDS));
                pause.setOnFinished(event -> guarded(this::beginBotTurn));
                pause.play();
            }
        }
    }

    private void beginBotTurn() {
        botService.startTurn();
        botStep();
    }

    private void botStep() {
        Optional<List<GameEvent>> step = botService.performNextStep();
        if (step.isEmpty()) {
            finishBotTurn();
            return;
        }
        logEvents(step.get());
        play(step.get(), this::afterBotStep);
    }

    private void afterBotStep() {
        model.checkWinCondition();
        if (model.isGameOver()) {
            unlockInput();
            view.showWinner(model.getWinnerId());
            refreshStatus();
            return;
        }
        // Come back through the event loop so that a long bot turn without animations never recurses deeply.
        if (view.isAnimationsEnabled()) {
            PauseTransition gap = new PauseTransition(Duration.seconds(BOT_STEP_GAP_SECONDS));
            gap.setOnFinished(event -> guarded(this::botStep));
            gap.play();
        } else {
            Platform.runLater(() -> guarded(this::botStep));
        }
    }

    private void finishBotTurn() {
        endTurn();
        view.showBanner("YOUR TURN", this::unlockInput);
    }

    /**
     * Plays the events of one action. The board is refreshed right away (the units being animated stay hidden)
     * and again when the animation is over; then {@code afterwards} continues the game.
     */
    private void play(List<GameEvent> events, Runnable afterwards) {
        guarded(() -> {
            boolean[] finished = {false};
            view.playEvents(events, () -> guarded(() -> {
                finished[0] = true;
                refresh();
                afterwards.run();
            }));
            if (!finished[0]) {
                refresh(); // clears the range overlays and updates the labels while the units move
            }
        });
    }

    /** Plays the events of a successful player command; returns false if there was nothing to play. */
    private boolean playPlayerAction(CommandResult result) {
        if (!result.wasSuccessful() || result.getEvents().isEmpty()) {
            return false;
        }
        lockInput();
        play(result.getEvents(), this::finishPlayerAction);
        return true;
    }

    private void finishPlayerAction() {
        unlockInput();
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
            refreshStatus();
        }
    }

    /** Logs what a command did, or why it was refused. */
    private void logResult(CommandResult result) {
        if (result.wasSuccessful() && !result.getEvents().isEmpty()) {
            logEvents(result.getEvents());
        } else {
            view.logInfo(result.getMessage());
        }
    }

    private void logEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            String line = switch (event) {
                case GameEvent.UnitDeployed e -> owner(e.playerId()) + " " + e.unitName() + " deployed.";
                case GameEvent.UnitMoved e -> owner(e.playerId()) + " " + e.unitName() + " moved.";
                case GameEvent.UnitAttacked e -> null; // the damage line says it all
                case GameEvent.UnitDamaged e -> owner(e.ownerId()) + " " + (e.isBase() ? "base" : e.unitName())
                        + " took " + e.damage() + " damage (" + Math.max(0, e.hpAfter()) + " HP left).";
                case GameEvent.UnitDestroyed e -> owner(e.ownerId()) + " " + (e.isBase() ? "base" : e.unitName())
                        + " was destroyed!";
                case GameEvent.UnitHealed e -> owner(e.playerId()) + " " + e.supporterName() + " restored "
                        + e.amount() + " HP to " + e.targetName() + ".";
                case GameEvent.BaseUpgraded e -> owner(e.playerId()) + " " + (e.unitName().equals("Base") ? "base" : e.unitName())
                        + " was upgraded to level " + e.level() + ".";
            };
            if (line != null) {
                view.logInfo(line);
            }
        }
    }

    private static String owner(int playerId) {
        return playerId == 1 ? "Your" : "Enemy";
    }

    private void lockInput() {
        animating = true;
        view.setInputLocked(true);
        refreshStatus();
    }

    private void unlockInput() {
        animating = false;
        view.setInputLocked(false);
        refresh(); // the highlights were hidden while locked; a selection that survived the action shows again
    }

    /** Runs the action and releases the input lock if it fails, so the game can never soft-lock. */
    private void guarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error e) {
            unlockInput();
            throw e;
        }
    }

    /** Upgrades the selected base (button in the inspector, or U). */
    private void upgradeSelected() {
        if (!canAct() || currentState != ControllerState.UNIT_SELECTED
                || !(model.findUnitById(selectedUnitId) instanceof MilitaryBase)) {
            return;
        }
        CommandResult result = upgradeCommand.execute(model.getCurrentPlayer(), selectedUnitId);
        logResult(result);
        if (!playPlayerAction(result)) {
            refresh();
        }
    }

    /** Arms a unit type for placement; picking the armed type again cancels it. */
    private void prepareAddUnit(UnitEnum type) {
        if (!canAct()) return;
        if (currentState == ControllerState.AWAITING_PLACEMENT && unitTypeToAdd == type) {
            cancel();
            return;
        }
        DeployOption option = optionFor(type);
        if (model.getCurrentPlayer().getResourcePoints() < option.cost()) {
            view.logInfo("Not enough resource points for a " + option.name() + " (" + option.cost() + " RP).");
            return;
        }
        String blocked = blockedReason(type);
        if (blocked != null) {
            view.logInfo("Cannot deploy a " + option.name() + ": " + blocked);
            return;
        }
        resetSelection();
        unitTypeToAdd = type;
        currentState = ControllerState.AWAITING_PLACEMENT;
        refresh();
    }

    private DeployOption optionFor(UnitEnum type) {
        return deployOptions.stream().filter(option -> option.type() == type).findFirst().orElseThrow();
    }

    private void resetSelection() {
        selectedUnitId = -1;
        unitTypeToAdd = null;
        currentState = ControllerState.NORMAL;
    }

    private void handleGridClick(MouseEvent event) {
        if (!canAct()) {
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            cancel();
            return;
        }
        Point clickedPoint = view.cellAt(event.getX(), event.getY());
        if (clickedPoint == null) {
            return;
        }
        BoardHighlights highlights = computeHighlights();

        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            CommandResult result = addCommand.execute(model.getCurrentPlayer(), unitTypeToAdd.name(), clickedPoint);
            logResult(result);
            if (result.wasSuccessful()) {
                resetSelection();
            }
            if (!playPlayerAction(result)) {
                refresh();
            }
            return;
        }

        Unit clickedUnit = model.getUnitAt(clickedPoint);
        Player currentPlayer = model.getCurrentPlayer();
        boolean animated = false;

        if (currentState == ControllerState.NORMAL) {
            if (clickedUnit != null && model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                selectedUnitId = model.getUnitID(clickedUnit);
                currentState = ControllerState.UNIT_SELECTED;
            }
        } else if (currentState == ControllerState.UNIT_SELECTED) {
            Unit selectedUnit = model.findUnitById(selectedUnitId);
            if (selectedUnit == null) {
                resetSelection();
            } else if (clickedUnit != null && model.getUnitID(clickedUnit) == selectedUnitId) {
                resetSelection();
            } else if (clickedUnit != null && !model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                CommandResult result = attackCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                logResult(result);
                if (result.wasSuccessful()) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (clickedUnit != null && highlights.supportTargets().contains(clickedPoint)) {
                CommandResult result = healCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                logResult(result);
                if (result.wasSuccessful()) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (clickedUnit == null && highlights.moveTiles().contains(clickedPoint)) {
                CommandResult result = moveCommand.execute(currentPlayer, selectedUnitId, clickedPoint);
                logResult(result);
                // a unit that can still act stays selected, so its targets show right after the move
                if (!result.wasSuccessful() || !selectedUnit.isAvailable() || !canStillAct(selectedUnitId, selectedUnit)) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (clickedUnit == null) {
                resetSelection(); // a click on an unreachable tile just drops the selection
            } else {
                selectedUnitId = model.getUnitID(clickedUnit);
            }
        }
        if (animated) {
            return;
        }
        refresh();
        model.checkWinCondition();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
            refreshStatus();
        }
    }

    // --- What the view shows ---

    private void refresh() {
        view.update(model, computeHighlights());
        refreshStatus();
    }

    /** The hint, the armed deploy card and the end turn suggestion, which change with the input lock too. */
    private void refreshStatus() {
        view.setArmedDeploy(currentState == ControllerState.AWAITING_PLACEMENT ? unitTypeToAdd : null);
        view.setBlockedDeploys(blockedDeploys());
        view.setEndTurnSuggested(canAct() && currentState == ControllerState.NORMAL && nothingLeftToDo());
        view.setHint(hint());
    }

    /** Where the selected unit may move and shoot, or where the armed unit type may be deployed. */
    private BoardHighlights computeHighlights() {
        if (!canAct()) {
            return BoardHighlights.NONE;
        }
        if (currentState == ControllerState.AWAITING_PLACEMENT) {
            return new BoardHighlights(-1, Set.of(), Set.of(), Set.of(), Set.of(), deployTiles(unitTypeToAdd));
        }
        Unit selected = currentState == ControllerState.UNIT_SELECTED ? model.findUnitById(selectedUnitId) : null;
        if (selected == null || !selected.isAvailable()) {
            return new BoardHighlights(selectedUnitId, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
        }

        Set<Point> moves = new HashSet<>();
        if (selected instanceof MovableUnit && !selected.hasMoved()) {
            moves.addAll(Movement.reachable(model, selected).keySet());
        }
        Set<Point> range = new HashSet<>();
        Set<Point> targets = new HashSet<>();
        if (selected instanceof AssaultClass assault) {
            Point origin = selected.getPosition();
            int shootingRange = model.getShootingRange(assault);
            for (Point point : circleGenerator.getCoordinatesInRange(origin, shootingRange)) {
                if (isOnBoard(point) && !point.equals(origin)) {
                    range.add(point);
                }
            }
            for (int targetId : model.getOpponent().getPlayerDeck().getUnitIDsInRange(origin, shootingRange)) {
                targets.add(model.findUnitById(targetId).getPosition());
            }
        }
        Set<Point> supportTargets = new HashSet<>();
        for (int targetId : healCommand.findTargets(model.getCurrentPlayer(), selectedUnitId)) {
            supportTargets.add(model.findUnitById(targetId).getPosition());
        }
        return new BoardHighlights(selectedUnitId, moves, range, targets, supportTargets, Set.of());
    }

    private Set<Point> deployTiles(UnitEnum type) {
        Player player = model.getCurrentPlayer();
        return addCommand.findDeployablePositions(player, unitFactory.createUnit(player.getPlayerID(), type, PROBE_POSITION));
    }

    /** Why the type cannot be deployed right now (resources aside), or null if it can. */
    private String blockedReason(UnitEnum type) {
        if (!deployTiles(type).isEmpty()) {
            return null;
        }
        Unit unit = unitFactory.createUnit(model.getCurrentPlayer().getPlayerID(), type, PROBE_POSITION);
        boolean hasBuilding = model.getCurrentPlayer().getPlayerDeck().getBuildings().stream()
                .anyMatch(building -> unit instanceof Infrastructure ? building.getConstructionRadius() > 0 : building.canDeploy(unit));
        if (Command.isSiteBuilding(unit)) {
            boolean hasVehicle = model.getCurrentPlayer().getPlayerDeck().getUnits().values().stream()
                    .anyMatch(own -> own instanceof ArmoredRecoveryVehicle && own.isAvailable());
            if (!hasVehicle) return "needs a ready Armored Recovery Vehicle next to free base ruins.";
            return unit instanceof NavyBase ? "move your Recovery Vehicle next to free base ruins by the water."
                    : "move your Recovery Vehicle next to free base ruins.";
        }
        if (unit instanceof Infrastructure) return "no free open ground near your base or an army base.";
        if (!hasBuilding && unit instanceof Aircraft) return "build an Air Base first.";
        if (!hasBuilding && unit instanceof Ship) return "build a Naval Base first.";
        return "no free tile to deploy it on.";
    }

    /** The reasons of every type that cannot be deployed right now, for the deploy panel. */
    private Map<UnitEnum, String> blockedDeploys() {
        Map<UnitEnum, String> blocked = new EnumMap<>(UnitEnum.class);
        if (!canAct()) {
            return blocked;
        }
        for (DeployOption option : deployOptions) {
            String reason = blockedReason(option.type());
            if (reason != null) {
                blocked.put(option.type(), reason);
            }
        }
        return blocked;
    }

    private boolean isOnBoard(Point point) {
        Point size = model.getBattlefield().getSize();
        return point.x >= 0 && point.x < size.x && point.y >= 0 && point.y < size.y;
    }

    /** True when no unit can act any more and no unit can be bought: all that is left is ending the turn. */
    private boolean nothingLeftToDo() {
        for (Map.Entry<Integer, Unit> entry : model.getCurrentPlayer().getPlayerDeck().getUnits().entrySet()) {
            Unit unit = entry.getValue();
            if (unit instanceof MovableUnit && unit.isAvailable() && (!unit.hasMoved() || canStillAct(entry.getKey(), unit))) {
                return false;
            }
        }
        int resources = model.getCurrentPlayer().getResourcePoints();
        return deployOptions.stream().noneMatch(option -> option.cost() <= resources && !deployTiles(option.type()).isEmpty());
    }

    /** True if the unit has something to do from where it stands: a target to shoot, heal or build on. */
    private boolean canStillAct(int unitId, Unit unit) {
        Player player = model.getCurrentPlayer();
        if (unit instanceof AssaultClass assault && !model.getOpponent().getPlayerDeck()
                .getUnitIDsInRange(unit.getPosition(), model.getShootingRange(assault)).isEmpty()) {
            return true;
        }
        if (!healCommand.findTargets(player, unitId).isEmpty()) {
            return true;
        }
        return unit instanceof ArmoredRecoveryVehicle && model.getBattlefield().getBaseSites().stream()
                .anyMatch(site -> addCommand.isFreeSite(site) && Command.isNextTo(unit.getPosition(), site));
    }

    private String hint() {
        if (model.isGameOver()) {
            return "Game over.";
        }
        if (!model.isPlayerOneTurn()) {
            return "The enemy is making its move…  Click the board or press Space to fast-forward.";
        }
        if (animating) {
            return "Click the board or press Space to fast-forward.";
        }
        switch (currentState) {
            case AWAITING_PLACEMENT -> {
                DeployOption option = optionFor(unitTypeToAdd);
                if (deployTiles(unitTypeToAdd).isEmpty()) {
                    return "There is no free tile for a " + option.name() + " any more.  Esc to cancel.";
                }
                return "Deploy " + option.name() + " (" + option.cost() + " RP): click a green tile.  "
                        + "Right-click or Esc to cancel.";
            }
            case UNIT_SELECTED -> {
                Unit selected = model.findUnitById(selectedUnitId);
                if (selected instanceof MilitaryBase base) {
                    return base.canUpgrade()
                            ? "Press U (or Upgrade in the inspector) to raise this base to level " + (base.getLevel() + 1)
                              + " for " + base.getUpgradeCost() + " RP.  Esc to deselect."
                            : "This base is fully upgraded.  Esc to deselect.";
                }
                if (selected == null || !selected.isAvailable()) {
                    return "This unit cannot act right now.  Select another unit or press E to end your turn.";
                }
                if (selected.hasMoved()) {
                    return "Moved: " + (selected instanceof AssaultClass ? "red brackets to attack" : "act from here")
                            + ", or Esc if this unit is done.";
                }
                if (selected instanceof ArmoredRecoveryVehicle
                        && !addCommand.findDeployablePositions(model.getCurrentPlayer(),
                        unitFactory.createUnit(1, UnitEnum.ARMYBASE, PROBE_POSITION)).isEmpty()) {
                    return "Next to base ruins: pick a base in the Build tab to build it here, or move on.  Esc to deselect.";
                }
                if (selected instanceof SupportClass support && support.getSupportPoints() > 0) {
                    return "Blue tiles: move.  Green brackets: " + (support.canSupport(model.getCurrentPlayer().getPlayerDeck().getBase())
                            ? "repair" : "heal") + " a damaged friend.  Right-click or Esc to deselect.";
                }
                if (selected instanceof SupportClass) {
                    return "Blue tiles: move.  Units within 3 tiles of this scout shoot 1 tile further.  Esc to deselect.";
                }
                return "Blue tiles: move.  Red brackets: attack.  Right-click or Esc to deselect.";
            }
            default -> {
                if (nothingLeftToDo()) {
                    return "Nothing left to do this turn.  Press E to end your turn.";
                }
                return "Select one of your units, or deploy a new one with keys 1–9 (Tab switches the deploy tab).  "
                        + "Press E to end your turn.";
            }
        }
    }
}
