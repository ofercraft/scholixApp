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
        ImageButton accountButton = findViewById(R.id.account_button);
        if (accountButton != null) {
            accountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAccountPopup(v);
                }
            });
        }
        // Load Today's Schedule (day = -1 means "today")
        fetchTodaySchedule(-1);
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
    private void fetchTodaySchedule(final int day) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Fetching schedule...");
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    String grade = prefs.getString("class_code", "default_grade");
                    String institution = prefs.getString("institution", "default_institution");
                    Log.d(TAG, "Institution: " + institution);
                    Log.d(TAG, "Grade: " + grade);

                    JSONObject data = new JSONObject();
                    data.put("institutionCode", institution);
                    data.put("selectedValue", grade);
                    data.put("typeView", 1);

                    String savedCookies = prefs.getString("cookies", "");


                    RequestBody body = RequestBody.create(
                            data.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://webtopserver.smartschool.co.il/server/api/shotef/ShotefSchedualeData")
                            .addHeader("Cookie", savedCookies) // include the saved cookie here
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String respBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response: " + respBody);


                    JSONObject jsonResponse = new JSONObject(respBody);
                    JSONArray daysArray = jsonResponse.getJSONArray("data");

                    Calendar calendar = Calendar.getInstance();
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)-1; // Sunday=1, Monday=2, ...
                    int todayIndex = day; // Monday becomes index 0, Sunday becomes index 6
                    if (day==-1){
                        todayIndex=dayOfWeek;
//
                    }
                    if (todayIndex < 0 || todayIndex >= daysArray.length()) {
                        Log.w(TAG, "todayIndex out of range: " + todayIndex);
                        return;
                    }

                    JSONObject todayData = daysArray.getJSONObject(todayIndex);
                    JSONArray hoursData = todayData.getJSONArray("hoursData");

                    // Merge "scheduale" and "changes" into a single ScheduleItem per hour.
                    Map<Integer, ScheduleItem> hourMap = new HashMap<>();

                    for (int i = 0; i < hoursData.length(); i++) {
                        JSONObject hourObj = hoursData.getJSONObject(i);
                        if (hourObj.has("scheduale")) {

                            JSONArray schedualeArray = hourObj.getJSONArray("scheduale");
                            processScheduleArray(schedualeArray, hourMap, false,  false, false);
                        }
                        if (hourObj.has("scheduale")) {
                            JSONArray scheduleArray = hourObj.getJSONArray("scheduale");
                            for (int k = 0; k < scheduleArray.length(); k++) {
                                JSONObject scheduleItem = scheduleArray.getJSONObject(k);
                                if (scheduleItem.has("changes")) {
                                    JSONArray changesArray = scheduleItem.getJSONArray("changes");
                                    processScheduleArray(changesArray, hourMap, true,  false, false);


                                    for (int j = 0; j < changesArray.length(); j++) {
                                        JSONObject change = changesArray.getJSONObject(j);
                                        // For example, get the definition field:
                                        String definition = change.optString("definition", "");
                                        String type = change.optString("type", "");
                                        String group = change.optString("group", "");
                                        String fillUpType = change.optString("fillUpType", "");
                                        // Process or log the change information as needed
                                        Log.d("ChangeSnippet", "Definition: " + definition +
                                                ", Type: " + type +
                                                ", Group: " + group +
                                                ", FillUpType: " + fillUpType);

                                    }
                                }
                            }
                        }

                        if (hourObj.has("events")) {
                            JSONArray eventsArray = hourObj.getJSONArray("events");
                            processScheduleArray(eventsArray, hourMap, false, true, false);
                        }


                    }

                    final ArrayList<ScheduleItem> items = new ArrayList<>(hourMap.values());
                    Collections.sort(items, new Comparator<ScheduleItem>() {
                        @Override
                        public int compare(ScheduleItem o1, ScheduleItem o2) {
                            return Integer.compare(o1.hourNum, o2.hourNum);
                        }
                    });

                    for (ScheduleItem item : items) {
                        Log.d(TAG, "Item: Hour=" + item.hourNum + ", Subject=" + item.subject +
                                ", Teacher=" + item.teacher + ", ColorClass=" + item.colorClass +
                                ", Changes=" + item.changes);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scheduleItems.clear();
                            scheduleItems.addAll(items);
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching schedule", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Error fetching schedule", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void processScheduleArray(JSONArray array, Map<Integer, ScheduleItem> hourMap, boolean isChange, boolean isEvents, boolean isExam) throws Exception {
        for (int j = 0; j < array.length(); j++) {
            JSONObject itemObj = array.getJSONObject(j);
            if (isEvents){
                System.out.println(itemObj);
                System.out.println(itemObj);
                System.out.println(itemObj);
                System.out.println(itemObj);

            }
            int hourNum = itemObj.optInt("hour", 0);
            ScheduleItem schedItem = hourMap.get(hourNum);
            if (schedItem==null && itemObj.has("from_hour")){
                hourNum = itemObj.optInt("from_hour", 0);
                schedItem = hourMap.get(hourNum);
            }
            if (schedItem == null) {
                if (isEvents){
                    System.out.println("hfu");
                }
                boolean find = isEvents && isChange && isExam;
                String subject = find ? "לא זמין" : cleanSubject(itemObj.optString("subject", "לא זמין"));
                String teacher = find ? "לא זמין" :
                        itemObj.optString("teacherPrivateName", "לא זמין") + " " +
                                itemObj.optString("teacherLastName", "לא זמין");
                String colorClass = find ? "default-cell" : findColorClass(subject);
                schedItem = new ScheduleItem(hourNum, subject, teacher, colorClass, "");
                hourMap.put(hourNum, schedItem);
            }

            else {
                if (isEvents){
                    System.out.println("hhhhhhhhhhhhhhhhhh");
                    String title = itemObj.optString("title", "לא זמין");
                    schedItem.addChange(title);
                }
                if (!schedItem.teacher.equals(itemObj.optString("teacherPrivateName", "לא זמין") + " " +
                        itemObj.optString("teacherLastName", "לא זמין")) && itemObj.optString("teacherPrivateName", "לא זמין")!="לא זמין"){
                    schedItem.teacher = schedItem.teacher + " / " +itemObj.optString("teacherPrivateName", "לא זמין") + " " +
                            itemObj.optString("teacherLastName", "לא זמין");
                }
                if (!schedItem.subject.equals(cleanSubject(itemObj.optString("subject", "לא זמין"))) && itemObj.optString("subject", "לא זמין")!="לא זמין"){
                    schedItem.subject = schedItem.subject + " / " +itemObj.optString("subject", "לא זמין");
                }
            }


            if (itemObj.optInt("original_hour", -1)==-1 &&  itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")){
                schedItem.addChange("ביטול שיעור");
            }
            if (itemObj.optInt("original_hour", -1)==hourNum &&  itemObj.optString("definition", "לא זמין").equals("ביטול שיעור")){
                schedItem.addChange("ביטול שיעור");
            }

            if (itemObj.optInt("original_hour", -1)!=-1){
                ScheduleItem originalHour = hourMap.get(itemObj.optInt("original_hour", 0));
                originalHour.addChange("ביטול שיעור");
            }
            if (itemObj.optString("definition", "לא זמין").equals("מילוי מקום")){
                schedItem.addChange("מילוי מקום של " + itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין"));
            }
            if (itemObj.optString("definition", "לא זמין").equals("הזזת שיעור")){
                schedItem.addChange("מילוי מקום של " + itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין"));
            }


            if (isChange) {
                String definition = itemObj.optString("definition", "");
                String type = itemObj.optString("type", "");
                String group = itemObj.optString("group", "");
                String fillUpType = itemObj.optString("fillUpType", "");
                // Combine these fields to form a detailed change info string.
                String changeDetails = definition;
                if (!group.isEmpty()) {
                    changeDetails += " (" + group + ")";
                }
                // You can append additional info if needed:
                if (!type.isEmpty()) {
                    changeDetails += " [" + type + "]";
                }
                if (!fillUpType.isEmpty()) {
                    changeDetails += " {" + fillUpType + "}";
                }
            }
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
                        Toast.makeText(HomeActivity.this, "Username clicked", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.menu_settings:
                        startActivity(new Intent(HomeActivity.this, SettingsActivity.class));

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