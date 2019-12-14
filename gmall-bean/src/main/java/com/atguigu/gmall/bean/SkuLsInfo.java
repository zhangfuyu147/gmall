package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {
    //skuID
    String id;
    //价格
    BigDecimal price;
    //名称
    String skuName;
    //分类ID
    String catalog3Id;
    //默认图片
    String skuDefaultImg;
    //热度排名
    Long hotScore=0L;
    //平台属性值ID集合
    List<SkuLsAttrValue> skuAttrValueList;
}
