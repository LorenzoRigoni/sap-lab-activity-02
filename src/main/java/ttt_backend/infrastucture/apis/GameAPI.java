package ttt_backend.infrastucture.apis;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import ttt_backend.application.GameApplication;
import ttt_backend.domain.models.Game;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GameAPI extends VerticleBase {
    private static final Logger logger = Logger.getLogger("[TicTacToe Backend]");
    private final GameApplication application;
    private final int port;

    public GameAPI(GameApplication application, int port) {
        this.application = application;
        this.port = port;
        logger.setLevel(Level.INFO);
    }


    @Override
    public Future<?> start() {
        logger.log(Level.INFO, "TTT Server initializing...");

        final HttpServer server = vertx.createHttpServer();

        final Router router = Router.router(vertx);
        router.route(HttpMethod.POST, "/api/registerUser").handler(this::registerUser);
        router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame);
        router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame);
        router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove);

        handleEventSubscription(server);

        router.route("/public/*").handler(StaticHandler.create());

        final var future = server
                .requestHandler(router)
                .listen(port);

        future.onSuccess(res -> logger.log(Level.INFO, "TTT Server started on port " + port));

        return future;
    }

    protected void registerUser(RoutingContext context) {
        logger.log(Level.INFO, "RegisterUser request");

        context.request().handler(buffer -> {
           final JsonObject userInfo = buffer.toJsonObject();
           final var userName = userInfo.getString("userName");
           final var user = this.application.registerUser(userName);

           final var reply = new JsonObject();
           reply.put("userId", user.id());
           reply.put("userName", userName);
           try {
               sendReply(context.response(), reply);
           } catch (Exception e) {
               sendError(context.response());
           }
        });
    }

    protected void createNewGame(RoutingContext context) {
        logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());

        final var game = this.application.createNewGame();

        final var reply = new JsonObject();
        reply.put("gameId", game.getId());
        try {
            sendReply(context.response(), reply);
        } catch (Exception e) {
            sendError(context.response());
        }
    }

    protected void joinGame(RoutingContext context) {
        logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());

        context.request().handler(buffer -> {
            final JsonObject joinInfo = buffer.toJsonObject();
            final var userId = joinInfo.getString("userId");
            final var gameId = joinInfo.getString("gameId");
            final var symbol = joinInfo.getString("symbol");
            final var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;

            final var reply = new JsonObject();
            try {
                this.application.joinGame(userId, gameId, gameSym);
                reply.put("result", "accepted");
                try {
                    sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join succeeded");
                } catch (Exception e) {
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

    protected void makeAMove(RoutingContext context) {
        logger.log(Level.INFO, "makeAMove request - " + context.currentRoute().getPath());

        context.request().handler(buffer -> {
            final JsonObject moveInfo = buffer.toJsonObject();
            final var userId = moveInfo.getString("userId");
            final var gameId = moveInfo.getString("gameId");
            final var symbol = moveInfo.getString("symbol");
            final int x = Integer.parseInt(moveInfo.getString("x"));
            final int y = Integer.parseInt(moveInfo.getString("y"));
            final var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;
            final var reply = new JsonObject();

            try {
                final var updatedGame = this.application.makeMove(userId, gameId, gameSym, x, y);
                reply.put("result", "accepted");
                try {
                    sendReply(context.response(), reply);
                    logger.log(Level.INFO, "Join succeeded");
                } catch (Exception e) {
                    sendError(context.response());
                }

                final var eb = vertx.eventBus();

                final var evMove = new JsonObject();
                evMove.put("event", "new-move");
                evMove.put("x", x);
                evMove.put("y", y);
                evMove.put("symbol", symbol);

                final var gameAddress = getBusAddressForAGame(gameId);
                eb.publish(gameAddress, evMove);

                if (updatedGame.isGameEnd()) {
                    final var evEnd = new JsonObject();
                    evEnd.put("event", "game-ended");

                    if (updatedGame.isTie()) {
                        evEnd.put("result", "tie");
                    } else {
                        final var sym = updatedGame.getWinner().get();
                        if (sym.equals(Game.GameSymbolType.CROSS)) {
                            evEnd.put("winner", "cross");
                        } else {
                            evEnd.put("winner", "circle");
                        }
                    }
                    eb.publish(gameAddress, evEnd);
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

    /*
     *
     * Handling frontend subscriptions to receive events
     * when joining a game, using websockets
     *
     */
    protected void handleEventSubscription(HttpServer server) {
        server.webSocketHandler(webSocket -> {
            logger.log(Level.INFO, "New TTT subscription accepted.");

            /*
             *
             * Receiving a first message including the id of the game
             * to observe
             *
             */
            webSocket.textMessageHandler(openMsg -> {
                logger.log(Level.INFO, "For game: " + openMsg);
                JsonObject obj = new JsonObject(openMsg);
                String gameId = obj.getString("gameId");

                /*
                 * Subscribing events on the event bus to receive
                 * events concerning the game, to be notified
                 * to the frontend using the websocket
                 *
                 */
                EventBus eb = vertx.eventBus();

                var gameAddress = getBusAddressForAGame(gameId);
                eb.consumer(gameAddress, msg -> {
                    JsonObject ev = (JsonObject) msg.body();
                    logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
                    webSocket.writeTextMessage(ev.encodePrettily());
                });

                /*
                 *
                 * When both players joined the game and both
                 * have the websocket connection ready,
                 * the game can start
                 *
                 */
                try {
                    this.application.startGame(gameId);
                    final var evGameStarted = new JsonObject();
                    evGameStarted.put("event", "game-started");
                    eb.publish(gameAddress, evGameStarted);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    /**
     *
     * Get the address on the Vert.x event bus
     * to handle events related to a specific game
     *
     * @param gameId
     * @return
     */
    private String getBusAddressForAGame(String gameId) {
        return "ttt-events-" + gameId;
    }

    private void sendReply(HttpServerResponse response, JsonObject reply) {
        response.putHeader("content-type", "application/json");
        response.end(reply.toString());
    }

    private void sendError(HttpServerResponse response) {
        response.setStatusCode(500);
        response.putHeader("content-type", "application/json");
        response.end();
    }
}
