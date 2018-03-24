package com.kircherelectronics.compassexplorer.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.kircherelectronics.compassexplorer.R;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Configuration activity.
 */
public class ConfigActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final String COMPLIMENTARY_QUATERNION_ENABLED_KEY = "imuocf_quaternion_enabled_preference";
    public static final String COMPLIMENTARY_QUATERNION_COEFF_KEY = "imuocf_quaternion_coeff_preference";

    public static final String KALMAN_QUATERNION_ENABLED_KEY = "imuokf_quaternion_enabled_preference";

    public static final String MEAN_FILTER_SMOOTHING_ENABLED_KEY = "mean_filter_smoothing_enabled_preference";
    public static final String MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY = "mean_filter_smoothing_time_constant_preference";

    private SwitchPreference spComplimentaryQuaternionEnabled;
    private SwitchPreference spKalmanQuaternionEnabled;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/*
         * Read preferences resources available at res/xml/preferences.xml
		 */
        addPreferencesFromResource(R.xml.preferences);

        spComplimentaryQuaternionEnabled = (SwitchPreference) findPreference(COMPLIMENTARY_QUATERNION_ENABLED_KEY);

        spKalmanQuaternionEnabled = (SwitchPreference) findPreference(KALMAN_QUATERNION_ENABLED_KEY);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(COMPLIMENTARY_QUATERNION_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();
                edit.putBoolean(KALMAN_QUATERNION_ENABLED_KEY, false);
                edit.apply();
                spKalmanQuaternionEnabled.setChecked(false);
            }
        }

        if (key.equals(KALMAN_QUATERNION_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();
                edit.putBoolean(COMPLIMENTARY_QUATERNION_ENABLED_KEY, false);
                edit.apply();
                spComplimentaryQuaternionEnabled.setChecked(false);
            }
        }

        if (key.equals(COMPLIMENTARY_QUATERNION_COEFF_KEY)) {
            if (Double.valueOf(sharedPreferences.getString(key, "0.5")) > 1) {
                sharedPreferences.edit().putString(key, "0.5").apply();
                ((EditTextPreference) findPreference(COMPLIMENTARY_QUATERNION_COEFF_KEY)).setText("0.5");
                Toast.makeText(getApplicationContext(), "The filter constant must be less than or equal to 1", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }
}