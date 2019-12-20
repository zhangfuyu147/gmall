package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartInfo implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    String id;
    @Column
    String userId; //用户ID
    @Column
    String skuId; //商品ID
    @Column
    BigDecimal cartPrice; //加入购物车时的价格
    @Column
    Integer skuNum; //商品数量
    @Column
    String imgUrl; //图片路径
    @Column
    String skuName; //商品名称
    @Column
    String isChecked="1";

    // 实时价格
    @Transient
    BigDecimal skuPrice;
}
