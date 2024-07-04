package com.example.appbluetootharduino;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.appbluetootharduino.servicio.BluetoothSerialService;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private MainActivity.MyServiceMessageHandler myServiceMessageHandler;

    protected BluetoothSerialService myBluetoothSerialService = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private String mConnectedDeviceName = null;

    private boolean mBoundService = false;



    private ImageView imgUp, imgDown, imgAutomatic, imgLeft, imgRight, imgStop, imgConnect;
    private TextView nombres;

    final static String UP="a";
    final static String DOWN="d";
    final static String LEFT="b";
    final static String RIGHT="c";
    final static String AUTOMATIC="f";
    final static String STOP="e";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgUp = findViewById(R.id.arriba);
        imgDown = findViewById(R.id.abajo);
        imgLeft = findViewById(R.id.izquierda);
        imgRight = findViewById(R.id.derecha);
        imgAutomatic = findViewById(R.id.automatico);
        imgStop = findViewById(R.id.parar);
        imgConnect = findViewById(R.id.connectar);
        nombres = findViewById(R.id.Nombres);
        nombres.setText(R.string.string_names);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(this,"No hay adaptador de bluetooth",Toast.LENGTH_LONG);
        }else{
            Intent intent = new Intent(getApplicationContext(), BluetoothSerialService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        myServiceMessageHandler = new MainActivity.MyServiceMessageHandler(this,this);

        listeners();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothSerialService.MySerialServiceBinder binder = (BluetoothSerialService.MySerialServiceBinder) service;
            myBluetoothSerialService = binder.getService();
            mBoundService = true;
            myBluetoothSerialService.setMessageHandler(myServiceMessageHandler);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundService = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if(!bluetoothAdapter.isEnabled()){
            Thread thread = new Thread(){
                @SuppressLint("MissingPermission")
                @Override
                public void run(){
                    try{
                        bluetoothAdapter.enable();
                    }catch (RuntimeException e){
                        Toast.makeText(MainActivity.this,"Error, no hay permisos para el bluetooth",Toast.LENGTH_LONG);
                    }
                }
            };
            thread.start();
        }

        if(myBluetoothSerialService != null) onBluetoothStateChange(myBluetoothSerialService.getState());
    }

    private static class MyServiceMessageHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;
        private final Context mContext;

        MyServiceMessageHandler(Context context, MainActivity activity){
            mContext = context;
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constantes.MESSAGE_STATE_CHANGE:
                    mActivity.get().onBluetoothStateChange(msg.arg1);
                    break;
                case Constantes.MESSAGE_DEVICE_NAME:
                    mActivity.get().mConnectedDeviceName = msg.getData().getString(Constantes.DEVICE_NAME);
                    break;
                case Constantes.MESSAGE_TOAST:
                    break;
            }
        }
    }


    private void onBluetoothStateChange(int currentState){

        switch (currentState){
            case BluetoothSerialService.STATE_CONNECTED:
                break;
            case BluetoothSerialService.STATE_CONNECTING:
                break;
            case BluetoothSerialService.STATE_LISTEN:
                break;
            case BluetoothSerialService.STATE_NONE:
                break;
        }

    }

    private void listeners() {



        imgConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled()){
                    if(myBluetoothSerialService != null){
                        if(myBluetoothSerialService.getState() == myBluetoothSerialService.STATE_CONNECTED){
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Desconectar")
                                    .setMessage("Desconectar dispositivo")
                                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(myBluetoothSerialService != null) myBluetoothSerialService.disconnectService();
                                        }
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();

                        }else{
                            Intent serverIntent = new Intent(MainActivity.this, ListaDispositivos.class);
                            startActivity(serverIntent);

                        }
                    }else{
                        Toast.makeText(MainActivity.this,"No hay servicio de serial establecido", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(UP);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(DOWN);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(LEFT);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(RIGHT);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgAutomatic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(AUTOMATIC);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(STOP);
                }else{
                    Toast.makeText(MainActivity.this,"Debe de conectar su bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){

            mConnectedDeviceName = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.EXTRA_DEVICE_ADDRESS);
            connectToDevice(mConnectedDeviceName);
        }


    }

    private void connectToDevice(String macAddress){
        if(macAddress == null){
            Intent serverIntent = new Intent(getApplicationContext(), ListaDispositivos.class);
            startActivityForResult(serverIntent, Constantes.CONNECT_DEVICE_SECURE);
        }else{
            Intent intent = new Intent(getApplicationContext(), BluetoothSerialService.class);
            intent.putExtra(BluetoothSerialService.KEY_MAC_ADDRESS, macAddress);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                getApplicationContext().startForegroundService(intent);
            }else{
                startService(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBoundService){
            myBluetoothSerialService.setMessageHandler(null);
            unbindService(serviceConnection);
            mBoundService = false;
        }

        stopService(new Intent(this, BluetoothSerialService.class));
    }

}