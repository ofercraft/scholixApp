package com.scholix.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Schedule screen with six RTL day‑tabs (“ראשון” … “שישי”).
 * Tap a tab → fetch and render that day’s schedule.
 */
public class ScheduleActivity extends BaseActivity {

    private static final String TAG = "ScheduleActivity";

    // ────────── UI ───────────────────────────────────────────
    private RecyclerView  scheduleRecyclerView;
    private ScheduleAdapter adapter;
    private TabLayout     dayTabs;
    private TextView      dayLabel;   // optional label under the tabs

    // ────────── DATA ─────────────────────────────────────────
    private final ArrayList<ScheduleItem> scheduleItems = new ArrayList<>();
    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    // subject → colourClass
    private static final Map<String,String> SUBJECT_COLORS = new HashMap<>();
    static {
        SUBJECT_COLORS.put("מתמטיקה האצה", "lightgreen-cell");
        SUBJECT_COLORS.put("מדעים",       "lightyellow-cell");
        SUBJECT_COLORS.put("של``ח",       "lightgreen-cell");
        SUBJECT_COLORS.put("חינוך",       "pink-cell");
        SUBJECT_COLORS.put("ערבית",       "lightblue-cell");
        SUBJECT_COLORS.put("היסטוריה",    "lightred-cell");
        SUBJECT_COLORS.put("עברית",       "lightpurple-cell");
        SUBJECT_COLORS.put("חינוך גופני", "lightorange-cell");
        SUBJECT_COLORS.put("נחשון",       "lightyellow-cell");
        SUBJECT_COLORS.put("אנגלית",      "lime-cell");
        SUBJECT_COLORS.put("ספרות",       "blue-cell");
        SUBJECT_COLORS.put("תנך",         "lightgrey-cell");
        SUBJECT_COLORS.put("תנ``ך",       "lightgrey-cell");
        SUBJECT_COLORS.put("cancel",      "cancel-cell");
    }

    @Override protected int getLayoutResourceId() { return R.layout.activity_schedule; }

    // ───────────────────────── onCreate ──────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // RecyclerView
        scheduleRecyclerView = findViewById(R.id.schedule_recycler_view);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(this, scheduleItems);
        scheduleRecyclerView.setAdapter(adapter);
/*        scheduleRecyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));*/

        // Day‑Tabs
        dayTabs  = findViewById(R.id.day_tabs);
//        dayLabel = findViewById(R.id.day_label);

        // index for “today” (Sun=0 … Fri=5; Sat→0)
        int todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 6) % 7;
        if (todayIdx > 5) todayIdx = 0;

        if (dayTabs != null) {
            dayTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    Log.d(TAG, "Tab selected index=" + tab.getPosition());
                    updateDayLabel(tab.getPosition());
                    fetchSchedule(tab.getPosition());
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
            dayTabs.selectTab(dayTabs.getTabAt(todayIdx));   // will trigger fetchSchedule
        } else {
            // Fallback: no TabLayout in the layout → just load today's schedule once
            Log.w(TAG, "day_tabs not found in layout – loading schedule once");
            fetchSchedule(todayIdx);
        }

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNav);
    }

    // ───────────────────────── helpers ───────────────────────────────
    private void updateDayLabel(int day) {
        if (dayLabel == null) return;
        switch (day) {
            case 0: dayLabel.setText("ראשון");  break;
            case 1: dayLabel.setText("שני");    break;
            case 2: dayLabel.setText("שלישי");  break;
            case 3: dayLabel.setText("רביעי");  break;
            case 4: dayLabel.setText("חמישי");  break;
            case 5: dayLabel.setText("שישי");   break;
            default: dayLabel.setText("היום");
        }
    }

    /** Fetch schedule for a given day index (0=Sun … 5=Fri). */
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
                        Toast.makeText(ScheduleActivity.this,
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
}
