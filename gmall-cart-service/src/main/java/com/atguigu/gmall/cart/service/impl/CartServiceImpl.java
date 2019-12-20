package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.conf.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {

        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //判断一下缓存中是否有数据
        if (!jedis.exists(cartKey)) {
            //缓存中没有数据,从数据库中获取
            loadCartCache(userId);
        }
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOneByExample(example);
        if (cartInfoExist != null) {
            // 购物车中有数据
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);

            // 初始化实时价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            // 更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

            // 更新缓存
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        }else {
            //第一次添加购物车
            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setSkuId(skuId);

            cartInfoMapper.insertSelective(cartInfo1);
            // 放入缓存！
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo1));
            cartInfoExist = cartInfo1;
        }
        // 放入缓存
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        setCartExpireTime(userId, jedis, cartKey);

        // 关闭：
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //获取缓存中的所有数据
        List<String> stringList = jedis.hvals(cartKey);
        if (stringList != null && stringList.size() > 0) {
            for (String cartJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //集合排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            //从数据库中查询数据，并放入缓存
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    //合并购物车
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        //获取登录状态的购物车数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            //判断是否与未登录购物车的数据相同
            for (CartInfo cartInfo : cartInfoNoLoginList) {
                //声明一个变量表示有相同的商品
                boolean isMatch = false;
                //进行合并
                for (CartInfo info : cartInfoList) {
                    if (cartInfo.getSkuId().equals(info.getSkuId())) {
                        //商品ID相同，则商品数量相加
                        info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                        //更新数据
                        cartInfoMapper.updateByPrimaryKeySelective(info);
                        //改变状态
                        isMatch = true;
                    }
                }
                //没有相同的商品数据,直接插入新的数据
                if (!isMatch) {
                    cartInfo.setId(null);
                    cartInfo.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfo);
                }
            }
        }else {
            //购物车中没有数据,将未登录购物车中的数据添加进去
            for (CartInfo cartInfo : cartInfoNoLoginList) {
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }
        }
        //返回合并后的购物车数据
        List<CartInfo> infoList = loadCartCache(userId);
        return infoList;
    }

    //删除未登录购物车
    @Override
    public void deleteCartList(String userTempId) {
        //删除数据库和缓存中的相关数据
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userTempId);
        //删除数据库数据
        cartInfoMapper.deleteByExample(example);

        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userTempId + CartConst.USER_CART_KEY_SUFFIX;
        //删除缓存中的数据
        jedis.del(cartKey);
        //关闭
        jedis.close();
    }

    //更新购物车商品的状态
    @Override
    public void checkCart(String skuId, String userId, String isChecked) {
        //先修改数据库
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);

        //在删除缓存
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //删除缓存中的数据
        jedis.del(cartKey,skuId);

        //将数据库中的数据添加到缓存中
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            CartInfo cartInfoQuery = cartInfoList.get(0);
            //进行实时价格初始化
            cartInfoQuery.setSkuPrice(cartInfoQuery.getSkuPrice());
            //转化json串存放入缓存中
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoQuery));
        }
        //关闭
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //从缓存中获取数据
        List<String> cartList = jedis.hvals(cartKey);
        if (cartList != null && cartList.size() > 0) {
            for (String cartJson : cartList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                if ("1".equals(cartInfo.getIsChecked())) {
                    cartInfoList.add(cartInfo);
                }
            }
        }
        jedis.close();
        return cartInfoList;
    }

    //设置过期时间
    private void setCartExpireTime(String userId, Jedis jedis, String cartKey) {
        // 设置过期时间：与用户的过期时间一致！
        // 用key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;

        // 获取过期时间
        Long ttl = jedis.ttl(userKey);

        if (!jedis.exists(userKey)){
            jedis.expire(cartKey,30*24*3600);
        }else {
            jedis.expire(cartKey,ttl.intValue());
        }
    }

    // 根据用户Id 查询数据库
    @Override
    public List<CartInfo> loadCartCache(String userId) {
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //查询数据库
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList == null || cartInfoList.size() == 0) {
            return null;
        }
        HashMap<String,String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //以map的方式一次添加多个
            map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }
        jedis.hmset(cartKey,map);

        jedis.close();
        return cartInfoList;

    }
}
