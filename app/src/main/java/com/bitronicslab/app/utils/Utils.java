package com.bitronicslab.app.utils;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ivan on 2/25/16.
 */
public class Utils {
    public static final long SAMPLE_RATE_PERIOD = 10; // ms
    public static final int SAMPLE_RATE = (int) (1 / (SAMPLE_RATE_PERIOD * 1e-3)); // Hz
    public static final double MOCK_SIGNAL_SPEED = 0.1d; // radian/SAMPLE_RATE_PERIOD

    public static double mockF(double t) {
        return Math.sin(t);
    }

    public static String saveSignalRecords(ArrayList<Entry> entries) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Bitronics/");
        dir.mkdirs();
        File file = new File(dir, new SimpleDateFormat("hhmmddMMyyyy'.txt'", Locale.getDefault()).format(new Date()));
        try {
            FileWriter fw = new FileWriter(file);
            for (Entry entry : entries) {
                fw.write(entry.getXIndex() * SAMPLE_RATE_PERIOD + ";" + entry.getVal());
                fw.write(System.getProperty("line.separator"));
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return file.getAbsolutePath();

    }
}
