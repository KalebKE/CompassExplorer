package com.kircherelectronics.compassexplorer.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.compassexplorer.R;
import com.kircherelectronics.compassexplorer.gauge.GaugeCalibration;
import com.kircherelectronics.compassexplorer.preferences.HardIronPreferences;
import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;
import com.kircherelectronics.fsensor.filter.fusion.OrientationComplimentaryFusion;
import com.kircherelectronics.fsensor.filter.fusion.OrientationFusion;
import com.kircherelectronics.fsensor.util.magnetic.tilt.TiltCompensationUtil;
import com.kircherelectronics.fsensor.util.offset.Calibration;
import com.kircherelectronics.fsensor.util.offset.CalibrationUtil;
import com.kircherelectronics.fsensor.util.offset.FitPoints;
import com.kircherelectronics.fsensor.util.offset.ThreeSpacePoint;

import java.util.ArrayList;

/**
 * Hard Iron Field Calibrator will compensate for constant, local magnetic
 * fields that are caused by hard iron once it has been calibrated. Hard-iron
 * corrections are determined by rotating the sensor through a minimum of 360�,
 * then determining the distance from (0, 0) to the center of the circle by
 * identifying the average of the maximum and minimum values for each of the
 * axes.These offsets are then subtracted from the raw x and y magnetometer
 * data, thus largely eliminating the hard-iron distortion.
 * <p>
 * Hard-iron effects are constant regardless of orientation or position of the
 * sensing platform. These constant offsets can be stored once calculated and
 * subtracted from the raw magnetometer data.
 * <p>
 * Each new location needs to be calibrated. Moving the device just a few inches
 * is enough to change the hard iron generated magnetic fields in some cases.
 * <p>
 * The compass uses a three-axis accelerometer and three-axis magnetometer. The
 * accelerometer measures the components of the earth's gravity and the
 * magnetometer measures the components of earth's magnetic field (the
 * geomagnetic field). Since both the accelerometer and magnetometer are fixed
 * on the Android device, their readings change according to the orientation of
 * the Android device. If the Android device remains flat, then the compass
 * heading could be computed from the arctangent of the ratio of the two
 * horizontal magnetic field components. Since, in general, the Android device
 * will have an arbitrary orientation, the compass heading is a function of all
 * three accelerometer readings and all three magnetometer readings. It is
 * possible for a smartphone eCompass to be calibrated by the owner in the
 * street with no a priori knowledge of location or the direction of magnetic
 * north.A four parameter calibration, comprising the three hard-iron offsets
 * plus the geomagnetic-field strength, may be sufficient for Android device
 * without strong soft-iron interference.
 * <p>
 * The hard-iron-calibration is intended to be the first in a chain of three
 * calibrations. It is intended to be followed by the tilt-compensation
 * calibration which is then followed by the soft-iron calibration (if
 * required). Note that the calibration only needs to be run once per vehicle as
 * long as the calibration is saved to the shared preferences. The hard-iron
 * calibration model is only required to calibrate the algorithm and at no other
 * time. The hard-iron calibration should be accessed by other classes through
 * the shared preferences.
 *
 * @author Kaleb
 * @version %I%, %G%
 */
public class CalibrationActivity extends AppCompatActivity implements OnClickListener, SensorEventListener {

	/*
     * Developer Note: Considerations for frames of orientation... The tilt
	 * compensation algorithm uses the industry standard �NED� (North, East,
	 * Down) coordinate system to label axes on the mobile phone. The x-axis of
	 * the phone is the Android device pointing direction, the y-axis points to
	 * the right and the z-axis points downward. A positive yaw angle is defined
	 * to be a clockwise rotation about the positive z-axis. Similarly, a
	 * positive pitch angle and positive roll angle are defined as clockwise
	 * rotations about the positive y- and positive x-axes respectively. It is
	 * crucial that the accelerometer and magnetometer outputs are aligned with
	 * the phone coordinate system. The Android orientation is NOT the same as
	 * the NED orientation! Different Android devices may have different
	 * orientations of the accelerometer and magnetometer packages and even the
	 * same Android device may be mounted in different orientations within the
	 * final product.
	 * 
	 * To verify your device is oriented to NED: Once the rotations and
	 * reflections are applied in software, a final check should be made while
	 * watching the raw accelerometer and magnetometer data from the Android
	 * device:
	 * 
	 * 1. Place the Android device flat on the table. The z-axis accelerometer
	 * should read +1g and the x and y axes negligible values. Invert the
	 * Android device so that the z-axis points upwards and verify that the
	 * z-axis accelerometer now indicates -1g. Repeat with the y-axis pointing
	 * downwards and then upwards to check that the y-axis reports 1g and then
	 * reports -1g. Repeat once more with the x-axis pointing downwards and then
	 * upwards to check that the x-axis reports 1g and then -1g.
	 * 
	 * 2. The horizontal component of the geomagnetic field always points to the
	 * magnetic north pole. In the northern hemisphere, the vertical component
	 * also points downward with the precise angle being dependent on location.
	 * When the Android device x-axis is pointed northward and downward, it
	 * should be possible to find a maximum value of the measured x component of
	 * the magnetic field. It should also be possible to find a minimum value
	 * when the Android device is aligned in the reverse direction. Repeat the
	 * measurements with the Android device y- and z-axes aligned first with,
	 * and then against, the geomagnetic field which should result in maximum
	 * and minimum readings in the y- and then z-axes.
	 */

