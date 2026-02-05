/*
 * Copyright (C) 2026 The Waydroid-ATV project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.settings.system;

import android.app.ActivityManager;
import android.app.tvsettings.TvSettingsEnums;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Map;
import java.util.stream.Collectors;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Keep
public class WaydroidOptionsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "WaydroidOptionsFragment";
    private static final String CONFIG_FILE = "/data/misc/waydroid_settings";

    private static final String KEY_CURRENT_NATIVE_BRIDGE = "current_native_bridge";
    private static final String KEY_NATIVE_BRIDGE = "native_bridge";
    private static final String KEY_CURRENT_GRALLOC_IMPL = "current_gralloc_impl";
    private static final String KEY_GRALLOC_IMPL = "gralloc_impl";
    private static final String KEY_CURRENT_CODEC2_IMPL = "current_codec2_impl";
    private static final String KEY_CODEC2_IMPL = "codec2_impl";
    private static final String KEY_DEVICE_SPOOF = "device_spoof";
    private static final String KEY_HWACCEL = "hwaccel";
    private static final String KEY_HWACCEL_DRM_PRIME = "hwaccel_drm_prime";
    private static final String KEY_HWACCEL_FORCE_HWACCEL = "hwaccel_force_hwaccel";
    private static final String KEY_FORWARD_NOTIFICATIONS = "forward_notifications";

    private static final String PROPERTY_NATIVE_BRIDGE = "ro.dalvik.vm.native.bridge";
    private static final String PROPERTY_GRALLOC = "ro.hardware.gralloc";
    private static final String PROPERTY_DEVICE_SPOOF = "ro.waydroid.device-spoof";
    private static final String PROPERTY_CODEC2_IMPL = "ro.waydroid.codec2-impl";
    private static final String PROPERTY_HWACCEL = "media.sf.hwaccel";
    private static final String PROPERTY_HWACCEL_CODECS = "ro.waydroid.hwcodecs";
    private static final String PROPERTY_HWACCEL_DRM_PRIME = "debug.ffmpeg-codec2.hwaccel.drm";
    private static final String PROPERTY_HWACCEL_FORCE_HWACCEL = "debug.ffmpeg-codec2.hwaccel.force";
    private static final String PROPERTY_FORWARD_NOTIFICATIONS = "persist.waydroid.forward_notifications";

    private HashMap<String, String> waydroidConfig = new HashMap<String, String>();

    private Preference mCurrentNativeBridge;
    private ListPreference mNativeBridge;
    private Preference mCurrentGrallocImpl;
    private ListPreference mGrallocImpl;
    private ListPreference mDeviceSpoof;
    private Preference mCurrentCodec2Impl;
    private ListPreference mCodec2Impl;
    private TwoStatePreference mHwaccel;
    private TwoStatePreference mHwaccelDrmPrime;
    private TwoStatePreference mHwaccelForceHwaccel;
    private TwoStatePreference mForwardNotifications;

    private void setListPrefValue(ListPreference pref, String value) {
        CharSequence[] values = pref.getEntryValues();

        for (int i = 0; i < values.length; i++) {
            if (values[i].toString().equals(value)) {
                pref.setValueIndex(i);
                return;
            }
        }
    }

    private String getHwCodecList() {
        String hwcodecs_prop = SystemProperties.get(PROPERTY_HWACCEL_CODECS, "");
        ArrayList<String> hwcodecs = new ArrayList<String>();

        if (hwcodecs_prop.contains("MPG2D") || hwcodecs_prop.contains("MG2SD")) hwcodecs.add("MPEG-2");
        if (hwcodecs_prop.contains("MPG4D")) hwcodecs.add("MPEG-4");
        if (hwcodecs_prop.contains("H263D")) hwcodecs.add("H.263");
        if (hwcodecs_prop.contains("H264D") || hwcodecs_prop.contains("S264D")) hwcodecs.add("H.264");
        if (hwcodecs_prop.contains("HEVCD") || hwcodecs_prop.contains("S265D")) hwcodecs.add("H.265");
        if (hwcodecs_prop.contains("VP80D") || hwcodecs_prop.contains("VP80D")) hwcodecs.add("VP8");
        if (hwcodecs_prop.contains("VP90D") || hwcodecs_prop.contains("VP9FD")) hwcodecs.add("VP9");
        if (hwcodecs_prop.contains("AV10D") || hwcodecs_prop.contains("AV1FD")) hwcodecs.add("AV1");

        return String.join(" ", hwcodecs);
    }

    private void initPreferenceVariables() {
        mCurrentNativeBridge = findPreference(KEY_CURRENT_NATIVE_BRIDGE);
        mNativeBridge = (ListPreference) findPreference(KEY_NATIVE_BRIDGE);

        mCurrentGrallocImpl = findPreference(KEY_CURRENT_GRALLOC_IMPL);
        mGrallocImpl = (ListPreference) findPreference(KEY_GRALLOC_IMPL);

        mDeviceSpoof = (ListPreference) findPreference(KEY_DEVICE_SPOOF);

        mCurrentCodec2Impl = findPreference(KEY_CURRENT_CODEC2_IMPL);
        mCodec2Impl = (ListPreference) findPreference(KEY_CODEC2_IMPL);
        mHwaccel = (TwoStatePreference) findPreference(KEY_HWACCEL);
        mHwaccelDrmPrime = (TwoStatePreference) findPreference(KEY_HWACCEL_DRM_PRIME);
        mHwaccelForceHwaccel = (TwoStatePreference) findPreference(KEY_HWACCEL_FORCE_HWACCEL);

        mForwardNotifications = (TwoStatePreference) findPreference(KEY_FORWARD_NOTIFICATIONS);

        mNativeBridge.setOnPreferenceChangeListener(this);
        mGrallocImpl.setOnPreferenceChangeListener(this);
        mDeviceSpoof.setOnPreferenceChangeListener(this);
        mCodec2Impl.setOnPreferenceChangeListener(this);

        mCurrentNativeBridge.setSummary(getSystemPropertySummary(PROPERTY_NATIVE_BRIDGE));
        mCurrentGrallocImpl.setSummary(getSystemPropertySummary(PROPERTY_GRALLOC));
        mCurrentCodec2Impl.setSummary(getSystemPropertySummary(PROPERTY_CODEC2_IMPL));

        mHwaccelForceHwaccel.setSummary(
            mHwaccelForceHwaccel.getSummary() + "\n\n" +
            getResources().getString(R.string.hwaccel_supported_codecs) + ": " +
            getHwCodecList()
        );

        setListPrefValue(mNativeBridge, waydroidConfig.getOrDefault(PROPERTY_NATIVE_BRIDGE, "libhoudini.so"));
        setListPrefValue(mGrallocImpl, waydroidConfig.getOrDefault(PROPERTY_GRALLOC, ""));
        setListPrefValue(mDeviceSpoof, waydroidConfig.getOrDefault(PROPERTY_DEVICE_SPOOF, ""));
        setListPrefValue(mCodec2Impl, waydroidConfig.getOrDefault(PROPERTY_CODEC2_IMPL, ""));

        mHwaccel.setChecked(getBooleanConfig(PROPERTY_HWACCEL, true));
        mHwaccelDrmPrime.setChecked(getBooleanConfig(PROPERTY_HWACCEL_DRM_PRIME, false));
        mHwaccelForceHwaccel.setChecked(getBooleanConfig(PROPERTY_HWACCEL_FORCE_HWACCEL, false));
        mForwardNotifications.setChecked(getBooleanConfig(PROPERTY_FORWARD_NOTIFICATIONS, false));
    }

    private String getSystemPropertySummary(String property) {
        return SystemProperties.get(property,
                getResources().getString(R.string.device_info_default));
    }

    private void loadConfig() {
        try {
            for (String line : Files.readAllLines(Paths.get(CONFIG_FILE))) {
                String[] keypair = line.split("=", 2);
                waydroidConfig.put(keypair[0], keypair[1]);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read config file: " + e.getMessage());
        }
    }

    private void updateConfig() {
        try {
            String content = waydroidConfig.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n")) + "\n";

            Files.write(Paths.get(CONFIG_FILE), content.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Failed to update config file: " + e.getMessage());
        }
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        return waydroidConfig.getOrDefault(key, SystemProperties.get(key, defaultValue ? "1" : "0")).equals("1");
    }

    private void setConfig(String key, String value) {
        if (value.isEmpty()) {
            waydroidConfig.remove(key);
        } else if (value.equals("_")) {
            waydroidConfig.put(key, "");
        } else if (!key.startsWith("persist.")) {
            waydroidConfig.put(key, value);
        }

        if (!key.startsWith("ro.")) {
            // sync property with system
            SystemProperties.set(key, value);
        }

        updateConfig();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.waydroid_options, null);
        loadConfig();
        initPreferenceVariables();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (ActivityManager.isUserAMonkey()) {
            return false;
        }

        if (preference == mHwaccel) {
            setConfig(PROPERTY_HWACCEL, mHwaccel.isChecked() ? "1" : "0");
        } else if (preference == mHwaccelDrmPrime) {
            setConfig(PROPERTY_HWACCEL_DRM_PRIME, mHwaccelDrmPrime.isChecked() ? "1" : "0");
        } else if (preference == mHwaccelForceHwaccel) {
            setConfig(PROPERTY_HWACCEL_FORCE_HWACCEL, mHwaccelForceHwaccel.isChecked() ? "1" : "0");
        } else if (preference == mForwardNotifications) {
            setConfig(PROPERTY_FORWARD_NOTIFICATIONS, mForwardNotifications.isChecked() ? "1" : "0");
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNativeBridge) {
            setConfig(PROPERTY_NATIVE_BRIDGE, newValue.toString());
        } else if (preference == mGrallocImpl) {
            setConfig(PROPERTY_GRALLOC, newValue.toString());
        } else if (preference == mDeviceSpoof) {
            setConfig(PROPERTY_DEVICE_SPOOF, newValue.toString());
        } else if (preference == mCodec2Impl) {
            setConfig(PROPERTY_CODEC2_IMPL, newValue.toString());
        }

        return true;
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.SYSTEM;
    }
}
