package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.conf.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {
    // 调用mapper层
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
//        return baseCatalog2Mapper.selectByExample();
        // select * from basecatalog2 from where catalog1Id= ？
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2) {
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3) {
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //第一张表：sku_info
        skuInfoMapper.insertSelective(skuInfo);
        //第二张表：sku_img
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (checkListIsEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        //第三张表：sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (checkListIsEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        //第四张表：sku_SaleAttr_Value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (checkListIsEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

//        return getSkuInfoRedisSet(skuId);
        return getSkuInfoRedisson(skuId);
    }

    public SkuInfo getSkuInfoRedisson(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        Jedis jedis = null;

        try {
            // 获取jedis
            jedis = redisUtil.getJedis();
            //定义key
            String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            //获取缓存的数据
            String skuJson = jedis.get(skuInfoKey);
            //判断
            if (skuJson == null) {
                //没有数据，从数据库中查询，并放入缓存中
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.108.134:6379");
                //获取redisson
                RedissonClient redissonClient = Redisson.create(config);
                RLock lock = redissonClient.getLock("my-lock");
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);

                if (res) {
                    try {
                        //查询数据库数据
                        skuInfo = getSkuInfoDB(skuId);
                        //将获取的数据存放入缓存中,并且设置过期时间
                        jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                    } finally {
                        lock.unlock();
                    }
                }
            }else {
                //缓存中有数据，直接获取返回
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoRedisSet(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        Jedis jedis = null;

        try {
            // 获取jedis
            jedis = redisUtil.getJedis();
            //定义key
            String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            //获取缓存的数据
            String skuJson = jedis.get(skuInfoKey);
            //判断
            if (skuJson == null) {
                //没有数据，从数据库中查询，并上锁
                //1、定义锁
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                //2、定义锁的值
                String token = UUID.randomUUID().toString().replace("-","");
                //3、上锁
                String lockKey = jedis.set(skuLockKey, token, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);

                if ("ok".equals(lockKey)) {
                    //上锁成功,去查询数据库数据
                    skuInfo = getSkuInfoDB(skuId);
                    //将获取的数据存放入缓存中,并且设置过期时间
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                    //解除锁（使用lua脚本）
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));

                    return skuInfo;
                }else {
                    //表示上锁操作失败
                    Thread.sleep(1000);
                    return getSkuInfoRedisSet(skuId);
                }
            }else {
                //缓存中有数据，直接获取返回
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        //根据skuID获取skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //根据skuID查询到skuImage集合
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        //将图片集合添加入skuInfo中
        skuInfo.setSkuImageList(skuImageList);
        //添加平台属性值集合
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

       return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());

    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        List<SkuSaleAttrValue> skuSaleAttrValueList  = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);

        return skuSaleAttrValueList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);

        return baseAttrInfoList;
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo) {
        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        // 修改！ baseAttrInfo
        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else {
            // 保存！
            // baseAttrInfo 代表页面传递过来的数据！
            // 分别插入到两张表 baseAttrInfo ，baseAttrValue
            // attrName , catalog3Id
            // System.out.println("插入之前："+baseAttrInfo.getId());
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        System.out.println("插入之后："+baseAttrInfo.getId());
        //  baseAttrValue  修改 {先删除原有数据，在新增所有的数据！}
        // delete * from baseAttrValue where attrId = ? baseAttrInfo.getId()
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);
        System.out.println("删除数据");

        // baseAttrValue |  接收baseAttrValue 的集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 保存数据  valueName , attrId  = baseAttrInfo.getId();
                baseAttrValue.setAttrId(baseAttrInfo.getId()); // baseAttrInfo.getId();主键
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getAtrrInfo(String attrId) {
        // select * from baseAttrInfo where id = attrId;
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        // 赋值
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        return null;
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        // spuInfo 表示从前台页面传递过来的数据
//        spuInfo
        spuInfoMapper.insertSelective(spuInfo);
//        spuImage'
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
//        spuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //        spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
//                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
//                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
//                        spuSaleAttrValue.setSpuId(spuInfo.getId());
//                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
//                    }
//                }
                if(checkListIsEmpty(spuSaleAttrValueList)){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }
    //根据spuId获取spuImage中的所有图片列表

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    // 判断集合是否为空！
    // 泛型方法
//    public <T>  boolean checkListIsEmpty(ArrayList<T> list){
//        if (list!=null && list.size()>0){
//            return true;
//        }
//        return false;
//    }
    public <T> boolean checkListIsEmpty(List<T> list){
        if (list!=null && list.size()>0){
            return true;
        }
        return false;
    }
}