	/*
     * Developer Note: Considerations for the sensor values...The acceleration
	 * due to gravity is g = 9.81 ms-2. B is the geomagnetic field strength
	 * which varies over the earth's surface from a minimum of 22 muT over South
	 * America to a maximum of 67muT south of Australia. Delta is the angle of
	 * inclination of the geomagnetic field measured downwards from horizontal
	 * and varies over the earth's surface from -90 degrees at the south
	 * magnetic pole, through zero near the equator to +90 degress at the north
	 * magnetic pole. Detailed geomagnetic field maps are available from the
	 * World Data Center for Geomagnetism at
	 * http://wdc.kugi.kyoto-u.ac.jp/igrf/. There is no requirement to know the
	 * details of the geomagnetic field strength nor inclination angle in order
	 * for the eCompass software to function since these cancel in the angle
	 * calculations
	 */

    private static final String tag = CalibrationActivity.class
            .getSimpleName();

    private final static int DELTA = 1;

    // Note: the offsets need to be calibrated to each new location.
    // The x-axis max for the hard iron offset calculation.
    private ArrayList<ThreeSpacePoint> threeSpacePointList;

    private GaugeCalibration gaugeCalibration;

    // Indicate the algorithm is currently calibrating the hard iron offset.
    private boolean hardIronCalibrating = false;
    // Indicate the instance has permission to write to the shared prefs.
    private boolean write = true;

    private float x = 0.0f;
    private float xOld = 0.0f;
    private float y = 0.0f;
    private float yOld = 0.0f;

    // x-axis hard iron offset
    private float hardIronAlpha = 0.0f;
    // y-axis hard iron offset
    private float hardIronBeta = 0.0f;
    // z-axis hard iron offset
    private float hardIconGamma = 0.0f;

    // x-axis soft iron scalar
    private float softIronAlpha = 0.0f;
    // y-axis soft iron scalar
    private float softIronBeta = 0.0f;
    // z-axis soft iron scalar
    private float softIronGamma = 0.0f;

    private float[] fusedOrientation = new float[3];
    private float[] acceleration = new float[4];
    private float[] magnetic = new float[3];
    private float[] tempMagnetic = new float[3];
    private float[] rotation = new float[3];

    private int numSamples = 0;

    private Button startButton;

    private TextView numSamplesLabel;

    private SensorManager sensorManager;

    private OrientationFusion orientationFusion;

    private MeanFilter meanFilterOrientation;
    private MeanFilter meanFilterMagnetic;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the view here (for now).
        setContentView(R.layout.layout_calibrate);

        gaugeCalibration = findViewById(R.id.gauge_calibration);

        threeSpacePointList = new ArrayList<>();

        startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        numSamplesLabel = this.findViewById(R.id.value_num_samples);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        orientationFusion = new OrientationComplimentaryFusion();
        meanFilterOrientation = new MeanFilter();
        meanFilterMagnetic = new MeanFilter();
    }

    public void onStart() {
        super.onStart();

        showHintsDialog();
    }

    public void onResume() {
        super.onResume();

        // Register for sensor updates.
        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * {@inheritDoc}
     */
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
            orientationFusion.setAcceleration(acceleration);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, tempMagnetic, 0, event.values.length);
            orientationFusion.setMagneticField(meanFilterMagnetic.filter(this.tempMagnetic));
            this.magnetic = TiltCompensationUtil.compensateTilt(new float[]{tempMagnetic[0], -tempMagnetic[1], tempMagnetic[2]}, new float[]{fusedOrientation[1], fusedOrientation[2], 0});
            this.magnetic[1] = -this.magnetic[1];

            if(hardIronCalibrating) {
                addMeasurement();
            }

            updateHardIronFieldUI();

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, rotation, 0, event.values.length);
            // Filter the rotation
            fusedOrientation = orientationFusion.filter(this.rotation);

            fusedOrientation = meanFilterOrientation.filter(fusedOrientation);
        }
    }

    @Override
    public void onClick(View v) {
        // If the user starts the samples
        if (v.equals(startButton)) {
            if (hardIronCalibrating) {
                clearAllPoints();
                startButton.setText("Calibrate");
                processHardIronCompensation();
                this.hardIronCalibrating = false;
            } else if (!hardIronCalibrating) {
                startButton.setText("Finish");
                clearAllPoints();
                hardIronCalibrating = true;
            }
        }
    }


    private void showHintsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Hard-Iron Calibration");

        View layout = getLayoutInflater().inflate(
                R.layout.calibrate_hard_iron_tip_0_layout, null);

        builder.setView(layout);

        final Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Button button = layout.findViewById(R.id.button_finished);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    /**
     * Update the UI with the magnetic field vectors.
     */
    private void updateHardIronFieldUI() {
        setPoint(magnetic[0], magnetic[1], getResources().getColor(R.color.colorPrimary));
    }

    /**
     * Set the magnetic field data.
     *
     * @param x     the x point
     * @param y     the y point
     * @param color the color of the point
     */
    private void setPoint(float x, float y, int color) {
        this.x = x;
        this.y = y;

        // Only update the points if they have moved a certain distance from the
        // previous point.
        if ((this.x > this.xOld + DELTA || this.x < this.xOld - DELTA)
                || ((this.y > this.yOld + DELTA || this.y < this.y - DELTA))) {

            gaugeCalibration.setValue(this.x, this.y, color);

            this.xOld = this.x;
            this.yOld = this.y;
        }
    }

    private void clearAllPoints() {
        gaugeCalibration.clearAllPoints();
    }

    /**
     * This method takes a measurement of the magnetic field as the Android
     * device is rotated through 360 degrees. The results are then used to
     * perform a least squares fit of the data to determine the offset of the
     * locus of the ellipsoid from the center, (0,0). A hard-iron distortion can
     * be visibly identified by an offset of the origin of the ideal circle from
     * (0, 0). Compensating for hard-iron distortion is straightforward,
     * accomplished by determining the x and y offsets and then applying these
     * constants directly to the data. It is important to note that tilt
     * compensation must be applied prior to determining hard-iron corrections.
     * <p>
     * Hard-iron corrections are typically determined by rotating the sensor
     * through a minimum of 360�, then determining the distance from (0, 0) to
     * the center of the circle by identifying the average of the maximum and
     * minimum values for each of the axes.
     */
    private void addMeasurement() {

        threeSpacePointList.add(new ThreeSpacePoint(magnetic[0], magnetic[1], magnetic[2]));
        numSamples++;

        updateNumSamplesLabel(numSamples);
    }

    private void updateNumSamplesLabel(int numSamples) {
        numSamplesLabel.setText(String.valueOf(numSamples));
    }

    /**
     * These offsets are then subtracted from the raw x and y magnetometer data,
     * thus largely eliminating the hard-iron distortion.
     * <p>
     * Hard-iron effects are constant regardless of orientation or position of
     * the sensing platform. These constant offsets can be stored once
     * calculated and subtracted from the raw magnetometer data.
     * <p>
     * ://www.sensorsmag.com/sensors/motion-velocity-displacement/compensating
     * -tilt-hard-iron-and-soft-iron-effects-6475
     */
    private void processHardIronCompensation() {
        // If the algorithm has just been calibrated, find the circle
        // coordinates from the sampled data.
        if (hardIronCalibrating) {
            processHardIronLeastSquares();

            // Save the new calibration
            startWritePrefsAlert();
        }
    }

    private void processHardIronLeastSquares() {
        FitPoints fitPoints = new FitPoints(threeSpacePointList);
        Calibration calibration = CalibrationUtil.getCalibration(fitPoints);

        // Set Vx
        hardIronAlpha = (float) calibration.offset.getEntry(0);
        // Set Vy
        hardIronBeta = (float) calibration.offset.getEntry(1);
        // Set Vz
        hardIconGamma = (float) calibration.offset.getEntry(2);

        softIronAlpha = (float) calibration.scalar.getEntry(0, 0);
        softIronBeta = (float) calibration.scalar.getEntry(1, 1);
        softIronGamma = (float) calibration.scalar.getEntry(2, 2);

        // move all the old data from the lists.
        threeSpacePointList.clear();
    }

    /**
     * Write new calibrations out to the shared preferences.
     */
    private void writeHardIronPrefs() {
        if (write) {
            SharedPreferences.Editor editor = getSharedPreferences(HardIronPreferences.KEY, Activity.MODE_PRIVATE).edit();
            editor.putFloat(HardIronPreferences.HARD_IRON_ALPHA, hardIronAlpha);
            editor.putFloat(HardIronPreferences.HARD_IRON_BETA, hardIronBeta);
            editor.putFloat(HardIronPreferences.HARD_IRON_GAMMA, hardIconGamma);

            editor.putFloat(HardIronPreferences.SOFT_IRON_ALPHA, softIronAlpha);
            editor.putFloat(HardIronPreferences.SOFT_IRON_BETA, softIronBeta);
            editor.putFloat(HardIronPreferences.SOFT_IRON_GAMMA, softIronGamma);

            editor.apply();

            CharSequence text = "The calibration has been saved.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        } else {
            CharSequence text = "The calibration has not been saved.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }
    }

    /**
     * Start the alert dialog to confirm writing to shared preferences.
     */
    private void startWritePrefsAlert() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Attention:");

        // set dialog message
        alertDialogBuilder
                .setMessage("Save offset to vehicle preferences?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        write = true;
                        writeHardIronPrefs();

                        dialog.cancel();

                        CharSequence text = "The offset was saved.";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(
                                CalibrationActivity.this, text,
                                duration);
                        toast.show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        write = false;
                        dialog.cancel();

                        CharSequence text = "The offset was not saved.";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(
                                CalibrationActivity.this, text,
                                duration);
                        toast.show();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
