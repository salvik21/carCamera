package com.example.cardvr.cloud;

import androidx.annotation.Nullable;

import com.example.cardvr.database.CloudUploadTaskEntity;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class GoogleDriveManager {
    private static final String DRIVE_FILES =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private final CloudAuthenticationManager auth;

    public GoogleDriveManager(CloudAuthenticationManager auth) {
        this.auth = auth;
    }

    public UploadResult upload(CloudUploadTaskEntity task, String checksum,
                               @Nullable String folderId) throws Exception {
        String token = auth.getAccessToken();
        File file = task.localPath == null ? null : new File(task.localPath);
        if (file == null || !file.isFile()) throw new java.io.FileNotFoundException(task.localPath);
        String boundary = "dashcam_" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(DRIVE_FILES).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        JSONObject metadata = new JSONObject();
        metadata.put("name", file.getName());
        if (folderId != null) metadata.put("parents", new org.json.JSONArray().put(folderId));
        try (OutputStream out = connection.getOutputStream();
             BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Type: application/json; charset=UTF-8\r\n\r\n");
            write(out, metadata.toString());
            write(out, "\r\n--" + boundary + "\r\n");
            write(out, "Content-Type: " + contentType(file) + "\r\n\r\n");
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            write(out, "\r\n--" + boundary + "--\r\n");
        }
        int code = connection.getResponseCode();
        if (code == 401 || code == 403) {
            auth.invalidateToken(token);
            throw new SecurityException("Google Drive authorization required: HTTP " + code);
        }
        if (code < 200 || code >= 300) {
            throw new java.io.IOException("Google Drive upload failed: HTTP " + code);
        }
        String body = readFully(connection.getInputStream());
        JSONObject response = new JSONObject(body);
        return new UploadResult(response.optString("id"), checksum);
    }

    private static String readFully(InputStream inputStream) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }

    private static void write(OutputStream out, String value) throws java.io.IOException {
        out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String contentType(File file) {
        return file.getName().endsWith(".mp4") ? "video/mp4" : "application/json";
    }

    public static final class UploadResult {
        public final String remoteFileId;
        public final String checksum;

        UploadResult(String remoteFileId, String checksum) {
            this.remoteFileId = remoteFileId;
            this.checksum = checksum;
        }
    }
}
