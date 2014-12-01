package com.test.helloeeg;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.*;

public class HelloEEGActivity extends Activity {
	BluetoothAdapter bluetoothAdapter;

	TextView tv;
	Button b;

	private int lastAtt = Integer.MIN_VALUE;
	private int lastMed = Integer.MIN_VALUE;
	private long lastTime = System.currentTimeMillis();
	private long lastBlink = 0;

	private boolean doCam = false;

	TGDevice tgDevice;
	final boolean rawEnabled = false;

	CameraPreview mPreview;
	FrameLayout preview;
	Camera mCamera;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.textView1);
		tv.setText("");
		tv.append("Android version: "
				+ Integer.valueOf(android.os.Build.VERSION.SDK) + "\n");
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Create an instance of Camera
		mCamera = getCameraInstance();
		mCamera.setDisplayOrientation(90);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		if (bluetoothAdapter == null) {
			// Alert user that Bluetooth is not available
			Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		} else {
			/* create the TGDevice */
			tgDevice = new TGDevice(bluetoothAdapter, handler);
		}
	}

	@Override
	public void onDestroy() {
		tgDevice.close();
		super.onDestroy();
	}

	/**
	 * Handles messages from TGDevice
	 */
	private final Handler handler = new Handler() {
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			long curTime = System.currentTimeMillis();

			Camera.Parameters parameters = mCamera.getParameters();
			int zoom = parameters.getZoom();
			
			switch (msg.what) {
			case TGDevice.MSG_STATE_CHANGE:

				switch (msg.arg1) {
				case TGDevice.STATE_IDLE:
					break;
				case TGDevice.STATE_CONNECTING:
					tv.append("Connecting...\n");
					break;
				case TGDevice.STATE_CONNECTED:
					tv.append("Connected.\n");
					tgDevice.start();
					break;
				case TGDevice.STATE_NOT_FOUND:
					tv.append("Can't find\n");
					break;
				case TGDevice.STATE_NOT_PAIRED:
					tv.append("not paired\n");
					break;
				case TGDevice.STATE_DISCONNECTED:
					tv.append("Disconnected mang\n");
				}

				break;
			case TGDevice.MSG_POOR_SIGNAL:
				// signal = msg.arg1;
				// tv.append("PoorSignal: " + msg.arg1 + "\n");
				break;
			case TGDevice.MSG_RAW_DATA:
				// raw1 = msg.arg1;
				// tv.append("Got raw: " + msg.arg1 + "\n");
				break;
			case TGDevice.MSG_HEART_RATE:
				// tv.append("Heart rate: " + msg.arg1 + "\n");
				break;
			case TGDevice.MSG_ATTENTION:
				// att = msg.arg1;
				tv.append("Attention: " + msg.arg1 + "\n");
				int att = msg.arg1;

				if (att > 40 && att > lastAtt && curTime - lastTime > 1500) {
					if (!doCam) {
						tv.setBackgroundColor(Color.argb(255, 255, 119, 155));
						lastTime = curTime;
					} else {
						if(zoom+30<parameters.getMaxZoom())
							parameters.setZoom(zoom+30);
						mCamera.setParameters(parameters);
					}
				}
				lastAtt = att;
				// Log.v("HelloA", "Attention: " + att + "\n");
				break;
			case TGDevice.MSG_MEDITATION:
				// tv.append("Meditation: " + msg.arg1 + "\n");
				// int mdAtt = msg.arg1;

				/*
				 * if(mdAtt>45 && lastMed<mdAtt && curTime-lastTime>1500){
				 * tv.setBackgroundColor(Color.argb(255, 0, 206, 209)); lastTime
				 * = curTime; } lastMed = mdAtt;
				 */
				break;
			case TGDevice.MSG_BLINK:
				// tv.append("Blink: " + msg.arg1 + "\n");
				
				if (curTime - lastTime > 1500 && curTime-lastBlink<1500) {
					if(!doCam){
					tv.setBackgroundColor(Color.argb(255, 0, 206, 209));
					lastTime = curTime;
					}else{
						parameters.setZoom(0);
						if(zoom-30>0)
							parameters.setZoom(zoom-30);
						mCamera.setParameters(parameters);
					}
				}
				lastBlink = curTime;
				break;
			case TGDevice.MSG_RAW_COUNT:
				// tv.append("Raw Count: " + msg.arg1 + "\n");
				break;
			case TGDevice.MSG_LOW_BATTERY:
				// Toast.makeText(getApplicationContext(), "Low battery!",
				// Toast.LENGTH_SHORT).show();
				break;
			case TGDevice.MSG_RAW_MULTI:
				// TGRawMulti rawM = (TGRawMulti)msg.obj;
				// tv.append("Raw1: " + rawM.ch1 + "\nRaw2: " + rawM.ch2);
			default:
				break;
			}

		}
	};

	public void doStuff(View view) {
		if (tgDevice.getState() != TGDevice.STATE_CONNECTING
				&& tgDevice.getState() != TGDevice.STATE_CONNECTED)
			tgDevice.connect(rawEnabled);
		// tgDevice.ena
	}

	public void doBkg(View view) {
		doCam = false;
		tv.setVisibility(View.VISIBLE);
		preview.setVisibility(View.GONE);
	}

	public void doVideo(View view) {
		doCam = true;
		tv.setVisibility(View.GONE);
		preview.setVisibility(View.VISIBLE);
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			System.out.println("camera not available");
		}
		return c; // returns null if camera is unavailable
	}

	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;

		public CameraPreview(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
				Log.d("Mind", "Error setting camera preview: " + e.getMessage());
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// empty. Take care of releasing the Camera preview in your
			// activity.
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// set preview size and make any resize, rotate or
			// reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
				Log.d("Mind",
						"Error starting camera preview: " + e.getMessage());
			}
		}
	}
}