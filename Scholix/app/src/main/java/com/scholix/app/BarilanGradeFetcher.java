package com.scholix.app;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarilanGradeFetcher {

    private static final String TAG = "BarilanGradeFetcher";
    private static final String LOGIN_URL = "https://biumath.michlol4.co.il/api/Login/Login";
    private static final String API_URL = "https://biumath.michlol4.co.il/api/Grades/Data";
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    private String token;
    private ExecutorService executor;
    private int year;
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public BarilanGradeFetcher(Context context, String username, String password, int year, GradesCallback callback) {
        this.year = year; // Store year for potential use
        System.out.println(year);
        System.out.println(year);
        System.out.println(year);
        System.out.println(year);
        System.out.println(year);
        System.out.println(year);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> login(username, password, callback));
    }

    private void login(String username, String password, GradesCallback callback) {
        try {
            JSONObject loginData = new JSONObject();
            loginData.put("captchaToken", JSONObject.NULL);
            loginData.put("zht", username);
            loginData.put("password", password);
            loginData.put("loginType", "student");

            RequestBody loginBody = RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8"));
            Request loginRequest = new Request.Builder()
                    .url(LOGIN_URL)
                    .post(loginBody)
                    .build();

            Response loginResponse = client.newCall(loginRequest).execute();
            JSONObject loginJson = new JSONObject(loginResponse.body().string());

            if (loginJson.getBoolean("success")) {
                this.token = loginJson.getString("token");
                Log.d(TAG, "Login successful");
                fetchGrades(callback);
            } else {
                Log.e(TAG, "Login failed");
                callback.onGradesFetched(new ArrayList<>());
            }
        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            callback.onGradesFetched(new ArrayList<>());
        }
    }

    public void fetchGrades(GradesCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Grade> gradeList = new ArrayList<>();
            try {
                JSONObject requestBodyJson = new JSONObject().put("urlParameters", new JSONObject());
                RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + token)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API request failed: " + response.code());
                    callback.onGradesFetched(gradeList);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONArray gradesArray = jsonResponse.getJSONObject("collapsedCourses")
                        .getJSONArray("clientData")
                        .getJSONObject(year-1)
                        .getJSONArray("__body");
                System.out.println("d");
                System.out.println("d");
                System.out.println("d");
                System.out.println("d");
                System.out.println("d");
                System.out.println(gradesArray.toString());
                for (int i = 0; i < gradesArray.length(); i++) {
                    JSONObject gradeObject = gradesArray.getJSONObject(i);
                    String subject = gradeObject.optString("krs_shm", "לא ידוע");
                    String finalGrade = "N/A";

                    if (gradeObject.has("moed_3_zion") && !gradeObject.isNull("moed_3_zion"))
                        finalGrade = gradeObject.getString("moed_3_zion");
                    else if (gradeObject.has("moed_2_zion") && !gradeObject.isNull("moed_2_zion"))
                        finalGrade = gradeObject.getString("moed_2_zion");
                    else if (gradeObject.has("moed_1_zion") && !gradeObject.isNull("moed_1_zion"))
                        finalGrade = gradeObject.getString("moed_1_zion");
                    else if (gradeObject.has("bhnzin") && !gradeObject.isNull("bhnzin") && isNumeric(gradeObject.getString("bhnzin")))
                        finalGrade = gradeObject.getString("bhnzin");

                    if (!finalGrade.equals("N/A"))
                        gradeList.add(new Grade("בר אילן", subject, finalGrade));
                }

                Log.d(TAG, "Grades fetched successfully: " + gradeList.size() + " records");
            } catch (Exception e) {
                Log.e(TAG, "Error fetching grades", e);
            }

            callback.onGradesFetched(gradeList);
        });
    }

    public interface GradesCallback {
        void onGradesFetched(List<Grade> grades);
    }
}
