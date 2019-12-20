package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    public  String  saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    public String getTradeNo(String userId);

    /**
     * 验证流水号
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    public  boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 删除流水号
     * @param userId
     */
    public void deleteTradeCode(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);
}
