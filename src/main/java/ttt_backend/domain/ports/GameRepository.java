package ttt_backend.domain.ports;

import ttt_backend.domain.models.Game;

/**
 * This interface represents the port for the games' repository.
 */
public interface GameRepository {

    /**
     * Save the game in the repository.
     *
     * @param game the game to save
     */
    void save(Game game);

    /**
     * Get game by the id.
     *
     * @param id the ID of the game
     * @return the game
     */
    Game getGameById(String id);
}
