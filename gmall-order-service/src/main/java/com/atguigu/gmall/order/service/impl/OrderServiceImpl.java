package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.conf.RedisUtil;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //设置创建时间
        orderInfo.setCreateTime(new Date());
        //设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //生成第三方支付编号
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //设置总金额
        orderInfo.sumTotalAmount();
        //设置订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //设置进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //添加
        orderInfoMapper.insertSelective(orderInfo);
        //保存订单明细数据
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setId(null);
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义一个key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //生成一个流水号
        String outTradeNo = UUID.randomUUID().toString().replace("-","");
        //保存到缓存中
        jedis.set(tradeNoKey,outTradeNo);
        //关闭
        jedis.close();
        return outTradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        //获取缓存中的数据
        Jedis jedis = redisUtil.getJedis();
        //定义一个key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String redisTradeNo = jedis.get(tradeNoKey);
        jedis.close();
        if (redisTradeNo != null && tradeCodeNo.equals(redisTradeNo)) {
            return true;
        }
        return false;
    }

    @Override
    public void deleteTradeCode(String userId) {
        //获取缓存中的数据
        Jedis jedis = redisUtil.getJedis();
        //定义一个key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //删除
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String res = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);


        return "1".equals(res);
    }
}
