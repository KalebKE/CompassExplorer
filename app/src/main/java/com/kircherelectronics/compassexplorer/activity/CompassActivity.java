package com.kircherelectronics.compassexplorer.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.compassexplorer.R;
import com.kircherelectronics.compassexplorer.datalogger.DataLoggerManager;
import com.kircherelectronics.compassexplorer.gauge.GaugeBearing;
import com.kircherelectronics.compassexplorer.gauge.GaugeRotation;
import com.kircherelectronics.compassexplorer.preferences.HardIronPreferences;
import com.kircherelectronics.compassexplorer.view.VectorDrawableButton;
import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;
import com.kircherelectronics.fsensor.filter.fusion.OrientationComplimentaryFusion;
import com.kircherelectronics.fsensor.filter.fusion.OrientationFusion;
import com.kircherelectronics.fsensor.filter.fusion.OrientationKalmanFusion;
import com.kircherelectronics.fsensor.util.magnetic.AzimuthUtil;
import com.kircherelectronics.fsensor.util.magnetic.tilt.TiltCompensationUtil;
import com.kircherelectronics.fsensor.util.offset.Calibration;
import com.kircherelectronics.fsensor.util.offset.CalibrationUtil;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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
 * The main activity displays the orientation estimated by the sensor(s) and
 * provides an interface for the user to modify settings, reset or view help.
 *
 * @author Kaleb
 */
