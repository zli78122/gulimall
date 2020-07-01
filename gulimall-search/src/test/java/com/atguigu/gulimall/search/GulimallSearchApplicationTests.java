package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 复杂检索
     */
    @Test
    public void test2() throws IOException {
        // 1.创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        // 指定索引
        searchRequest.indices("bank");
        // 指定检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 构造检索条件
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));

        // 聚合条件
        // 1-1 按年龄的值分布聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);
        // 1-2 计算平均薪资
        AvgAggregationBuilder balanceAgg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAgg);

        searchRequest.source(sourceBuilder);

        // 2.执行检索
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println(searchResponse.toString());

        // 3.分析结果
        // 3-1 获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            String index = hit.getIndex();
            System.out.println(index);
            String id = hit.getId();
            System.out.println(id);
            String sourceAsString = hit.getSourceAsString();
            System.out.println(sourceAsString);
        }

        // 3-2 获取检索的分析信息
        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageTerms = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageTerms.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            System.out.println("年龄:" + keyAsString + "; 数量:" + docCount);
        }

        Avg balanceAvg = aggregations.get("balanceAvg");
        System.out.println("平均薪资:" + balanceAvg.getValue());
    }

    /**
     * 新增 / 修改
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");

        User user = new User();
        user.setUserName("zli78122");
        user.setAge(20);
        user.setGender("M");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        // 创建索引 & 保存数据
        IndexResponse index = client.index(indexRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println(index);
    }

    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }

    @Test
    public void test() {
        System.out.println(client);
    }
}
