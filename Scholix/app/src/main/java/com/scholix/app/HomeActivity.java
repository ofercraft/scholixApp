package com.scholix.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import androidx.core.view.GravityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;


public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";
    private RecyclerView scheduleRecyclerView;
    private ScheduleAdapter adapter;
    private ArrayList<ScheduleItem> scheduleItems;
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    // Mapping for subject -> colorClass (using cleaned keys)
    private static final Map<String, String> SUBJECT_COLORS = new HashMap<>();
    static {
        SUBJECT_COLORS.put("מתמטיקה האצה", "lightgreen-cell");
        SUBJECT_COLORS.put("מדעים", "lightyellow-cell");
        SUBJECT_COLORS.put("של``ח", "lightgreen-cell"); // cleaned version of "של`ח"
        SUBJECT_COLORS.put("חינוך", "pink-cell");
        SUBJECT_COLORS.put("ערבית", "lightblue-cell");
        SUBJECT_COLORS.put("היסטוריה", "lightred-cell");
        SUBJECT_COLORS.put("עברית", "lightpurple-cell");
        SUBJECT_COLORS.put("חינוך גופני", "lightorange-cell");
        SUBJECT_COLORS.put("נחשון", "lightyellow-cell");
        SUBJECT_COLORS.put("אנגלית", "lime-cell");
        SUBJECT_COLORS.put("ספרות", "blue-cell");
        SUBJECT_COLORS.put("תנך", "lightgrey-cell");
        SUBJECT_COLORS.put("תנ``ך", "lightgrey-cell");

    }

    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    private int currentDay = -1;
    private TextView dayLabel;

    private Button btnPrevious, btnNext;

    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"


    // Drawer and menu button
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    @Override
    protected int getLayoutResourceId() {
        // Provide the layout specific to MainActivity
        return R.layout.activity_home;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Ensure your XML now includes a BottomNavigationView with id "bottom_navigation"
//
//        // Day Navigation Controls
//        btnPrevious = findViewById(R.id.btn_previous);
//        btnNext = findViewById(R.id.btn_next);
//        dayLabel = findViewById(R.id.day_label);
//        dayLabel.setText("היום");
//
//        btnPrevious.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentDay == -1) {
//                    currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
//                }
//                currentDay = (currentDay == 0) ? 6 : currentDay - 1;
//                updateDayLabel(currentDay);
//                fetchTodaySchedule(currentDay);
//            }
//        });
//
//        btnNext.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentDay == -1) {
//                    currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
//                }
//                currentDay = (currentDay == 6) ? 0 : currentDay + 1;
//                updateDayLabel(currentDay);
//                fetchTodaySchedule(currentDay);
//            }
//        });

        // RecyclerView for Schedule
        scheduleRecyclerView = findViewById(R.id.schedule_recycler_view);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleItems = new ArrayList<>();
        adapter = new ScheduleAdapter(this, scheduleItems);
        scheduleRecyclerView.setAdapter(adapter);

        // Bottom Navigation Bar Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);

        // Setup account menu on the person icon
//        ImageButton accountButton = findViewById(R.id.account_button);
//        if (accountButton != null) {
//            accountButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    showAccountPopup(v);
//                }
//            });
//        }
        int todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 6) % 7;
        if (todayIdx > 5) todayIdx = 0;
        fetchSchedule(todayIdx);
    }


    private void updateDayLabel(int day) {
        // Convert the day number to a day name. Modify as needed.
        String dayName;
        switch (day) {
            case 0: dayName = "ראשון"; break;
            case 1: dayName = "שני"; break;
            case 2: dayName = "שלישי"; break;
            case 3: dayName = "רביעי"; break;
            case 4: dayName = "חמישי"; break;
            case 5: dayName = "שישי"; break;
            case 6: dayName = "שבת"; break;
            default: dayName = "היום"; break;
        }
        dayLabel.setText(dayName);
    }
    private void fetchSchedule(final int dayIndex) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String grade       = prefs.getString("class_code",      "default_grade");
                String institution = prefs.getString("institution",     "default_institution");
                String cookies     = prefs.getString("cookies",         "");

                JSONObject payload = new JSONObject();
                payload.put("institutionCode", institution);
                payload.put("selectedValue",   grade);
                payload.put("typeView",        1);

                RequestBody body = RequestBody.create(
                        payload.toString(), MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url("https://webtopserver.smartschool.co.il/server/api/shotef/ShotefSchedualeData")
                        .addHeader("Cookie", cookies)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String respBody   = response.body() != null ? response.body().string() : "";

                JSONArray daysArr = new JSONObject(respBody).getJSONArray("data");
                if (dayIndex < 0 || dayIndex >= daysArr.length()) return;

                JSONObject dayObj  = daysArr.getJSONObject(dayIndex);
                JSONArray  hours   = dayObj.getJSONArray("hoursData");
                Map<Integer,ScheduleItem> hourMap = new HashMap<>();


                for (int i = 0; i < hours.length(); i++) {
                    JSONObject hour = hours.getJSONObject(i);
                    if (hour.has("scheduale") &&
                            hour.getJSONArray("scheduale").length() > 0) {
                        processScheduleArray2(hour, hourMap);   // original helper
                    }
                }

                ArrayList<ScheduleItem> items = new ArrayList<>(hourMap.values());
                Collections.sort(items, Comparator.comparingInt(o -> o.hourNum));

                runOnUiThread(() -> {
                    scheduleItems.clear();
                    scheduleItems.addAll(items);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching schedule", e);
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this,
                                "Error fetching schedule", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void processScheduleArray2(JSONObject hourObj, Map<Integer, ScheduleItem> hourMap) throws Exception {
        JSONArray scheduleArray = hourObj.getJSONArray("scheduale");
        JSONObject scheduleItem = scheduleArray.getJSONObject(0);

        String subject = cleanSubject(scheduleItem.optString("subject", "לא זמין"));
        String teacher = scheduleItem.optString("teacherPrivateName", "לא זמין") + " " + scheduleItem.optString("teacherLastName", "לא זמין");
        int hourNum = scheduleItem.optInt("hour", -1);
        String colorClass = findColorClass(subject);

        ScheduleItem schedItem = new ScheduleItem(hourNum, subject, teacher, colorClass, "");

        if (hourObj.has("exams")) {
            JSONArray examsArray = hourObj.getJSONArray("exams");
            for (int j = 0; j < examsArray.length(); j++) {
                JSONObject examObj = examsArray.getJSONObject(j);
                subject = examObj.optString("title", "מבחן");
                teacher = examObj.optString("supervisors", "לא זמין");

                colorClass = "exam-cell";
                schedItem.setExam(subject);
            }
        }

        JSONArray changesArray = scheduleItem.getJSONArray("changes");

        for (int j = 0; j < changesArray.length(); j++) {
            JSONObject itemObj = changesArray.getJSONObject(j);
            if (itemObj.optInt("original_hour", -1) == -1 &&
                    itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")) {
                schedItem.addChange("ביטול שיעור");
                schedItem.setColorClass("cancel-cell");
            }
            if (itemObj.optInt("original_hour", -1) == hourNum &&
                    itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")) {
                schedItem.addChange("ביטול שיעור");
                schedItem.setColorClass("cancel-cell");

            }
            if (itemObj.optInt("original_hour", -1) != -1) {
                ScheduleItem originalHour = hourMap.get(itemObj.optInt("original_hour", 0));
                originalHour.addChange("ביטול שיעור");
                schedItem.setColorClass("cancel-cell");

            }
            if (itemObj.optString("definition", "לא זמין").equals("מילוי מקום")) {
                schedItem.addChange("מילוי מקום של " + itemObj.optString("privateName", "לא זמין") +
                        " " + itemObj.optString("lastName", "לא זמין"));
            }
            if (itemObj.optString("definition", "לא זמין").equals("הזזת שיעור")) {
                schedItem.addChange("מילוי מקום של " + itemObj.optString("privateName", "לא זמין") +
                        " " + itemObj.optString("lastName", "לא זמין"));
            }
        }
        if (hourObj.has("events") && hourObj.getJSONArray("events").length() > 0){
            JSONArray events = hourObj.optJSONArray("events");
            JSONObject event = events.getJSONObject(0);
            String title = event.getString("title");
            String type = event.getString("title");
            String accompaniers = event.getString("accompaniers") != null ? event.getString("accompaniers").replaceAll(",\\s*$", "") : null;


            if (accompaniers!="," && accompaniers!=" " && accompaniers!="" && accompaniers!=null)
                schedItem.setTeacher(accompaniers);
            schedItem.setSubject(title);
            schedItem.removeChanges();
        }
        hourMap.put(hourNum, schedItem);
    }

    private String cleanSubject(String subject) {
        if (subject == null) return "לא זמין";
        return subject.replace("", "").replace("\"", "").trim();
    }

    private String findColorClass(String subject) {
        if (SUBJECT_COLORS.containsKey(subject)) {
            return SUBJECT_COLORS.get(subject);
        }
        for (String key : SUBJECT_COLORS.keySet()) {
            if (subject.contains(key)) {
                return SUBJECT_COLORS.get(key);
            }
        }
        return "default-cell";
    }
    private void showAccountPopup(View anchor) {
        PopupMenu popupMenu = new PopupMenu(HomeActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        // Reflection hack to force icons to show in the PopupMenu
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

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_username:
//                        Toast.makeText(HomeActivity.this, "Username clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.menu_settings:
                        startActivity(new Intent(HomeActivity.this, PlatformsActivity.class));

//                        Toast.makeText(HomeActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.menu_logout:
                        // Clear stored login data and navigate back to MainActivity
                        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();
                        editor.apply();
                        Toast.makeText(HomeActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
}