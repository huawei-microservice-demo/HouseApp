package com.huawei.cse.houseapp.product.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.huawei.cse.houseapp.product.api.ProductInfo;
import com.huawei.cse.houseapp.product.api.ProductService;
import com.huawei.cse.houseapp.product.dao.ProductMapper;
import com.huawei.paas.cse.tcc.annotation.TccTransaction;
import com.netflix.config.DynamicPropertyFactory;

import io.servicecomb.provider.rest.common.RestSchema;
import io.servicecomb.swagger.invocation.exception.InvocationException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@RestSchema(schemaId = "product")
@RequestMapping(path = "/")
public class ProductServiceImpl implements ProductService {
    private AtomicLong db = new AtomicLong(0);

    private static AtomicLong reqCount = new AtomicLong(0);

    private static AtomicLong lastStatTime = new AtomicLong(System.currentTimeMillis());

    static {
        new Thread(() -> {
            while (true) {
                reqCount.compareAndSet(reqCount.get(), 0L);
                lastStatTime.compareAndSet(lastStatTime.get(), System.currentTimeMillis());
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // private ProductMapper productMapper = new MockedProductMapper(); //内存测试
    @Inject
    private ProductMapper productMapper;

    @Inject
    PlatformTransactionManager txManager;

    @Override
    @GetMapping(path = "searchAll")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", dataType = "integer", format = "int32", paramType = "query")})
    public List<ProductInfo> searchAll(@RequestParam(name = "userId") int userId) {
        double qps = reqCount.incrementAndGet() * 1000.0d / (System.currentTimeMillis() - lastStatTime.get());
        int configQps = DynamicPropertyFactory.getInstance().getIntProperty("cse.test.product.qps", 10).get();
        if (qps > configQps) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long sleep = DynamicPropertyFactory.getInstance().getLongProperty("cse.test.product.wait", -1).get();
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return productMapper.getAllProducts();
    }

    @Override
    @GetMapping(path = "searchAllForCustomer")
    public List<ProductInfo> searchAllForCustomer() {
        return productMapper.getAllProducts();
    }

    // 实际是重置数据接口，不改名字了。
    @Override
    @PostMapping(path = "login")
    public long login(@RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password) {
        // 使用测试账号登陆，登陆成功分配唯一的选房账号. 这里主要是为了并发和性能测试方便，实际业务场景需要按照要求设计。
        if ("test".equals(username) && "test".equals(password)) {
            productMapper.clear();

            for (int i = 1; i <= 100; i++) {
                ProductInfo info = new ProductInfo();
                info.setId(i);
                info.setPrice(1000000);
                if (i <= 9) {
                    info.setProductName("product0" + i);
                } else {
                    info.setProductName("product" + i);
                }
                info.setReserved(false);
                info.setReservedUserId(-1);
                info.setSold(false);
                productMapper.createProduct(info);
            }
            return 1L;
        } else {
            return -1;
        }
    }

    @Override
    @PostMapping(path = "buyWithoutTransaction")
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean buyWithoutTransaction(@RequestParam(name = "productId") long productId,
            @RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        ProductInfo info = productMapper.getProductInfo(productId);
        if (info == null) {
            throw new InvocationException(400, "", "product id not valid");
        }
        if (price != info.getPrice()) {
            throw new InvocationException(400, "", "product price not valid");
        }
        if (info.isSold()) {
            throw new InvocationException(400, "", "product already sold");
        }
        info.setSold(true);
        info.setReservedUserId(userId);
        productMapper.updateProductInfo(info);
        return true;
    }

    @Override
    @PostMapping(path = "buy2pc")
    @ApiResponse(code = 400, response = String.class, message = "")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean buyWithTransaction2pc(@RequestParam(name = "productId") long productId,
            @RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        ProductInfo info = productMapper.getProductInfo(productId);
        if (info == null) {
            throw new InvocationException(400, "", "product id not valid");
        }
        if (price != info.getPrice()) {
            throw new InvocationException(400, "", "product price not valid");
        }
        if (info.isSold()) {
            throw new InvocationException(400, "", "product already sold");
        }
        info.setSold(true);
        info.setReservedUserId(userId);
        productMapper.updateProductInfo(info);
        return true;
    }

    @Override
    @TccTransaction(cancelMethod = "cancelBuy", confirmMethod = "confirmBuy")
    @PostMapping(path = "buy")
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean buyWithTransaction(@RequestParam(name = "productId") long productId,
            @RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = txManager.getTransaction(def);

        try {
            // 使用锁机制，防止多线程并发对于product的同时抢购。 getProductInfo使用了for
            // update，事务会加锁，不会并发。这里使用了spring事务。
            ProductInfo info = productMapper.getProductInfo(productId);
            if (info == null) {
                throw new InvocationException(400, "", "product id not valid");
            }
            if (price != info.getPrice()) {
                txManager.commit(status);
                throw new InvocationException(400, "", "product price not valid");
            }
            if (info.isReserved() || info.isSold()) {
                txManager.commit(status);
                return false;
            }
            info.setReserved(true);
            info.setReservedUserId(userId);
            productMapper.updateProductInfo(info);
            txManager.commit(status);
            return true;
        } catch (Exception e) {
            txManager.rollback(status);
            throw e;
        }
    }

    @ApiOperation(hidden = true, value = "")
    public void cancelBuy(long productId, long userId, double price) {
        ProductInfo info = productMapper.getProductInfo(productId);
        if (info == null) {
            return;
        }
        if (info.isReserved() && info.getReservedUserId() == userId) {
            info.setReserved(false);
            productMapper.updateProductInfo(info);
        }
    }

    @ApiOperation(hidden = true, value = "")
    public void confirmBuy(long productId, long userId, double price) {
        ProductInfo info = productMapper.getProductInfo(productId);
        if (info == null) {
            return;
        }
        if (info.isReserved()) {
            info.setReserved(false);
            info.setSold(true);
            productMapper.updateProductInfo(info);
        }
    }

    @Override
    @PostMapping(path = "add")
    public void addProduct(double price) {
        ProductInfo info = new ProductInfo();
        long i = db.incrementAndGet();
        info.setId(i);
        info.setPrice(1000000);
        info.setProductName("product" + i);
        info.setReserved(false);
        info.setReservedUserId(-1);
        info.setSold(false);
        productMapper.createProduct(info);

    }

    @Override
    @GetMapping(path = "queryReduced")
    public double queryReduced() {
        Double reduced = productMapper.queryReduced();
        if (reduced == null) {
            return 0D;
        } else {
            return reduced;
        }
    }
}
