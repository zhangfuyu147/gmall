package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.conf.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;


import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24*7;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findUserInfo(UserInfo userInfo) {
        return null;
    }

    @Override
    public List<UserInfo> findByNickName(String nickName) {
        return null;
    }

    @Override
    public void addUser(UserInfo userInfo) {

    }

    @Override
    public void updUser(UserInfo userInfo) {

    }

    @Override
    public void delUser(UserInfo userInfo) {

    }

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {

        Example example = new Example(UserAddress.class);
        example.createCriteria().andEqualTo("userId",userId);

        return userAddressMapper.selectByExample(example);
    }

    @Override
    public List<UserAddress> findUserAddressListByUserId(UserAddress userAddress) {
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //将密码进行MD5加密
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info != null) {
            //获取jedis
            Jedis jedis = redisUtil.getJedis();
            //定义key
            String userkey = userKey_prefix+info.getId()+userinfoKey_suffix;
            //保存
            jedis.setex(userkey,userKey_timeOut, JSON.toJSONString(info));
            //关闭并返回
            jedis.close();
            return info;
        }

        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String key = userKey_prefix+userId+userinfoKey_suffix;
        //获取缓存中的数据
        String userJson = jedis.get(key);
        if (!StringUtils.isEmpty(userJson)) {
            //将json串转换成对象
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            //返回数据
            return userInfo;
        }
        jedis.close();
        return null;
    }
}
