package com.amayadream.yonghui;

import com.alibaba.fastjson.JSONObject;
import com.amayadream.yonghui.service.Bark;
import com.amayadream.yonghui.service.YongHui;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 1.使用Charles抓包, 最好抓永辉的小程序, 抓到这个链接的请求即可. https://api.yonghuivip.com/webapi/cms-activity-rest/h5/activity/page
 * 2.将YongHui和Bark类里的TODO改成自己的配置
 * 3.启动Main类即可
 *
 * @author :  Amayadream
 * @date :  2022.04.30 18:01
 */
@Slf4j
public class Main {

    // 执行间隔, 单位: 秒
    private static long periodSecond = 45;

    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    public static void main(String[] args) {
        executorService.scheduleAtFixedRate(() -> {
            try {
                YongHui yongHui = new YongHui(new OkHttpClient());
                JSONObject jsonObject = yongHui.fetchActivityJson();
                if (jsonObject == null) {
                    log.info("[Main] 获取接口数据失败");
                    Bark.sendNotice("获取接口数据失败, 定时任务终止");
                    executorService.shutdown();
                    return;
                }

                List<String> customSkuNames = yongHui.analyzeActivityJson(jsonObject);
                if (customSkuNames == null || customSkuNames.isEmpty()) {
                    log.info("[Main] 执行结束, 当前无有效商品, 时间: {}", LocalDateTime.now());
                    return;
                }

                String msg = String.format("当前有 %s 款商品, 请关注: %s", customSkuNames.size(), Joiner.on(" 、 ").join(customSkuNames));
                log.info("[Main] 执行结束, {}", msg);

                Bark.sendNotice(msg);
            } catch (Exception e) {
                log.error("[Main] 执行出错, 定时任务终止", e);
                Bark.sendNotice("执行出错, 定时任务终止");
                executorService.shutdown();
            }
        }, 5L, periodSecond, TimeUnit.SECONDS);
    }

}

