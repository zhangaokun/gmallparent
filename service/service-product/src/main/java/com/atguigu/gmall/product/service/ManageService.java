package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    /**
     * 查询所有的一级信息
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类id 查询三级数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     *查询一级二级三级
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    BaseAttrInfo getAttrInfo(long attrId);

    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam,SpuInfo spuInfo );

    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存所有信息
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId查询spuImageList集合
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 查询属性和属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    //保存sku数据
    void saveSkuInfo(SkuInfo skuInfo);

    //sku分页查询
    IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam);

    /**
     * 商品上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据skuId获取sku信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 获取sku价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     *根据skuId和spuId查询销售属性值集合
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId 查询map 集合属性
     * @param spuId
     * @return
     */
    //Map getSkuValueIdsMap(Long spuId);

    Map getSaleAttrValuesBySpu(Long spuId);

    /**
     * 获取全部分类信息
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     *通过skuId集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);

}


