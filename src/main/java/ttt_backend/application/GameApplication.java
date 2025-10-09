package ttt_backend.application;

import ttt_backend.domain.models.*;
import ttt_backend.domain.ports.GameRepository;
import ttt_backend.domain.ports.UserRepository;

/**
 * This class represents the application of the architecture.
 */
public class GameApplication {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private int userId;
    private int gameId;

    public GameApplication(UserRepository userRepository, GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userId = 1;
        this.gameId = 1;
    }

    /**
     * Save the user in the db.
     *
     * @param username the username of the user
     * @return the registered user
     */
    public User registerUser(String username) {
        return this.userRepository.save(new User("user-" + this.userId++, username));
    }

    /**
     * Create a new game.
     *
     * @return the new game created
     */
    public Game createNewGame() {
        return this.gameRepository.save(new Game("game-" + this.gameId++));
    }

    /**
     * Start the game.
     *
     * @param gameId the ID of the game to start
     * @throws CannotStartGameException if the game cannot be started
     */
    public void startGame(String gameId) throws CannotStartGameException {
        final Game game = this.gameRepository.getGameById(gameId).orElseThrow();
        if (game.bothPlayersJoined())
            game.start();
    }

    /**
     * Let user join to a game.
     *
     * @param userId the user ID
     * @param gameId the game ID
     * @param symbol the symbol of the user
     * @throws InvalidJoinException if the user cannot join the game
     */
    public void joinGame(String userId, String gameId, Game.GameSymbolType symbol) throws InvalidJoinException {
        final User user = this.userRepository.getUserById(userId).orElseThrow();
        final Game game = this.gameRepository.getGameById(gameId).orElseThrow();
        game.joinGame(user, symbol);
        this.gameRepository.save(game);
    }

    /**
     * Let user make a move.
     *
     * @param userId the user ID
     * @param gameId the game ID
     * @param symbol the symbol of the user
     * @param x the row of the symbol
     * @param y the column of the symbol
     * @return the new state of the game
     * @throws InvalidMoveException if the move is not valid
     */
    public Game makeMove(String userId, String gameId, Game.GameSymbolType symbol, int x, int y) throws InvalidMoveException {
        final User user = this.userRepository.getUserById(userId).orElseThrow();
        final Game game = this.gameRepository.getGameById(gameId).orElseThrow();
        game.makeAmove(user, symbol, x, y);
        return this.gameRepository.save(game);
    }
}
