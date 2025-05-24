package ApiModules;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import AppModules.Models.Post;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "ApiClient";
    public static final String BASE_URL = "https://shadow-talk-server.vercel.app";
    private static ApiClient instance;
    private final OkHttpClient client;
    private final Handler mainHandler;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public interface PostsCallback {
        void onSuccess(List<Post> posts, int totalPosts);
        void onError(String errorMessage);
    }

    public interface LikeCallback {
        void onSuccess(boolean isLiked, int newLikeCount);
        void onError(String errorMessage);
    }

    public void toggleLikePost(int uaid, int pid, String sId, final LikeCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("uaid", uaid);
            jsonBody.put("pid", pid);
            if (sId != null && !sId.isEmpty()) {
                jsonBody.put("s_id", sId);
            }

            RequestBody body = RequestBody.create(JSON, jsonBody.toString());

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/toggle-like-post")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Like toggle API call failed", e);
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Like toggle response: " + responseBody);

                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Server error: " + response.code()));
                            return;
                        }

                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");

                        if (!success) {
                            mainHandler.post(() -> callback.onError(jsonResponse.optString("message", "Unknown error")));
                            return;
                        }

                        String action = jsonResponse.getString("action");
                        boolean isLiked = action.equals("liked");

                        // For now, we'll just increment/decrement by 1 since the server
                        // doesn't return the new like count. In a real app, you should
                        // fetch the updated post or have the server return the new count.
                        mainHandler.post(() -> callback.onSuccess(isLiked, -1));

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        mainHandler.post(() -> callback.onError("Data parsing error: " + e.getMessage()));
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error", e);
            mainHandler.post(() -> callback.onError("Request creation error: " + e.getMessage()));
        }
    }

    public void getPosts(int limit, int offset, final PostsCallback callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/api/allpost")
                .newBuilder()
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", String.valueOf(offset))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Server Response Code: " + response.code());
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Server error: " + response.code()));
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Server Response: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (!jsonResponse.getBoolean("success")) {
                        mainHandler.post(() -> callback.onError(jsonResponse.optString("message", "Unknown error")));
                        return;
                    }

                    int totalPosts = jsonResponse.getInt("totalPosts");
                    JSONArray postsArray = jsonResponse.getJSONArray("posts");
                    List<Post> posts = new ArrayList<>();

                    for (int i = 0; i < postsArray.length(); i++) {
                        JSONObject postJson = postsArray.getJSONObject(i);
                        Post post = new Post();
                        post.setPid(postJson.getInt("pid"));
                        post.setUaid(postJson.getInt("uaid"));
                        post.setTitle(postJson.optString("title", ""));
                        post.setContent(postJson.optString("content", ""));
                        post.setPostImage(postJson.optString("postImage", null));
                        post.setUsername(postJson.optString("username", "Anonymous"));
                        post.setProfileImage(postJson.optString("profileImage", null));
                        post.setLikeCount(postJson.optInt("likeCount", 0));

                        // Set a relative timestamp (this would normally come from server)
                        post.setTimestamp("2 hours ago");

                        posts.add(post);
                    }

                    mainHandler.post(() -> callback.onSuccess(posts, totalPosts));

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    mainHandler.post(() -> callback.onError("Data parsing error: " + e.getMessage()));
                }
            }
        });
    }
}