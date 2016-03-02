package com.bitronicslab.app.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentCallback;
import com.afollestad.assent.PermissionResultSet;
import com.bitronicslab.app.R;
import com.bitronicslab.app.adapters.SignalTypeSpinnerAdapter;
import com.bitronicslab.app.services.SignalService;
import com.bitronicslab.app.utils.Event;
import com.bitronicslab.app.utils.FFT;
import com.bitronicslab.app.utils.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class VisualisationFragment extends BluetoothFragment implements SignalService.SignalCallback {

    private static final float MAX_VOLTAGE = 5.2f;
    private static final int MAX_ADC = 255;

    private static final int CHART_WIDTH_IN_POINTS = 512;
    private static final int WRITE_PERMISSION_REQUEST = 51;
    private final int SPECTRUM_CALCULATION_PERIOD = 5; //in points

    private static boolean CONVERT_TO_VOLTAGE = true;
    private final ArrayList<Entry> signalEntries = new ArrayList<>();

    @Bind(R.id.signal_chart)
    LineChart signalChart;
    @Bind(R.id.spectrum_chart)
    LineChart spectrumChart;

    ViewGroup spinnerContainer;
    private Toolbar toolbar;


    double[] signalPoints = new double[CHART_WIDTH_IN_POINTS];
    private int pointsNumber = 0;
    private LineDataSet signalDataSet;
    private LineData signalData;

    private LineDataSet spectrumDataSet;
    private LineData spectrumData;
    private MenuItem playPauseMenuItem;
    private MenuItem saveMenuItem;
    private int pointsCount;
    private long startTime;

    private float maxSpectrumMagnitude;

    public VisualisationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_visualisation, null);
        super.loadBluetoothViews(view);
        ButterKnife.bind(this, view);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        setupSpinner();

        configureSignalChart();
        configureSpectrumChart();
        return view;
    }

    private void setupSpinner() {
        spinnerContainer = (ViewGroup) getActivity().findViewById(R.id.toolbar_spinner_container);
        spinnerContainer.setVisibility(View.VISIBLE);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);

        SignalTypeSpinnerAdapter spinnerAdapter = new SignalTypeSpinnerAdapter(getActivity(), getResources().getStringArray(R.array.signal_types));

        Spinner signalTypeSpinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
        signalTypeSpinner.setAdapter(spinnerAdapter);

        signalTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void configureSpectrumChart() {
        spectrumDataSet = new LineDataSet(new ArrayList<Entry>(), "Magnitude");
        spectrumDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        spectrumChart.setScaleYEnabled(false);
        spectrumDataSet.setLineWidth(1.5f);
        spectrumDataSet.setDrawCircles(false);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(spectrumDataSet);
        spectrumData = new LineData(new ArrayList<String>(), dataSets);
        spectrumData.setDrawValues(false);

        spectrumChart.setData(spectrumData);
        spectrumChart.setDragEnabled(true);
        spectrumChart.getLegend().setEnabled(false);
        spectrumChart.setScaleXEnabled(true);
        spectrumChart.setDescription(null);
        spectrumChart.setScaleYEnabled(false);

        spectrumChart.getAxisRight().setEnabled(false);
        spectrumChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
    }

    private void configureSignalChart() {
        ArrayList<String> xVals = new ArrayList<>();
        for (int i = 0; i < CHART_WIDTH_IN_POINTS; i++) {
            xVals.add(i * Utils.SAMPLE_RATE_PERIOD + "");
        }
        signalDataSet = new LineDataSet(signalEntries, "Voltage");
        signalDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        signalDataSet.setDrawCircles(false);
        signalDataSet.setLineWidth(1.5f);
        signalChart.setScaleYEnabled(false);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(signalDataSet);
        signalData = new LineData(xVals, dataSets);
        signalData.setDrawValues(false);

        signalChart.setData(signalData);
        signalChart.setDragEnabled(true);
        signalChart.getLegend().setEnabled(false);
        signalChart.setScaleXEnabled(true);
        signalChart.setDescription(null);
        //signalChart.getXAxis().setDrawLabels(false);

        signalChart.getAxisRight().setEnabled(false);
        signalChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        signalChart.getAxisLeft().setAxisMaxValue(MAX_VOLTAGE);
    }

    @Subscribe
    public void serviceBound(Event.Bluetooth.ServiceBound event) {
        mService.setCallback(this);
    }

    private float getVoltage(int adc) {
        return adc * MAX_VOLTAGE / MAX_ADC;
    }

    @Override
    public void onDetach() {
        spinnerContainer.setVisibility(View.GONE);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        super.onDetach();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showConnected(Event.Bluetooth.Connected event) {
        playPauseMenuItem.setEnabled(true);
        saveMenuItem.setEnabled(true);
    }

    @Override
    public void newValue(final int channel, final int value) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("dbg", System.currentTimeMillis() - startTime + "");
                double voltage = CONVERT_TO_VOLTAGE ? getVoltage(value) : value;
                signalDataSet.addEntry(new Entry((float) voltage, pointsNumber));
                signalChart.notifyDataSetChanged();
                pointsNumber++;
                if (pointsNumber >= CHART_WIDTH_IN_POINTS) {
                    signalData.addXValue(pointsNumber * Utils.SAMPLE_RATE_PERIOD + "");
                    System.arraycopy(signalPoints, 1, signalPoints, 0, CHART_WIDTH_IN_POINTS - 1);
                    signalPoints[CHART_WIDTH_IN_POINTS - 1] = voltage;
                    signalChart.moveViewToX(pointsNumber - CHART_WIDTH_IN_POINTS);
                    signalChart.setVisibleXRangeMaximum(CHART_WIDTH_IN_POINTS);
                } else {
                    signalPoints[pointsNumber] = voltage;
                    signalChart.invalidate();
                }

                pointsCount++;
                if (pointsCount > SPECTRUM_CALCULATION_PERIOD) {
                    double[] magnitudes = FFT.calculateSpectrum(signalPoints);
                    int length = magnitudes.length;

                    for (int i = 0; i < length; i++) {
                        int frequency = Utils.SAMPLE_RATE * i / length;
                        float magnitude = (float) Math.abs(magnitudes[i]) / length;
                        maxSpectrumMagnitude = Math.max(maxSpectrumMagnitude, magnitude);
                        Entry entry = spectrumDataSet.getEntryForXIndex(frequency);
                        if (entry == null || entry.getXIndex() != frequency) { //no entry for this frequency
                            entry = new Entry(magnitude, frequency);
                            spectrumData.addXValue(frequency + "");
                            spectrumDataSet.addEntry(entry);
                        } else {
                            entry.setVal(magnitude);
                        }
                    }
                    spectrumChart.getAxisLeft().setAxisMaxValue(maxSpectrumMagnitude);

                    spectrumData.notifyDataChanged();
                    spectrumChart.notifyDataSetChanged();
                    spectrumChart.invalidate();
                    pointsCount = 0;
                }
            }
        });

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_visualisation, menu);
        playPauseMenuItem = menu.findItem(R.id.pause_play);
        saveMenuItem = menu.findItem(R.id.save);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.pause_play:
                Event.BUS.post(new Event.Signal.TogglePlayPause());
                item.setIcon(mService.isPaused() ? R.drawable.ic_play_arrow_white_24dp : R.drawable.ic_pause_white_24dp);
                break;
            case R.id.save:
                if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
                    Assent.requestPermissions(new AssentCallback() {
                        @Override
                        public void onPermissionResult(PermissionResultSet permissionResultSet) {
                            if (permissionResultSet.allPermissionsGranted()) {
                                saveSignalRecords();
                            } else {
                                Toast.makeText(getActivity(), "Sorry, you have to grant writing permissions.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, WRITE_PERMISSION_REQUEST, Assent.WRITE_EXTERNAL_STORAGE);
                    break;
                }
                saveSignalRecords();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveSignalRecords() {
        String fileName = Utils.saveSignalRecords(signalEntries);
        if (fileName != null) {
            Toast.makeText(getActivity(), "Successfully saved to " + fileName , Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Sorry, something went wrong.", Toast.LENGTH_SHORT).show();
        }
    }
}
