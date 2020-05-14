package com.atguigu.gmall.list.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    // mysql 中的数据，应该通过feign 远程调用来的。
    private ProductFeignClient productFeignClient;
    @Autowired
    // 引入一个操作 elasticSearch 的类。
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 上架
     *
     * @param skuId
     */
    @Override
    public void upperGoods(Long skuId) {
        // 上架 mysql -- > es
        // 将实体类Goods 中的数据放入es 中。
        Goods goods = new Goods();
        // 给goods 赋值。
        // 通过productFeignClient 先查询到skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        // 直接赋值
        goods.setId(skuInfo.getId());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
//        通过远程调用来查找商品的价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId); // 直接查询的价格
//        goods.setPrice(skuPrice.doubleValue());
        goods.setCreateTime(new Date());
//        查询品牌数据 可以通过skuInfo 中的数据来得到品牌的Id
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        if (null != trademark) {
            // goods.setTmId(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        }
//     获取分类数据 可以通过skuInfo 中的数据来得到三级分类Id
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (null != categoryView) {
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }
//          给平台属性赋值
//        通过远程调用serivce-product 中的查询方法获取平台属性，平台属性值数据。
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        if (null != attrList && attrList.size() > 0) {
            // 循环获取里面的数据
            // 将每个销售属性存储起来。
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                // 赋值平台属性对象
                SearchAttr searchAttr = new SearchAttr();
                // 存储平台属性的Id
                searchAttr.setAttrId(baseAttrInfo.getId());
                // elasticSearch 中需要存储的平台属性名。
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                // 存储平台属性值名称。
                // 先获取到平台属性值的集合数据
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                // 获取平台属性值的名称
                String valueName = attrValueList.get(0).getValueName();
                searchAttr.setAttrValue(valueName);
                // 将每一个销售属性，销售属性值返回去。
                return searchAttr;
            }).collect(Collectors.toList());
            // 保存数据
            goods.setAttrs(searchAttrList);
        }
        // 保存：
        goodsRepository.save(goods);
    }

    /**
     * 下架
     *
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {
        //下架的本质就是删除es中的数据
        goodsRepository.deleteById(skuId);
    }

    /**
     * 更新热点
     *
     * @param skuId
     */
    @Override
    public void incrHotScore(Long skuId) {
        //定义key
        String hotKey = "hotScore";
        //保存数据
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId" + skuId, 1);
        if (hotScore % 10 == 0) {
            //更新es
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);

        }

    }

    /**
     * 根据用户输入的条件查询数据
     *
     * @param searchParam
     * @return
     * @throws IOException
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //基本思路
        /*
        1，先制作dsl语句
        2，执行dsl语句
        3，获取执行的结果
        * */
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //引入操作es的客户端
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取执行之后的数据，在这个方法中可以将总记录书放入total中
        SearchResponseVo responseVo = parseSearchResult(response);
        //设置分页相关的数据
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setPageNo(searchParam.getPageNo());
        //设置总条数可以从es中获取hits.total 所以此处省略
        // responseVo.setTotal();
        // 设置总页数 10 3 4 | 9 3 3
        // 传统的 totalPages=(total%pageSize==0?total/pageSize:total/pageSize+1);
        // 新的公式，公司开发有很多都使用这个公式。
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }


    //制作返回的结果集
    private SearchResponseVo parseSearchResult(SearchResponse response) {
//        private List<SearchResponseTmVo> trademarkList;
//        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
//        private List<Goods> goodsList = new ArrayList<>();
//        private Long total;//总记录数
//        private Integer pageSize;//每页显示的内容
//        private Integer pageNo;//当前页面
//        private Long totalPages;
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //品牌数据通过聚合得到的
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        //获取品牌的id Aggregation接口中并没有获取到桶的方法，所以在这进行转化
        //parsedLongTerms是他的实现
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //从桶中获取数据
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            //获取品牌的id
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            searchResponseTmVo.setTmId(Long.parseLong(((Terms.Bucket) bucket).getKeyAsString()));
            //获取品牌的名称
            Map<String, Aggregation> tmIdSubAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            // tmNameAgg 品牌名称的agg 品牌数据类型是String
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmNameAgg");
            // 获取到品牌的名称并赋值
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            // 获取品牌的logo
            ParsedStringTerms tmlogoUrlAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmLogoUrlAgg");
            String tmlogoUrl = tmlogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmlogoUrl);
            // 返回品牌
            return searchResponseTmVo;
        }).collect(Collectors.toList());

        //获取品牌数据
        searchResponseVo.setTrademarkList(trademarkList);
        //获取平台属性数据，应该也是从聚合中获取
        //attrAge 数据类型是nested ，转化一下
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //获取attrIdAgg 平台属性id数据
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        //判断桶的集合不能为空
        if (null != buckets && buckets.size() > 0) {
            //循环遍历数据
            List<SearchResponseAttrVo> attrsList = buckets.stream().map(bucket -> {
                // 获取平台属性对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                // 获取attrNameAgg 中的数据 名称数据类型是String
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                // 赋值平台属性的名称
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //赋值平台属性值集合，获取attrValueAgg
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<String> valueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                //返回平台属性对象
                searchResponseAttrVo.setAttrValueList(valueList);
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrsList(attrsList);
        }
        // 获取商品数据 goodsList
        // 声明一个存储商品的集合
        List<Goods> goodsList = new ArrayList<>();
        //品牌数据需要从查询结果中获取
        SearchHits hits = response.getHits();
        SearchHit[] subHits = hits.getHits();
        if (null != subHits && subHits.length > 0) {
            // 循环遍历数据
            for (SearchHit subHit : subHits) {
                // 获取商品的json 字符串
                String goodsJson = subHit.getSourceAsString();
                // 直接将json 字符串变成Goods.class
                Goods goods = JSONObject.parseObject(goodsJson, Goods.class);
                // 获取商品的时候，如果按照商品名称查询时，商品的名称显示的时候，应该高亮。但是，现在这个名称不是高亮
                // 从高亮中获取商品名称
                if (subHit.getHighlightFields().get("title") != null) {
                    // 说明当前用户查询是按照全文检索的方式查询的。
                    // 将高亮的商品名称赋值给goods
                    // [0] 因为高亮的时候，title 对应的只有一个值。
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                // 添加商品到集合
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);
        //总记录数
        searchResponseVo.setTotal(hits.totalHits);
        return searchResponseVo;
    }

    //自动生成dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //查询器：相当于es里的 {}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //声明一个QueryBuilder 对象 query ：bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断查询关键字
        if (StringUtil.isNotEmpty(searchParam.getKeyword())) {
            //创建QueryBuilder对象
            //MatchAllQueryBuilder matchAllQueryBuilder = new MatchAllQueryBuilder("title",searchParam.getKeyword());
            //BoolQueryBuilder must = boolQueryBuilder.must(matchAllQueryBuilder);
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }
        //设置品牌：trademark= 2 ：华为 2 =tmId 华为= tName
        String trademark = searchParam.getTrademark();
        if (StringUtil.isNotEmpty(trademark)) {
            //不为空说明用户按照品牌查询
            String[] split = StringUtils.split(trademark, ":");
            //select * from baseTrademark id = ？
            if (null != split && split.length == 2) {
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);
                boolQueryBuilder.filter(tmId);
            }
        }

        //terms，term
        //terms：表示范围取值 select * from where id in(1,2,4)
        //terms：表示精确取值 select * from where id = ?
        //设置分类Id 过滤 通过一级分类id 二级分类id 三级分类id
        if (null != searchParam.getCategory1Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (null != searchParam.getCategory2Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (null != searchParam.getCategory3Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        //平台属性
        //props=23:4G:运行内存
        //平台属性Id 平台属性值名称 平台属性名
        //nested 将平台属性，属性值作为独立的数据查询
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                //split判断分割之后的格式 是否正确
                if (null != split && split.length == 3) {
                    //构建查询语句
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //匹配查询
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    //将subBoolQuery 放入boolQuery
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                    //将boolQuery 放入总的查询器
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //执行query方法
        searchSourceBuilder.query(boolQueryBuilder);
        //构建分页
        //开始条数
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        //排序 1：hotScore 2：price
        String order = searchParam.getOrder();
        if (StringUtils.isNotBlank(order)) {
            //进行分割数据
            String[] split = StringUtils.split(order, ":");
            //判断1：hotScore |3|price
            if (null != split && split.length == 2) {
                //设置排序规则
                //定义一个排序字段
                String field = null;
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                // 默认走根据热度进行降序排列。
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }
        //设置高亮
        //声明一个高亮的对象，然后进行高亮规则
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//商品的名称高亮
        highlightBuilder.preTags("<span style=color:red>"); //设置标签属性
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //设置聚合
        //聚合品牌
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")//品牌id
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))//品牌名称
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //将聚合规则的添加到查询器
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        //平台属性
        //设置nested 聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")//平台属性id
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))//平台属性名称
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));//平台属性值
        //设置有效的数据 ，查询的时候哪些字段需要显示
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);

        //GET /goods/info/_search
        //设置索引库index ，type
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        //打印dsl语句
        String query = searchSourceBuilder.toString();
        System.out.println("dsl" + query);
        return searchRequest;
    }
}
