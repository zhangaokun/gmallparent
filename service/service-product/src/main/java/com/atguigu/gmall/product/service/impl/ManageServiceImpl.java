package com.atguigu.gmall.product.service.impl;

import ch.qos.logback.core.joran.conditional.ElseAction;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;

    /**
     * 查询所有的一级分类信息
     *
     * @return
     */

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    /**
     * 根据一级分类id查询二级分类数据
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {

        QueryWrapper queryWrapper = new QueryWrapper<BaseCategory2>();
        queryWrapper.eq("category1_id", category1Id);

        return baseCategory2Mapper.selectList(queryWrapper);
    }

    /**
     * 根据二级id查询所有三级分类信息
     *
     * @param category2Id
     * @return
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper queryWrapper = new QueryWrapper<BaseCategory3>();
        queryWrapper.eq("category2_id", category2Id);

        return baseCategory3Mapper.selectList(queryWrapper);
    }

    /**
     * 根据分类Id 获取平台属性数据
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    /**
     * 保存平台的属性
     *
     * @param baseAttrInfo
     * @return
     */
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        baseAttrInfoMapper.insert(baseAttrInfo);
        //得到的页面所要保存的平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                // base_attr_value ：平台属性值！
                // id，valueName，attrId 页面提交过来的数据只有valueName
                // id 它是主键自增，attrId 是baseAttrInfo.getId();
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    /**
     * 查询最新的平台熟悉你集合数据
     *
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(long attrId) {

        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        // 查询到最新的平台属性值集合数据放入平台属性中！
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    /**
     * 分页查询
     *
     * @param pageParam
     * @param spuInfo
     * @return
     */
    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {

        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper();

        queryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        queryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 查询所有的销售属性数据
     *
     * @return
     */

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {

        return baseSaleAttrMapper.selectList(null);
    }

    /**
     * 保存所有属性
     *
     * @param spuInfo
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo: 商品表
        spuInfoMapper.insert(spuInfo);
        //spuSaleAttr: 销售属性表：
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                //spuSaleAttrValue: 销售属性值表：
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
        //spuImage: 商品的图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
    }

    /**
     * 根据SpuId查询spuImageList集合
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);
        return spuImageMapper.selectList(queryWrapper);
    }

    /**
     * 查询属性和属性值
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存数据
     *
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        //skuImage：库存单元图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            //循环遍历
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //skuAttrValue: 库存单元与平台属性，平台属性值的关系
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        //调用判断集合方法
        //skuInfo: 库存单元表
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        //skuSaleAttrValue: 销售属性，销售属性值
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        //商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    /**
     * 分页查询商品属性sku
     *
     * @param pageParam
     * @return
     */
    //分页查询sku
    @Override
    @Transactional
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 更改销售状态
     *
     * @param skuId
     */
    @Override
    @Transactional
    public void onSale(Long skuId) {
        //更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(1);
        skuInfoMapper.updateById(skuInfoUp);
        //商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);

    }

    /**
     * 更改销售状态
     *
     * @param skuId
     */
    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        //更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(0);
        skuInfoMapper.updateById(skuInfoUp);
        //商品下架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    /**
     * 根据skuId获取sku信息
     * 实现类
     *
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {
        // return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    //利用redisson分布式锁
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义存储商品的key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //如果缓存为空，说明走数据库
            if (null == skuInfo) {
                //利用redisson定义分布式锁
                //定义分布式锁的key
                String LockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
                RLock lock = redissonClient.getLock(LockKey);
                //准备上锁
                /*
                 * lock.lock()
                 * lock.lock(10,TimeUnit.SECONDS
                 * boolean res = lock.tryLock(100,10,TimeUnit.seconds)

                 * */
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (flag) {
                    try {
                        skuInfo = getSkuInfoDB(skuId);
                        //为了防止缓存穿透
                        if (null == skuInfo) {
                            SkuInfo skuInfo1 = new SkuInfo();
                            //因为这个对象是空的
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //将数据库中的数据放入缓存
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    Thread.sleep(1000);
                    //调用查询方法
                    return getSkuInfo(skuId);
                }
            } else {
                // 如果用户查询一个在数据库中根本不存在的数据时，那么我们存储一个空对象放入了缓存。
                // 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的！
                if (null == skuInfo.getId()) {
                    return null;
                }
                // 走缓存
                return skuInfo;

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //防止缓存宕机，可以先走数据库顶一下
        return getSkuInfoDB(skuId);
    }

    //利用redis分布式锁，查询数据
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
        /*
        1.  定义存储商品{sku} key = sku:skuId:info
        2.  去缓存中获取数据
         */
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // 获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            // 整合流程
            if (skuInfo == null) {
                // 走db ，放入缓存。注意添加锁
                // 定义分布式锁的lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                // 获取一个随机字符串
                String uuid = UUID.randomUUID().toString();
                // 为了防止缓存击穿，执行分布式锁的命令
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                // 判断是否添加锁成功
                // 获取到分布式锁！
                if (isExist) {
                    // 获取到了分布式锁，走数据库查询数据并放入缓存。
                    System.out.println("获取到分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    // 判断数据库中的数据是否为空
                    if (skuInfo == null) {
                        // 为了防止缓存穿透，赋值一个空对象放入缓存。
                        SkuInfo skuInfo1 = new SkuInfo();
                        // 放入的超时时间。 24*60*60 一天 ，最好这个空对象的过期时间不要太长。
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                        return skuInfo1;
                    }
                    // 从数据库查询出来的数据要是不为空
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    //  删除锁 定义的lua 脚本
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // 根据锁的key 找锁的值，进行删除
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                    // 返回数据
                    return skuInfo;
                } else {
                    // 未获取到分布式锁，其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 调用查询方法。
                    return getSkuInfo(skuId);
                }
            } else {
                // 如果用户查询一个在数据库中根本不存在的数据时，那么我们存储一个空对象放入了缓存。
                // 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的！
                if (null == skuInfo.getId()) {
                    return null;
                }
                // 走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            // 记录缓存宕机的日志，报警，管理员赶紧处理。
            e.printStackTrace();
        }
        // 如果缓存宕机了，那么我优先让应用程序访问数据库。
        // return skuInfo;
        return getSkuInfoDB(skuId);
    }

    //根据skuId在数据库中查询数据
    private SkuInfo getSkuInfoDB(Long skuId) {
        //查询数据库
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (skuInfo != null) {
            // 根据skuId 查询图片列表集合
            QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();

            queryWrapper.eq("sku_id", skuId);

            List<SkuImage> skuImageList = skuImageMapper.selectList(queryWrapper);

            //查出来的集合值赋给skuInfo
            skuInfo.setSkuImageList(skuImageList);


        }
        return skuInfo;
    }

    /**
     * 通过三级分类id查询分类信息
     *
     * @param category3Id
     * @return
     */
    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {

        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 获取sku价格
     *
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = "skuPrice:")
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuId != null) {
            return skuInfo.getPrice();
        }
        return new BigDecimal("0");
    }

    /**
     * 根据skuId和spuId查询销售属性值集合
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }


    /**
     * 根据spuId 查询map 集合属性
     *
     * @param spuId
     * @return
     */
//    @Override
//    //@GmallCache(prefix = "skuValueIdsMap:")
//    public Map getSkuValueIdsMap(Long spuId) {
//        HashMap<Object, Object> hashMap = new HashMap<>();
//        // 通过mapper查询数据
//        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
//        if (mapList!=null && mapList.size()>0){
//            for (Map map : mapList) {
//                // value_ids 作为key，sku_id 作为value
//                hashMap.put(map.get("value_ids"),map.get("sku_id"));
//            }
//        }
//        xxx
//        return hashMap;
//    }
    @Override
    @GmallCache(prefix = "skuValueIdsMap:")
    public Map getSaleAttrValuesBySpu(Long spuId) {
        HashMap<Object, Object> hashMap = new HashMap<>();
        // 通过mapper查询数据
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            for (Map map : mapList) {
                // value_ids 作为key，sku_id 作为value
                hashMap.put(map.get("value_ids"), map.get("sku_id"));
            }
        }
        return hashMap;
    }


    @Override
    @GmallCache(prefix = "baseCategoryList")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        /*
        1.  先获取到所有的分类数据 一级，二级，三级分类数据
        2.  开始组装数据
                组装条件就是分类Id 为主外键
        3.  将组装的数据封装到 List<JSONObject> 数据中！
         */
        // 分类数据在视图中
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 按照一级分类Id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 定义一个index;
        int index = 1;
        // 获取一级分类的数据，一级分类的Id，一级分类的名称
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            // 获取一级分类Id
            Long category1Id = entry1.getKey();
            // 放入一级分类Id
            // 声明一个对象
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            // 存储categoryName 数据
            List<BaseCategoryView> category2List = entry1.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            category1.put("categoryName",category1Name);

            // categoryChild 一会写！
            // 迭代index
            index++;
            // 获取二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 准备给二级分类数据 ，二级分类数据添加到一级分类的categoryChild中！
            List<JSONObject> category2Child = new ArrayList<>();
            // 二级分类数据可能有很多条数据
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 获取二级分类数据的Id
                Long category2Id = entry2.getKey();
                // 声明一个二级分类数据的对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                // 放入二级分类的名称
                List<BaseCategoryView> category3List = entry2.getValue();

                category2.put("categoryName",category3List.get(0).getCategory2Name());

                // 将二级分类数据添加到二级分类的集合中
                category2Child.add(category2);

                // 获取三级数据
                List<JSONObject> category3Child = new ArrayList<>();
                // 循环category3List 数据
                category3List.stream().forEach(category3View ->{
                    // 声明一个三级分类数据的对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    // 将三级分类数据添加到三级分类数据的集合
                    category3Child.add(category3);
                });

                // 二级中应该还有一个 categoryChild 添加的三级分类数据
                category2.put("categoryChild",category3Child);
            }
            // 将二级分类数据放入一级分类里面
            category1.put("categoryChild",category2Child);
            // 将所有的 category1 添加到集合中
            list.add(category1);
        }

        return list;
    }

    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {

        return baseTrademarkMapper.selectById(tmId);
    }

    /**
     * 通过skuId集合来查询数据
     * @param skuId
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //baseTrademarkMapper写错了，之前写的是品牌的
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    /**
     * 根据属性id获取属性值
     *
     * @param attrId
     * @return
     */
    private List<BaseAttrValue> getAttrValueList(Long attrId) {

        QueryWrapper queryWrapper = new QueryWrapper<BaseAttrValue>();
        queryWrapper.eq("attr_id", attrId);

        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(queryWrapper);
        return baseAttrValueList;
    }
}
