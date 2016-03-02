package com.bitronicslab.app.fragments;


import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bitronicslab.app.R;
import com.bitronicslab.app.services.SignalService;
import com.bitronicslab.app.utils.Event;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothFragment extends Fragment {


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Intent serviceIntent;

    protected SignalService mService;

    ViewGroup loadingView;
    ViewGroup contentView;
    TextView messageTextView;
    ProgressBar progressBar;
    Button reconnectButton;


    private static final int REQUEST_ENABLE_BT = 0;
    private boolean fragmentPaused = false;
    private boolean signalPaused;

    public BluetoothFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent = new Intent(getActivity(), SignalService.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().bindService(serviceIntent, mConnection, Service.BIND_AUTO_CREATE);
        return null;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (messageTextView != null)
            messageTextView.setText(R.string.connecting);
    }

    protected void loadBluetoothViews(View view) {
        loadingView = (ViewGroup) view.findViewById(R.id.loading);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        contentView = (ViewGroup) view.findViewById(R.id.content);
        messageTextView = (TextView) view.findViewById(R.id.message);
        reconnectButton = (Button) view.findViewById(R.id.reconnect);
        reconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Event.BUS.post(new Event.Bluetooth.AttemptToConnect());
                progressBar.setVisibility(View.VISIBLE);
                messageTextView.setText(R.string.connecting);
                reconnectButton.setVisibility(View.GONE);
            }
        });
    }

    @Subscribe
    public void processEnableBluetoothRequest(Event.Bluetooth.RequestEnable event) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showConnectedMessage(Event.Bluetooth.Connected event) {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showTimeoutMessage(Event.Bluetooth.Timeout event) {
        progressBar.setVisibility(View.GONE);
        messageTextView.setText(R.string.timeout);
        reconnectButton.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestPermissions(Event.Bluetooth.RequestPermissions event) {
        new AlertDialog.Builder(getActivity())
                .setTitle("This app needs location access")
                .setMessage("Please grant location access to discover bluetooth devices (Android 6.0)")
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }


                }).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void openBluetoothSettings(Event.Bluetooth.OpenBluetoothSettings event) {
        Toast.makeText(getActivity(), "Find your device and pair it", Toast.LENGTH_SHORT).show();

        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Event.BUS.post(new Event.Bluetooth.AttemptToConnect());
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Functionality limited")
                            .setMessage("Since location access has not been granted, this app will not be able to discover devices.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), R.string.must_enable_bt, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            default:
                break;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d("dbg", "connected");
            mService = ((SignalService.LocalBinder) binder).getService();
            mService.setPaused(signalPaused);
            Event.BUS.post(new Event.Bluetooth.ServiceBound());
            if (mService.isDeviceConnected()) {
            } else {
                Event.BUS.post(new Event.Bluetooth.AttemptToConnect());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        signalPaused = mService.isPaused();
        getActivity().unbindService(mConnection);
        Event.BUS.unregister(this);
        fragmentPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("Dbg", "resume");
        if (fragmentPaused)
            getActivity().bindService(serviceIntent, mConnection, Service.BIND_AUTO_CREATE);
        Event.BUS.register(this);
        fragmentPaused = false;
    }
}
