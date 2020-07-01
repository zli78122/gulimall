package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    // 商品上架 - 将 sku 保存到 Elasticsearch 中
    @Override
    public Boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        // 批量操作
        BulkRequest bulkRequest = new BulkRequest();

        for (SkuEsModel model : skuEsModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.index(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String jsonString = JSON.toJSONString(model);
            indexRequest.source(jsonString, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        // 批量 新增/修改
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);

        // 批量操作是否发生错误
        boolean b = bulk.hasFailures();
        if (b) {
            // 批量操作 发生错误
            log.error("商品上架错误: {}", bulk.buildFailureMessage());
        } else {
            // 批量操作 没有发生错误
            List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
                return item.getId();
            }).collect(Collectors.toList());

            log.info("商品上架完成: {}, 返回数据: {}", collect, bulk.toString());
        }

        return b;
    }
}
