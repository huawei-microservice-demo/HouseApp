package com.huawei.cse.houseapp.user.service;

import javax.inject.Inject;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.huawei.cse.houseapp.user.api.UserInfo;
import com.huawei.cse.houseapp.user.api.UserService;
import com.huawei.cse.houseapp.user.dao.UserMapper;
import com.huawei.paas.cse.tcc.annotation.TccTransaction;
import com.netflix.config.DynamicPropertyFactory;

import io.servicecomb.provider.rest.common.RestSchema;
import io.servicecomb.serviceregistry.RegistryUtils;
import io.servicecomb.swagger.invocation.exception.InvocationException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@RestSchema(schemaId = "user")
@RequestMapping(path = "/")
public class UserServiceImpl implements UserService {

    // private UserMapper userMapper = new MockedUserMapper();
    @Inject
    private UserMapper userMapper;

    //新增的登陆接口
    @Override
    @PostMapping(path = "login2")
    public long login2(@RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password) {
        // 使用测试账号登陆，登陆成功分配唯一的选房账号. 这里主要是为了并发和性能测试方便，实际业务场景需要按照要求设计。 
        try {
            UserInfo info = userMapper.getUserInfo(Long.parseLong(username.substring("user".length())));
            if (info == null) {
                return -1;
            }
            if (password.equals("test")) {
                return info.getUserId();
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    //实际是重置数据接口，不改名字了。 
    @Override
    @PostMapping(path = "login")
    public long login(@RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password) {
        // 使用测试账号登陆，登陆成功分配唯一的选房账号. 这里主要是为了并发和性能测试方便，实际业务场景需要按照要求设计。 
        if ("test".equals(username) && "test".equals(password)) {
            userMapper.clear();

            for (int i = 1; i <= 100; i++) {
                UserInfo userInfo = new UserInfo();
                userInfo.setUserId(i);
                userInfo.setUserName("user" + i);
                userInfo.setReserved(false);
                userInfo.setTotalBalance(10000000d);
                userMapper.createUser(userInfo);
            }
            return 1L;
        } else {
            return -1;
        }
    }

    @Override
    @PostMapping(path = "buyWithoutTransaction")
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean buyWithoutTransaction(@RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        UserInfo info = userMapper.getUserInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "user id not valid");
        }
        if (info.getTotalBalance() < price) {
            throw new InvocationException(400, "", "user do not got so mush money");
        }
        info.setTotalBalance(info.getTotalBalance() - price);
        userMapper.updateUserInfo(info);
        return true;
    }

    @Override
    @PostMapping(path = "buy2pc")
    @ApiResponse(code = 400, response = String.class, message = "")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean buyWithTransaction2pc(@RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        UserInfo info = userMapper.getUserInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "user id not valid");
        }
        if (info.getTotalBalance() < price) {
            throw new InvocationException(400, "", "user do not got so mush money");
        }
        info.setTotalBalance(info.getTotalBalance() - price);
        userMapper.updateUserInfo(info);
        return true;
    }

    @Override
    @TccTransaction(cancelMethod = "cancelBuy", confirmMethod = "confirmBuy")
    @PostMapping(path = "buy")
    @ApiResponse(code = 400, response = String.class, message = "")
    public boolean buyWithTransaction(@RequestParam(name = "userId") long userId,
            @RequestParam(name = "price") double price) {
        UserInfo info = userMapper.getUserInfo(userId);
        if (info == null) {
            throw new InvocationException(400, "", "user id not valid");
        }
        if (info.isReserved()) {
            throw new InvocationException(400, "", "user have already reserved a house");
        }

        if (info.getTotalBalance() < price) {
            return false;
        } else {
            info.setReserved(true);
            userMapper.updateUserInfo(info);
            return true;
        }
    }

    @ApiOperation(hidden = true, value = "")
    public void cancelBuy(long userId, double price) {
        UserInfo info = userMapper.getUserInfo(userId);
        if (info == null) {
            return;
        }
        if (info.isReserved()) {
            info.setReserved(false);
            userMapper.updateUserInfo(info);
        }
    }

    @ApiOperation(hidden = true, value = "")
    public void confirmBuy(long userId, double price) {
        UserInfo info = userMapper.getUserInfo(userId);
        if (info == null) {
            return;
        }
        if (info.isReserved()) {
            info.setReserved(false);
            info.setTotalBalance(info.getTotalBalance() - price);
            userMapper.updateUserInfo(info);
        }
    }

    @Override
    @GetMapping(path = "queryReduced")
    public double queryReduced() {
        boolean isThrow =
            DynamicPropertyFactory.getInstance().getBooleanProperty("cse.test.throwexception", false).get();
        if (isThrow) {
            throw new IllegalStateException("user controlled exception");
        }

        long sleep = DynamicPropertyFactory.getInstance().getLongProperty("cse.test.wait", -1).get();
        String myid = RegistryUtils.getMicroserviceInstance().getInstanceId();
        String instanceId = DynamicPropertyFactory.getInstance().getStringProperty("cse.test.myinstanceid", "").get();
        if (sleep > 0 && myid.equals(instanceId)) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Double reduced = userMapper.queryReduced();
        if (reduced == null) {
            reduced = 0D;
        }
        return 100 * 10000000 - reduced;
    }

    @Override
    @GetMapping(path = "getUserInfo")
    public UserInfo getUserInfo(@RequestParam("userName") String userName) {
        return userMapper.getUserInfo(Long.parseLong(userName.substring("user".length())));
    }
}
