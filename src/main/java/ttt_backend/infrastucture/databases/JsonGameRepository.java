package ttt_backend.infrastucture.databases;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ttt_backend.domain.models.Game;
import ttt_backend.domain.ports.GameRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class JsonGameRepository extends JsonRepository implements GameRepository {
    private static final Path FILE_PATH = Paths.get("games.json");

    @Override
    public Game save(Game game) {
        try {
            final JsonArray games = getJsonContent(FILE_PATH);

            final Optional<JsonObject> existingGame = games.stream()
                    .map(obj -> (JsonObject) obj)
                    .filter(g -> g.getString("id").equals(game.getId()))
                    .findFirst();

            existingGame.ifPresent(games::remove);

            games.add(JsonObject.mapFrom(game));

            saveOnJsonFile(FILE_PATH, games);

            return game;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Game> getGameById(String id) {
        try {
            final JsonArray games = getJsonContent(FILE_PATH);

            return games.stream()
                    .map(obj -> ((JsonObject) obj).mapTo(Game.class))
                    .filter(g -> g.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
