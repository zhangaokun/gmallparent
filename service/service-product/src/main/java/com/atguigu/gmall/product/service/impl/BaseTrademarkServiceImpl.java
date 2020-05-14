package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper,BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /**
     * 分页查询
     * @param pageParam
     * @return
     */
    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam) {

        QueryWrapper<BaseTrademark> queryWrapper = new QueryWrapper<>();

        queryWrapper.orderByDesc("id");

        IPage<BaseTrademark> pate = baseTrademarkMapper.selectPage(pageParam,queryWrapper);
        return pate;
    }

    /**
     * 查询所有品牌
     * @return
     */
    @Override
    public List<BaseTrademark> getTrademarkList() {

        return baseTrademarkMapper.selectList(null);
    }
}
