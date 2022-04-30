package com.amayadream.yonghui.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;

/**
 * @author :  Amayadream
 * @date :  2022.04.30 19:09
 */
@Slf4j
public class Bark {

    // Bark推送地址 TODO 请修改成你bark app里的地址
    private static String prefixUrl = "https://api.day.app/xxx/";

    private static OkHttpClient okHttpClient = new OkHttpClient();

    public static void sendNotice(String msg) {
        try {
            String encodedMsg = URLEncoder.encode(msg, "UTF-8");
            String url = prefixUrl + "永辉超市上新啦/" + encodedMsg;
            Request request = new Request.Builder().url(url).get().build();
            Response response = okHttpClient.newCall(request).execute();
            response.isSuccessful();
        } catch (Exception e) {
            log.error("[sendNotice] 发送Bark通知失败, 通知内容: {}", msg, e);
        }
    }

}
