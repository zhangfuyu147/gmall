package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 添加购物车数据
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void  addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 根据用户ID查询购物车信息
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userTempId);

    /**
     * 合并购物车
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId);

    /**
     * 删除未登录购物车
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 更新购物车商品的状态
     * @param skuId
     * @param userId
     * @param isChecked
     */
    void checkCart(String skuId, String userId, String isChecked);

    /**
     * 根据用户ID获取购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户ID查询最新价格
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
