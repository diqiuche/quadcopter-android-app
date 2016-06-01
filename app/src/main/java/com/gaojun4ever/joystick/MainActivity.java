package com.gaojun4ever.joystick;
import android.app.AlertDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;

import android.widget.Button;
import android.widget.CompoundButton;

import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private RockerLeft rockerLeft;
    private RockerRight rockerRight;
    private TextView gas_textview;
    private TextView yaw_textview;
    private TextView roll_textview;
    private TextView pitch_textview;

    private Button devChose_button;
    private TextView macAddr_textview;
    private Switch connect_switch;
    private TextView btStatus_textview;
    private Button cal_button;
    private Switch PWON_switch;

    private static final int REQUEST_ENABLE_BT = 3;
    private List<BT_List> btDatas;
    private LeDeviceListAdapter  mLeDeviceListAdapter;


    private static String MACaddress;
    private boolean isBtChosed=false;//是否选择蓝牙设备
    private boolean mConnected = false;//蓝牙连接状态



    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;

    private String TAG = "BLE";
    private boolean mScanning;
    private Handler mHandler;

    private int bs2015_fly_gas_value=60;
    private int bs2015_fly_roll_value=0;
    private int bs2015_fly_pitch_value=0;
    private boolean isArmed=false;
    private Thread mThread;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private ServiceConnection mServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService=((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG,"Unable to initialize Bluetooth");
                finish();
            }

            Log.e(TAG, "mBluetoothLeService is okay");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
            Log.e(TAG, "mBluetoothLeService is not okay");

        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //连接成功
                Log.e(TAG, "Only gatt, just wait");
                btStatus_textview.setText("连接中");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //断开连接
                mConnected = false;
                btStatus_textview.setText("未连接");


            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //可以开始干活了
            {
                mConnected = true;
                btStatus_textview.setText("已连接");

                Log.e(TAG, "In what we need");

            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //收到数据

                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null) {
                    Log.e(TAG, "RCcmd: "+data);
                }
            }
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while(mConnected&&isArmed){

                    bs2015_fly_roll_value=rockerRight.getXvalue();
                    bs2015_fly_pitch_value=rockerRight.getYvalue();
                    String bs005_dir_roll_command=Integer.toHexString(bs2015_fly_roll_value).toUpperCase();
                    String bs005_dir_pitch_command=Integer.toHexString(bs2015_fly_pitch_value).toUpperCase();
                    if(bs005_dir_roll_command.length()<2) bs005_dir_roll_command="0"+bs005_dir_roll_command;
                    if(bs005_dir_roll_command.length()<2) bs005_dir_pitch_command="0"+bs005_dir_pitch_command;
                    String bs005_dir_up_command=bs005_dir_roll_command;
                    String bs005_gas_ctrl_str=ProtocalUtils.calculate_gas_str(rockerLeft.getYvalue());
                    String bs005_ctrl_command=ProtocalUtils.adding_protocal("RC" + bs005_gas_ctrl_str + bs005_dir_up_command + "0000");
                    Log.e(TAG, "cmd: "+bs005_ctrl_command);
                    if(mConnected){

                        mBluetoothLeService.WriteValue(bs005_ctrl_command);
                    }

                    try{
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        rockerLeft = (RockerLeft) findViewById(R.id.left_rudder);
        rockerRight = (RockerRight) findViewById(R.id.right_rudder);

        gas_textview= (TextView) findViewById(R.id.gas_textview);
        yaw_textview= (TextView) findViewById(R.id.yaw_textview);
        roll_textview= (TextView) findViewById(R.id.roll_textview);
        pitch_textview= (TextView) findViewById(R.id.pitch_textview);

        devChose_button= (Button) findViewById(R.id.devChose_button);
        macAddr_textview= (TextView) findViewById(R.id.macAddr_textview);
        connect_switch= (Switch) findViewById(R.id.connect_switch);
        btStatus_textview= (TextView) findViewById(R.id.btStatus_textview);
        cal_button= (Button) findViewById(R.id.cal_btuuon);
        PWON_switch= (Switch) findViewById(R.id.PWON_switch);

        cal_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnected){
                    String bs005_ctrl_command=ProtocalUtils.adding_protocal("CAL");
                    mBluetoothLeService.WriteValue(bs005_ctrl_command);
                    Log.e(TAG, "cmd: " + bs005_ctrl_command);
                }else{


                }
            }
        });

        PWON_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(mConnected){
                    if(isChecked){
                        String bs005_ctrl_command=ProtocalUtils.adding_protocal("PWON");
                        mBluetoothLeService.WriteValue(bs005_ctrl_command);
                        isArmed=true;
                        mThread.start();
                        Log.e(TAG, "cmd: " + bs005_ctrl_command);
                    }else{
                        String bs005_ctrl_command=ProtocalUtils.adding_protocal("PWOF");
                        mBluetoothLeService.WriteValue(bs005_ctrl_command);
                        isArmed=false;
                        Log.e(TAG, "cmd: " + bs005_ctrl_command);
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "连接未成功请重新连接", Toast.LENGTH_SHORT).show();
                }

            }
        });


        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        btDatas=new ArrayList<BT_List>();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mHandler = new Handler();


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();




        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());




        final AlertDialog.Builder builder=new AlertDialog.Builder(this);
        //选择设备按钮响应
        devChose_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBluetoothAdapter.isEnabled()){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
//
                scanLeDevice(true);

                builder.setTitle("选择设备");
                builder.create();
                final AlertDialog.Builder builder1 = builder.setSingleChoiceItems(mLeDeviceListAdapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(which);
                        MACaddress = device.getAddress();
                        macAddr_textview.setText("获取地址成功");
                        Log.e(TAG, MACaddress);


                        isBtChosed = true;
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });


