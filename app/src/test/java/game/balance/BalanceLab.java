package game.balance;

import game.controller.BotService;
import game.enums.UnitEnum;
import game.events.GameEvent;
import game.game.UnitFactory;
import game.game.commands.*;
import game.model.Game;
import game.units.*;

import java.awt.Point;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

/**
 * Bot-vs-bot tournaments for balancing (not a unit test). Prints side win rates, game length, openings and, per unit
 * type, what it cost and what it achieved; optionally specialist bots (one unit family only) and ablations (the
 * full bot against one that may not buy a family) against the full bot.
 *
 * <pre>gradle balanceLab --args="400 40 120"   # mirror games, specialist games, ablation games</pre>
 * Set DUMP=1 to print the board of games that hit the round limit; BALANCE_CHECK=1 makes it fail (exit 1) when one
 * side wins more than 65 % of the games or more than 10 % hit the round limit (used by CI).
 */
public class BalanceLab {
    static final int MAX_ROUNDS = 60;
    static final UnitFactory F = new UnitFactory();

    static class Stat { int bought, rp, dmg, hqDmg, bldDmg, airDmg, kills, lost, attacks; }
    static final Set<String> AIR = Set.of("F-4EJ Kai", "F-15", "F-2", "F-35", "A-10");
    static Map<String, Stat> stats = new TreeMap<>();
    static Stat st(String n) { return stats.computeIfAbsent(n, k -> new Stat()); }

    record Result(int winner, int rounds, String hqKiller, int firstContact, int firstHqHit, String end) {}
    static int firstContact, firstHqHit;
    static String[] firstBuy = new String[3];
    static Map<String, int[]> openings = new TreeMap<>();
    static List<String> log = new ArrayList<>();

