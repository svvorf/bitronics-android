package com.bitronicslab.app.utils;


import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
import android.util.Log;


import com.bitronicslab.app.services.SignalService;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by ivan on 2/22/16.
 */
public class Event {
    public static EventBus BUS = new EventBus();

    public static class Bluetooth {
        public static class NotSupported {
            public NotSupported() {
            }
        }

        public static class RequestEnable {
            public RequestEnable() {
            }
        }

        public static class AttemptToConnect {
            private BluetoothDevice bluetoothDevice;

            public AttemptToConnect() {
            }

            public AttemptToConnect(BluetoothDevice bluetoothDevice) {
                this.bluetoothDevice = bluetoothDevice;
            }

            public BluetoothDevice getBluetoothDevice() {
                return bluetoothDevice;
            }
        }

        public static class Connected {
            public Connected() {
            }
        }


        public static class Timeout {
            public Timeout() {
            }
        }

        public static class UnknownError {
            public UnknownError() {
            }
        }

        public static class RequestPermissions {
            public RequestPermissions() {
            }
        }

        public static class OpenBluetoothSettings {
            public OpenBluetoothSettings() {
            }
        }

        public static class ServiceBound {
            public ServiceBound() {

            }
        }
    }

    public static class Signal {

        public static class TogglePlayPause {
            public TogglePlayPause() {
            }
        }
    }


}
