package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;
    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        //获取用户ID
        String userId = (String) request.getAttribute("userId");
        //获取用户的收货地址
        List<UserAddress> userAddressList = userInfoService.findUserAddressListByUserId(userId);
        //获取购物车数据
        List<CartInfo> cartList = cartService.getCartCheckedList(userId);
        //保存订单明细数据
        ArrayList<OrderDetail> detailsList = new ArrayList<>();
        if (cartList != null && cartList.size() > 0) {
            for (CartInfo cartInfo : cartList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                //将数据添加到集合中
                detailsList.add(orderDetail);
            }
        }
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailsList);
        orderInfo.sumTotalAmount();
        //保存作用域，渲染页面
        request.setAttribute("detailsList",detailsList);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        request.setAttribute("userAddressList",userAddressList);
        //保存流水号
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo) {
        //获取用户ID
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        //验证流水号
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            request.setAttribute("errMsg","不能重复提交订单");
            return "tradeFail";
        }
        //删除流水号
        orderService.deleteTradeCode(userId);
        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
                if (!result) {
                    request.setAttribute("errMsg",orderDetail.getSkuNum() + "库存不足，请重新下单");
                    return "tradeFail";
                }
                //验证价格
                SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
                if (skuInfo.getPrice().compareTo(orderDetail.getOrderPrice()) != 0) {
                    //查询最新的价格，放入缓存
                    cartService.loadCartCache(userId);
                }
            }
        }


        //保存订单
        String orderId = orderService.saveOrder(orderInfo);
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

}
