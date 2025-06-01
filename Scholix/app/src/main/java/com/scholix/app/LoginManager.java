package com.scholix.app;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class LoginManager {

    // Use the unsafe OkHttp client for testing SSL issues.
    // (Remember: this bypasses certificate validation and should not be used in production)
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    /**
     * Validates login credentials by sending a POST request.
     *
     * @param username The username.
     * @param password The password.
     * @return A LoginResult containing the result and details or error message.
     * @throws IOException   if a network error occurs.
     * @throws JSONException if JSON parsing fails.
     */
    public LoginResult validateLogin(String username, String password) throws IOException, JSONException {
        // Endpoint for login
        String loginUrl = "https://webtopserver.smartschool.co.il/server/api/user/LoginByUserNameAndPassword";
        // Encrypt the username with our updated Base64 encryption
        String encryptedData = encryptStringToServer(username + "0");

        // Prepare the JSON request
        JSONObject loginData = new JSONObject();
        loginData.put("Data", encryptedData);
        loginData.put("UserName", username);
        loginData.put("Password", password);
        loginData.put("deviceDataJson", "{\"isMobile\":true,\"isTablet\":false,\"isDesktop\":false," +
                "\"getDeviceType\":\"Mobile\",\"os\":\"Android\",\"osVersion\":\"6.0\"," +
                "\"browser\":\"Chrome\",\"browserVersion\":\"122.0.0.0\",\"browserMajorVersion\":122," +
                "\"screen_resolution\":\"1232 x 840\",\"cookies\":true," +
                "\"userAgent\":\"Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36\"}");

        RequestBody requestBody = RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(loginUrl)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        JSONObject jsonResponse = new JSONObject(responseBody);

        // Check if the response contains an error or a null data field
        if (jsonResponse.isNull("data")) {
            // Retrieve the error description from the response; if not available, set a default error message.
            String errorMsg = jsonResponse.optString("errorDescription", "Unknown error occurred");
            // Print the error to the console/log
            return new LoginResult(false, errorMsg, null);
        }

        // Extract cookies and user data from the response
        List<String> cookies = response.headers("Set-Cookie");
        JSONObject dataObj = jsonResponse.getJSONObject("data");
        String studentId = dataObj.getString("userId");
        String classCode = dataObj.getString("classCode") + "|" + dataObj.get("classNumber").toString();
        String institution = dataObj.getString("institutionCode");
        String name = dataObj.getString("firstName") + " " + dataObj.getString("lastName");

        return new LoginResult(true, "fine", new Object[]{cookies, studentId, dataObj, classCode, institution, name});
    }

    /**
     * Updated encryption method that encodes the input as Base64.
     */
    private String encryptStringToServer(String data, String smartKey) {
        try {
            int keySize = 256;              // bits
            int iterations = 100;
            int saltLength = 16;            // 16 bytes
            int ivLength = 16;              // 16 bytes

            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[saltLength];
            random.nextBytes(salt);

            // Derive key using PBKDF2 with HMAC-SHA1
            PBEKeySpec spec = new PBEKeySpec(smartKey.toCharArray(), salt, iterations, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            // Generate random IV
            byte[] iv = new byte[ivLength];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Encrypt data using AES/CBC/PKCS5Padding
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Concatenate salt + iv + ciphertext
            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Overloaded method using the default smartKey value.
    private String encryptStringToServer(String data) {
        String defaultSmartKey = "01234567890000000150778345678901";
        return encryptStringToServer(data, defaultSmartKey);
    }

    /**
     * LoginResult class to encapsulate login outcomes.
     */
    public static class LoginResult {
        public boolean success;
        public String message;
        public Object details;

        public LoginResult(boolean success, String message, Object details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }
    }
}
