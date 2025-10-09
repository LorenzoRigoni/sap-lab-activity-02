package ttt_backend.infrastucture.databases;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ttt_backend.domain.models.User;
import ttt_backend.domain.ports.UserRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class JsonUserRepository extends JsonRepository implements UserRepository {
    private static final Path FILE_PATH = Paths.get("users.json");

    @Override
    public User save(User user) {
        try {
            final JsonArray users = getJsonContent(FILE_PATH);

            final Optional<JsonObject> existingUser = users.stream()
                    .map(obj -> (JsonObject) obj)
                    .filter(u -> u.getString("id").equals(user.id()))
                    .findFirst();

            if (existingUser.isPresent()) {
                existingUser.get().put("username", user.name());
            } else {
                users.add(JsonObject.mapFrom(user));
            }

            saveOnJsonFile(FILE_PATH, users);

            return user;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> getUserById(String id) {
        try {
            final JsonArray users = getJsonContent(FILE_PATH);

            return users.stream()
                    .map(obj -> ((JsonObject) obj).mapTo(User.class))
                    .filter(u -> u.id().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
