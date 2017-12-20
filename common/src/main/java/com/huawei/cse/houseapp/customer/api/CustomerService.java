package com.huawei.cse.houseapp.customer.api;

import java.util.List;

import com.huawei.cse.houseapp.product.api.ProductInfo;

public interface CustomerService {
    boolean buyWithTransaction2PC(long userId, long productId, double price);

    boolean buyWithTransaction(long userId, long productId, double price);

    boolean buyWithoutTransaction(long userId, long productId, double price);

    long login(String user, String password);

    String balance();

    public List<ProductInfo> searchAllProducts();
}
