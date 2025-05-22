package com.scholix.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends BaseActivity {

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();
    private SharedPreferences prefs;
    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    private static final String TAG = "MessagesActivity";
    private static final String API_URL = "https://webtopserver.smartschool.co.il/server/api/messageBox/GetMessagesInbox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        messageAdapter = new MessageAdapter(messageList);
        messagesRecyclerView.setAdapter(messageAdapter);

        loadMessagesFromApi();
//
//        ImageButton accountButton = findViewById(R.id.account_button);
//        accountButton.setOnClickListener(this::showAccountPopup);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);
    }
    private void showAccountPopup(View anchor) {
        PopupMenu popupMenu = new PopupMenu(MessagesActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        // Force show icons
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadMessagesFromApi() {
        String cookies = prefs.getString("cookies", "");
        String institutionCode = prefs.getString("institution", "");

        new Thread(() -> {
            List<Message> fetchedMessages = fetchMessages(1, cookies, institutionCode);
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(fetchedMessages);
                messageAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private List<Message> fetchMessages(int pageId, String cookies, String institutionCode) {
        List<Message> messages = new ArrayList<>();

        try {
            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("PageId", pageId);
            requestBodyJson.put("LabelId", 0);
            requestBodyJson.put("HasRead", JSONObject.NULL);
            requestBodyJson.put("SearchQuery", "");

            RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Cookie", cookies)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "API request failed: " + response.code());
                return messages;
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (!jsonResponse.getBoolean("status")) {
                Log.e(TAG, "API returned unsuccessful status");
                return messages;
            }

            JSONArray messagesArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject msgObj = messagesArray.getJSONObject(i);

                String subject = msgObj.optString("subject", "No Subject");
                String senderFName = msgObj.optString("student_F_name", "Unknown");
                String senderLName = msgObj.optString("student_L_name", "");
                String sendingDate = msgObj.optString("sendingDate", "");
                String senderId = msgObj.optString("senderId", "");
                String userImageToken = msgObj.optString("userImageToken", "");

                String description = senderFName + " " + senderLName + " • " + sendingDate.substring(0, 10);

                String iconUrl = "https://webtopserver.smartschool.co.il/serverImages/api/stream/GetImage?id=" +
                        senderId + "&instiCode=" + institutionCode + "&token=" + userImageToken +
                        "&usertype=1&ts=" + System.currentTimeMillis();

                messages.add(new Message(senderFName + " " + senderLName, subject, iconUrl));
            }

            Log.d(TAG, "Fetched messages: " + messages.size());

        } catch (Exception e) {
            Log.e(TAG, "Error fetching messages", e);
        }

        return messages;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_messages;
    }
}
