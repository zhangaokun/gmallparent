package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper,UserAddress> implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    //根据用户id 查询用户的收货地址列表
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        //操作哪个数据库表，则就使用那个表对应的mapper
        //new Example() 你操作的那个表，则对应的传入表的实体类
        //select * from userAddress where userId = ?
        QueryWrapper<UserAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);

        List<UserAddress> userAddressList = userAddressMapper.selectList(queryWrapper);

        return userAddressList;
    }
}
