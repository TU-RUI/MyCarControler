package com.example.mycarcontroller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class Mode3Activity extends Activity implements SensorEventListener,
Camera.PreviewCallback, SurfaceHolder.Callback {

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
	private TextView ip;
	private Button bluetoothbtn;
	
	private ImageView face;
	// SurfaceView对象：(视图组件)视频显示
	private SurfaceView mSurfaceview = null;
	// SurfaceHolder对象：(抽象接口)SurfaceView支持类
	private SurfaceHolder mSurfaceHolder = null;
	// Camera对象，相机预览
	private Camera mCamera = null;
	// 开放端口,传感器数据,指令
	private static final int PORT = 8080;
	// 开放端口,视频
	private static final int PORT2 = 8081;
	private static final int MESSAGE = 0;
	private static final int MOVE = 0x12;
	// 传感器
	private SensorManager sm;
	private int sensorflag = 0;
	// 加速度,亮度,方向
	private String accelerometer, light, orientation;
	// 返回给客户端的文本,从客户端获得的文本
	private String sendString, getString;
	// 发送视频宽度比例
	private float VideoWidthRatio = 1;
	// 发送视频高度比例
	private float VideoHeightRatio = 1;
	// 发送视频宽度
	private int VideoWidth = 320;
	// 发送视频高度
	private int VideoHeight = 240;
	// 视频格式索引
	private int VideoFormatIndex = 0;
	// 视频质量
	private int VideoQuality = 40;
	// 定义服务器端套接字
	ServerSocket server = null;
	// 定义正在连接服务器的客户端的套接字
	Socket socket = null;
	// 来自客户端的输入流
	InputStream in = null;
	// 输出到客户端的输出流
	OutputStream out = null;
	// 处理当前界面UI
	Handler UIHandler;
	// 蓝牙适配器
	private BluetoothAdapter bluetoothadapter;
	// 存放蓝牙设备地址列表
	private ArrayAdapter<String> mDevicesArrayAdapter;
	private BluetoothDevice mbluetoothdevice = null;
	private BluetoothSocket bluetoothsocket = null;
	//当前摄像头位置
	private int cameraPosition = 0;
	//闪光灯
	private Boolean isLightOpen = false;

	private int tempPreRate = 0;
	private OutputStream btos;
	private MediaPlayer mediaplayer;
	private int faceId = 0;
	
	private int faces[] = {R.drawable.face0,R.drawable.face1,R.drawable.face2,R.drawable.face3,R.drawable.face4};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mode3);
		initView();
		UIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
//				text.setText(msg.obj.toString() + "\n");
				if(msg.arg1 == 9){
					ChangeFace();
				}
