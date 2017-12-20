/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.cse.houseapp.edge;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.servicecomb.edge.core.AbstractEdgeDispatcher;
import io.servicecomb.edge.core.EdgeInvocation;
import io.vertx.core.Future;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import rx.Observer;

public class ApiDispatcher extends AbstractEdgeDispatcher {
    private static Logger LOGGER = LoggerFactory.getLogger(ApiDispatcher.class);

    @Override
    public int getOrder() {
        return 10002;
    }

    @Override
    public void init(Router router) {
        String regex = "/api/([^\\/]+)/(.*)";
        router.routeWithRegex(regex).handler(CookieHandler.create());
        router.routeWithRegex(regex).handler(createBodyHandler());
        router.routeWithRegex(regex).failureHandler(this::onFailure).handler(this::onRequest);
    }

    protected void onRequest(RoutingContext context) {
        Map<String, String> pathParams = context.pathParams();
        String microserviceName = pathParams.get("param0");
        String path = "/" + pathParams.get("param1");

        // 检查会话是否合法, for DEMO, just check if session-id exists
        Cookie sessionId = context.getCookie("session-id");
        String userName = context.request().getHeader("userId");
        if (sessionId == null && !isUserNameCorrect(userName)) {
            LOGGER.info("unauthenticated user, send to login");
            context.response().setStatusCode(302);
            context.response().putHeader("Location", "/ui/customer-website/login.html");
            context.response().end();
            return;
        }

        if (sessionId != null && !isUserNameCorrect(userName)) {
            context.request().headers().add("userId", sessionId.getValue());
        }
        Future<Void> future = Future.future();
        EdgeInvocation invoker = new EdgeInvocation() {
            public void edgeInvoke() {
                try {
                    super.edgeInvoke();
                } finally {
                    future.complete();
                }
            }
        };
        invoker.init(microserviceName, context, path, httpServerFilters);
        rx.Observable<EdgeInvocation> fs = new EdgeCommand(invoker, future).observe();
        fs.subscribe(new Observer<EdgeInvocation>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("", e);
                context.response().setStatusCode(500);
                context.response().end();
            }

            @Override
            public void onNext(EdgeInvocation t) {
            }
        });
    }

    private boolean isUserNameCorrect(String userName) {
        if (userName == null) {
            return false;
        }

        long u;
        try {
            u = Long.parseLong(userName);
        } catch (NumberFormatException e) {
            return false;
        }
        if (u < 0) {
            return false;
        } else {
            return true;
        }
    }
}
