package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        return null;
    }
}
