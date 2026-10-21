package game.model;

import game.game.Player;
import game.game.commands.AttackCommand;
import game.game.map.MapSize;
import game.game.map.MapType;
import game.testing.Fixtures;
import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.AssaultTanks.TypeTen;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Up to four bots, everyone against everyone. */
class MultiplayerTest {

    private static Game game(int enemies) {
        Game game = new Game(new GameSettings(MapType.CONTINENTS, MapSize.MEDIUM, enemies));
        game.setFogOfWar(false);
        return game;
    }

    private static void destroyBase(Player player) {
        var base = player.getPlayerDeck().getBase();
        base.setHealthPoints(-base.getHealthPoints());
    }

    @Test
    void everyPlayerHasItsOwnBaseInItsOwnCorner() {
        Game game = game(4);

        assertEquals(5, game.getPlayers().size());
        Set<Point> bases = new HashSet<>();
        Set<Integer> ids = new HashSet<>();
        for (Player player : game.getPlayers()) {
            bases.add(player.getPlayerDeck().getBase().getPosition());
            ids.add(player.getPlayerDeck().getBase().getBaseID());
        }
        assertEquals(Set.of(new Point(0, 0), new Point(15, 15), new Point(15, 0), new Point(0, 15), new Point(8, 8)), bases);
        assertEquals(Set.of(1, 2, 3, 4, 5), ids);

        int unitId = Fixtures.place(game, 5, new TypeTen(5, new Point(9, 9)));
        assertFalse(ids.contains(unitId), "unit ids never collide with the bases' ids");
    }

    @Test
    void theMapSizeComesFromTheSettings() {
        for (MapSize size : MapSize.values()) {
            Game game = new Game(new GameSettings(MapType.ARCHIPELAGO, size, 2));
            assertEquals(new Point(size.tiles(), size.tiles()), game.getBattlefield().getSize());
            assertEquals(new Point(size.tiles() - 1, size.tiles() - 1), game.getPlayerTwo().getPlayerDeck().getBase().getPosition());
        }
        assertEquals(new Point(16, 16), new Game().getBattlefield().getSize(), "medium is the default");
    }

    @Test
    void turnsGoRoundAndSkipPlayersWhoAreOut() {
        Game game = game(3);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            order.add(game.getCurrentPlayer().getPlayerID());
            game.switchTurn();
        }
        assertEquals(List.of(1, 2, 3, 4), order);
        assertTrue(game.isPlayerOneTurn());

        destroyBase(game.getPlayer(3));
        assertEquals(List.of(game.getPlayer(3)), game.checkWinCondition());
        assertFalse(game.isGameOver(), "three players are still in");

        game.switchTurn();
        game.switchTurn();
        assertEquals(4, game.getCurrentPlayer().getPlayerID(), "player 3 is skipped");
    }

    @Test
    void aPlayerWhoseBaseFallsLosesItsArmy() {
        Game game = game(2);
        Fixtures.place(game, 3, new TypeTen(3, new Point(14, 1)));

        destroyBase(game.getPlayer(3));
        game.checkWinCondition();

        assertTrue(game.isEliminated(game.getPlayer(3)));
        assertEquals(1, game.getPlayer(3).getPlayerDeck().getUnits().size(), "only the ruined base is left");
        assertFalse(game.getEnemies(game.getPlayerOne()).contains(game.getPlayer(3)));
    }

    @Test
    void theLastBaseStandingWins() {
        Game game = game(2);
        destroyBase(game.getPlayer(2));
        game.checkWinCondition();
        assertFalse(game.isGameOver());

        destroyBase(game.getPlayer(3));
        game.checkWinCondition();
        assertTrue(game.isGameOver());
        assertEquals(1, game.getWinnerId());
    }

    @Test
    void theGameIsLostWhenThePlayersBaseFalls() {
        Game game = game(3);
        destroyBase(game.getPlayerOne());
        game.checkWinCondition();

        assertTrue(game.isGameOver());
        assertEquals(0, game.getWinnerId(), "no single winner among the bots still fighting");
    }

    @Test
    void botsFightEachOtherToo() {
        Game game = game(2);
        int attacker = Fixtures.place(game, 2, new TypeNinety(2, new Point(10, 4)));
        int target = Fixtures.place(game, 3, new TypeTen(3, new Point(11, 4)));

        assertTrue(new AttackCommand(game).findTargets(game.getPlayer(2), attacker).contains(target));
    }
}
