package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Api(tags = "品牌的增删改查")
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //分页控制器product/baseTrademark
    @ApiOperation(value = "分页控制器")
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size) {
        Page<BaseTrademark> param = new Page<>(page, size);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(param);

        return Result.ok(baseTrademarkIPage);
    }

    @ApiOperation(value = "新获取品牌")
    @GetMapping("get/{id}")
    public Result get(@PathVariable String id){
        BaseTrademark baseTrademark  = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark );
    }

    @ApiOperation(value = "新增品牌")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark banner) {

        baseTrademarkService.save(banner);
        return Result.ok();
    }

    @ApiOperation(value = "修改品牌")
    @PostMapping("update")
    public Result updateById(@RequestBody BaseTrademark banner) {
        baseTrademarkService.updateById(banner);
        return Result.ok();
    }

    @ApiOperation(value = "删除品牌")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    @ApiOperation(value = "查询全部品牌")
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
       List<BaseTrademark> baseTrademarkList  = baseTrademarkService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }
}
