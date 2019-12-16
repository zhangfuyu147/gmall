package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;


    @RequestMapping("{skuId}.html")
    public String getSkuInfo(@PathVariable String skuId, HttpServletRequest request) {
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        //通过spuId ，skuId 查询销售属性集合
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("saleAttrList",saleAttrList);
        //销售属性值之间切换
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
                if (key.length() > 0) {
                    key += "|";
                }
                key += skuSaleAttrValue.getSaleAttrValueId();
                if ((i+1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())) {
                    map.put(key,skuSaleAttrValue.getSkuId());
                    key = "";
                }
            }
        }
        //将map变成json字符串
        String valuesSkuJson = JSON.toJSONString(map);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        //更新es
        listService.incrHotScore(skuId);

        return "item";
    }
}
