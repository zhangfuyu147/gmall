package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController // @ResponseBody +@Controller
@CrossOrigin
public class MangeController {

    // 调用服务层
    @Reference
    private ManageService manageService;

    // 获取一级分类数据
    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){

        return manageService.getCatalog1();
    }
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2 (String catalog1Id, HttpServletRequest request, BaseCatalog2 baseCatalog2){
        String catalog1Id1 = request.getParameter("catalog1Id");
//        System.out.println("baseCatalog2:"+baseCatalog2);
//        System.out.println(catalog1Id1);
        return manageService .getCatalog2(baseCatalog2);
    }

    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3){
        return manageService.getCatalog3(baseCatalog3);
    }

    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(BaseAttrInfo baseAttrInfo, String catalog3Id){
        return manageService.getAttrInfoList(catalog3Id);
    }

    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }

    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo baseAttrInfo =  manageService.getAtrrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }


}
