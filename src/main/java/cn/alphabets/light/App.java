package cn.alphabets.light;

import cn.alphabets.light.cache.CacheManager;
import cn.alphabets.light.config.ConfigManager;
import cn.alphabets.light.http.AuthHandler;
import cn.alphabets.light.http.Dispatcher;
import cn.alphabets.light.http.TimeoutHandler;
import cn.alphabets.light.http.session.MongoSessionStoreImpl;
import cn.alphabets.light.http.session.SessionHandlerImpl;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * App
 * Created by luohao on 16/10/20.
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    private Vertx vertx;
    private HttpServer server;
    private Router router;

    public App() {

        // 运行环境
        vertx = Vertx.vertx(new VertxOptions());

        // 路由
        router = Router.router(vertx);

        // web服务
        server = vertx.createHttpServer();
    }

    public void start() {

        Environment env = Environment.instance();
        CacheManager.INSTANCE.setUp(env.getAppName());
        ConfigManager.INSTANCE.setUp();

        // set timeout
        router.route().handler(TimeoutHandler.create(ConfigManager.INSTANCE.getAppTimeout() * 1000));

        // Print each request
        router.route().handler(LoggerHandler.create(LoggerFormat.SHORT));

        // Handle static resources
        router.route("/static/*").handler(StaticHandler.create().setWebRoot("static"));

        // Handle cookie
        router.route().handler(CookieHandler.create());

        //session处理, 超时30天
        long sessionTimeoute = 1000L * 60 * 60 * ConfigManager.INSTANCE.getAppSessionTimeout();
        router.route().handler(SessionHandlerImpl
                .create(new MongoSessionStoreImpl(env.getAppName(), vertx))
                .setNagHttps(false)
                .setSessionTimeout(sessionTimeoute));

        // Handle body
        router.route().handler(BodyHandler.create());

        // Handle login
        if (!env.app.isDev()) {
            router.route().handler(AuthHandler.create());
        }

        // The 'x-response-time' on the headband is used to indicate the server response time
        if (env.app.isDev()) {
            router.route().handler(ResponseTimeHandler.create());
        }

        // Route the Web request
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.routeProcessAPI(router);
        dispatcher.routeDataAPI(router);
        dispatcher.routeView(router);

        // Start the web server
        server.requestHandler(router::accept).listen(env.getAppPort());
        logger.info(String.format("Running on http://%s:%s/", "127.0.0.1", env.getAppPort()));
    }
}
