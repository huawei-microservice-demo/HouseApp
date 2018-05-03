package com.huawei.cse.houseapp.edge;

import org.apache.servicecomb.edge.core.AbstractEdgeDispatcher;
import org.apache.servicecomb.edge.core.EdgeInvocation;
import org.apache.servicecomb.swagger.invocation.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;

public class DefaultDispatcher extends AbstractEdgeDispatcher {
    private static Logger LOGGER = LoggerFactory.getLogger(LoginDispatcher.class);

    private static final String LOGIN_URI = "/api/user-service/login2";

    private static final String USER_SERVICE = "user-service";

    private static final String LOGIN_PATH = "/login2";

    @Override
    public int getOrder() {
        return 10001;
    }

    @Override
    public void init(Router router) {
        router.routeWithRegex("/").handler(CookieHandler.create());
        router.routeWithRegex("/").handler(createBodyHandler());
        router.routeWithRegex("/").failureHandler(this::onFailure).handler(this::onRequest);
    }

    protected void onRequest(RoutingContext context) {
        context.response().setStatusCode(302);
        context.response().putHeader("Location", "/ui/customer-website/login.html");
        context.response().end();
    }
}
