package ttt_backend.domain.ports;

import ttt_backend.domain.model.Game;

import java.util.Optional;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findGameById(String id);
}
