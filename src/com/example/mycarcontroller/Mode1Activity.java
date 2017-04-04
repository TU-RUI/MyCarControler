package com.example.mycarcontroller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class Mode1Activity extends Activity {

	private Button up,down,left,right,headup,headdown,headleft,headright,bt;
	private TextView info;
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
	private BluetoothAdapter bluetoothadapter;
	// 存放蓝牙设备地址列表
	private ArrayAdapter<String> mDevicesArrayAdapter;
	private BluetoothDevice mbluetoothdevice = null;
	private BluetoothSocket bluetoothsocket = null;
	Handler UIHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mode1);
		initView();
		UIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				info.append(msg.obj.toString() + "\n");
			}
		};
	}


	//初始化视图
	public void initView(){
		up = (Button) findViewById(R.id.UP);
		down = (Button) findViewById(R.id.DOWN);
		left = (Button) findViewById(R.id.LEFT);
		right = (Button) findViewById(R.id.RIGHT);
		bt = (Button) findViewById(R.id.bluetooth);
		headup = (Button) findViewById(R.id.HeadUP);
		headdown = (Button) findViewById(R.id.HeadDOWN);
		headleft = (Button) findViewById(R.id.HeadLEFT);
		headright = (Button) findViewById(R.id.HeadRIGHT);
		up.setOnTouchListener(listener1);
		down.setOnTouchListener(listener1);
		left.setOnTouchListener(listener1);
		right.setOnTouchListener(listener1);
		headup.setOnTouchListener(listener2);
		headdown.setOnTouchListener(listener2);
		headleft.setOnTouchListener(listener2);
		headright.setOnTouchListener(listener2);
		bt.setOnClickListener(listener);
		info = (TextView) findViewById(R.id.info);
	}

	//设置连接蓝牙按扭监听
	OnClickListener listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//打开蓝牙,获取已绑定蓝牙设备
			bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
			if (!bluetoothadapter.isEnabled()) {
				bluetoothadapter.enable();
			}
			mDevicesArrayAdapter = new ArrayAdapter<String>(Mode1Activity.this,
					R.layout.bluetoothdevice);
			Set<BluetoothDevice> BondedDevices = bluetoothadapter.getBondedDevices();
			for(BluetoothDevice device : BondedDevices){
				mDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
			if (mDevicesArrayAdapter.isEmpty()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Mode1Activity.this);
				builder.setTitle(R.string.app_name);
				builder.setMessage("没有得到已绑定设备");
				builder.create().show();
			}else{
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Mode1Activity.this);
				builder.setTitle("请选择您的设备");
				builder.setAdapter(mDevicesArrayAdapter, new android.content.DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						String info = mDevicesArrayAdapter.getItem(which);
						String address = info.substring(info.length() - 17);
						Log.i("info", info);
						Log.i("address", address);
						// 得到蓝牙设备句柄
						new btcoonthread(address).start();
					}
				});
				builder.create().show();
			}
		}

	};

	OnTouchListener listener1 = new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				switch (v.getId()){
				case R.id.UP:
					new btsenddata('a').start();
					break;
				case R.id.DOWN:
					new btsenddata('c').start();
					break;
				case R.id.LEFT:
					new btsenddata('d').start();
					break;
				case R.id.RIGHT:
					new btsenddata('b').start();
					break;
				}
			}else if(event.getAction() == MotionEvent.ACTION_UP){
				new btsenddata('e').start();
			}
			return false;
		}
	};

	OnTouchListener listener2 = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				switch (v.getId()){
				case R.id.HeadUP:
					new btsenddata('i').start();
					break;
				case R.id.HeadDOWN:
					new btsenddata('h').start();
					break;
				case R.id.HeadRIGHT:
					new btsenddata('f').start();
					break;
				case R.id.HeadLEFT:
					new btsenddata('g').start();
					break;
				}
			}else if(event.getAction() == MotionEvent.ACTION_UP){
				new btsenddata('k').start();
			}
			return false;
		}
	};


	/**
	 * 蓝牙连接线程
	 * @author tr
	 *
	 */
	class btcoonthread extends Thread {
		private String address;

		private btcoonthread(String address) {
			this.address = address;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			mbluetoothdevice = bluetoothadapter.getRemoteDevice(address);
			if (bluetoothsocket == null) {
				try {
					bluetoothsocket = mbluetoothdevice
							.createRfcommSocketToServiceRecord(UUID
									.fromString(MY_UUID));
					bluetoothsocket.connect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Message msg = Message.obtain();
				msg.obj = "蓝牙已连接上";
				UIHandler.sendMessage(msg);
			}
		}
	}

	/**
	 * 蓝牙发送数据线程
	 * 
	 * @author tr
	 * 
	 */
	class btsenddata extends Thread {

		private char message;

		private btsenddata(char message) {
			this.message = message;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			Message msg = Message.obtain();
			if (bluetoothsocket != null) {
				try {
					OutputStream btos = bluetoothsocket.getOutputStream();
					btos.write(message);
					btos.flush();
					Log.e("bluetoothsend", message+"");
					//					msg.obj = message + "信息发送成功";
					//					UIHandler.sendMessage(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					msg.obj = "信息发送失败,I/O错误";
					UIHandler.sendMessage(msg);
				}

			} else {
				msg.obj = "信息发送失败,socket为空";
				UIHandler.sendMessage(msg);
			}
		}
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(bluetoothadapter!=null && bluetoothadapter.isEnabled()){
			bluetoothadapter.disable();
		}
		super.onDestroy();
	}


}
