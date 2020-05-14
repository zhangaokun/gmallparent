package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品基础属性接口")
@RequestMapping("admin/product")
@RestController
//@CrossOrigin
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 查询所有的一级分类信息
     *
     * @return
     */

    @ApiOperation(value = "查询所有的一级分类信息")
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    /**
     * 根据一级分类id查询二级分类数据
     *
     * @param category1Id
     * @return
     */
    @ApiOperation(value = "查询所有的二级分类信息")
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(
            @PathVariable("category1Id") Long category1Id
    ) {
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    /**
     * 根据二级id查询所有三级分类信息
     *
     * @param category2Id
     * @return
     */
    @ApiOperation(value = "查询所有的三级分类信息")
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable("category2Id") Long category2Id) {
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    /**
     * 根据分类Id 获取平台属性数据
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @ApiOperation(value = "查询所有的三级分类信息")
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(
            @PathVariable("category1Id") Long category1Id,
            @PathVariable("category2Id") Long category2Id,
            @PathVariable("category3Id") Long category3Id
    ) {
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(baseAttrInfoList);
    }

    /**
     * 保存平台的属性
     *
     * @param baseAttrInfo
     * @return
     */
    @ApiOperation(value = "保存平台属性")
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        // 前台数据都被封装到该对象中baseAttrInfo
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    @ApiOperation(value = "修改平台属性")
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable long attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(baseAttrValueList);
    }

    @ApiOperation(value = "分页查询")
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size,
                        SpuInfo spuInfo) {
        Page<SpuInfo> pageParam = new Page<>(page, size);
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);

        return Result.ok(spuInfoIPage);
    }

    //http://api.gmall.com/admin/product/list/1/10
    @ApiOperation(value = "sku分页查询")
    @GetMapping("list/{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit) {
        Page<SkuInfo> pageParam = new Page<>(page, limit);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(pageParam);
        return Result.ok(skuInfoIPage);
    }

    @ApiOperation(value = "商品上架")
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }
    @ApiOperation(value = "商品下架")
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
