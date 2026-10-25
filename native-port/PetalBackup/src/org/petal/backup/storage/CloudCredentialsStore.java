/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.storage;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONException;
import org.json.JSONObject;

public final class CloudCredentialsStore {
    private static final String ALIAS = "org.petal.backup.cloud.v1";
    private final Context mContext;

    public CloudCredentialsStore(Context context) {
        mContext = context.getApplicationContext();
    }

    private static synchronized SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    public void save(String provider, JSONObject config)
            throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        cipher.updateAAD(provider.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(config.toString().getBytes(StandardCharsets.UTF_8));
        String value = "v1:" + Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        if (!mContext.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE).edit()
                .putString(provider, value).commit()) throw new IOException("Connection save failed");
    }

    public JSONObject load(String provider)
            throws GeneralSecurityException, IOException, JSONException {
        String value = mContext.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE)
                .getString(provider, null);
        if (value == null) return new JSONObject();
        String[] parts = value.split(":", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) throw new IOException("Unknown connection format");
        byte[] iv;
        byte[] encrypted;
        try {
            iv = Base64.decode(parts[1], Base64.NO_WRAP);
            encrypted = Base64.decode(parts[2], Base64.NO_WRAP);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid connection format", exception);
        }
        if (iv.length != 12 || encrypted.length < 16) throw new IOException("Invalid connection data");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        cipher.updateAAD(provider.getBytes(StandardCharsets.UTF_8));
        return new JSONObject(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
    }
}
