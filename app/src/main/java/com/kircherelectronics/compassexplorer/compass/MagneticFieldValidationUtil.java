package com.kircherelectronics.compassexplorer.compass;

import android.hardware.GeomagneticField;
import android.hardware.SensorManager;

/**
 * Created by kaleb on 3/17/18.
 */

public class MagneticFieldValidationUtil {

    // The range, +/- the expected value, that the magnitude of the magnetic
    // field must be in for it to be considered normal.
    private static final int MAGNETIC_DISTORTION_EPSILON = 9;

    /**
     * Validate the magnetic field.
     *
     * @param magnetic the magnetic field measurements.
     */
    private MagneticFieldState validateMagneticField(float[] magnetic, GeomagneticField geoMagField) {
        // Detect distorted magnetic fields....
        double magnitude = Math.sqrt(Math.pow(magnetic[0], 2)
                + Math.pow(magnetic[1], 2) + Math.pow(magnetic[2], 2));

        // In the case that a GPS location can be acquired.
        if (geoMagField != null) {
            // Get the expected magnetic field strength in nT and convert to
            // mT
            double fieldStrength = geoMagField.getFieldStrength() * 0.001;

            // If the magnetic field is out of range, then it is distorted
            if ((magnitude < (fieldStrength - MAGNETIC_DISTORTION_EPSILON) || magnitude > (fieldStrength + MAGNETIC_DISTORTION_EPSILON))) {
                return MagneticFieldState.DISTORTED;
            } else {
                return MagneticFieldState.NORMAL;
            }
        }
        // In-case the GPS isn't turned on or can't acquire a signal.else {
        // If the magnetic field is out of range, then it is distorted
        if ((magnitude < SensorManager.MAGNETIC_FIELD_EARTH_MIN || magnitude > SensorManager.MAGNETIC_FIELD_EARTH_MAX)) {
            return MagneticFieldState.DISTORTED;
        } else {
            return MagneticFieldState.NORMAL;
        }
    }

    public enum MagneticFieldState {
        NORMAL,
        DISTORTED
    }
}
