package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 拦截器
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 进入控制器之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws java.lang.Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, java.lang.Object handler) throws java.lang.Exception {
        System.out.println("进入拦截器...");
        //获取token,并将token存放入cookie中
        String token = request.getParameter("newToken");
        //判断,如果有token。放入cookie中
        if (token != null) {
            //将token存放入cookie中
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //判断，如果没有token，在cookie中获取token
        if (token == null) {
            //在cookie中取出token
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        //判断，如果有token，获取用户名称
        if (token != null) {
            //从token中获取用户名称
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");

            request.setAttribute("nickName",nickName);
        }
        return true;
    }
    //从token中获取用户信息
    private Map getUserMapByToken(String token) {
        //截取token
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        //创建base64对象
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        //获取解密的字符串
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        //将字节数组转换为字符串
        String userJson = new String(bytes);
        //将字符串转化为map
        Map map = JSON.parseObject(userJson, Map.class);
        return map;
    }

    /**
     * 进入控制器之后，返回视图之前
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws java.lang.Exception
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, java.lang.Object handler, ModelAndView modelAndView) throws java.lang.Exception {

    }

    /**
     * 视图渲染之后
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws java.lang.Exception
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, java.lang.Object handler, java.lang.Exception ex) throws java.lang.Exception {

    }

}
