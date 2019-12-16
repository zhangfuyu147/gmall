package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserInfoService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    private String key;
    @RequestMapping("index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    //登录
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo) {
        //调用服务层方法得到信息
        UserInfo info = userInfoService.login(userInfo);
        //判断信息
        if (info !=null) {
            //获取保存用户信息
            HashMap<String,Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            //获取IP地址
            String salt = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(key,map,salt);
            return token;

        }
        //登录失败
        return "fail";
    }

    //用户认证
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        //获取token、sale
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //调用JWT工具类，进行解密
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        //判断
        if (map != null && map.size() > 0) {
            //获取用户ID
            String userId = (String) map.get("userId");
            //调用服务层方法进行验证
            UserInfo userInfo = userInfoService.verify(userId);
            //返回
            if (userInfo != null) {
                return "success";
            }
        }
        //认证失败
        return "fail";
    }

}
