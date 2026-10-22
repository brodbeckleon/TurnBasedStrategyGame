package game.controller;

import game.events.GameEvent;
import game.game.Ability;
import game.game.Command;
import game.game.Movement;
import game.game.Player;
import game.game.Research;
import game.game.UnitFactory;
import game.game.Vision;
import game.game.commands.*;
import game.interfaces.MovableUnit;
import game.model.Game;
import game.enums.UnitEnum;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.SupportClass;
import game.units.TransportShip;
import game.units.Unit;
import game.units.transports.Osumi;
import game.interfaces.militaryBranches.Navy;
import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.Helicopter;
import game.units.assaultClasses.Infantry;
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
        AWAITING_PLACEMENT,
        AWAITING_ABILITY_TARGET
    }

    private final Game model;
    private final GameView view;

    private final AddCommand addCommand;
    private final MoveCommand moveCommand;
    private final AttackCommand attackCommand;
    private final HealCommand healCommand;
    private final UpgradeCommand upgradeCommand;
    private final ResearchCommand researchCommand;
    private final AbilityCommand abilityCommand;
    private final Map<Integer, BotService> bots = new java.util.HashMap<>(); // one per enemy, by player id
    private final UnitFactory unitFactory = new UnitFactory();
    private final CircleGenerator circleGenerator = new CircleGenerator();
    private final List<DeployOption> deployOptions;


    private ControllerState currentState = ControllerState.NORMAL;
    private UnitEnum unitTypeToAdd = null;
    private Ability armedAbility = null;
    private int selectedUnitId = -1; // -1 means no unit is selected
    private boolean animating = false; // true while an animation or the bot turn blocks the player's input
    private boolean stopped = false; // the player left the game: nothing more happens in it
    private int round = 1;
    private static Difficulty difficulty = Difficulty.NORMAL; // kept for the next game ("Play again")
    private final List<GameEvent> enemyTurn = new ArrayList<>(); // what the player saw of the enemy's turn
    private Sight sightBefore; // what the player saw before its current action

    public GameController(Game model, GameView view) {
        this.model = model;
        this.view = view;

        this.addCommand = new AddCommand(model);
        this.moveCommand = new MoveCommand(model);
        this.attackCommand = new AttackCommand(model);
        this.healCommand = new HealCommand(model);
        this.upgradeCommand = new UpgradeCommand(model);
        this.researchCommand = new ResearchCommand(model);
        this.abilityCommand = new AbilityCommand(model);
        for (Player player : model.getPlayers()) {
            if (player != model.getPlayerOne()) {
                bots.put(player.getPlayerID(), new BotService(model, addCommand, moveCommand, attackCommand, healCommand,
                        upgradeCommand, new java.util.Random(), player.getPlayerID()));
            }
        }
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
        if (unit instanceof Infantry) return DeployOption.Category.INFANTRY;
        if (unit instanceof Aircraft) return DeployOption.Category.AIR;
        if (unit instanceof Ship || unit instanceof TransportShip) return DeployOption.Category.NAVY;
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
            case "Kongo" -> "Aegis destroyer: double damage to aircraft, covers the fleet from air attacks, and hunts submarines.";
            case "Soryu" -> "Submarine: torpedoes deal double damage to ships. Attacks only ships; only ships and aircraft can attack it.";
            case "Izumo" -> "Aircraft carrier, a mobile air base: aircraft are deployed onto its deck and can land on it. Unarmed.";
            case "Osumi" -> "Amphibious assault ship: carries one ground unit across the water (drive onto it, then off at another shore). Docked at the coast it deploys infantry and light vehicles. Unarmed.";
            case "Infantry" -> "Cheap riflemen. Cross forests freely and the only ground unit that can climb mountains (good cover).";
            case "Anti-Tank Team" -> "Type 01 LMAT team: fragile, but deals double damage to tanks. Moves like infantry.";
            case "AH-64D" -> "Apache attack helicopter: 1.5x damage to tanks. Flies anywhere, but flies low: ground fire hits it at full strength.";
            case "Army Base" -> "Forward base: +2 RP per round, deploys army units on top of it, and a Radar Station can be built near it. Built on base ruins by a Recovery Vehicle.";
            case "Air Base" -> "Airfield: +2 RP per round, deploys aircraft on top of it (so does a carrier at sea). Built on base ruins by a Recovery Vehicle.";
            case "Naval Base" -> "Harbour: +2 RP per round, launches ships onto the water next to it. Built on base ruins next to water by a Recovery Vehicle.";
            case "Radar Station" -> "Friendly units within 4 tiles shoot 1 tile further.";
            default -> unit instanceof Aircraft ? "Flies over any terrain; ground fire only deals three quarters of its damage to it." : "";
        };
        if (unit instanceof Infrastructure) {
            return text;
        }
        String orders = unit instanceof AssaultClass
                ? unit instanceof ActsAfterMoving ? " Can move and attack in the same turn." : " Moves or attacks each turn, not both."
                : "";
        String where = unit instanceof Helicopter ? " Deploys on top of your base, an army base, an air base or a carrier."
                : unit instanceof Aircraft ? " Deploys on top of an air base or a carrier."
                : unit instanceof Navy ? " Launched onto the water next to a naval base."
                : Osumi.isLight(unit) ? " Deploys on top of your base, an army base or a docked Osumi."
                : " Deploys on top of your base or an army base.";
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
        view.setAttackPreview(this::attackPreview);
        bots.values().forEach(bot -> bot.setDifficulty(difficulty));
        view.setDifficulty(difficulty, level -> {
            difficulty = level;
            bots.values().forEach(bot -> bot.setDifficulty(level));
            view.logInfo("Bot difficulty: " + level + ".");
        });
        view.setOnPickReady(this::selectUnit);
        view.setResearch(this::research, research -> researchCommand.refusal(model.getPlayerOne(), research));
        view.setAbilities(this::prepareAbility, ability -> abilityCommand.notReady(model.getPlayerOne(), ability),
                this::abilityArea);
        view.setFogOfWar(model.isFogOfWar(), fog -> {
            model.setFogOfWar(fog);
            if (canAct()) refresh();
        });
    }

    /** Ends this game for good (the player went back to the main menu): a running enemy turn stops too. */
    public void stop() {
        stopped = true;
    }

    private boolean canAct() {
        return model.isPlayerOneTurn() && !model.isGameOver() && !animating && !stopped;
    }

    private void handleKey(KeyEvent event) {
        if (!canAct()) return;
        switch (event.getCode()) {
            case E -> endTurn();
            case U -> upgradeSelected();
            case N -> selectNextReady();
            case R -> view.toggleResearch();
            case Z -> prepareAbility(Ability.RADAR_SWEEP);
            case X -> prepareAbility(Ability.ARTILLERY_BARRAGE);
            case C -> prepareAbility(Ability.AIRSTRIKE);
            case V -> prepareAbility(Ability.CRUISE_MISSILE);
            case B -> prepareAbility(Ability.PARATROOPERS);
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

    /** Passes the turn to the next player still in the game; a bot then plays its turn. */
    private void endTurn() {
        resetSelection(); // Deselect unit when turn ends
        if (model.isPlayerOneTurn()) {
            enemyTurn.clear(); // the summary covers all enemy turns until the player's next one
        }
        model.switchTurn();
        if (model.isPlayerOneTurn()) {
            round++;
            view.setRound(round);
            view.logInfo("— Round " + round + ": your turn —");
        } else {
            view.logInfo("— " + model.getPlayerName(model.getCurrentPlayer().getPlayerID()) + " turn —");
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
                String name = model.getPlayerName(model.getCurrentPlayer().getPlayerID()).toUpperCase();
                view.showBanner(name + " TURN", () -> guardedBot(this::beginBotTurn));
            } else {
                PauseTransition pause = new PauseTransition(Duration.seconds(BOT_THINK_SECONDS));
                pause.setOnFinished(event -> guardedBot(this::beginBotTurn));
                pause.play();
            }
        }
    }

    private BotService currentBot() {
        return bots.get(model.getCurrentPlayer().getPlayerID());
    }

    private void beginBotTurn() {
        currentBot().startTurn();
        botStep();
    }

    private void botStep() {
        Sight before = sight();
        Optional<List<GameEvent>> step = currentBot().performNextStep();
        if (step.isEmpty()) {
            finishBotTurn();
            return;
        }
        List<GameEvent> visible = seenEvents(step.get(), before);
        enemyTurn.addAll(visible);
        logEvents(visible);
        play(visible, this::afterBotStep);
    }

    /** The enemy units and the tiles player one sees, taken before an action so its events can be filtered. */
    private record Sight(Map<Integer, Unit> enemies, Set<Point> tiles) {
    }

    private Sight sight() {
        return new Sight(enemyUnits(), model.getVisibleTiles(model.getPlayerOne()));
    }

    /** Every unit of every enemy, by id. */
    private Map<Integer, Unit> enemyUnits() {
        Map<Integer, Unit> units = new java.util.HashMap<>();
        for (Player player : model.getPlayers()) {
            if (player != model.getPlayerOne()) units.putAll(player.getPlayerDeck().getUnits());
        }
        return units;
    }

    /**
     * What player one sees of the events under fog of war: enemy units only where it could see them (stealth units
     * and submarines included), a move only along the part of its path in view, and a shot from the fog only by
     * the damage it does.
     */
    private List<GameEvent> seenEvents(List<GameEvent> events, Sight before) {
        if (!model.isFogOfWar()) {
            return events;
        }
        Map<Integer, Unit> enemies = new java.util.HashMap<>(before.enemies());
        enemies.putAll(enemyUnits());
        Set<Point> seen = new HashSet<>(before.tiles());
        seen.addAll(model.getVisibleTiles(model.getPlayerOne()));
        java.util.function.BiPredicate<Integer, Point> sees = (id, pos) -> {
            Unit unit = enemies.get(id);
            return unit == null ? seen.contains(pos) : Vision.canSee(model.getPlayerOne(), unit, pos, seen);
        };
        List<GameEvent> visible = new ArrayList<>();
        for (GameEvent event : events) {
            switch (event) {
                case GameEvent.UnitDeployed e -> {
                    if (e.playerId() == 1 || sees.test(e.unitId(), e.pos())) visible.add(e);
                }
                case GameEvent.UnitMoved e -> {
                    if (e.playerId() == 1) {
                        visible.add(e);
                        continue;
                    }
                    int first = -1, last = -1;
                    for (int i = 0; i < e.path().size(); i++) {
                        if (sees.test(e.unitId(), e.path().get(i))) {
                            if (first < 0) first = i;
                            last = i;
                        }
                    }
                    if (last > first) {
                        List<Point> path = e.path().subList(first, last + 1);
                        visible.add(new GameEvent.UnitMoved(e.playerId(), e.unitId(), e.unitName(),
                                path.get(0), path.get(path.size() - 1), path));
                    }
                }
                case GameEvent.UnitAttacked e -> {
                    // a shot from the fog shows only by its damage; a shot into the fog not at all
                    if (sees.test(e.targetId(), e.to()) && (e.playerId() == 1 || sees.test(e.attackerId(), e.from()))) {
                        visible.add(e);
                    }
                }
                case GameEvent.UnitDamaged e -> {
                    if (e.ownerId() == 1 || sees.test(e.unitId(), e.pos())) visible.add(e);
                }
                case GameEvent.UnitDestroyed e -> {
                    if (e.ownerId() == 1 || sees.test(e.unitId(), e.pos())) visible.add(e);
                }
                case GameEvent.UnitHealed e -> {
                    // the animation shows the supporter too, so a repair from the fog stays unseen
                    if (e.playerId() == 1 || (sees.test(e.supporterId(), e.from()) && sees.test(e.targetId(), e.pos()))) {
                        visible.add(e);
                    }
                }
                case GameEvent.BaseUpgraded e -> {
                    if (e.playerId() == 1 || seen.contains(e.pos())) visible.add(e);
                }
                case GameEvent.UnitPromoted e -> {
                    if (e.playerId() == 1 || sees.test(e.unitId(), e.pos())) visible.add(e);
                }
                case GameEvent.ResearchCompleted e -> {
                    if (e.playerId() == 1) visible.add(e); // what the enemy researches stays secret
                }
                case GameEvent.AbilityUsed e -> {
                    // an enemy's sweep goes unnoticed; its shells and bombs are seen where they land
                    if (e.playerId() == 1 || (e.ability() != Ability.RADAR_SWEEP && e.area().stream().anyMatch(seen::contains))) {
                        visible.add(e);
                    }
                }
            }
        }
        return visible;
    }

    private void afterBotStep() {
        checkWinCondition();
        if (model.isGameOver()) {
            unlockInput();
            view.showWinner(model.getWinnerId());
            refreshStatus();
            return;
        }
        // Come back through the event loop so that a long bot turn without animations never recurses deeply.
        if (view.isAnimationsEnabled()) {
            PauseTransition gap = new PauseTransition(Duration.seconds(BOT_STEP_GAP_SECONDS));
            gap.setOnFinished(event -> guardedBot(this::botStep));
            gap.play();
        } else {
            Platform.runLater(() -> guardedBot(this::botStep));
        }
    }

    /** Ends the bot's turn: the next bot takes over, or the player gets a summary of what it saw. */
    private void finishBotTurn() {
        endTurn();
        if (model.isPlayerOneTurn()) {
            String summary = summarizeEnemyTurn();
            view.logInfo((bots.size() == 1 ? "Enemy turn: " : "Enemy turns: ") + summary);
            view.showBanner("YOUR TURN", summary, this::unlockInput);
        }
    }

    /** One line on what the player saw of the enemy's turn: attacks, losses on both sides, new enemy units. */
    private String summarizeEnemyTurn() {
        int attacks = 0, spotted = 0, hqDamage = 0, moves = 0;
        List<String> yourLosses = new ArrayList<>();
        int enemyLosses = 0;
        for (GameEvent event : enemyTurn) {
            switch (event) {
                case GameEvent.UnitAttacked e -> { if (e.playerId() != 1) attacks++; }
                case GameEvent.UnitDeployed e -> spotted++;
                case GameEvent.UnitMoved e -> moves++;
                case GameEvent.UnitDamaged e -> { if (e.isBase() && e.ownerId() == 1) hqDamage += e.damage(); }
                case GameEvent.UnitDestroyed e -> {
                    if (e.ownerId() == 1 && !e.isBase()) yourLosses.add(e.unitName());
                    else if (e.ownerId() != 1) enemyLosses++;
                }
                default -> { }
            }
        }
        if (enemyTurn.isEmpty()) {
            return "the enemy moved unseen in the fog.";
        }
        List<String> parts = new ArrayList<>();
        if (moves > 0) parts.add(moves == 1 ? "1 move seen" : moves + " moves seen");
        parts.add(attacks == 0 ? "no attacks" : attacks == 1 ? "1 attack" : attacks + " attacks");
        if (!yourLosses.isEmpty()) parts.add("you lost " + String.join(", ", yourLosses));
        if (enemyLosses > 0) parts.add("the enemy lost " + (enemyLosses == 1 ? "1 unit" : enemyLosses + " units"));
        if (hqDamage > 0) parts.add("your base took " + hqDamage + " damage");
        if (spotted > 0) parts.add(spotted == 1 ? "1 new enemy unit spotted" : spotted + " new enemy units spotted");
        return String.join(", ", parts) + ".";
    }

    /** The units that can still do something this turn, in a stable order. */
    private List<Integer> readyUnits() {
        List<Integer> ready = new ArrayList<>();
        for (Map.Entry<Integer, Unit> entry : new java.util.TreeMap<>(model.getPlayerOne().getPlayerDeck().getUnits()).entrySet()) {
            Unit unit = entry.getValue();
            if (isReady(entry.getKey(), unit)) {
                ready.add(entry.getKey());
            }
        }
        return ready;
    }

    /** Selects a ready unit (a click in the ready list). */
    private void selectUnit(int unitId) {
        if (!canAct() || model.findUnitById(unitId) == null) return;
        resetSelection();
        selectedUnitId = unitId;
        currentState = ControllerState.UNIT_SELECTED;
        refresh();
    }

    /** Selects the next unit that is still ready (N). */
    private void selectNextReady() {
        List<Integer> ready = readyUnits();
        if (ready.isEmpty()) return;
        int next = ready.stream().filter(id -> id > selectedUnitId).findFirst().orElse(ready.get(0));
        selectUnit(next);
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
        // the player's own actions can draw fire from the fog (anti-air): it sees the hits, not the shooters
        play(sightBefore == null ? result.getEvents() : seenEvents(result.getEvents(), sightBefore),
                this::finishPlayerAction);
        return true;
    }

    private void finishPlayerAction() {
        checkWinCondition(); // first: a knocked-out enemy's units leave the board before it is redrawn
        unlockInput();
        if (model.isGameOver()) {
            view.showWinner(model.getWinnerId());
            refreshStatus();
        }
    }

    /** Logs what a command did, or why it was refused. */
    private void logResult(CommandResult result) {
        if (result.wasSuccessful() && !result.getEvents().isEmpty()) {
            // the shells and shots of the player's own action may hit enemies it cannot see
            logEvents(sightBefore == null ? result.getEvents() : seenEvents(result.getEvents(), sightBefore));
            if (result.getMessage().startsWith("Ambush!")) {
                view.logInfo(result.getMessage());
            }
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
                case GameEvent.UnitPromoted e -> owner(e.playerId()) + " " + e.unitName() + " was promoted to "
                        + AssaultClass.rankTitle(e.rank()) + " (+" + Math.round(e.rank() * AssaultClass.DAMAGE_PER_RANK * 100)
                        + "% damage).";
                case GameEvent.ResearchCompleted e -> e.playerId() == 1 ? "Research complete: " + e.researchName() + "."
                        : model.getPlayerName(e.playerId()) + " researched " + e.researchName() + ".";
                case GameEvent.AbilityUsed e -> (e.playerId() == 1 ? "You " : model.getPlayerName(e.playerId()) + " ")
                        + switch (e.ability()) {
                            case PARATROOPERS -> e.playerId() == 1 ? "drop paratroopers." : "drops paratroopers.";
                            case AIRSTRIKE -> (e.playerId() == 1 ? "call" : "calls") + " in an airstrike.";
                            case CRUISE_MISSILE -> (e.playerId() == 1 ? "fire" : "fires") + " a cruise missile.";
                            default -> (e.playerId() == 1 ? "call" : "calls") + " in a " + e.ability().label().toLowerCase() + ".";
                        };
            };
            if (line != null) {
                view.logInfo(line);
            }
        }
    }

    private String owner(int playerId) {
        return playerId == 1 ? "Your" : model.getPlayerName(playerId) + (bots.size() == 1 ? "" : "'s");
    }

    /** Knocks out the players whose base has fallen and says so in the log. */
    private void checkWinCondition() {
        for (Player out : model.checkWinCondition()) {
            view.logInfo(out == model.getPlayerOne() ? "Your base has fallen!"
                    : model.getPlayerName(out.getPlayerID()) + "'s base has fallen: it is out of the game!");
        }
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

    /**
     * Runs a step of the bot turn; if it fails, the bot's turn ends there and the player gets the next one, so the
     * game can never get stuck on the enemy turn.
     */
    private void guardedBot(Runnable action) {
        if (stopped) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | Error e) {
            System.err.println("Bot turn failed: " + e);
            e.printStackTrace();
            if (!model.isPlayerOneTurn() && !model.isGameOver()) {
                view.logInfo("The enemy turn was cut short by an error.");
                endTurn();
            }
            unlockInput();
        }
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

    /** What the selected unit's attack on the unit at the cell would do, for the inspector; null if none. */
    private AttackPreview attackPreview(Point cell) {
        if (currentState == ControllerState.AWAITING_ABILITY_TARGET && canAct() && armedAbility != Ability.RADAR_SWEEP
                && armedAbility != Ability.PARATROOPERS) {
            Unit target = switch (armedAbility) {
                case AIRSTRIKE -> abilityCommand.airstrikeTarget(model.getCurrentPlayer(), cell);
                case CRUISE_MISSILE -> abilityCommand.cruiseTarget(model.getCurrentPlayer(), cell);
                default -> model.getUnitSeenAt(cell, model.getCurrentPlayer());
            };
            if (target == null || !model.isVisibleTo(model.getCurrentPlayer(), target)) return null;
            if (target == null || model.isUnitOwnedByCurrentPlayer(model.getUnitID(target))) return null;
            int damage = abilityCommand.predictDamage(armedAbility, target);
            int hpAfter = target.getHealthPoints() - damage;
            return new AttackPreview(damage, Math.max(0, hpAfter), hpAfter <= 0, 0, 0, false);
        }
        if (currentState != ControllerState.UNIT_SELECTED || !canAct()) return null;
        Unit target = model.getUnitSeenAt(cell, model.getCurrentPlayer());
        if (target == null || model.isUnitOwnedByCurrentPlayer(model.getUnitID(target))) return null;
        return attackCommand.preview(model.getCurrentPlayer(), selectedUnitId, model.getUnitID(target));
    }

    /** Upgrades the selected base (button in the inspector, or U). */
    private void upgradeSelected() {
        if (!canAct() || currentState != ControllerState.UNIT_SELECTED
                || !(model.findUnitById(selectedUnitId) instanceof MilitaryBase)) {
            return;
        }
        sightBefore = sight();
        CommandResult result = upgradeCommand.execute(model.getCurrentPlayer(), selectedUnitId);
        logResult(result);
        if (!playPlayerAction(result)) {
            refresh();
        }
    }

    /** Starts a research project (a click in the research tree). */
    private void research(Research research) {
        if (!canAct()) return;
        sightBefore = sight();
        CommandResult result = researchCommand.execute(model.getCurrentPlayer(), research);
        logResult(result);
        if (!playPlayerAction(result)) {
            refresh();
        }
    }

    /** Arms an ability for targeting (button or Z/X/C); picking the armed one again cancels it. */
    private void prepareAbility(Ability ability) {
        if (!canAct()) return;
        if (currentState == ControllerState.AWAITING_ABILITY_TARGET && armedAbility == ability) {
            cancel();
            return;
        }
        String notReady = abilityCommand.notReady(model.getCurrentPlayer(), ability);
        if (notReady != null) {
            view.logInfo(ability.label() + ": " + notReady);
            return;
        }
        resetSelection();
        armedAbility = ability;
        currentState = ControllerState.AWAITING_ABILITY_TARGET;
        refresh();
    }

    /** The tiles the armed ability would reach if aimed at the cell, for the board; empty if none is armed. */
    private List<Point> abilityArea(Point cell) {
        if (currentState != ControllerState.AWAITING_ABILITY_TARGET || cell == null || !canAct()) return List.of();
        return abilityCommand.area(armedAbility, cell);
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
        armedAbility = null;
        currentState = ControllerState.NORMAL;
    }

    private void handleGridClick(MouseEvent event) {
        if (!canAct()) {
            return;
        }
        if (event.getClickCount() > 1) {
            return; // the second click of a double-click would only deselect what the first one picked
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
        sightBefore = sight();

        if (currentState == ControllerState.AWAITING_ABILITY_TARGET) {
            CommandResult result = abilityCommand.execute(model.getCurrentPlayer(), armedAbility, clickedPoint);
            logResult(result);
            if (result.wasSuccessful()) {
                resetSelection();
            }
            if (!playPlayerAction(result)) {
                refresh();
            }
            return;
        }
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

        Player currentPlayer = model.getCurrentPlayer();
        // a hidden enemy is an empty tile to the player (moving there is an ambush), or just the building it is on
        Unit clickedUnit = model.getUnitSeenAt(clickedPoint, currentPlayer);
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
            } else if (clickedUnit != null && selectedUnit.getPosition().equals(clickedPoint)
                    && model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                // clicking the selected unit's tile again goes down the stack (the units on deck, then the building
                // or ship under them); past the bottom it drops the selection
                List<Unit> stack = model.getStackAt(clickedPoint);
                int next = stack.indexOf(selectedUnit) + 1;
                if (next > 0 && next < stack.size() && model.isUnitOwnedByCurrentPlayer(model.getUnitID(stack.get(next)))) {
                    selectedUnitId = model.getUnitID(stack.get(next));
                } else {
                    resetSelection();
                }
            } else if (clickedUnit != null && !model.isUnitOwnedByCurrentPlayer(model.getUnitID(clickedUnit))) {
                CommandResult result = attackCommand.execute(currentPlayer, selectedUnitId, model.getUnitID(clickedUnit));
                logResult(result);
                if (result.wasSuccessful()) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (clickedUnit != null && highlights.supportTargets().contains(clickedPoint)) {
                CommandResult result = healCommand.execute(currentPlayer, selectedUnitId, supportTargetAt(clickedPoint));
                logResult(result);
                if (result.wasSuccessful()) {
                    resetSelection();
                }
                animated = playPlayerAction(result);
            } else if (highlights.moveTiles().contains(clickedPoint)
                    && (clickedUnit == null || model.canStandOn(selectedUnit, clickedPoint))) {
                // an empty tile, or one of the player's buildings or ships with room on top
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
        checkWinCondition();
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
        view.setArmedAbility(currentState == ControllerState.AWAITING_ABILITY_TARGET ? armedAbility : null);
        view.setBlockedDeploys(blockedDeploys());
        view.setReadyUnits(canAct() ? readyUnits() : List.of());
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
        if (currentState == ControllerState.AWAITING_ABILITY_TARGET) {
            // the enemies it can hit are marked; the barrage and the sweep may also be aimed at empty ground
            Set<Point> targets = new HashSet<>();
            if (armedAbility == Ability.PARATROOPERS) {
                // where the squad can land, like deploy tiles
                return new BoardHighlights(-1, Set.of(), Set.of(), Set.of(), Set.of(),
                        abilityCommand.targetTiles(model.getCurrentPlayer(), armedAbility));
            }
            if (armedAbility == Ability.CRUISE_MISSILE) {
                targets.addAll(abilityCommand.targetTiles(model.getCurrentPlayer(), armedAbility));
            } else if (armedAbility != Ability.RADAR_SWEEP) {
                Set<Point> aim = abilityCommand.targetTiles(model.getCurrentPlayer(), armedAbility);
                for (Unit enemy : model.getVisibleEnemies(model.getCurrentPlayer())) {
                    if (enemy.getHealthPoints() > 0 && aim.contains(enemy.getPosition())
                            && !model.isEliminated(model.getOwner(enemy))) {
                        targets.add(enemy.getPosition());
                    }
                }
            }
            return new BoardHighlights(-1, Set.of(), Set.of(), targets, Set.of(), Set.of());
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
            for (int targetId : attackCommand.findTargets(model.getCurrentPlayer(), selectedUnitId)) {
                targets.add(model.findUnitById(targetId).getPosition());
            }
        }
        Set<Point> supportTargets = new HashSet<>();
        for (int targetId : healCommand.findTargets(model.getCurrentPlayer(), selectedUnitId)) {
            supportTargets.add(model.findUnitById(targetId).getPosition());
        }
        return new BoardHighlights(selectedUnitId, moves, range, targets, supportTargets, Set.of());
    }

    /** The unit on the tile the selected support unit would heal or repair: the top one, else the building under it. */
    private int supportTargetAt(Point point) {
        List<Integer> targets = healCommand.findTargets(model.getCurrentPlayer(), selectedUnitId);
        return model.getStackAt(point).stream().map(model::getUnitID).filter(targets::contains)
                .findFirst().orElseThrow();
    }

    private Set<Point> deployTiles(UnitEnum type) {
        Player player = model.getCurrentPlayer();
        return addCommand.findDeployablePositions(player, unitFactory.createUnit(player.getPlayerID(), type, PROBE_POSITION));
    }

    /** Why the type cannot be deployed right now (resources aside), or null if it can. */
    private String blockedReason(UnitEnum type) {
        Optional<Research> research = Research.requiredFor(type);
        if (research.isPresent() && !model.getCurrentPlayer().hasResearched(research.get())) {
            return "research " + research.get().label() + " first (Research, R).";
        }
        Unit unit = unitFactory.createUnit(model.getCurrentPlayer().getPlayerID(), type, PROBE_POSITION);
        if (!(unit instanceof Infrastructure) && addCommand.isArmyFull(model.getCurrentPlayer())) {
            return "your army is at its limit (" + model.getArmyLimit(model.getCurrentPlayer())
                    + " units): hold more bases or research Logistics.";
        }
        if (!deployTiles(type).isEmpty()) {
            return null;
        }
        boolean hasBuilding = model.getCurrentPlayer().getPlayerDeck().getBuildings().stream()
                .anyMatch(building -> unit instanceof Infrastructure ? building.getConstructionRadius() > 0 : building.canDeploy(unit))
                || model.getCurrentPlayer().getPlayerDeck().getUnits().values().stream()
                .anyMatch(own -> own instanceof TransportShip ship && ship.canDeploy(unit, true));
        if (Command.isSiteBuilding(unit)) {
            boolean hasVehicle = model.getCurrentPlayer().getPlayerDeck().getUnits().values().stream()
                    .anyMatch(own -> own instanceof ArmoredRecoveryVehicle && own.isAvailable());
            if (!hasVehicle) return "needs a ready Armored Recovery Vehicle next to free base ruins.";
            return unit instanceof NavyBase ? "move your Recovery Vehicle next to free base ruins by the water."
                    : "move your Recovery Vehicle next to free base ruins.";
        }
        if (unit instanceof Infrastructure) return "no free open ground near your base or an army base.";
        if (!hasBuilding && unit instanceof Aircraft) return "build an Air Base or launch a carrier first.";
        if (!hasBuilding && unit instanceof Navy) return "build a Naval Base first.";
        if (unit instanceof Navy) return "the water around your naval base is taken.";
        return "a unit stands on top of every building that deploys it: move it off first"
                + (Osumi.isLight(unit) ? " (or dock an Osumi at the coast)." : ".");
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
            if (isReady(entry.getKey(), unit)) {
                return false;
            }
        }
        int resources = model.getCurrentPlayer().getResourcePoints();
        return deployOptions.stream().noneMatch(option -> option.cost() <= resources && blockedReason(option.type()) == null);
    }

    /** True if the unit can still do something this turn: move somewhere, or act from where it stands. */
    private boolean isReady(int unitId, Unit unit) {
        if (!(unit instanceof MovableUnit) || !unit.isAvailable()) return false;
        return (!unit.hasMoved() && !Movement.reachable(model, unit).isEmpty()) || canStillAct(unitId, unit);
    }

    /** True if the unit has something to do from where it stands: a target to shoot, heal or build on. */
    private boolean canStillAct(int unitId, Unit unit) {
        Player player = model.getCurrentPlayer();
        if (!attackCommand.findTargets(player, unitId).isEmpty()) {
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
            case AWAITING_ABILITY_TARGET -> {
                String where = switch (armedAbility) {
                    case RADAR_SWEEP -> "click any tile to reveal everything within " + armedAbility.radius() + " of it";
                    case ARTILLERY_BARRAGE -> "click a tile you can see: it and every tile around it are shelled";
                    case AIRSTRIKE -> "click an enemy unit or building you can see";
                    case CRUISE_MISSILE -> "click any enemy building, seen or not";
                    case PARATROOPERS -> "click a green tile to drop a rifle squad there";
                };
                return armedAbility.label() + " (" + armedAbility.cost() + " RP): " + where + ".  Right-click or Esc to cancel.";
            }
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
                    if (base.canUpgrade(model.getMaxBaseLevel(model.getCurrentPlayer()))) {
                        return "Press U (or Upgrade in the inspector) to raise this base to level " + (base.getLevel() + 1)
                                + " for " + base.getUpgradeCost() + " RP.  Esc to deselect.";
                    }
                    return base.getLevel() < MilitaryBase.MAX_LEVEL
                            ? "Research Industrial Expansion to raise bases to level " + MilitaryBase.MAX_LEVEL + ".  Esc to deselect."
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
                return "Select a unit (N: next ready one), or deploy with keys 1–9 (Tab switches the deploy tab).  "
                        + "R: research, Z/X/C/V/B: abilities, E: end your turn.";
            }
        }
    }
}
