package com.huawei.cse.houseapp.customer.service;

import java.util.List;

import org.apache.servicecomb.provider.pojo.RpcReference;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.huawei.cse.houseapp.account.api.AccountService;
import com.huawei.cse.houseapp.customer.api.CustomerService;
import com.huawei.cse.houseapp.product.api.ProductInfo;
import com.huawei.cse.houseapp.product.api.ProductService;
import com.huawei.cse.houseapp.user.api.UserService;
import com.huawei.paas.cse.tcc.annotation.TccTransaction;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@RestSchema(schemaId = "customer")
@RequestMapping(path = "/")
public class CustomerServiceImpl implements CustomerService {
    @RpcReference(microserviceName = "user-service", schemaId = "user")
    private UserService userService;

    @RpcReference(microserviceName = "product-service", schemaId = "product")
    private ProductService productService;

    @RpcReference(microserviceName = "account-service", schemaId = "account")
    private AccountService accountService;

    @Override
    @TccTransaction(cancelMethod = "cancelBuy", confirmMethod = "confirmBuy")
    @PostMapping(path = "buy")
    @ApiResponse(code = 400, response = String.class, message = "buy failed")
    public boolean buyWithTransaction(@RequestHeader(name = "userId") long userId,
            @RequestParam(name = "productId") long productId, @RequestParam(name = "price") double price) {
        if (!userService.buyWithTransaction(userId, price)) {
            throw new InvocationException(400, "user do not got so much money", "user do not got so much money");
        }
        if (!productService.buyWithTransaction(productId, userId, price)) {
            throw new InvocationException(400, "product already sold", "product already sold");
        }
        if (!accountService.payWithTransaction(userId, price)) {
            throw new InvocationException(400, "pay failed", "pay failed");
        }
        return true;
    }

    @Override
    @PostMapping(path = "buyWithoutTransaction")
    @ApiResponse(code = 400, response = String.class, message = "buy failed")
    public boolean buyWithoutTransaction(@RequestHeader(name = "userId") long userId,
            @RequestParam(name = "productId") long productId, @RequestParam(name = "price") double price) {
        // product will lock, put it in front
        if (!productService.buyWithoutTransaction(productId, userId, price)) {
            throw new InvocationException(400, "product already sold", "product already sold");
        }
        if (!userService.buyWithoutTransaction(userId, price)) {
            throw new InvocationException(400, "user do not got so much money", "user do not got so much money");
        }
        if (!accountService.payWithoutTransaction(userId, price)) {
            throw new InvocationException(400, "pay failed", "pay failed");
        }
        return true;
    }

    @Override
    @PostMapping(path = "buy2pc")
    @ApiResponse(code = 400, response = String.class, message = "buy failed")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean buyWithTransaction2PC(@RequestHeader(name = "userId") long userId,
            @RequestParam(name = "productId") long productId, @RequestParam(name = "price") double price) {
        // product will lock, put it in front
        if (!productService.buyWithTransaction2pc(productId, userId, price)) {
            throw new InvocationException(400, "product already sold", "product already sold");
        }
        if (!userService.buyWithTransaction2pc(userId, price)) {
            throw new InvocationException(400, "user do not got so much money", "user do not got so much money");
        }
        if (!accountService.payWithTransaction2pc(userId, price)) {
            throw new InvocationException(400, "pay failed", "pay failed");
        }
        return true;
    }

    @ApiOperation(hidden = true, value = "")
    public void cancelBuy(long userId, long productId, double price) {
        //不做事情。生产代码可以记录审计日志。
    }

    @ApiOperation(hidden = true, value = "")
    public void confirmBuy(long userId, long productId, double price) {
        //不做事情。生产代码可以记录审计日志。 
    }

    //实际是重置数据接口，不改名字了。 
    @Override
    @PostMapping(path = "login")
    public long login(@RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password) {
        productService.login(username, password);
        accountService.login(username, password);
        return userService.login(username, password);
    }

    @Override
    @GetMapping(path = "searchAllProducts")
    public List<ProductInfo> searchAllProducts() {
        return productService.searchAllForCustomer();
    }

    @Override
    @GetMapping(path = "balance")
    public String balance() {
        double user = userService.queryReduced();
        double acct = accountService.queryReduced();
        double prod = productService.queryReduced();
        return String.format("user:%s;acct:%s;prod:%s", user, acct, prod);
    }
}
