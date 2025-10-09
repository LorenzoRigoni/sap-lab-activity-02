package ttt_backend.infrastucture.databases;

import io.vertx.core.json.JsonArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class JsonRepository {

    protected JsonArray getJsonContent(Path path) throws IOException {
        if (!Files.exists(path))
            return new JsonArray();
        try {
            final String content = Files.readString(path);
            return content.isEmpty() ? new JsonArray() : new JsonArray(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void saveOnJsonFile(Path path, JsonArray array) throws IOException {
        Files.writeString(path, array.encodePrettily(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}
