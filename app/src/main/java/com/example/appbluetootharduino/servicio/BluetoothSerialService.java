package com.example.appbluetootharduino.servicio;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.example.appbluetootharduino.Constantes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSerialService extends Service {

    public static final String KEY_MAC_ADDRESS = "KEY_MAC_ADDRESS";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter mAdapter;

    private Handler mHandlerActivity;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private int mState;
    private int mNewState;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private final IBinder mBinder = new MySerialServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;
        mNewState = mState;

        if (mAdapter == null) {

            Toast.makeText(this, "Ha ocurrido un error", Toast.LENGTH_LONG);
            stopSelf();
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {

            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.start();

        if (intent != null) {
            String deviceAddress = intent.getStringExtra(KEY_MAC_ADDRESS);
            if (deviceAddress != null) {
                try {
                    BluetoothDevice device = mAdapter.getRemoteDevice(deviceAddress.toUpperCase());
                    this.connect(device, true);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, "Ha ocurrido un error", Toast.LENGTH_LONG);
                    disconnectService();
                    stopSelf();
                }
            }
        } else {
            Toast.makeText(this, "Ha ocurrido un error", Toast.LENGTH_LONG);
            disconnectService();
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        updateUserInterfaceTitle();
    }

    synchronized void connect(BluetoothDevice device, boolean secure) {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    public void disconnectService() {
        this.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public synchronized int getState() {
        return mState;
    }

    public class MySerialServiceBinder extends Binder {
        public BluetoothSerialService getService() {
            return BluetoothSerialService.this;
        }
    }

    public void setMessageHandler(Handler myServiceMessageHandler) {
        this.mHandlerActivity = myServiceMessageHandler;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                }
            } catch (IOException | NullPointerException e) {
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();


            try {
                mmSocket.connect();
            } catch (IOException | NullPointerException e) {
                try {
                    mmSocket.close();
                } catch (IOException | NullPointerException e2) {
                }
                connectionFailed();
                return;
            }
            synchronized (this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        if (mHandlerActivity != null) {
            Message msg = mHandlerActivity.obtainMessage(Constantes.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }
        updateUserInterfaceTitle();

        try {
            wait(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException | NullPointerException e) {
            }

            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {

        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException | NullPointerException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
            }
        }
    }

    synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();
    }

    private void connectionFailed() {
        if(mHandlerActivity != null){
            Message msg = mHandlerActivity.obtainMessage(Constantes.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.TOAST, "No se puede conectar al dispositivo");
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();
        this.start();
    }
    private void connectionLost() {
        if(mHandlerActivity != null){
            Message msg = mHandlerActivity.obtainMessage(Constantes.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.TOAST, "Conexion perdida");
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();

        this.start();
    }

    public void serialWriteString(String s){
        byte buffer[] = s.getBytes();
        this.serialWriteBytes(buffer);
    }
    private void serialWriteBytes(byte[] b) {

        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            mConnectedThread.write(b);
        }
    }



    //Verificador de estado
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;
        if(mHandlerActivity != null){
            mHandlerActivity.obtainMessage(Constantes.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
        }
    }

}
