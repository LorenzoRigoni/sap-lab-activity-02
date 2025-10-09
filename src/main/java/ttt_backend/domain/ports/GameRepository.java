package ttt_backend.domain.ports;

import ttt_backend.domain.models.Game;

import java.util.Optional;

/**
 * This interface represents the port for the games' repository.
 */
public interface GameRepository {

    /**
     * Save the game in the repository.
     *
     * @param game the game to save
     * @return the game saved
     */
    Game save(Game game);

    /**
     * Get game by the id.
     *
     * @param id the ID of the game
     * @return the game
     */
    Optional<Game> getGameById(String id);
}
