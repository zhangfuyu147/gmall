package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;
    @Reference
    private ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        // 如何获取到用户Id
        String userId = (String) request.getAttribute("userId");
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        if (userId==null){
            // 未登录 在cookie保存一个临时的用户Id ,用户在未登录状态下不是第一次添加购物车！
            userId = CookieUtil.getCookieValue(request,"user-key",false);
            // 用户第一次添加购物车
            if (userId==null){
                userId= UUID.randomUUID().toString().replace("-","");
                // userId 放入到cookie！
                CookieUtil.setCookie(request,response,"user-key",userId,7*24*3600,false);
            }
        }
        // 添加购物车
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 获取skuInfo
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 如何获取到用户Id
        String userId = (String) request.getAttribute("userId");
        if (userId==null){
            // 走未登录 临时用户Id
            String userTempId  = CookieUtil.getCookieValue(request, "user-key", false);
            if (userTempId!=null){
                // 获取未登录购物车数据
                cartInfoList = cartService.getCartList(userTempId);
            }
        }else {
            //登录了，从缓存中获取未登录的购物车数据
            String userTempId  = CookieUtil.getCookieValue(request, "user-key", false);
            List<CartInfo> cartInfoNoLoginList = new ArrayList<>();
            if (userTempId != null) {
                //获取未登录状态的购物车数据
                cartInfoNoLoginList = cartService.getCartList(userTempId);
                if (cartInfoNoLoginList != null && cartInfoNoLoginList.size() > 0) {
                    //合并购物车
                    cartInfoList = cartService.mergeToCartList(cartInfoNoLoginList,userId);
                    //删除未登录购物车数据
                    cartService.deleteCartList(userTempId);
                }
            }
            if (userTempId == null || (cartInfoNoLoginList == null || cartInfoNoLoginList.size() == 0)) {
                //用户临时ID为空或者未登录购物车为空,从数据库中获取
                cartInfoList = cartService.getCartList(userId);
            }
        }

        // 保存到作用域
        request.setAttribute("cartInfoList",cartInfoList);

        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request) {
        //获取页面传递过来的数据
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        //获取用户ID
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            //用户未登录，获取临时用户ID
            userId = CookieUtil.getCookieValue(request,"user-key",false);
        }
        //调用方法
        cartService.checkCart(skuId,userId,isChecked);

    }

    //去结算方法
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {
        //获取用户ID
        String userId = (String) request.getAttribute("userId");

        String userTempId = CookieUtil.getCookieValue(request, "user-key", false);
        if (!StringUtils.isEmpty(userTempId)) {
            //获取未登录购物车数据
            List<CartInfo> cartInfoNoLoginList = cartService.getCartList(userTempId);
            //
            if (cartInfoNoLoginList != null && cartInfoNoLoginList.size() > 0) {
                //调用合并的方法
                cartService.mergeToCartList(cartInfoNoLoginList,userId);
                //删除未登录购物车数据
                cartService.deleteCartList(userTempId);

            }
        }
        return "redirect://trade.gmall.com/trade";
    }

}
