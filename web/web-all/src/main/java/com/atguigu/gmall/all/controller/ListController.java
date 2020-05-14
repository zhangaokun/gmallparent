package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表接口
 * </p>
 */
@Controller
@RequestMapping
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;

    /**
     * 列表搜索
     *
     * @param searchParam
     * @return
     */
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model) {
        //将数据保存，在index.html渲染
        //数据从何而来service-list
        Result<Map> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());

        //拼接url,网址栏的数据
        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("urlParam", urlParam);

        //获取品牌传递的参数
        String trademarkParam = makeTrademark(searchParam.getTrademark());
        model.addAttribute("trademarkParam", trademarkParam);

        //获取平台属性条件回显
        List<Map<String, String>> propsParamList = makeProps(searchParam.getProps());
        model.addAttribute("propsParamList", propsParamList);

        //处理排序规则
        Map<String, Object> orderMap = dealOrder(searchParam.getOrder());
        model.addAttribute("orderMap", orderMap);
        return "list/index";
    }

    /**
     * 处理品牌条件回显问题
     *
     * @param trademark
     * @return
     */
    private String makeTrademark(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = StringUtils.split(trademark, ":");
            if (split != null && split.length == 2) {
                return "品牌" + split[1];
            }
        }
        return "";
    }

    /**
     * 处理平台属性条件回显
     *
     * @param props
     * @return
     */
    private List<Map<String, String>> makeProps(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        // 2:v:n
        if (props != null && props.length != 0) {
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if (split != null && split.length == 3) {
                    //声明一个map
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    //制作返回的url
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断关键字
        if (searchParam.getKeyword() != null) {
            urlParam.append("keyWord=").append(searchParam.getKeyword());
        }
        //判断一级分类
        if (searchParam.getCategory1Id() != null) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //判断二级分类
        if (searchParam.getCategory2Id() != null) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //判断三级分类
        if (searchParam.getCategory3Id() != null) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //判断品牌
        if (searchParam.getTrademark() != null) {
            if (urlParam.length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //判断平台属性值
        if (searchParam.getProps() != null) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?" + urlParam.toString();
    }

    //设置排序规则
    private Map<String, Object> dealOrder(String order) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isNotEmpty(order)) {
            //order：asc
            String[] split = order.split(":");
            //符合格式
            if (split != null && split.length == 2) {
                map.put("type", split[0]);
                map.put("sort", split[1]);
            }
        } else {
            map.put("type", "1");
            map.put("sort", "asc");
        }
        return map;
    }

}
