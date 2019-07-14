package com.casten.buzz;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static android.telephony.TelephonyManager.CALL_STATE_IDLE;
import static android.telephony.TelephonyManager.CALL_STATE_OFFHOOK;
import static android.telephony.TelephonyManager.CALL_STATE_RINGING;
import static com.casten.buzz.BuzzProto.createBuzzPayload;


public class MainActivity extends BlunoLibrary {
	private Button buttonScan;
	private Button buttonSerialSend;
	private EditText serialSendText;
	private TextView serialReceivedText;

	public void initChannels(Context context) {
		if (Build.VERSION.SDK_INT < 26) {
			return;
		}
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationChannel channel = new NotificationChannel("default",
				"Channel name",
				NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription("Channel description");
		notificationManager.createNotificationChannel(channel);
	}

	final int MY_PERMISSIONSS = 0;

	void handlePermissions(){
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONSS);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONSS);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONSS);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONSS);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_PRIVILEGED}, MY_PERMISSIONSS);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONSS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// permission was granted, yay! Do the
					// contacts-related task you need to do.
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request.
		}
	}

	Timer timer = null;

	void startRinging() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						serialSend(createBuzzPayload());
					}
				});
			}
		};
		timer.scheduleAtFixedRate(timerTask, 0, 1000);
	}

	void stopRinging() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	void registerPhoneStateListener() {
		TelephonyManager tManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tManager.listen(new PhoneStateListener() {
			public void onCallStateChanged(int state, String incomingNumber) {
				HashMap<Integer,String> strings = new HashMap<Integer,String>(){{
					put(CALL_STATE_RINGING,"Ringing");
					put(CALL_STATE_IDLE,"Idle");
					put(CALL_STATE_OFFHOOK,"Offhook");
				}};
				String str = (String)strings.get(state);
				Log.d("foo","call State: "+str+" "+state);
				switch(state) {
					case CALL_STATE_RINGING:
						startRinging();
						break;
					default:
						stopRinging();
						break;
				}
			}
			}, PhoneStateListener.LISTEN_CALL_STATE);
	}


	@RequiresApi(api = Build.VERSION_CODES.Q)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		onCreateProcess();                                                        //onCreate Process by BlunoLibrary
		initChannels(getApplicationContext());

		handlePermissions();
		registerPhoneStateListener();

		serialBegin(115200);                                                    //set the Uart Baudrate on BLE chip to 115200

		serialReceivedText = (TextView) findViewById(R.id.serialReveicedText);    //initial the EditText of the received data
		serialSendText = (EditText) findViewById(R.id.serialSendText);            //initial the EditText of the sending data

		buttonSerialSend = (Button) findViewById(R.id.buttonSerialSend);        //initial the button for sending the data
		buttonSerialSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String text = serialSendText.getText().toString();
				if (text != null && text.length() > 0) {
					// TODO Auto-generated method stub
					serialSend(createBuzzPayload(Integer.parseInt(text)));                //send the data to the BLUNO
				} else {
					Toast.makeText(getApplicationContext(), "No data!", Toast.LENGTH_LONG).show();
				}
			}
		});

		buttonScan = (Button) findViewById(R.id.buttonScan);                    //initial the button for scanning the BLE device
		buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				buttonScanOnClickProcess();                                        //Alert Dialog for selecting the BLE device
			}
		});

		Button buttonNotify = (Button) findViewById(R.id.buttonNotify);        //initial the button for sending the data
		buttonNotify.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "default")
						.setSmallIcon(R.drawable.ic_launcher_background)
						.setContentTitle("Title")
						.setContentText("Content")
						.setPriority(NotificationCompat.PRIORITY_DEFAULT);
				NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

// notificationId is a unique int for each notification that you must define
				notificationManager.notify(0, builder.build());
			}
		});
	}

	boolean notifying = false;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String text = intent.getStringExtra("verb");
			Log.d("yo",text);
			if (text.equals("start")) {
				notifying = true;
				while (notifying) {
					serialSend(createBuzzPayload());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			else{
				notifying = false;
			}
		}
	};




	private BroadcastReceiver receiver = new BroadcastReceiver() {
		private static final String TAG = "PhoneCallReceiver";
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(getApplicationContext(), "received", Toast.LENGTH_SHORT);
			Bundle extras = intent.getExtras();
			printExtras(extras);
		}
		private void printExtras(Bundle extras) {
			for (String key : extras.keySet()) {
				Object value = extras.get(key);
				Log.d(TAG, "EventSpy PhoneState extras : " + key + " = " + value);
			}
		}
	};



	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PHONE_STATE");
		registerReceiver(receiver, filter);
		super.onResume();
		System.out.println("BlUNOActivity onResume");
		onResumeProcess();                                                        //onResume Process by BlunoLibrary
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);                    //onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
    protected void onPause() {
        super.onPause();
        onPauseProcess();                                                        //onPause Process by BlunoLibrary
		unregisterReceiver(receiver);
    }

	protected void onStop() {
		super.onStop();
		onStopProcess();                                                        //onStop Process by BlunoLibrary
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
		onDestroyProcess();                                                        //onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {                                            //Four connection state
			case isConnected:
				buttonScan.setText("Connected");
				break;
			case isConnecting:
				buttonScan.setText("Connecting");
				break;
			case isToScan:
				buttonScan.setText("Scan");
				break;
			case isScanning:
				buttonScan.setText("Scanning");
				break;
			case isDisconnecting:
				buttonScan.setText("isDisconnecting");
				break;
			default:
				break;
		}
	}

	@Override
	public void onSerialReceived(String theString) {                            //Once connection data received, this function will be called
		// TODO Auto-generated method stub
		serialReceivedText.append(theString);                            //append the text into the EditText
		//The Serial data from the BLUNO may be sub-packaged, so using a buffer to hold the String is a good choice.
		((ScrollView) serialReceivedText.getParent()).fullScroll(View.FOCUS_DOWN);
	}

}