package com.scholix.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";
    private RecyclerView scheduleRecyclerView;
    private ScheduleAdapter adapter;
    private ArrayList<ScheduleItem> scheduleItems;
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    // Mapping for subject -> colorClass (using cleaned keys)
    private static final Map<String,String> SUBJECT_COLORS = new HashMap<>();
    static {
        SUBJECT_COLORS.put("מתמטיקה האצה", "lightgreen-cell");
        SUBJECT_COLORS.put("מדעים",       "lightyellow-cell");
        SUBJECT_COLORS.put("של`ח",       "lightgreen-cell");
        SUBJECT_COLORS.put("חינוך",       "pink-cell");
        SUBJECT_COLORS.put("ערבית",       "lightblue-cell");
        SUBJECT_COLORS.put("היסטוריה",    "lightred-cell");
        SUBJECT_COLORS.put("עברית",       "lightpurple-cell");
        SUBJECT_COLORS.put("חינוך גופני", "lightorange-cell");
        SUBJECT_COLORS.put("נחשון",       "lightyellow-cell");
        SUBJECT_COLORS.put("אנגלית",      "lime-cell");
        SUBJECT_COLORS.put("ספרות",       "blue-cell");
        SUBJECT_COLORS.put("תנך",         "lightgrey-cell");
        SUBJECT_COLORS.put("תנ`ך",       "lightgrey-cell");
        SUBJECT_COLORS.put("cancel",      "cancel-cell");
    }

    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    private int currentDay = -1;
    private TextView dayLabel;


    private RecyclerView gradesRecyclerView;
    private GradeAdapter gradeAdapter;
    private List<Grade> gradeList;
    private SharedPreferences prefs;
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

        scheduleRecyclerView = findViewById(R.id.schedule_recycler_view);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleItems = new ArrayList<>();
        adapter = new ScheduleAdapter(this, scheduleItems);
        scheduleRecyclerView.setAdapter(adapter);

        // Bottom Navigation Bar Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);

        int todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 6) % 7;
        if (todayIdx > 5) todayIdx = 0;
        fetchScheduleUpdated(todayIdx);



        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Load accounts list
        String json = prefs.getString("accounts_list", null);
        List<Account> savedAccounts = new ArrayList<>();

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Account>>() {}.getType();
            savedAccounts = gson.fromJson(json, type);
        }

        // Read global cookies and studentId for now (for Webtop accounts, until individual cookies saved)
        String savedCookies = prefs.getString("cookies", "");
        String studentId = prefs.getString("student_id", "");




        // Loop through all accounts
        for (Account account : savedAccounts) {
            if (account.getSource().equals("Webtop")) {
                // Fetch Webtop grades, pass year from Account
                WebtopGradeFetcher webtopFetcher = new WebtopGradeFetcher(savedCookies, studentId, account.getYear());
                new Thread(() -> {
                    List<Grade> webtopGrades = webtopFetcher.fetchGrades("b");
                    runOnUiThread(() -> {
                        TextView averageGrade = findViewById(R.id.average_grade);
                        int average = webtopFetcher.getAverage(webtopGrades);

                        averageGrade.setText(String.valueOf(average));

                    });
                }).start();
            }

        }

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
    @SuppressLint("NotifyDataSetChanged")
    private void fetchScheduleOriginal(final int dayIndex) {
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
                    if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                        processScheduleOriginal(hour, hourMap);
                    }
                }

                ArrayList<ScheduleItem> items = new ArrayList<>(hourMap.values());
                items.sort(Comparator.comparingInt(o -> o.hourNum));

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
    private void processScheduleOriginal(JSONObject hourObj, Map<Integer, ScheduleItem> hourMap) throws Exception {
        JSONArray scheduleArray = hourObj.getJSONArray("scheduale");
        JSONObject scheduleItem = scheduleArray.getJSONObject(0);

        String subject = cleanSubject(scheduleItem.optString("subject", "לא זמין"));
        String teacher = scheduleItem.optString("teacherPrivateName", "לא זמין") + " " + scheduleItem.optString("teacherLastName", "לא זמין");
        int hourNum = scheduleItem.optInt("hour", -1);
        String colorClass = findColorClass(subject);

        ScheduleItem schedItem = new ScheduleItem(hourNum, subject, teacher, colorClass, "");
        hourMap.put(hourNum, schedItem);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void fetchScheduleUpdated(final int dayIndex) {
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
                Map<Integer,ScheduleItem> hourMap2 = new HashMap<>();

                for (int i = 0; i < hours.length(); i++) {
                    JSONObject hour = hours.getJSONObject(i);
                    if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                        processScheduleOriginal(hour, hourMap2);
                    }
                }
                for (int i = 0; i < hours.length(); i++) {
                    JSONObject hour = hours.getJSONObject(i);
                    if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                        processScheduleUpdated(hour, hourMap, hourMap2);
                    }
                }

                ArrayList<ScheduleItem> items = new ArrayList<>(hourMap.values());
                items.sort(Comparator.comparingInt(o -> o.hourNum));

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

    private void processScheduleUpdated(JSONObject hourObj, Map<Integer, ScheduleItem> hourMap) throws Exception {
        processScheduleUpdated(hourObj, hourMap, new HashMap<>()); // calls the full version
    }

    private void processScheduleUpdated(JSONObject hourObj, Map<Integer, ScheduleItem> hourMap, Map<Integer, ScheduleItem> hourMap2) throws Exception {
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
        boolean cancel=false;
        for (int j = 0; j < changesArray.length(); j++) {
            JSONObject itemObj = changesArray.getJSONObject(j);
            if (itemObj.optInt("original_hour", -1) == -1 &&
                    itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")) {
//                schedItem.addChange("ביטול שיעור");
//                schedItem.setColorClass("cancel-cell");
                cancel=true;
            }
            if (itemObj.optInt("original_hour", -1) == hourNum &&
                    itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")) {
//                schedItem.addChange("ביטול שיעור");
//                schedItem.setColorClass("cancel-cell");
                cancel=true;

            }
            if (itemObj.optInt("original_hour", -1) != -1) {
                ScheduleItem originalHour = hourMap.get(itemObj.optInt("original_hour", 0));
//                originalHour.addChange("ביטול שיעור");
//                schedItem.setColorClass("cancel-cell");
                cancel=true;

            }

            if (itemObj.optString("definition", "לא זמין").equals("הזזת שיעור")) {
                String fillTeacher = itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין");

                boolean found=false;
                for (ScheduleItem existing : hourMap2.values()) {
                    if (existing.teacher.equals(fillTeacher)) {
                        schedItem.setSubject(existing.subject);
                        schedItem.setTeacher(existing.teacher);
                        schedItem.setColorClass(existing.colorClass);
                        found=true;
                        break;
                    }
                }
                if(!found){
                    schedItem.addChange("מילוי מקום של " + fillTeacher);
                }
            }
            if (itemObj.optString("definition", "לא זמין").equals("מילוי מקום")) {
                String fillTeacher = itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין");

                boolean found=false;
                for (ScheduleItem existing : hourMap2.values()) {
                    if (existing.teacher.equals(fillTeacher)) {
                        schedItem.setSubject(existing.subject);
                        schedItem.setTeacher(existing.teacher);
                        schedItem.setColorClass(existing.colorClass);

                        found=true;
                        break;
                    }
                }
                if(!found){
                    schedItem.addChange("מילוי מקום של " + fillTeacher);
                }
            }

        }
        if (hourObj.has("events") && hourObj.getJSONArray("events").length() > 0){
            JSONArray events = hourObj.optJSONArray("events");
            assert events != null;
            JSONObject event = events.getJSONObject(0);
            String title = event.getString("title");
            String type = event.getString("title");
            String accompaniers = event.getString("accompaniers").replaceAll(",\\s*$", "");


            if (!accompaniers.equals(",") && !accompaniers.equals(" ") && !accompaniers.isEmpty())
                schedItem.setTeacher(accompaniers);
            schedItem.setSubject(title);
            schedItem.removeChanges();
        }
        if (!cancel){
            hourMap.put(hourNum, schedItem);
        }
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
        // Try loading from preferences if already generated
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedColor = prefs.getString("color_" + subject, null);
        if (savedColor != null) {
            SUBJECT_COLORS.put(subject, savedColor);
            return savedColor;
        }
        // Generate a random pastel color class name
        String[] colorPool = {"red", "green", "blue", "orange", "yellow", "purple", "teal", "lime", "pink"};
        String randomColor = "custom-" + colorPool[new Random().nextInt(colorPool.length)] + "-cell";

        // Save to both map and preferences
        SUBJECT_COLORS.put(subject, randomColor);
        prefs.edit().putString("color_" + subject, randomColor).apply();

        return randomColor;
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