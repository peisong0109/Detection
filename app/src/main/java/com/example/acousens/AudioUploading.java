package com.example.acousens;

import com.example.acousens.EventBus.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioUploading {

    /**
     * The imgur client ID for OkHttp recipes. If you're using imgur for anything other than running
     * these examples, please request your own client ID! https://api.imgur.com/oauth2
     */
    private static final String IMGUR_CLIENT_ID = "123";
    private static final MediaType MEDIA_TYPE_WAV = MediaType.parse("audio/wav");

    private static final OkHttpClient client = new OkHttpClient();

    public static void run(File f, String pro, String phone) throws Exception {
        final File file=f;
        new Thread() {
            @Override
            public void run() {
                //子线程需要做的工作
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("pro", pro)
                        .addFormDataPart("phone", phone)
                        .addFormDataPart("file", file.getName(),
                                RequestBody.create(file, MEDIA_TYPE_WAV))
                        .build();
                //设置为自己的ip地址
                Request request = new Request.Builder()
                        .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                        .url("http://82.156.79.199:5000/upload")
                        .post(requestBody)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()){
                        EventBus.getDefault().post(new MessageEvent(0,"Unexpected code " + response));
                    }
//                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

//                    System.out.println(response.body().string());
                    EventBus.getDefault().post(new MessageEvent(1,response.body().string()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
