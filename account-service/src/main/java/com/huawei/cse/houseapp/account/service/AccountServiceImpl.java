package com.huawei.cse.houseapp.account.service;

import javax.inject.Inject;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.huawei.cse.houseapp.account.api.AccountService;
import com.huawei.cse.houseapp.account.dao.AccountInfo;
import com.huawei.cse.houseapp.account.dao.AccountMapper;
import com.huawei.paas.cse.tcc.annotation.TccTransaction;

import io.servicecomb.provider.pojo.RpcSchema;
import io.servicecomb.swagger.invocation.exception.InvocationException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@RpcSchema(schemaId = "account")
public class AccountServiceImpl implements AccountService {
    //内存测试
    //private AccountMapper accountMapper = new MockedAccountMapper();
    @Inject
    private AccountMapper accountMapper;

    @Override
    public long login(String username,
            String password) {
        // 使用测试账号登陆，登陆成功分配唯一的选房账号. 这里主要是为了并发和性能测试方便，实际业务场景需要按照要求设计。 
        if ("test".equals(username) && "test".equals(password)) {
            accountMapper.clear();

            for (int i = 1; i <= 100; i++) {
                AccountInfo info = new AccountInfo();
                info.setUserId(i);
                info.setReserved(false);
                info.setTotalBalance(8000000);
                accountMapper.createAccountInfo(info);
            }
            return 1L;
        } else {
            return -1;
        }
    }

    @Override
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean payWithoutTransaction(long userId, double amount) {
        AccountInfo info = accountMapper.getAccountInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "account id not valid");
        }
        if (info.getTotalBalance() < amount) {
            throw new InvocationException(400, "", "account do not have enouph money");
        }
        info.setTotalBalance(info.getTotalBalance() - amount);
        accountMapper.updateAccountInfo(info);
        return true;
    }

    
    @Override
    @ApiResponse(code = 400, response = String.class, message = "")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean payWithTransaction2pc(long userId, double amount) {
        AccountInfo info = accountMapper.getAccountInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "account id not valid");
        }
        if (info.getTotalBalance() < amount) {
            throw new InvocationException(400, "", "account do not have enouph money");
        }
        info.setTotalBalance(info.getTotalBalance() - amount);
        accountMapper.updateAccountInfo(info);
        return true;
    }

    @Override
    @TccTransaction(cancelMethod = "cancelPay", confirmMethod = "confirmPay")
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean payWithTransaction(long userId, double amount) {
        AccountInfo info = accountMapper.getAccountInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "account id not valid");
        }
        if (info.isReserved()) {
            throw new InvocationException(400, "", "account is already in transaction");
        }
        if (info.getTotalBalance() < amount) {
            throw new InvocationException(400, "", "account do not have enouph money");
        }
        info.setReserved(true);
        accountMapper.updateAccountInfo(info);
        return true;
    }

    @ApiOperation(hidden = true, value = "")
    public void cancelPay(long userId, double amount) {
        AccountInfo info = accountMapper.getAccountInfo(userId);
        if (info == null) {
            return;
        }
        if (info.isReserved()) {
            info.setReserved(false);
            accountMapper.updateAccountInfo(info);
        }
    }

    @ApiOperation(hidden = true, value = "")
    public void confirmPay(long userId, double amount) {
        AccountInfo info = accountMapper.getAccountInfo(userId);
        if (info == null) {
            return;
        }
        if (info.isReserved()) {
            info.setReserved(false);
            info.setTotalBalance(info.getTotalBalance() - amount);
            accountMapper.updateAccountInfo(info);
        }
    }

    @Override
    public double queryReduced() {
        Double reduced = accountMapper.queryReduced();
        if (reduced == null) {
            reduced = 0D;
        }
        return 100 * 8000000 - reduced;
    }
}
