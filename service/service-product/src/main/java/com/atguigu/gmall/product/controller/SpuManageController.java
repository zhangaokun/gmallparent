package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product")
@Api(tags = "销售属性")
public class SpuManageController {
    // 引入服务层
    @Autowired
    private ManageService manageService;

    @ApiOperation("查询所有销售属性")
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        // 查询所有的销售属性集合
        List<BaseSaleAttr> baseSaleAttrList= manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    @ApiOperation("保存sku数据")
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){

        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

}
