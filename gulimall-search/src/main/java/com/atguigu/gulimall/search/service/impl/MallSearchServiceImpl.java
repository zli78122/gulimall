package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    /**
     * 检索
     *
     * @param param 检索参数
     * @return 检索结果
     */
    @Override
    public SearchResult search(SearchParam param) {
        // 构建检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        SearchResult result = null;
        try {
            // 执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
            // 构建结果数据 - 分析响应结果，把响应数据封装成SearchResult对象
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 构建结果数据 - 分析响应结果，把响应数据封装成SearchResult对象
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1.返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                // 将 JSON字符串 转换为 SkuEsModel对象
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    // 设置高亮标题
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        // 2.当前查询到的商品所涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            // 属性名
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            // 属性值
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attr_values = attr_value_agg.getBuckets().stream().map(item -> {
                String attrValueItem = ((Terms.Bucket) item).getKeyAsString();
                return attrValueItem;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attr_values);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // 3.当前查询到的商品所涉及到的所有品牌信息
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            // 品牌名称
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            // 品牌图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 4.当前查询到的商品所涉及到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            // 分类名称
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        // 5.分页
        // 当前页码
        result.setPageNum(param.getPageNum());
        // 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 总页码
        int totalPages = total % EsConstant.PRODUCT_PAGE_SIZE == 0 ? ((int) (total / EsConstant.PRODUCT_PAGE_SIZE)) : ((int) (total / EsConstant.PRODUCT_PAGE_SIZE + 1));
        result.setTotalPages(totalPages);
        // 页码集合 - [1, 2, 3, ... totalPages]
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 6.构建面包屑导航
        // 属性的面包屑导航
        // attrs=[属性id]_属性值1:属性值2&attrs=[属性id]_属性值1:属性值2
        // e.g.   attrs=1_5寸:6寸&attrs=2_16G:8G
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                // 将 每一个attrs请求参数 都封装为 一个面包屑导航对象 (NavVo)
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // 1_5寸:6寸   ->   [1, 5寸:6寸]
                String[] s = attr.split("_");
                // 设置 面包屑导航的值 为 属性值
                navVo.setNavValue(s[1]);
                // 根据 attrId 查询 属性详细信息
                R attrInfoR = productFeignService.info(Long.parseLong(s[0]));
                if (attrInfoR.getCode() == 0) {
                    // 查询属性详细信息成功
                    // 从 attrInfoR 中获取 attrResponseVo
                    AttrResponseVo attrResponseVo = attrInfoR.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    // 设置 面包屑导航的名称 为 属性名称
                    navVo.setNavName(attrResponseVo.getAttrName());
                } else {
                    // 查询属性详细信息失败
                    // 设置 面包屑导航的名称 为 属性id
                    navVo.setNavName(s[0]);
                }
                // attrIds : 已选择的所有属性 - 请求参数中包含的所有属性
                result.getAttrIds().add(Long.parseLong(s[0]));

                // 设置 NavVo对象 的 link属性 : 取消当前面包屑导航后，页面跳转的地址
                // 清除地址中当前属性的请求参数 - 获取所有的请求参数，清除当前属性的请求参数
                String replace = replaceQueryString(param, attr, "attrs"); // attr = "1_5寸:6寸"
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(collect);
        }
        // 品牌的面包屑导航
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            // 将 品牌请求参数 封装为 一个面包屑导航对象 (NavVo)
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            // 设置 面包屑导航的名称 为 "品牌"
            navVo.setNavName("品牌");

            // 根据 brandId 获取 品牌信息
            R brandInfoR = productFeignService.getBrands(param.getBrandId());
            if (brandInfoR.getCode() == 0) {
                // 查询品牌信息成功
                // 从 brandInfoR 中获取 brandVoList
                List<BrandVo> brandVoList = brandInfoR.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                for (BrandVo brandVo : brandVoList) {
                    // 设置 面包屑导航的值
                    navVo.setNavValue(brandVo.getName());
                    // 设置 NavVo对象 的 link属性 : 取消当前面包屑导航后，页面跳转的地址
                    // 清除地址中当前属性的请求参数 - 获取所有的请求参数，清除当前属性的请求参数
                    String replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
                    navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                }
            }
            // 封装 面包屑导航数据
            result.getNavs().add(navVo);
        }

        return result;
    }

    // 清除地址中当前属性的请求参数 - 获取所有的请求参数，清除当前属性的请求参数
    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            // 浏览器把空格翻译成'%20'，而Java把空格翻译成'+'
            encode = encode.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return param.get_queryString().replace("&" + key + "=" + encode, "");
    }

    /**
     * 构建检索请求
     *   模糊匹配
     *   过滤 (按照属性、分类、品牌、价格区间、库存)
     *   排序
     *   分页
     *   高亮
     *   聚合分析
     *
     * DSL语句示例 : dsl.json
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // 构建DSL语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /*
         * 查询 : 模糊匹配、过滤 (按照属性、分类、品牌、价格区间、库存)
         */
        // 1.构建BoolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1 bool->must : 模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2 bool->filter : 按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2 bool->filter : 按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2 bool->filter : 按照所有指定的属性查询
        // attrs=[属性id]_属性值1:属性值2&attrs=[属性id]_属性值1:属性值2
        // e.g.   attrs=1_5寸:6寸&attrs=2_16G:8G
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // 1_5寸:6寸   ->   [1, 5寸:6寸]
                String[] s = attrStr.split("_");
                // 属性id = 1
                String attrId = s[0];
                // 属性值 = 5寸:6寸
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个attr属性都对应一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 1.2 bool->filter : 按照是否有库存查询
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        // 1.2 bool->filter : 按照价格区间
        // 价格区间 : skuPrice=1_500/_500/500_
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte((s[0]));
                }
                if (param.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte((s[0]));
                }
            }
            boolQuery.filter(rangeQuery);
        }
        // 构建以上所有查询条件
        sourceBuilder.query(boolQuery);

        /*
         * 排序、分页、高亮
         */
        // 2.1 排序
        // sort=saleCount_asc/saleCount_desc
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            // 字段_排序规则 : saleCount_asc   ->   [saleCount, asc]
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        // 2.2 分页
        // from = (pageNum - 1) * pageSize
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGE_SIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGE_SIZE);
        // 2.3 高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /*
         * 聚合分析
         */
        // 3.1 品牌聚合 - brand
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        // 品牌聚合的子聚合 : 检索对应的品牌名称和图片
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        // 3.2 分类聚合 - catalog
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(20);
        // 分类聚合的子聚合 : 检索对应的分类名称
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        // 3.3 属性聚合 - attr
        // 嵌入式聚合 (nested aggregation)
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 聚合分析出当前attrId对应的属性名 - attrName
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合分析出当前attrId对应的所有可能的属性值 - attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        // 构建检索请求
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }
}
