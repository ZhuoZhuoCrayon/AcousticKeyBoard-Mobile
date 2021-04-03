package tech.oom.idealrecorderdemo.ak;

import com.google.gson.Gson;
import okhttp3.*;
import tech.oom.idealrecorder.utils.Log;

import java.io.IOException;

public class AkClient {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();

    public static Call post(String url, Object json_obj, Callback callback) {
        Gson gson = new Gson();
        String json = gson.toJson(json_obj);
        RequestBody body = RequestBody.create(JSON, json);

        Log.d("MainActivity", "post params -> " + json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }
}
