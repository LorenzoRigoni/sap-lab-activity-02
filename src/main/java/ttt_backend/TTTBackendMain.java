package ttt_backend;

import io.vertx.core.Vertx;
import ttt_backend.adapters.in.TTTBackendController;
import ttt_backend.adapters.out.JsonGameRepository;
import ttt_backend.adapters.out.JsonUserRepository;
import ttt_backend.application.GameService;
import ttt_backend.domain.ports.GameRepository;
import ttt_backend.domain.ports.UserRepository;

/**
 * TicTacToe Game Server backend - without a clear software architecture
 *
 * @author aricci
 */

public class TTTBackendMain {

    static final int BACKEND_PORT = 8080;

    public static void main(String[] args) {
        final UserRepository userRepository = new JsonUserRepository();
        final GameRepository gameRepository = new JsonGameRepository();
        final GameService gameService = new GameService(gameRepository, userRepository);

        var vertx = Vertx.vertx();
        var server = new TTTBackendController(BACKEND_PORT, gameService);
        vertx.deployVerticle(server);
    }
}
