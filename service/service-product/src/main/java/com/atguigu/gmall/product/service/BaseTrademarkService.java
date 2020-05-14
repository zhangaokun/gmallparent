package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 分页查询
     * @param pageParam
     * @return
     */
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    /**
     * 查询所有品牌
     * @return
     */
    List<BaseTrademark> getTrademarkList();
}
