package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkuLsParams implements Serializable {
    //表示skuName的关键字
    String  keyword;
    //三级分类ID
    String catalog3Id;
    //平台属性值ID
    String[] valueId;
    //当前页
    int pageNo=1;
    //每页显示的条数
    int pageSize=20;
}
