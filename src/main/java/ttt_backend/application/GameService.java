package ttt_backend.application;

import ttt_backend.domain.model.Game;
import ttt_backend.domain.model.InvalidJoinException;
import ttt_backend.domain.model.InvalidMoveException;
import ttt_backend.domain.model.User;
import ttt_backend.domain.ports.GameRepository;
import ttt_backend.domain.ports.UserRepository;

import java.util.UUID;

public class GameService {
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public GameService(GameRepository gameRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public User registerUser(String username) {
        return this.userRepository.save(new User("user-" + UUID.randomUUID(), username));
    }

    public Game createGame() {
        return this.gameRepository.save(new Game("game-" + UUID.randomUUID()));
    }

    public void joinGame(String gameId, String userId, Game.GameSymbolType symbolType) throws InvalidJoinException {
        final Game game = this.gameRepository.findGameById(gameId).orElseThrow();
        final User user = this.userRepository.findUserById(userId).orElseThrow();
        game.joinGame(user, symbolType);
        this.gameRepository.save(game);
    }

    public void makeMove(String gameId, String userId, Game.GameSymbolType symbolType, int x, int y) throws InvalidMoveException {
        final Game game = this.gameRepository.findGameById(gameId).orElseThrow();
        final User user = this.userRepository.findUserById(userId).orElseThrow();
        game.makeAmove(user, symbolType, x, y);
        this.gameRepository.save(game);
    }

    public Game getGameById(String gameId) throws NullPointerException {
        return this.gameRepository.findGameById(gameId).orElseThrow();
    }
}
