package ttt_backend.adapters.in;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;
import ttt_backend.application.GameService;
import ttt_backend.domain.model.Game;
import ttt_backend.domain.model.User;

import java.io.*;

/**
 * Event-loop based backend controller, based on Vert.x
 *
 * @author aricci
 */
public class TTTBackendController extends VerticleBase {

    private final int port;
    private final GameService gameService;
    static Logger logger = Logger.getLogger("[TicTacToe Backend]");
    static final String TTT_CHANNEL = "ttt-events";

    public TTTBackendController(int port, GameService gameService) {
        this.port = port;
        this.gameService = gameService;
        logger.setLevel(Level.INFO);
    }

    public Future<?> start() {
        logger.log(Level.INFO, "TTT Server initializing...");
        HttpServer server = vertx.createHttpServer();

        /* API routes */

        final Router router = Router.router(vertx);
        router.route(HttpMethod.POST, "/api/registerUser").handler(this::registerUser);
        router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame);
        router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame);
        router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove);
        this.handleEventSubscription(server, "/api/events");

        /* static files */

        router.route("/public/*").handler(StaticHandler.create());

        /* start the server */

        var fut = server
                .requestHandler(router)
                .listen(port);

        fut.onSuccess(res -> {
            logger.log(Level.INFO, "TTT Game Server ready - port: " + port);
        });

        return fut;
    }



    /* List of handlers mapping the API */

    /**
     * Register a new user
     *
     * @param context
     */
    protected void registerUser(RoutingContext context) {
        logger.log(Level.INFO, "RegisterUser request");
        context.request().handler(buf -> {

            /* add the new user */
            final JsonObject userInfo = buf.toJsonObject();
            final var userName = userInfo.getString("userName");
            final User user = this.gameService.registerUser(userName);

            final var reply = new JsonObject();
            reply.put("userId", user.id());
            reply.put("userName", userName);
            try {
                sendReply(context.response(), reply);
            } catch (Exception ex) {
                sendError(context.response());
            }
        });
    }

    /**
     * Create a New Game
     *
     * @param context
     */
    protected void createNewGame(RoutingContext context) {
        logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
        final Game game = this.gameService.createGame();

        final var reply = new JsonObject();
        reply.put("gameId", game.getId());
        try {
            sendReply(context.response(), reply);
        } catch (Exception ex) {
            sendError(context.response());
        }
    }

    /**
     * Join a Game
     *
     * @param context
     */
    protected void joinGame(RoutingContext context) {
        logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
        context.request().handler(buf -> {
            final JsonObject joinInfo = buf.toJsonObject();
            final String userId = joinInfo.getString("userId");
            final String gameId = joinInfo.getString("gameId");
            final String symbol = joinInfo.getString("symbol");
            final var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;

            final var reply = new JsonObject();
            try {
                this.gameService.joinGame(userId, gameId, gameSym);
                reply.put("result", "accepted");
                try {
                    sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join succeeded");
                } catch (Exception ex) {
                    sendError(context.response());
                }
            } catch (Exception ex) {
                reply.put("result", "denied");
                try {
                    sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join failed");
                } catch (Exception ex2) {
                    sendError(context.response());
                }
            }
        });
    }

    /**
     * Make a move in a game
     *
     * @param context
     */
    protected void makeAMove(RoutingContext context) {
        logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
        context.request().handler(buf -> {
            final var reply = new JsonObject();
            final JsonObject moveInfo = buf.toJsonObject();
            logger.log(Level.INFO, "move info: " + moveInfo);

            final String userId = moveInfo.getString("userId");
            final String gameId = moveInfo.getString("gameId");
            final String symbol = moveInfo.getString("symbol");
            final int x = Integer.parseInt(moveInfo.getString("x"));
            final int y = Integer.parseInt(moveInfo.getString("y"));

            final var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;
            try {
                this.gameService.makeMove(userId, gameId, gameSym, x, y);
                reply.put("result", "accepted");
                try {
                    sendReply(context.response(), reply);
                } catch (Exception ex) {
                    sendError(context.response());
                }

                /* notifying events */

                final var eb = vertx.eventBus();

                /* about the new move */

                final var evMove = new JsonObject();
                evMove.put("event", "new-move");
                evMove.put("x", x);
                evMove.put("y", y);
                evMove.put("symbol", symbol);

                /* the event is notified on the 'address' of specific game */

                final var gameAddress = TTT_CHANNEL + "-" + gameId;
                eb.publish(gameAddress, evMove);

                /* if the game ended, we need to notify an event */

                final var game = this.gameService.getGameById(gameId);
                if (game.isGameEnd()) {
                    final var evEnd = new JsonObject();
                    evEnd.put("event", "game-ended");
                    if (game.isTie()) {
                        evEnd.put("result", "tie");
                    } else if (game.getWinner().isPresent()) {
                        final var sym = game.getWinner().get();
                        if (sym.equals(Game.GameSymbolType.CROSS)) {
                            evEnd.put("winner", "cross");
                        } else {
                            evEnd.put("winner", "circle");
                        }
                    }
                    eb.publish(TTT_CHANNEL + "-" + gameId, evEnd);
                }

            } catch (Exception ex) {
                reply.put("result", "invalid-move");
                try {
                    sendReply(context.response(), reply);
                } catch (Exception ex2) {
                    sendError(context.response());
                }
            }
        });
    }


    /*
     * Handling frontend subscription to receive events for a specific game,
     * using websockets
     *
     */
    protected void handleEventSubscription(HttpServer server, String path) {
        server.webSocketHandler(webSocket -> {
            logger.log(Level.INFO, "New TTT subscription accepted.");

            /*
             * receiving a first message including the id of the game
             * to observe
             */
            webSocket.textMessageHandler(openMsg -> {
                logger.log(Level.INFO, "For game: " + openMsg);
                final JsonObject obj = new JsonObject(openMsg);
                final String gameId = obj.getString("gameId");

                /*
                 * subscribing events on the event bus to receive
                 * events concerning the game, to be notified
                 * then to the frontend, using the websocket
                 */
                EventBus eb = vertx.eventBus();

                final var gameAddress = TTT_CHANNEL + "-" + gameId;

                eb.consumer(gameAddress, msg -> {
                    final JsonObject ev = (JsonObject) msg.body();
                    logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
                    webSocket.writeTextMessage(ev.encodePrettily());
                });
            });
        });
    }
    /* Aux methods */

    private void sendReply(HttpServerResponse response, JsonObject reply) {
        response.putHeader("content-type", "application/json");
        response.end(reply.toString());
    }

    private void sendBadRequest(HttpServerResponse response, JsonObject reply) {
        response.setStatusCode(400);
        response.putHeader("content-type", "application/json");
        response.end(reply.toString());
    }

    private void sendError(HttpServerResponse response) {
        response.setStatusCode(500);
        response.putHeader("content-type", "application/json");
        response.end();
    }

}