//蓝牙连接按钮
        connect_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(isBtChosed){
                        mBluetoothLeService.connect(MACaddress);
                    }else{
                        Toast.makeText(getApplicationContext(), "请先选择设备", Toast.LENGTH_SHORT).show();
                        connect_switch.setChecked(false);
                    }


                } else {

                    btStatus_textview.setText("未连接");
                    mBluetoothLeService.close();
                    Toast.makeText(getApplicationContext(), "已断开连接", Toast.LENGTH_SHORT).show();

                }

            }
        });


        //摇杆样式设置
       // Bitmap rocker_bg= BitmapFactory.decodeResource(getResources(),R.drawable.rocker_bg);
       // Bitmap rocker_ctrl=BitmapFactory.decodeResource(getResources(),R.drawable.rocker_ctrl);
        //left_rockerLeft.setRockerBg(rocker_bg);
        //left_rockerLeft.setRockerCtrl(rocker_ctrl);
        // right_rocker.setRockerBg(rocker_bg);
        //right_rocker.setRockerCtrl(rocker_ctrl);


        //摇杆触摸响应
        rockerLeft.setRudderListener(new RockerLeft.RudderListener() {
            @Override
            public void onSteeringWheelChanged(int action, int angle) {
                if(action == RockerLeft.ACTION_RUDDER)
                {
                    gas_textview.setText("gas: "+rockerLeft.getYvalue());
                    yaw_textview.setText("yaw: "+rockerLeft.getXvalue());
                    roll_textview.setText("roll: "+rockerRight.getXvalue());
                    pitch_textview.setText("pitch: " + rockerRight.getYvalue());

                    bs2015_fly_roll_value=rockerRight.getXvalue();
                    bs2015_fly_pitch_value=rockerRight.getYvalue();

                }
            }
        });

        rockerRight.setRudderListener(new RockerRight.RudderListener() {
            @Override
            public void onSteeringWheelChanged(int action, int angle) {
                if (action == RockerRight.ACTION_RUDDER) {
                    gas_textview.setText("gas: " + rockerLeft.getYvalue());
                    yaw_textview.setText("yaw: " + rockerLeft.getXvalue());
                    roll_textview.setText("roll: " + rockerRight.getXvalue());
                    pitch_textview.setText("pitch: " + rockerRight.getYvalue());

                    bs2015_fly_roll_value=rockerRight.getXvalue();
                    bs2015_fly_pitch_value=rockerRight.getYvalue();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        scanLeDevice(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(MACaddress);
            Log.e(TAG, "Connect request result=" + result);
        }else{
            Log.e(TAG, "fuck!!!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        if(mConnected){
            String bs005_ctrl_command=ProtocalUtils.adding_protocal("PWOF");
            mBluetoothLeService.WriteValue(bs005_ctrl_command);
        }
        mLeDeviceListAdapter.clear();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnected){
            String bs005_ctrl_command=ProtocalUtils.adding_protocal("PWOF");
            mBluetoothLeService.WriteValue(bs005_ctrl_command);
        }

        this.unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        Log.e(TAG, "We are in destroy");
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            btDatas.add(new BT_List(device.getName(),device.getAddress()));
                        }
                    });
                }
            };

    private static IntentFilter makeGattUpdateIntentFilter() {                        //注册接收的事件
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }

}