//				if(msg.obj.toString().equals("蓝牙已连接上")){
//					bluetoothbtn.setVisibility(View.GONE);
//					
//				}
			}
		};
		new Server().start();
	}

	/**
	 * 作为服务端,等待客户端请求线程
	 * 
	 * @author tr
	 * 
	 */
	class Server extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				server = new ServerSocket(PORT);
				while (!server.isClosed()) {
					socket = server.accept();// 等待客户端连接,堵塞
					Message msg0 = Message.obtain();
					getString = socket.getInetAddress().getHostAddress()
							+ "已连接上";
					msg0.obj = getString;
					UIHandler.sendMessage(msg0);
					OpenSensor();// 打开传感器
					out = socket.getOutputStream();
					in = socket.getInputStream();
					while (socket.isConnected()) {
						char getChar = (char) in.read();
						Log.i("getchar", getChar + "");
//						Message msg2 = Message.obtain();
						//						msg2.obj = "接收到行动指令";
						//						UIHandler.sendMessage(msg2);
						if(getChar == 'x'){
							openLight();
						}else if(getChar == 'y'){
							changeCamera();
						}else if(getChar =='s'){
							playSound();
						}else if(getChar == 'z'){

							Message message = Message.obtain();
							message.arg1 = 9;
							UIHandler.sendMessage(message);
							Log.i("changeface","changeface");
						}else{
//							new btsenddata(getChar).start();
							btos.write(getChar);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				try {
					if(socket!=null&&!socket.isClosed()){
						socket.close();
					}
					if(server!=null&&!server.isClosed()){
						server.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

		private void playSound() {
			// TODO Auto-generated method stub
			int sounds[] = {R.raw.v1,R.raw.v2,R.raw.v3,R.raw.v4,R.raw.v5};
			Random rand = new Random();
			int i = rand.nextInt(); //int范围类的随机数
			i = rand.nextInt(4); //生成0-4以内的随机数

			i = (int)(Math.random() * 4); //0-4以内的随机数，用Matn.random()方式
			mediaplayer = MediaPlayer.create(Mode3Activity.this, sounds[i]);
			mediaplayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mp.release();
				}
			});
			if(mediaplayer != null){
				mediaplayer.stop();
			}
			try {
				mediaplayer.prepare();
				mediaplayer.start();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送传感器数据
	 * 
	 * @param data
	 */
	class sendSensor extends Thread {
		private String data;

		public sendSensor(String data) {
			this.data = data;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (socket.isConnected()) {
				try {
					out.write(data.getBytes());
					out.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 作为客户端给服务端发送视频数据
	 * 
	 * @author tr
	 * 
	 */
	class SendData extends Thread {
		private byte byteBuffer[] = new byte[1024];
		private OutputStream os;
		private ByteArrayOutputStream myoutputstream;

		private SendData(ByteArrayOutputStream myoutputstream) {
			this.myoutputstream = myoutputstream;
			try {
				myoutputstream.close();
			} catch (IOException e) {
				// TODO: handle exception
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
			if (socket != null&&socket.isConnected()) {
					Log.i("senddata2", "senddata2");
					Socket tempsocket = new Socket(socket.getInetAddress()
							.getHostAddress(), PORT2);
					os = tempsocket.getOutputStream();
					ByteArrayInputStream inputstream = new ByteArrayInputStream(
							myoutputstream.toByteArray());
					int amount;
					while ((amount = inputstream.read(byteBuffer)) != -1) {
						os.write(byteBuffer, 0, amount);
					}
					myoutputstream.flush();
					myoutputstream.close();
					os.flush();
					os.close();
					tempsocket.close();
			}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			
		}
	}

	/**
	 * 打开蓝牙
	 */
	private void openBuletooch() {
		bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothadapter.isEnabled()) {
			bluetoothadapter.enable();
		}
		mDevicesArrayAdapter = new ArrayAdapter<String>(Mode3Activity.this,
				R.layout.bluetoothdevice);
		Set<BluetoothDevice> BondedDevices = bluetoothadapter
				.getBondedDevices();
		for (BluetoothDevice device : BondedDevices) {
			mDevicesArrayAdapter.add(device.getName() + "\n"
					+ device.getAddress());
		}
		if (mDevicesArrayAdapter.isEmpty()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					Mode3Activity.this);
			builder.setTitle(R.string.app_name);
			builder.setMessage("没有得到已绑定设备");
			builder.create().show();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					Mode3Activity.this);
			builder.setTitle("请选择您的设备");
			builder.setAdapter(mDevicesArrayAdapter,
					new android.content.DialogInterface.OnClickListener() {

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

	/**
	 * 蓝牙连接线程
	 * 
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
					btos = bluetoothsocket.getOutputStream();
					Message msg = Message.obtain();
					msg.obj = "蓝牙已连接上";
					UIHandler.sendMessage(msg);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Message msg = Message.obtain();
					msg.obj = "蓝牙连接失败";
					UIHandler.sendMessage(msg);
				}

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
//					OutputStream btos = bluetoothsocket.getOutputStream();
					btos.write(message);
					msg.obj = message + "信息发送成功";
					UIHandler.sendMessage(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					msg.obj = "信息发送失败1";
					UIHandler.sendMessage(msg);
				}

			} else {
				msg.obj = "信息发送失败2";
				UIHandler.sendMessage(msg);
			}
		}
	}

	// 初始化视图
	private void initView() {
		// TODO Auto-generated method stub
		// 禁止屏幕休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		ip = (TextView) findViewById(R.id.myip);
		ip = (TextView) this.findViewById(R.id.myip);
		Log.i("ip", ip+"");
		ip.setText(getLocalIpAddress());
//		text = (TextView) findViewById(R.id.info);
		//		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
		//				.getDefaultDisplay();
		//		VideoWidth = display.getWidth() / 2;
		//		VideoWidth = display.getHeight() / 2;
		//        //获取屏幕的宽和高  
		//        DisplayMetrics dm = new DisplayMetrics();  
		//        getWindowManager().getDefaultDisplay().getMetrics(dm);  
		//        VideoWidth = dm.widthPixels/2;  
		//        Log.i("VideoWidth2", VideoWidth+"");
		//        VideoHeight = dm.heightPixels/2; 
		mSurfaceview = (SurfaceView) this.findViewById(R.id.surface);
		//		LayoutParams params = (LayoutParams) mSurfaceview.getLayoutParams();
		//		params.height = VideoHeight;
		//		params.width = VideoWidth;
		//		mSurfaceview.setLayoutParams(params);
		bluetoothbtn = (Button) findViewById(R.id.bluetoothbtn);
		bluetoothbtn.setOnClickListener(new View.OnClickListener() {// 打开蓝牙

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				openBuletooch();
				
			}
		});
		
		face = (ImageView) findViewById(R.id.face);
	}
	
	
	/**
	 * 更换表情
	 */
	private void ChangeFace(){	
		faceId++;
		face.setImageDrawable(getResources().getDrawable(faces[faceId]));
		if(faceId == 4){
			faceId = -1;
		}
	}

	// 获取本地IP
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("WifiPreference IpAddress", ex.toString());
		}
		return null;
	}

	// 打开传感器
	@SuppressWarnings("deprecation")
	public void OpenSensor() {
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);// 获得SensorManager对象
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);// 加速度传感器
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_LIGHT),
				SensorManager.SENSOR_DELAY_UI);// 光线传感器
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);// 方向传感器
	}

	// 关闭传感器
	public void CloseSensor() {
		sm.unregisterListener(this);
	}

	// 传感器监听事件
	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		//		if (sensorflag == 0) {
		//			JSONObject json;
		//			Map<String, String> map = new HashMap<String, String>();
		//			switch (event.sensor.getType()) {
		//			case Sensor.TYPE_ACCELEROMETER:
		//
		//				// accelerometer = "加速度:\n" + "X: " + event.values[0] + "\n" +
		//				// "Y: "
		//				// + event.values[1] + "\n" + "Z: " + event.values[2] + "\n";
		//				map.put("state", "accelerometer");
		//				map.put("x", event.values[0] + "");
		//				map.put("y", event.values[1] + "");
		//				map.put("z", event.values[2] + "");
		//				json = new JSONObject(map);
		//				// System.out.println(accelerometer);
		//
		//				new sendSensor(json.toString()).start();
		//				break;
		//			case Sensor.TYPE_LIGHT:
		//				// light = "亮度 : " + event.values[0] + "\n";
		//				map.put("state", "light");
		//				map.put("light", event.values[0] + "");
		//				json = new JSONObject(map);
		//				new sendSensor(json.toString()).start();
		//				// System.out.println(light);
		//				break;
		//			case Sensor.TYPE_ORIENTATION:
		//				// orientation = "方向: \n" + "X: " + event.values[0] + "\n" +
		//				// "Y: "
		//				// + event.values[1] + "\n" + "Z: " + event.values[2] + "\n";
		//				map.put("state", "orientation");
		//				map.put("x", event.values[0] + "");
		//				map.put("y", event.values[1] + "");
		//				map.put("z", event.values[2] + "");
		//				json = new JSONObject(map);
		//				new sendSensor(json.toString()).start();
		//				// System.out.println(orientation);
		//				break;
		//			}
		//		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/**
	 * 摄像头预览
	 */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub

//		if(tempPreRate<1){
//			tempPreRate++;
//			return;
//		}
//		tempPreRate = 0;
		try {
			if (data != null) {
				YuvImage image = new YuvImage(data, VideoFormatIndex,
						VideoWidth, VideoHeight, null);
				if (image != null) {
					ByteArrayOutputStream outstream = new ByteArrayOutputStream();
					// 在此设置图片的尺寸和质量
					image.compressToJpeg(new Rect(0, 0,
							(int) (VideoWidthRatio * VideoWidth),
							(int) (VideoHeightRatio * VideoHeight)),
							VideoQuality, outstream);
//					
					outstream.flush();
					// 启用线程将图像数据发送出去
					new SendData(outstream).start();
					// Log.i("senddata", "senddata");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//		 initCamera();  
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				//				mCamera.startPreview();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (mCamera == null) {
			return;
		}
		//		mCamera.stopPreview();
		mCamera.setPreviewCallback(this);
		//		mCamera.setDisplayOrientation(90); // 设置横行录制
		// 获取摄像头参数
		Camera.Parameters parameters = mCamera.getParameters();
//		parameters.setPreviewFpsRange(5, 10); 
		Size size = parameters.getPreviewSize();
		VideoWidth = size.width;
//		Log.i("VideoWidth1", VideoWidth+"");
		VideoHeight = size.height;
		VideoFormatIndex = parameters.getPreviewFormat();

		mCamera.startPreview();
		mCamera.autoFocus(null);         //自动对焦  
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		try {
			if (mCamera != null) {
				mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	


	//	/**
	//	 * 初始化摄像头
	//	 */
	//	public void initCamera(){
	//		if(!isPreview){  
	//            mCamera = Camera.open(); 
	//		}
	//        if(mCamera!=null && !isPreview){  
	//            try {  
	//                Camera.Parameters parameters = mCamera.getParameters();  
	//                //每台手机的摄像头所支持的图像预览或拍摄尺寸不尽相同，  
	//                //如果设置的图像尺寸，摄像头不支持，则会出错，  
	//                //因此在真机上测试前，先要确定摄像头支持哪些尺寸  
	//                parameters.setPreviewSize(VideoWidth, VideoWidth);  //设置预览图像的尺寸大小  
	////                parameters.setPreviewFpsRange(5, 10);                 //设置每秒显示5-10帧  
	//                Size size = parameters.getPreviewSize();
	//        		VideoWidth = size.width;
	//        		VideoHeight = size.height;
	//				VideoFormatIndex = parameters.getPreviewFormat(); 
	//                mCamera.setPreviewDisplay(mSurfaceHolder);                     //通过SurfaceView显示取景画面  
	//                //回调处理预览视频流类中的onPreviewFrame方法  
	//                //在onPreviewFrame方法中，启动发送视频流的线程  
	//                mCamera.setPreviewCallback(this);      
	//                mCamera.startPreview();           //开始预览  
	//                mCamera.autoFocus(null);         //自动对焦  
	//            } catch (IOException e) {  
	//                e.printStackTrace();  
	//            }  
	//            isPreview = true;  
	//        }  
	//	}
	
	
	/**
	 * 打开或关闭闪光灯
	 */
	private void openLight(){
		if(!isLightOpen){
			Camera.Parameters parameters = mCamera.getParameters();  
			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(parameters);
			isLightOpen = true;
		}else{
			Camera.Parameters parameters = mCamera.getParameters();  
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(parameters);
			isLightOpen = false;
		}


	}

	/**
	 * 改变摄像头
	 */
	private void changeCamera(){
		int count = mCamera.getNumberOfCameras();
		if(count==1){
			return;
		}
		if(cameraPosition==0){
			if (mCamera != null) {
				mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
			mCamera = mCamera.open(1);
			
			try {
				if (mCamera != null) {
					mCamera.setPreviewDisplay(mSurfaceHolder);
					//					mCamera.startPreview();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//			mCamera.stopPreview();
			mCamera.setPreviewCallback(this);
			//			mCamera.setDisplayOrientation(90); // 设置横行录制
			// 获取摄像头参数
			Camera.Parameters parameters = mCamera.getParameters();
//			parameters.setPreviewFpsRange(5, 10); 
			Size size = parameters.getPreviewSize();
			VideoWidth = size.width;
//			Log.i("VideoWidth1", VideoWidth+"");
			VideoHeight = size.height;
			VideoFormatIndex = parameters.getPreviewFormat();

			mCamera.startPreview();
			mCamera.autoFocus(null);         //自动对焦  
			cameraPosition = 1;
		}else if(cameraPosition==1){
			if (mCamera != null) {
				mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
			mCamera = mCamera.open(0);
			try {
				if (mCamera != null) {
					mCamera.setPreviewDisplay(mSurfaceHolder);
					//					mCamera.startPreview();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//			mCamera.stopPreview();
			mCamera.setPreviewCallback(this);
			//			mCamera.setDisplayOrientation(90); // 设置横行录制
			// 获取摄像头参数
			Camera.Parameters parameters = mCamera.getParameters();
//			parameters.setPreviewFpsRange(5, 10); 
			Size size = parameters.getPreviewSize();
			VideoWidth = size.width;
			VideoHeight = size.height;
			VideoFormatIndex = parameters.getPreviewFormat();

			mCamera.startPreview();
			mCamera.autoFocus(null);         //自动对焦  
			cameraPosition = 0;
		}

	}

	// Activity开始
	protected void onStart() {
		// TODO Auto-generated method stub
		mSurfaceHolder = mSurfaceview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
		mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 设置显示器类型，setType必须设置
		super.onStart();
		try {
			mCamera = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onStop() {
		super.onStop();
		try {
			if(server!=null&&!server.isClosed()){
				server.close();
			}
			if(socket!=null&&!socket.isClosed()){
				socket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if (mCamera != null) {
				mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}
