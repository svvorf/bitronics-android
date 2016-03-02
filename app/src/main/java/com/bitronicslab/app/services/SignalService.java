package com.bitronicslab.app.services;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import com.bitronicslab.app.utils.Event;
import com.bitronicslab.app.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class SignalService extends IntentService {

    private static final UUID SDP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String DEFAULT_DEVICE_NAME_START_PATTERN = "HC-0";


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    private final IBinder mBinder = new LocalBinder();
    private boolean deviceConnected;
    private InputStream mInputStream;

    private SignalCallback signalCallback;

    private int lastChannel = 0;
    private int lastValue = 0;
    private CountDownTimer timer;
    private long lastSentMillis;
    private SignalCallback callback;
    private boolean paused;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SignalService(String name) {
        super(name);
    }

    public SignalService() {
        super("SignalService");
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void attemptToConnect(Event.Bluetooth.AttemptToConnect event) {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (event.getBluetoothDevice() == null) {
            if (mBluetoothAdapter == null) {
                Log.d("Dbg", "bluetooth not supported");
                Event.BUS.post(new Event.Bluetooth.NotSupported());
                return;
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Log.d("Dbg", "request enable");
                Event.BUS.post(new Event.Bluetooth.RequestEnable());
                return;
            }
            mDevice = queryPairedDevice();
            if (mDevice == null) {
                Event.BUS.post(new Event.Bluetooth.OpenBluetoothSettings());
            } else {
                createSocket();
            }

        } else {
            mDevice = event.getBluetoothDevice();
            createSocket();
        }
    }

    double t = 0;

    //@Subscribe(threadMode = ThreadMode.MAIN)
    public void mockSignalGenerator(Event.Bluetooth.AttemptToConnect event) {

        Log.d("Dbg", Utils.SAMPLE_RATE + "");
        Event.BUS.post(new Event.Bluetooth.Connected());
        timer = new CountDownTimer(10000, Utils.SAMPLE_RATE_PERIOD) {

            @Override
            public void onTick(long millisUntilFinished) {
                //Event.BUS.post(new Event.Signal.NewValue(0, (int) (Utils.mockF(t) * 255)));
                if (callback != null) {
                    callback.newValue(0, (int) (Utils.mockF(t) * 255));
                    t += Utils.MOCK_SIGNAL_SPEED;
                   // Log.d("dbg", (10000 - millisUntilFinished) / Utils.SAMPLE_RATE_PERIOD + "");
                }
            }

            @Override
            public void onFinish() {
                timer.start();
            }
        }.start();

    }

    @Subscribe
    public void togglePlayPause(Event.Signal.TogglePlayPause event) {
        paused = !paused;
    }

    private void createSocket() {
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(SDP_UUID);
            connect();
        } catch (IOException e) {
            e.printStackTrace();
            Event.BUS.post(new Event.Bluetooth.UnknownError());
        }
    }

    private void connect() throws IOException {
        Log.d("Dbg", "connecting");
        mBluetoothAdapter.cancelDiscovery();
        try {
            mSocket.connect();
            Log.d("dbg", "connected: " + mSocket.isConnected());
            deviceConnected = mSocket.isConnected();
            Event.BUS.post(new Event.Bluetooth.Connected());
            lastSentMillis = System.currentTimeMillis();
            manageConnection();
        } catch (IOException e) {
            if (e.getMessage().contains("timeout"))
                Event.BUS.post(new Event.Bluetooth.Timeout());
            e.printStackTrace();
            mSocket.close();
        }
    }

    private void manageConnection() throws IOException {
        mInputStream = mSocket.getInputStream();
        byte[] buffer = new byte[1];  // buffer store for the stream
        int bytes; // bytes returned from read()
        while (true) {
            if (!paused) {
                try {
                    bytes = mInputStream.read(buffer);
                    if (buffer[0] == (byte) 'A') {
                        if (System.currentTimeMillis() - lastSentMillis > Utils.SAMPLE_RATE_PERIOD) {
                            lastSentMillis = System.currentTimeMillis();
                            callback.newValue(lastChannel, lastValue);
                        }
                    } else if (buffer[0] >= '0' && buffer[0] <= '9') {
                        lastChannel = Character.getNumericValue((char) buffer[0]);
                    } else {
                        lastValue = buffer[0] & 0xff;
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    private BluetoothDevice queryPairedDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d("Dbg", pairedDevices.size() + " paired devices");
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(DEFAULT_DEVICE_NAME_START_PATTERN)) {
                    return device;
                }
            }
        }
        return null;
    }


    private void stop() throws IOException {
        if (mSocket != null)
            mSocket.close();
        if (timer != null)
            timer.cancel();
    }

    public boolean isDeviceConnected() {
        return deviceConnected;
    }

    public void setCallback(SignalCallback callback) {
        this.callback = callback;
    }


    public class LocalBinder extends Binder {
        public SignalService getService() {
            return SignalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Event.BUS.register(this);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Event.BUS.unregister(this);
        try {
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.onUnbind(intent);
    }

    public interface SignalCallback {
        void newValue(int channel, int value);
    }
}
