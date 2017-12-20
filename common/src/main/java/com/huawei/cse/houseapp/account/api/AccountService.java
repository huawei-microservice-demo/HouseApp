package com.huawei.cse.houseapp.account.api;

public interface AccountService {
    boolean payWithTransaction(long userid, double amount);

    boolean payWithTransaction2pc(long userid, double amount);

    boolean payWithoutTransaction(long userid, double amount);

    long login(String userName, String password);
    
    double queryReduced();
}
