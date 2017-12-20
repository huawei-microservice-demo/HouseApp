package com.huawei.cse.houseapp.edge;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import io.servicecomb.edge.core.EdgeInvocation;
import io.vertx.core.Future;

public class EdgeCommand extends HystrixCommand<EdgeInvocation> {

    private EdgeInvocation invocation;
    private Future<Void> future;

    protected EdgeCommand(EdgeInvocation invocation, Future<Void> future) {
        super(HystrixCommandGroupKey.Factory.asKey("Provider.vkapp.edge-service.all"));
        this.invocation = invocation;
        this.future = future;
    }

    @Override
    protected EdgeInvocation run() throws Exception {
        invocation.edgeInvoke();
        while (true) {
            if (future.isComplete()) {
                break;
            } else {
                Thread.sleep(2);
            }
        }
        return invocation;
    }

}
