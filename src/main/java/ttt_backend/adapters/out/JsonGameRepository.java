package ttt_backend.adapters.out;

import ttt_backend.domain.model.Game;
import ttt_backend.domain.ports.GameRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonGameRepository implements GameRepository {
    private final Map<String, Game> games = new HashMap<>();

    @Override
    public Game save(Game game) {
        this.games.put(game.getId(), game);
        return game;
    }

    @Override
    public Optional<Game> findGameById(String id) {
        return Optional.ofNullable(this.games.get(id));
    }
}
