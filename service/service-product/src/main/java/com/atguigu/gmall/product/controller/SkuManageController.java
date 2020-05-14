package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product")
@Api(tags = "商品sku接口")
public class SkuManageController {
    @Autowired
    private ManageService manageService;

    /**
     * 根据spuId 查询销售属性集合
     * @param spuId
     * @return
     */
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId) {
        List<SpuImage> getSpuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(getSpuImageList);
    }
    /**
     * 查询属性和属性值
     * @param spuId
     * @return
     */
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {

        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);

        return Result.ok(spuSaleAttrList);
    }

    @ApiOperation(value = "保存sku")
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 调用服务层
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }
}