    static Result play(long seed, Predicate<UnitEnum> roster1, Predicate<UnitEnum> roster2, boolean record) {
        Game m = new Game();
        BotService[] bots = new BotService[3];
        for (int p = 1; p <= 2; p++) {
            bots[p] = new BotService(m, new AddCommand(m), new MoveCommand(m), new AttackCommand(m), new HealCommand(m),
                    new UpgradeCommand(m), new Random(seed * 31 + p), p);
        }
        bots[1].setRoster(roster1); bots[2].setRoster(roster2);
        Map<Integer, String> names = new HashMap<>();
        String lastAttacker = null, hqKiller = null;
        int round;
        firstContact = 0; firstHqHit = 0; firstBuy[1] = firstBuy[2] = null; log.clear();
        // the game starts with player one; Game() begins on player one's turn
        for (round = 1; round <= MAX_ROUNDS && !m.isGameOver(); round++) {
            for (int p = 1; p <= 2 && !m.isGameOver(); p++) {
                BotService bot = bots[p];
                bot.startTurn();
                Optional<List<GameEvent>> step;
                while ((step = bot.performNextStep()).isPresent()) {
                    for (GameEvent e : step.get()) {
                        switch (e) {
                            case GameEvent.UnitDeployed d -> {
                                names.put(d.unitId(), d.unitName());
                                if (firstBuy[d.playerId()] == null) firstBuy[d.playerId()] = d.unitName();
                                if (round <= 8) log.add("r" + round + " P" + d.playerId() + " buys " + d.unitName() + " @" + d.pos().x + "," + d.pos().y);
                                if (record) { Stat s = st(d.unitName()); s.bought++; s.rp += cost(d.unitName()); }
                            }
                            case GameEvent.UnitAttacked a -> {
                                lastAttacker = a.attackerName(); if (record) st(lastAttacker).attacks++;
                                if (firstContact == 0) firstContact = round;
                                if (round <= 8) log.add("r" + round + " P" + a.playerId() + " " + a.attackerName() + " attacks " + (a.targetIsBase() ? "HQ" : a.targetId()));
                                if (a.targetIsBase() && firstHqHit == 0) firstHqHit = round;
                            }
                            case GameEvent.UnitDamaged d -> {
                                if (record) {
                                    Stat s = st(lastAttacker);
                                    int real = Math.min(d.damage(), Math.max(0, d.hpBefore()));
                                    s.dmg += real;
                                    if (d.isBase()) s.hqDmg += real;
                                    else if (isBuilding(d.unitName())) s.bldDmg += real;
                                    if (AIR.contains(d.unitName())) s.airDmg += real;
                                }
                                if (d.isBase() && d.hpAfter() <= 0) hqKiller = lastAttacker;
                            }
                            case GameEvent.UnitDestroyed d -> { if (record && !d.isBase()) { st(lastAttacker).kills++; st(d.unitName()).lost++; } }
                            default -> {}
                        }
                    }
                    m.checkWinCondition();
                    if (m.isGameOver()) break;
                }
                m.switchTurn();
            }
        }
        int winner = m.isGameOver() ? m.getWinnerId() : 0;
        if (record) {
            for (int p = 1; p <= 2; p++) {
                int[] w = openings.computeIfAbsent("P" + p + " opens " + firstBuy[p], k -> new int[2]);
                w[0]++; if (winner == p) w[1]++;
            }
        }
        String end = String.format("HQ %d/%d units %d/%d inc %d/%d", m.getPlayerOne().getPlayerDeck().getBase().getHealthPoints(),
                m.getPlayerTwo().getPlayerDeck().getBase().getHealthPoints(), m.getPlayerOne().getPlayerDeck().getUnits().size(),
                m.getPlayerTwo().getPlayerDeck().getUnits().size(), m.getIncome(m.getPlayerOne()), m.getIncome(m.getPlayerTwo()));
        if (winner == 0 && System.getenv("DUMP") != null) {
            StringBuilder b = new StringBuilder(end).append("\n");
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    Point p = new Point(x, y);
                    Unit u = m.getUnitAt(p);
                    char c = switch (m.getBattlefield().getGeographyOfPoint(p)) { case WATER -> '~'; case MOUNTAIN -> '^'; default -> '.'; };
                    if (m.getBattlefield().isBaseSite(p)) c = 'r';
                    if (u != null) c = (char) ((u.getPlayerID() == 1 ? 'a' : 'A') + Math.min(25, Math.abs(u.getUnitName().hashCode()) % 26));
                    if (u instanceof Base) c = u.getPlayerID() == 1 ? '1' : '2';
                    b.append(c);
                }
                b.append("\n");
            }
            for (int p = 1; p <= 2; p++) {
                var deck = p == 1 ? m.getPlayerOne().getPlayerDeck() : m.getPlayerTwo().getPlayerDeck();
                deck.getUnits().values().forEach(u -> b.append(u.getPlayerID()).append(":").append(u.getUnitName()).append("@")
                        .append(u.getPosition().x).append(",").append(u.getPosition().y).append(" "));
                b.append("\n");
            }
            end = b.toString();
        }
        return new Result(winner, round - 1, hqKiller, firstContact, firstHqHit, end);
    }

    static Map<String, Integer> costs = new HashMap<>();
    static int cost(String name) {
        if (costs.isEmpty()) for (UnitEnum t : UnitEnum.values()) { Unit u = F.createUnit(1, t, new Point()); costs.put(u.getUnitName(), u.getResourceCost()); }
        return costs.getOrDefault(name, 0);
    }
    static boolean isBuilding(String name) { return List.of("Army Base", "Air Base", "Naval Base", "Radar Station").contains(name); }

    static Set<UnitEnum> set(UnitEnum... t) { Set<UnitEnum> s = EnumSet.of(UnitEnum.ARMOREDRECOVERYVEHICLE); s.addAll(List.of(t)); return s; }

    public static void main(String[] args) {
        PrintStream out = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream())); // silence "Bot: ..." lines
        int games = Integer.parseInt(args[0]);

        int[] wins = new int[3]; int totalRounds = 0; Map<String, Integer> killers = new TreeMap<>();
        List<Integer> lengths = new ArrayList<>();
        double contact = 0, hqHit = 0; int timeoutsShown = 0;
        for (int g = 0; g < games; g++) {
            Result r = play(g, t -> true, t -> true, true);
            wins[r.winner]++; totalRounds += r.rounds; lengths.add(r.rounds);
            if (r.hqKiller != null) killers.merge(r.hqKiller, 1, Integer::sum);
            contact += r.firstContact; hqHit += r.firstHqHit;
            if (r.winner == 0 && timeoutsShown++ < 5) out.println("  timeout: " + r.end);
        }
        out.printf("first contact avg round %.1f, first HQ hit avg round %.1f%n", contact / (double) games, hqHit / (double) games);
        Collections.sort(lengths);
        openings.forEach((k, w) -> out.printf("  %-40s %4d games, wins %.0f%%%n", k, w[0], 100.0 * w[1] / w[0]));
        out.println("  opening of the last game: " + log);
        out.printf("MIRROR %d games: P1 %d  P2 %d  timeout %d | rounds avg %.1f median %d min %d max %d%n", games,
                wins[1], wins[2], wins[0], totalRounds / (double) games, lengths.get(games / 2), lengths.get(0), lengths.get(games - 1));
        out.println("HQ destroyed by: " + killers);
        if (System.getenv("BALANCE_CHECK") != null) {
            // CI smoke test: a side that wins far too often or games that never end mean something broke
            double sideShare = Math.max(wins[1], wins[2]) / (double) games;
            double stalemates = wins[0] / (double) games;
            if (sideShare > 0.65 || stalemates > 0.10) {
                out.printf("BALANCE CHECK FAILED: side share %.0f%%, stalemates %.0f%%%n", 100 * sideShare, 100 * stalemates);
                System.exit(1);
            }
            out.println("balance check passed");
        }
        out.printf("%-31s %6s %5s %8s %8s %7s %6s %5s %6s %7s %6s%n", "unit", "bought", "RP", "dmg", "hqDmg", "bldDmg", "kills", "lost", "dmg/RP", "kill/RP", "air/RP");
        for (var e : stats.entrySet()) {
            Stat s = e.getValue();
            if (s.bought == 0 && s.dmg == 0) continue;
            out.printf("%-31s %6d %5d %8d %8d %7d %6d %5d %6.0f %7.2f %6.0f%n", e.getKey(), s.bought, s.rp, s.dmg, s.hqDmg, s.bldDmg, s.kills, s.lost,
                    s.rp == 0 ? 0 : s.dmg / (double) s.rp, s.rp == 0 ? 0 : s.kills / (double) s.rp, s.rp == 0 ? 0 : s.airDmg / (double) s.rp);
        }

        if (args.length > 2) {
            Map<String, Set<UnitEnum>> without = new LinkedHashMap<>();
            without.put("anti-air", EnumSet.of(UnitEnum.TYPEEIGHTYSEVEN, UnitEnum.TYPEEIGHTYONEC, UnitEnum.TYPENINETYTHREE));
            without.put("aircraft", EnumSet.of(UnitEnum.FFOUREJKAI, UnitEnum.FFIFTEEN, UnitEnum.FTWO, UnitEnum.FTHIRTYFIVE, UnitEnum.ATEN));
            without.put("artillery", EnumSet.of(UnitEnum.TYPENINETYNINESPH));
            without.put("tanks", EnumSet.of(UnitEnum.TYPETEN, UnitEnum.TYPENINETY, UnitEnum.TYPESIXTEEN));
            without.put("ifv", EnumSet.of(UnitEnum.TYPEEIGHTYNINE));
            without.put("navy", EnumSet.of(UnitEnum.YAMATO));
            without.put("support", EnumSet.of(UnitEnum.ARMOREDAMBULANCE, UnitEnum.ARMOREDRECONNAISSANCE));
            without.put("radar", EnumSet.of(UnitEnum.RADARSTATION));
            int n = Integer.parseInt(args[2]);
            for (var e : without.entrySet()) {
                int fullWins = 0, cutWins = 0, draws = 0;
                for (int g = 0; g < n; g++) {
                    boolean cutFirst = g % 2 == 0;
                    Predicate<UnitEnum> cut = t -> !e.getValue().contains(t);
                    Result r = cutFirst ? play(2000 + g, cut, t -> true, false) : play(2000 + g, t -> true, cut, false);
                    int cutId = cutFirst ? 1 : 2;
                    if (r.winner == 0) draws++; else if (r.winner == cutId) cutWins++; else fullWins++;
                }
                out.printf("WITHOUT %-10s: full bot wins %3d, bot without it wins %3d, timeouts %d  -> worth %+.0f%%%n",
                        e.getKey(), fullWins, cutWins, draws, 100.0 * (fullWins - cutWins) / n);
            }
        }

        if (args.length > 1 && !args[1].equals("0")) {
            Map<String, Set<UnitEnum>> specs = new LinkedHashMap<>();
            specs.put("tanks", set(UnitEnum.TYPETEN, UnitEnum.TYPENINETY, UnitEnum.TYPESIXTEEN));
            specs.put("ifv", set(UnitEnum.TYPEEIGHTYNINE));
            specs.put("artillery", set(UnitEnum.TYPENINETYNINESPH));
            specs.put("antiair", set(UnitEnum.TYPEEIGHTYSEVEN, UnitEnum.TYPEEIGHTYONEC, UnitEnum.TYPENINETYTHREE));
            specs.put("air", set(UnitEnum.FFOUREJKAI, UnitEnum.FFIFTEEN, UnitEnum.FTWO, UnitEnum.FTHIRTYFIVE, UnitEnum.ATEN));
            specs.put("navy+tanks", set(UnitEnum.YAMATO, UnitEnum.TYPETEN, UnitEnum.TYPENINETY));
            int n = Integer.parseInt(args[1]);
            for (var e : specs.entrySet()) {
                int specWins = 0, mixedWins = 0, draws = 0;
                for (int g = 0; g < n; g++) {
                    boolean specFirst = g % 2 == 0;
                    Predicate<UnitEnum> spec = e.getValue()::contains;
                    Result r = specFirst ? play(1000 + g, spec, t -> true, false) : play(1000 + g, t -> true, spec, false);
                    int specId = specFirst ? 1 : 2;
                    if (r.winner == 0) draws++; else if (r.winner == specId) specWins++; else mixedWins++;
                }
                out.printf("SPECIALIST %-11s vs mixed: %3d wins, %3d losses, %3d timeouts%n", e.getKey(), specWins, mixedWins, draws);
            }
        }
    }
}
