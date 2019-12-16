package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    @ResponseBody
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //保存商品数据
        request.setAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());

        // 获取到平台属性值Id 集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        // 调用方法将Id 集合传入
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        //  保存用户查询的条件
        String urlParam = makeUrlParam(skuLsParams);
        //声明一个面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        //当点击平台属性值ID时，平台属性消失
        if (attrList != null && attrList.size() > 0) {
            for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo =  iterator.next();
                // 得到平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                // 循环遍历
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                    for (String valueId : skuLsParams.getValueId()) {
                        for (BaseAttrValue baseAttrValue : attrValueList) {
                            //判一下是否相等
                            if (valueId.equals(baseAttrValue.getId())) {
                                //移除当前数据
                                iterator.remove();
                                //声明一个平台属性值对象
                                BaseAttrValue baseAttrValuenew = new BaseAttrValue();
                                //组成面包屑： 平台属性名称：平台属性值名称
                                baseAttrValuenew.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                                //制作新的urlParam
                                String newUrlParam = makeUrlParam(skuLsParams,valueId);
                                //将最新的参数付给当前变量
                                baseAttrValuenew.setValueName(newUrlParam);
                                baseAttrValueArrayList.add(baseAttrValuenew);
                            }
                        }
                        
                    }
                }
                
            }
        }
        //设置分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        //设置面包屑显示
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        //保存关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());
        request.setAttribute("urlParam",urlParam);
        //将数据保存到作用域中
        request.setAttribute("baseAttrInfoList",attrList);


        return "list";
    }

    /**
     * 制作参数
     * @param skuLsParam :: 表示用户url 中输入的查询参数条件
     * @param excludeValueIds :: 表示用户点击面包屑时传递过来的平台属性值Id
     * @return
     */
    public String makeUrlParam(SkuLsParams skuLsParam,String... excludeValueIds) {
        String urlParam = "";
        //使用关键字进行全文搜索
        if (skuLsParam.getKeyword() != null && skuLsParam.getKeyword().length() > 0) {
            urlParam += "keyword=" + skuLsParam.getKeyword();
        }
        //使用三级分类进行搜索
        if (skuLsParam.getCatalog3Id() != null && skuLsParam.getCatalog3Id().length() > 0) {
            urlParam += "catalog3Id=" + skuLsParam.getCatalog3Id();
        }
        //构造属性参数
        if (skuLsParam.getValueId() != null && skuLsParam.getValueId().length > 0) {
            for (String valueId : skuLsParam.getValueId()) {
                if (excludeValueIds != null && excludeValueIds.length > 0){
                    String excludeValueId = excludeValueIds[0];
                    if (valueId.equals(excludeValueId)) {
                        //停止
                        continue;
                    }
                }
                urlParam += "&valueId=" + valueId;
            }
        }
        return urlParam;
    }

}
