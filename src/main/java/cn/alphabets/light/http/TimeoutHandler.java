package cn.alphabets.light.http;

import cn.alphabets.light.config.ConfigManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

/**
 * TimeoutHandlerImpl
 * Created by luohao on 2016/10/28.
 */

public interface TimeoutHandler extends Handler<RoutingContext> {

    static TimeoutHandler create(Long timeout) {
        return new TimeoutHandlerImpl(timeout);
    }

    class TimeoutHandlerImpl implements TimeoutHandler {

        private static final Logger log = LoggerFactory.getLogger(TimeoutHandlerImpl.class);

        private long timeout;
        private Pattern ignore;
        HttpResponseStatus errorStatus = SERVICE_UNAVAILABLE;

        TimeoutHandlerImpl(long timeout) {

            this.timeout = timeout;
            if (timeout <= 0) {
                throw new IllegalArgumentException("time out can not be less than 0");
            }

            List<String> paths = ConfigManager.INSTANCE.getIgnoreTimeout();
            if (CollectionUtils.isNotEmpty(paths)) {
                String regex = StringUtils.join(paths, "|");
                ignore = Pattern.compile(regex.replace("*", ".*"));
            }
        }

        @Override
        public void handle(RoutingContext ctx) {

            String path = ctx.request().path();
            if (ignore != null && !ignore.matcher(path).matches()) {

                // We send a error response after timeout
                long tid = ctx.vertx().setTimer(timeout, t -> {
                    log.error("Request timeout : " + path);
                    ctx.response().setStatusCode(errorStatus.code()).end(errorStatus.reasonPhrase());
                });
                ctx.addBodyEndHandler(v -> ctx.vertx().cancelTimer(tid));
            }

            ctx.next();
        }
    }
}

