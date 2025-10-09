package ttt_backend;

import io.vertx.core.Vertx;
import ttt_backend.application.GameApplication;
import ttt_backend.infrastucture.apis.GameAPI;
import ttt_backend.infrastucture.databases.JsonGameRepository;
import ttt_backend.infrastucture.databases.JsonUserRepository;

public class TTTBackend {
    private static final int BACKEND_PORT = 8080;

    /**
     * Main method to launch the backend.
     *
     * @param args
     */
    public static void main(String[] args) {
        final var vertx = Vertx.vertx();
        final var application = new GameApplication(new JsonUserRepository(), new JsonGameRepository());
        vertx.deployVerticle(new GameAPI(application, BACKEND_PORT));
    }
}
