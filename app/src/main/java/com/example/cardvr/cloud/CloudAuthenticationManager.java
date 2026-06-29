package com.example.cardvr.cloud;

import android.accounts.Account;
import android.content.Context;

import androidx.annotation.Nullable;

import com.example.cardvr.database.CloudAccountEntity;
import com.example.cardvr.database.CloudDestination;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.security.MessageDigest;

public final class CloudAuthenticationManager {
    public static final String DRIVE_SCOPE =
            "oauth2:https://www.googleapis.com/auth/drive.file";

    private final Context context;
    private final CloudUploadRepository repository;

    public CloudAuthenticationManager(Context context) {
        this.context = context.getApplicationContext();
        repository = new CloudUploadRepository(context);
    }

    public void connect(String accountName) {
        long now = System.currentTimeMillis();
        CloudAccountEntity account = repository.getAccount();
        if (account == null) account = new CloudAccountEntity();
        account.provider = CloudDestination.GOOGLE_DRIVE;
        account.accountName = accountName;
        account.accountHash = sha256(accountName);
        account.connected = true;
        account.updatedAt = now;
        if (account.createdAt == 0L) account.createdAt = now;
        account.lastAuthError = null;
        repository.saveAccount(account);
    }

    public void disconnect() {
        CloudAccountEntity account = repository.getAccount();
        if (account == null) return;
        account.connected = false;
        account.updatedAt = System.currentTimeMillis();
        repository.updateAccount(account);
    }

    public boolean isConnected() {
        CloudAccountEntity account = repository.getAccount();
        return account != null && account.connected && account.accountName != null;
    }

    @Nullable
    public String accountName() {
        CloudAccountEntity account = repository.getAccount();
        return account == null ? null : account.accountName;
    }

    public String getAccessToken() throws Exception {
        String name = accountName();
        if (name == null || name.trim().isEmpty()) {
            throw new UserRecoverableAuthException("Google Drive не подключён", null);
        }
        return GoogleAuthUtil.getToken(context, new Account(name, "com.google"), DRIVE_SCOPE);
    }

    public void invalidateToken(String token) {
        // Do not call GoogleAuthUtil.invalidateToken here: on modern Android lint
        // flags it as requiring account-management privileges unavailable to normal apps.
        // A failed token is simply discarded and a fresh token is requested next time.
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
