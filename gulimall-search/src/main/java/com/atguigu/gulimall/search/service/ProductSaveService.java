package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    // 商品上架 - 将 sku 保存到 Elasticsearch 中
    Boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
