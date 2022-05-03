package com.amayadream.yonghui.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author :  Amayadream
 * @date :  2022.04.30 18:06
 */
@Slf4j
public class YongHui {

    // 请求地址 TODO 请修改成你抓包到的地址+参数
    private String url = "https://api.yonghuivip.com/webapi/cms-activity-rest/h5/activity/page?platform=wechatminiprogram&xxxx";

    // 请求头 TODO 请修改成你抓包的headers
    private Headers headers = Headers.of(
            "Host", "api.yonghuivip.com",
            "Origin", "https://cmsh5.yonghuivip.com",
            "xxx", "xxx");

    // 不关注的商品关键字, TODO 请填写你不想关注的商品关键字
    private Set<String> ignoreSkuKeywords = Sets.newHashSet("蔬菜", "鲜食套餐", "民生套餐包");

    private OkHttpClient okHttpClient;

    public YongHui(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public YongHui(Set<String> ignoreSkuKeywords, OkHttpClient okHttpClient) {
        this.ignoreSkuKeywords = ignoreSkuKeywords;
        this.okHttpClient = okHttpClient;
    }

    public JSONObject fetchActivityJson() {
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .get()
                .build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("[fetchActivityJson] 获取接口数据失败, 接口状态码: {}", response.code());
                return null;
            }
            String responseJson = response.body().string();
            JSONObject responseObject = JSON.parseObject(responseJson);
            if (!responseObject.containsKey("code") || !Objects.equals(responseObject.getInteger("code"), 0)) {
                log.error("[fetchActivityJson] 接口数据不符合预期, code: {}, 接口数据: {}", responseObject.get("code"), responseJson);
                return null;
            }

            return responseObject.getJSONObject("data");
        } catch (Exception e) {
            log.error("[fetchActivityJson] 请求永辉接口发生异常", e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public List<String> analyzeActivityJson(JSONObject jsonObject) {
        List<String> customSkuNames = Lists.newArrayListWithExpectedSize(20);

        JSONArray floors = jsonObject.getJSONArray("floors");
        for (Object floorObject : floors) {
            JSONObject floor = (JSONObject) floorObject;
            if (!Objects.equals(floor.getString("key"), "presalesku")) {
                continue;
            }

            JSONArray values = floor.getJSONArray("value");
            for (Object skuObject : values) {
                JSONObject sku = (JSONObject) skuObject;
                String title = sku.getString("title");
                String price = sku.getJSONObject("price").getString("price");

                String customSkuName = "【" + title + " @ " + price + "】";
                JSONObject cartAction = sku.getJSONObject("cartAction");
                Integer actionType = cartAction.getInteger("actionType");
                if (actionType == 3 || actionType == -2) {
                    continue;
                }
                String actionText = cartAction.getString("actionText");
                if (Objects.equals("已抢完", actionText) || Objects.equals("未开始", actionText)) {
                    continue;
                }

                // 忽略不感兴趣的sku关键字
                boolean shouldContinue = false;
                for (String ignoreSkuKeyword : ignoreSkuKeywords) {
                    if (title.contains(ignoreSkuKeyword)) {
                        shouldContinue = true;
                        break;
                    }
                }
                if (shouldContinue) {
                    continue;
                }

                customSkuNames.add(customSkuName);
            }
        }
        return customSkuNames;
    }

}
