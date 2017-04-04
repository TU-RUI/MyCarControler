package com.example.mycarcontroller;

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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Mode2Activity extends Activity {
	Socket socket = null;
	ServerSocket server = null;
	private TextView info, myip;
	private Button connect, up, down, left, right,headup,headdown,headright,headleft,sound,face;
	private Button openlight,changecamera;
	private EditText message, ip;
	private ImageView imageview;
	//	private SurfaceView surfaceview;
	//	private SurfaceHolder sh;
	private Canvas canvas;
	private LinearLayout ll;
	String  returnmsg;
	char messagestr;
	//视频端口
	private static final int PORT = 8080;
	//传感器端口
	private static final int PORT2 = 8081;
	private static String host;
	private static final int MESSAGE = 0;
	private static final int SENSOR_INFO = 0x123;
	private static final int TEXT = 0x12345;
	private static final int MOVE = 0x12;
	private Handler UIHandler;
	private Handler ImageHandler;
	InputStream in = null;
	OutputStream out = null;
	private Boolean isLightOn = false;
	private SharedPreferences sp;
	//	private Bitmap bitmap;
	private int ScreenWidth,ScreenHeight;
	private float scalewidth,scaleheight;
	private BlockingQueue<Bitmap>  imageQueue;
	private Boolean isThreadStart = false;
	Bitmap oldbitmap = null;
	int i = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mode2);
		//		获取屏幕的宽和高  
		imageQueue = new LinkedBlockingQueue<Bitmap>();
		DisplayMetrics dm = new DisplayMetrics();  
		getWindowManager().getDefaultDisplay().getMetrics(dm);  
		ScreenWidth = dm.widthPixels;  
		ScreenHeight = dm.heightPixels;
		initView();
		// UI更新
		UIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch (msg.what) {
				case MESSAGE:
					info.append(msg.obj.toString() + "\n");
					if (msg.obj.toString().equals("连接成功")) {
						ll.setVisibility(View.GONE);
					}
					break;
				}
			}
		};
		ImageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
//				Matrix matrix = new Matrix();
//				matrix.reset();
				//				matrix.setRotate(90);
				//				byte[] data = (byte[]) msg.obj;
				//				Log.i("imagesize", data.length+"");
				//				if(bitmap!=null){
				//					bitmap.recycle();
				//				}
				//				bitmap = BitmapFactory.decodeByteArray(data, 0,
				//						data.length);
				Bitmap bitmap = (Bitmap) msg.obj;
				//								Bitmap tempbitmap = Bitmap.createBitmap(bitmap, 0, 0,
				//										bitmap.getWidth(), bitmap.getHeight(), null, true);
				Drawable drawable = imageview.getDrawable();
				if(drawable!=null && drawable instanceof BitmapDrawable){
					BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		            oldbitmap = bitmapDrawable.getBitmap();
				}
//				Bitmap oldbitmap = ((BitmapDrawable)imageview.getDrawable()).getBitmap();
//				((BitmapDrawable)(imageview.getDrawable())).getBitmap().recycle();
				imageview.setImageBitmap(bitmap);
				//				canvas = sh.lockCanvas();
				//				//				canvas.drawBitmap(tempbitmap, null, null);
				//				canvas.drawBitmap(bitmap, matrix, null);
				//				Log.i("canvasDraw", "canvasDraw");
				//				sh.unlockCanvasAndPost(canvas);
				//				bitmap.recycle();
				//				if(bitmap != null && !bitmap.isRecycled()){
				//				    bitmap.recycle();
				//				    bitmap = null;//这里最好加上这一句
				//				  Log.e("freeBitmap", "=============recycle bitmap=======");
				//				}
				if(oldbitmap!=null){
					oldbitmap.recycle();
				}
			}
		};
		new server().start();// 作为服务器线程开启
		//	new server2().srart();//接收传感器线程
	}
	
	
