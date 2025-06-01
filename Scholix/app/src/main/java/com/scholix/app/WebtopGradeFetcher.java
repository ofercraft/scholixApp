package com.scholix.app;

import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class WebtopGradeFetcher {
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    private String cookies;
    private String studentId;
    private int year;

    // Constructor now accepts year
    public WebtopGradeFetcher(String cookies, String studentId, int year) {
        this.cookies = cookies;
        this.studentId = studentId;
        this.year = year;
    }

    public List<Grade> fetchGrades(String period) {
        List<Grade> gradeList = new ArrayList<>();

        try {
            if (!period.equals("a") && !period.equals("b") && !period.equals("ab")) {
                throw new IllegalArgumentException("Period must be 'a', 'b', or 'ab'");
            }

            int periodId = period.equals("a") ? 1103 : period.equals("b") ? 1102 : 0;
            int studyYear = (year > 0) ? year : java.time.LocalDate.now().getYear();

            if (studentId.isEmpty()) {
                Log.e("GradeFetcher", "Student ID is empty.");
                return gradeList;
            }

            // Build request JSON
            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("studyYear", studyYear);
            requestBodyJson.put("moduleID", 1);
            requestBodyJson.put("periodID", periodId);
            requestBodyJson.put("studentID", studentId);

            RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://webtopserver.smartschool.co.il/server/api/PupilCard/GetPupilGrades")
                    .addHeader("Cookie", cookies)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e("GradeFetcher", "API request failed: " + response.code());
                return gradeList;
            }

            // Parse response
            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray gradesArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < gradesArray.length(); i++) {
                JSONObject gradeObject = gradesArray.getJSONObject(i);
                String subject = gradeObject.optString("subject", "Unknown Subject");
                String title = gradeObject.optString("title", "Untitled");
                String grade = gradeObject.optString("grade", "N/A");

                if (!grade.equals("N/A") && !grade.equals("null")) {
                    gradeList.add(new Grade(subject, title, grade));
                }
            }

            Log.d("GradeFetcher", "Grades fetched: " + gradeList.size());

        } catch (Exception e) {
            Log.e("GradeFetcher", "Error fetching grades", e);
        }

        return gradeList;
    }
    public int getAverage(List<Grade> grades) {

        int total = 0;
        int count = 0;

        for (Grade g : grades) {
            try {
                int num = Integer.parseInt(g.getGrade().trim());
                total += num;
                count++;
            } catch (NumberFormatException e) {
                // skip non-numeric grades
            }
        }
        return (count > 0) ? Math.round((float) total / count) : 0;
    }
}