public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    private static final String tag = CompassActivity.class.getSimpleName();

    private final static int WRITE_EXTERNAL_STORAGE_REQUEST = 1000;

    // Indicate if the output should be logged to a .csv file
    private boolean logData = false;

    private boolean meanFilterEnabled;
    private boolean kalmanQuaternionEnabled;
    private boolean tiltCompensated = true;
    private boolean calibrated = true;

    private float[] fusedOrientation = new float[3];
    private float[] acceleration = new float[4];
    private float[] magnetic = new float[3];
    private float[] rotation = new float[3];

    private float azimuth;

    private Calibration calibration;

    // The gauge views. Note that these are views and UI hogs since they run in
    // the UI thread, not ideal, but easy to use.
    private GaugeBearing gaugeBearingCalibrated;
    private GaugeRotation gaugeTiltCalibrated;

    // Handler for the UI plots so everything plots smoothly
    protected Handler handler;

    protected Runnable runable;

    private TextView textViewHeading;

    private OrientationFusion orientationFusion;
    private MeanFilter meanFilter;

    private SensorManager sensorManager;

    private DataLoggerManager dataLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_compass);
        dataLogger = new DataLoggerManager(this);
        meanFilter = new MeanFilter();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        initUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gyroscope, menu);
        return true;
    }

    /**
     * Event Handling for Individual menu item selected Identify single menu
     * item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final Intent intent = new Intent();

        switch (item.getItemId()) {
            case R.id.action_calibrate:
                intent.setClass(this, CalibrationActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_reset:
                orientationFusion.reset();
                return true;
            case R.id.action_config:
                intent.setClass(this, ConfigActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_help:
                showHelpDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResume() {
        super.onResume();
        readPrefs();
        reset();

        calibration = getCalibrationFromPrefs();

        // Register for sensor updates.
        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        handler.post(runable);
    }

    public void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
        handler.removeCallbacks(runable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
            orientationFusion.setAcceleration(acceleration);
        } else  if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, magnetic, 0, event.values.length);
            orientationFusion.setMagneticField(this.magnetic);

            if(calibrated) {
                magnetic = CalibrationUtil.calibrate(magnetic, calibration);
            }

            if(tiltCompensated) {
                float[] output = TiltCompensationUtil.compensateTilt(new float[]{magnetic[0], -magnetic[1], magnetic[2]}, new float[]{fusedOrientation[1], fusedOrientation[2], 0});
                output[1] = -output[1];
                azimuth = AzimuthUtil.getAzimuth(output);
            } else {
                azimuth = AzimuthUtil.getAzimuth(magnetic);
            }

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, rotation, 0, event.values.length);
            // Filter the rotation
            fusedOrientation = orientationFusion.filter(this.rotation);

            if(meanFilterEnabled) {
                fusedOrientation = meanFilter.filter(fusedOrientation);
            }

            dataLogger.setRotation(fusedOrientation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }

    private boolean getPrefMeanFilterEnabled() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return prefs.getBoolean(ConfigActivity.MEAN_FILTER_SMOOTHING_ENABLED_KEY,
                false);
    }

    private float getPrefMeanFilterTimeConstant() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return prefs.getFloat(ConfigActivity.MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY, 0.5f);
    }

    private boolean getPrefKalmanQuaternionEnabled() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return prefs.getBoolean(ConfigActivity.KALMAN_QUATERNION_ENABLED_KEY,
                false);
    }

    private float getPrefComplimentaryQuaternionCoeff() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return Float.valueOf(prefs.getString(
                ConfigActivity.COMPLIMENTARY_QUATERNION_COEFF_KEY, "0.5"));
    }

    private void initStartButton() {
        final VectorDrawableButton button = findViewById(R.id.button_start);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!logData) {
                    button.setText("Stop Log");
                    startDataLog();
                } else {
                    button.setText("Start Log");
                    stopDataLog();
                }
            }
        });
    }

    /**
     * Initialize the UI.
     */
    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        textViewHeading = this.findViewById(R.id.value_heading);

        // Initialize the calibrated gauges views
        gaugeBearingCalibrated = findViewById(R.id.gauge_bearing_calibrated);
        gaugeTiltCalibrated = findViewById(R.id.gauge_tilt_calibrated);

        initStartButton();
    }

    private void reset() {

        if (kalmanQuaternionEnabled) {
            orientationFusion = new OrientationKalmanFusion();
        } else {
            orientationFusion = new OrientationComplimentaryFusion();
            orientationFusion.setTimeConstant(getPrefComplimentaryQuaternionCoeff());
        }

        handler = new Handler();

        runable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 100);
                updateText();
                updateGauges();
            }
        };
    }

    private void readPrefs() {
        meanFilterEnabled = getPrefMeanFilterEnabled();
        kalmanQuaternionEnabled = getPrefKalmanQuaternionEnabled();

        if(meanFilterEnabled) {
            meanFilter.setTimeConstant(getPrefMeanFilterTimeConstant());
        }
    }

    private void showHelpDialog() {
        Dialog helpDialog = new Dialog(this);

        helpDialog.setCancelable(true);
        helpDialog.setCanceledOnTouchOutside(true);
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = getLayoutInflater()
                .inflate(R.layout.layout_help_home, null);

        helpDialog.setContentView(view);

        helpDialog.show();
    }

    private void startDataLog() {
        logData = true;
        dataLogger.startDataLog();
    }

    private void stopDataLog() {
        logData = false;
        String path = dataLogger.stopDataLog();
        Toast.makeText(this, "File Written to: " + path, Toast.LENGTH_SHORT).show();
    }

    private void updateText() {
        textViewHeading.setText(String.format("%.2f", azimuth) + "°");
    }

    private void updateGauges() {
        gaugeBearingCalibrated.updateBearing(fusedOrientation[0]);
        gaugeTiltCalibrated.updateRotation(fusedOrientation);
    }

    private Calibration getCalibrationFromPrefs() {

        SharedPreferences prefs = getSharedPreferences(HardIronPreferences.KEY, Activity.MODE_PRIVATE);
        float hardIronAlpha = prefs.getFloat(HardIronPreferences.HARD_IRON_ALPHA, 0);
        float hardIronBeta = prefs.getFloat(HardIronPreferences.HARD_IRON_BETA, 0);
        float hardIronGamma = prefs.getFloat(HardIronPreferences.HARD_IRON_GAMMA, 0);

        RealVector offset = new ArrayRealVector(3);

        offset.setEntry(0, hardIronAlpha);
        offset.setEntry(1, hardIronBeta);
        offset.setEntry(2, hardIronGamma);

        float softIronAlpha = prefs.getFloat(HardIronPreferences.SOFT_IRON_ALPHA, 1);
        float softIronBeta = prefs.getFloat(HardIronPreferences.SOFT_IRON_BETA, 1);
        float softIronGamma = prefs.getFloat(HardIronPreferences.SOFT_IRON_GAMMA, 1);

        RealMatrix scalar = new Array2DRowRealMatrix(3, 3);
        scalar.setEntry(0, 0, softIronAlpha);
        scalar.setEntry(1, 1, softIronBeta);
        scalar.setEntry(2, 2, softIronGamma);

        return new Calibration(scalar, offset);
    }

}
