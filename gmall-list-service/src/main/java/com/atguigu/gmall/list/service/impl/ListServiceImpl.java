package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.conf.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    //保存数据
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
//        1.  dsl 语句
        String query = makeQueryStringForSearch(skuLsParams);
        Search search =  new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            //获取返回结果
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        2.  定义执行动作
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
//        3.  执行并获取返回结果集
        return skuLsResult;
    }

    //更新热度评分
    @Override
    public void incrHotScore(String skuId) {
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //定义一个key
        String key = "hotScore";
        Double hotScore = jedis.zincrby(key, 1, "skuId:" + skuId);
        if (hotScore % 10 == 0) {
            //更新es
            updateHotScore(skuId,Math.round(hotScore));
        }


    }

    //更新es
    private void updateHotScore(String skuId, long hotScore) {
        String updateDsl = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}\n";
        Update update = new Update.Builder(updateDsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //制作返回值数据
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        //将对象SkuLsResult的所有属性赋值就可以了
        SkuLsResult skuLsResult = new SkuLsResult();
        //页面显示商品结果的集合: List<SkuLsInfo> skuLsInfoList;
        ArrayList<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        //从结果集中获取数据进行赋值
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits != null && hits.size() > 0) {
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                //判断是否为高亮
                if (hit.highlight != null && hit.highlight.size() > 0) {
                    List<String> skuNameList = hit.highlight.get("skuName");
                    String skuNameHI = skuNameList.get(0);
                    skuLsInfo.setSkuName(skuNameHI);
                }
                skuLsInfoList.add(skuLsInfo);
            }
        }
        //添加到对象中
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        //总条数:long total;
        skuLsResult.setTotal(searchResult.getTotal());
        //总页数:long totalPages;
        long totalPages = (searchResult.getTotal() - skuLsParams.getPageSize()) / skuLsParams.getPageSize();
        //添加到对象中
        skuLsResult.setTotalPages(totalPages);
        //平台属性值ID集合:List<String> attrValueIdList;
        List<String> list = new ArrayList<>();
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        //判断
        if (buckets != null && buckets.size() > 0) {
            for (TermsAggregation.Entry bucket : buckets) {
                //获取平台属性ID值，并添加到集合中
                String valueId = bucket.getKey();
                list.add(valueId);
            }
        }
        //添加到对象中
        skuLsResult.setAttrValueIdList(list);
        //返回
        return skuLsResult;
    }

    //制作dsl语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //定义查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //定义下一级 {bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断用户查询的方式
        if (skuLsParams.getCatalog3Id() !=null && skuLsParams.getCatalog3Id().length() > 0) {
            //如果有三级分类ID说明是通过三级分类进行查询的
            //定义下一级 {"filter": [{"term": {"catalog3Id": "61"}}}
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            //
            boolQueryBuilder.filter(termQueryBuilder);
            //判断平台属性值ID是否有值
            if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                //有值，遍历添加
                for (String valueId : skuLsParams.getValueId()) {
                    TermQueryBuilder termQueryBuilderValuue = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                    boolQueryBuilder.filter(termQueryBuilderValuue);
                }
            }
        }

        if (skuLsParams.getKeyword() !=null && skuLsParams.getKeyword().length() > 0) {
            //如果有skuName说明用户是通过搜索框进行查询的
            //定义对象,创建查询bulid
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            //赋值
            boolQueryBuilder.must(matchQueryBuilder);
            //设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //添加高亮字典
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            //将高亮结果存放入查询器
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //设置分页
        int from = (skuLsParams.getPageNo()-1) * skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        //设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId.keyword");
        searchSourceBuilder.aggregation(groupby_attr);
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();

        System.out.println(query);

        return query;

    }
}