//	public static void releaseImageViewResouce(ImageView imageView) {
//        if (imageView == null) return;
//        Drawable drawable = imageView.getDrawable();
//        if (drawable != null && drawable instanceof BitmapDrawable) {
//            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//            Bitmap bitmap = bitmapDrawable.getBitmap();
//            if (bitmap != null && !bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
//        }
//    }

	// 设置连接按钮监听
	OnClickListener coonlistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			host = ip.getText().toString();
			Editor editor = sp.edit();
			editor.putString("ip", host);
			editor.commit();
			if (connect.getText().equals("连接")) {
				info.append("正在连接...\n");
				new ClientThread().start();
				connect.setText("断开");
			} else {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				connect.setText("连接");
				info.append("连接断开\n");
			}
		}
	};

	// 设置移动按钮监听
	OnTouchListener movelistener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				switch (v.getId()) {
				case R.id.UP:
					messagestr = 'a';
					new SendMsgThread(messagestr).start();
					break;
				case R.id.DOWN:
					messagestr = 'c';
					new SendMsgThread(messagestr).start();
					break;
				case R.id.RIGHT:
					messagestr = 'b';
					new SendMsgThread(messagestr).start();
					break;
				case R.id.LEFT:
					messagestr = 'd';
					new SendMsgThread(messagestr).start();
					break;
				}
			}else if(event.getAction() == MotionEvent.ACTION_UP){
				messagestr = 'e';
				new SendMsgThread(messagestr).start();
			}

			return false;
		}
	};
	
	// 设置舵机按钮监听
		OnTouchListener movelistener2 = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					switch (v.getId()) {
					case R.id.HeadUP:
						messagestr = 'i';
						new SendMsgThread(messagestr).start();
						break;
					case R.id.HeadDOWN:
						messagestr = 'h';
						new SendMsgThread(messagestr).start();
						break;
					case R.id.HeadLEFT:
						messagestr = 'g';
						new SendMsgThread(messagestr).start();
						break;
					case R.id.HeadRIGHT:
						messagestr = 'f';
						new SendMsgThread(messagestr).start();
						break;
					}
				}else if(event.getAction() == MotionEvent.ACTION_UP){
					messagestr = 'k';
					new SendMsgThread(messagestr).start();
				}

				return false;
			}
		};
	

	/**
	 * 设置切换摄像头打开闪光灯按钮
	 */
	OnClickListener listener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.openlight:

				messagestr = 'x';
				new SendMsgThread(messagestr).start();
				break;
			case R.id.changecamera:
				messagestr = 'y';
				new SendMsgThread(messagestr).start();
				break;
				
			case R.id.sound:
				messagestr = 's';
				new SendMsgThread(messagestr).start();
				break;
			case R.id.face:
				messagestr = 'z';
				new SendMsgThread(messagestr).start();
				break;
			}
		}
	};

	// 初始化视图
	private void initView() {
		// TODO Auto-generated method stub
		ll = (LinearLayout) findViewById(R.id.ll);
		info = (TextView) findViewById(R.id.info);
		myip = (TextView) findViewById(R.id.myip);
		myip.append(getLocalIpAddress());
		connect = (Button) findViewById(R.id.connect);
		connect.setOnClickListener(coonlistener);
		ip = (EditText) findViewById(R.id.ip);
		sp = getSharedPreferences("info", 0);
		ip.setText(sp.getString("ip", ""));
		imageview = (ImageView) findViewById(R.id.imageview);
		//		surfaceview = (SurfaceView) findViewById(R.id.surfaceview);
		//		sh = surfaceview.getHolder();
		up = (Button) findViewById(R.id.UP);
		down = (Button) findViewById(R.id.DOWN);
		left = (Button) findViewById(R.id.LEFT);
		right = (Button) findViewById(R.id.RIGHT);
		headup = (Button) findViewById(R.id.HeadUP);
		headdown = (Button) findViewById(R.id.HeadDOWN);
		headleft = (Button) findViewById(R.id.HeadLEFT);
		headright = (Button) findViewById(R.id.HeadRIGHT);
		sound = (Button) findViewById(R.id.sound);
		face = (Button) findViewById(R.id.face);
		up.setOnTouchListener(movelistener);
		down.setOnTouchListener(movelistener);
		left.setOnTouchListener(movelistener);
		right.setOnTouchListener(movelistener);
		headup.setOnTouchListener(movelistener2);
		headdown.setOnTouchListener(movelistener2);
		headright.setOnTouchListener(movelistener2);
		headleft.setOnTouchListener(movelistener2);
		openlight = (Button) findViewById(R.id.openlight);
		changecamera = (Button) findViewById(R.id.changecamera);
		openlight.setOnClickListener(listener);
		changecamera.setOnClickListener(listener);
		sound.setOnClickListener(listener);
		face.setOnClickListener(listener);
	}

	/**
	 * 客户端连接线程
	 * @author tr
	 *
	 */
	class ClientThread extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				socket = new Socket(host, PORT);
				in = socket.getInputStream();
				out = socket.getOutputStream();
				Message msg = Message.obtain();
				msg.what = MESSAGE;
				msg.obj = "连接成功";
				UIHandler.sendMessage(msg);
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
	 * 给服务端发送信号线程
	 * @author tr
	 *
	 */
	class SendMsgThread extends Thread {
		private char message;

		public SendMsgThread(char message) {
			// TODO Auto-generated constructor stub
			this.message = message;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (socket != null) {
				try {
					out.write(message);
					out.flush();
					Log.i("sendmsg", message+"");
					//					Message msg = Message.obtain();
					//					msg.what = MESSAGE;
					//					msg.obj = message + "指令发送成功";
					//					UIHandler.sendMessage(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				Message msg = Message.obtain();
				msg.what = MESSAGE;
				msg.obj = "未连接";
				UIHandler.sendMessage(msg);
			}
		}

	}

	/**
	 * 作为服务端接收视频
	 * @author tr
	 *
	 */
	class server extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				server = new ServerSocket(PORT2);
				while (!server.isClosed()) {
					Socket s = server.accept();
					//					Log.i("server", "connect");
					byte byteBuffer[] = new byte[1024];
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					InputStream in = s.getInputStream();
					// ByteArrayInputStream inputstream = new
					// ByteArrayInputStream(byteBuffer);
					int amount;
					while ((amount = in.read(byteBuffer)) != -1) {
						baos.write(byteBuffer, 0, amount);
					}
					byte[] data = baos.toByteArray();
					//					Log.i("server", "creatbitmap");
					Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
							data.length);
					//						imageQueue.put(bitmap);
					data = null;
					Message msg = Message.obtain();
					msg.obj = bitmap;
					ImageHandler.sendMessage(msg);
					//					if(!isThreadStart){
					//						new imageThread().start();
					//						isThreadStart = true;
					//					}

					in.close();
					baos.flush();
					baos.close();
					s.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				try {
					if(server!=null&&!server.isClosed()){
						server.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}


	/**
	 * 绘制图片线程
	 * @author tr
	 *
	 */
	class imageThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			Bitmap bitmap;
			try {
				while((bitmap = (Bitmap) imageQueue.take())!=null){
					//					scalewidth = (float)ScreenWidth/(float)bitmap.getWidth();
					//					scaleheight = (float)ScreenHeight/(float)bitmap.getHeight();
					//			scalewidth = scalewidth<scaleheight?scalewidth:scaleheight;
					//					Matrix matrix = new Matrix();
					//					//			matrix.setScale(scalewidth, scalewidth);
					//					matrix.reset();
					//					canvas = sh.lockCanvas();
					//					if(canvas!=null){
					//
					//						//			canvas.drawBitmap(tempbitmap, null, null);
					//						canvas.scale(scalewidth, scaleheight);
					//						canvas.drawBitmap(bitmap, matrix, null);
					//						sh.unlockCanvasAndPost(canvas);
					//						bitmap.recycle();
					//					}
					imageview.setImageBitmap(bitmap);



				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	//	public void onQuitButtonClicked(View v) {
	//		// TODO Auto-generated method stub
	//		try {
	//			server.close();
	//		} catch (IOException e1) {
	//			// TODO Auto-generated catch block
	//			e1.printStackTrace();
	//		}
	//		if (socket != null) {
	//			try {
	//				socket.close();
	//			} catch (IOException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//		}
	//		super.onDestroy();
	//		this.finish();
	//	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		try {
			if(server!=null&&!server.isClosed()){
				server.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
