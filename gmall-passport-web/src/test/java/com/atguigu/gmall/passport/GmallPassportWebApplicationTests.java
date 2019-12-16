package com.atguigu.gmall.passport;

import com.atguigu.gmall.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void test01() {
        String key = "zhangyu";
        String salt="192.168.108.134";
        Map<String,Object> map = new HashMap<>();
        map.put("userId","101");
        map.put("nickName","张余 ");
        String token = JwtUtil.encode(key, map, salt);
        System.out.println(token);
        System.out.println("-----------------------");
        Map<String, Object> decode = JwtUtil.decode(token, key, salt);
        System.out.println(decode);
    }

}
