package com.bitronicslab.app.utils;

import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

/**
 * Created by ivan on 2/25/16.
 */
public class FFT {
    public static double[] calculateSpectrum(double[] points) {
        DoubleFFT_1D fftDo = new DoubleFFT_1D(points.length);
        double[] fft = new double[points.length * 2];
        System.arraycopy(points, 0, fft, 0, points.length);
        fftDo.realForwardFull(fft);
        return fft;
    }


}
