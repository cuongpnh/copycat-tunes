package us.cuongpnh.study.localbroadcastmanager;

import us.cuongpnh.study.localbroadcastmanager.service.CountUpService;
import us.cuongpnh.study.localbroadcastmanager.service.CountUpService.CountUpBinder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static String TAG = MainActivity.class.toString();
	private boolean mIsServiceBound = false;
	private boolean mForceConnect = false;
	private Intent mCountUpIntent = null;
	private CountUpService mCountUpService = null;
	private TextView mTvHelloWorld = null;
	private Button mBtnSendMessage = null;
	private Button mBtnStartService = null;
	private Button mBtnStopService = null;
	private NotificationManager mNotificationManager = null;
	private static String IS_BOUND = "is_bound";
	private BroadcastReceiver mCounUpReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String s = intent.getStringExtra(CountUpService.COUNTUP_MESSAGE);
			Toast.makeText(getContext(), s.toString(), Toast.LENGTH_LONG)
					.show();
			mTvHelloWorld.setText(s.toString());
		}
	};

	private OnClickListener mBtnSendMessageOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mCountUpService.logMessage("Clicked at: "
					+ System.currentTimeMillis());
		}
	};
	private OnClickListener mBtnStartServiceOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			connectToService(false);
		}
	};
 
	private void connectToService(boolean forceConnect) {
		mForceConnect = forceConnect;
		if (mIsServiceBound == false || forceConnect) {
			if (mCountUpIntent == null) {
				mCountUpIntent = new Intent(getContext(), CountUpService.class);
			}
			bindService(mCountUpIntent, mCountUpConnection, Context.BIND_AUTO_CREATE);
			startService(mCountUpIntent);
			mBtnSendMessage.setVisibility(View.VISIBLE);
		}
	}

	private OnClickListener mBtnStopServiceOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mCountUpIntent != null && mIsServiceBound == true) {
				mNotificationManager.cancel(CountUpService.NOTIFY_ID);
				unbindService(mCountUpConnection);
				stopService(mCountUpIntent);
				mCountUpService.stopService();
				mIsServiceBound = false;
				mBtnSendMessage.setVisibility(View.GONE);
			}
		}
	};
	private ServiceConnection mCountUpConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected");
			CountUpBinder binder = (CountUpBinder) service;
			// Get Service
			mCountUpService = binder.getService();
			/*
			 * Binding old data from service here
			 */
			if (mForceConnect) {
				if (mCountUpService.getMessage() != null) {
					Log.d(TAG, "Old data: " + mCountUpService.getMessage());
					mTvHelloWorld.setText(mCountUpService.getMessage());
				}
			}

			mIsServiceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "onServiceDisconnected");
			mIsServiceBound = false;
		}
	};

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				(mCounUpReceiver),
				new IntentFilter(CountUpService.COUNTUP_RESULT));
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		if (mIsServiceBound == true && isMyServiceRunning(CountUpService.class)) {
			unbindService(mCountUpConnection);
		}
	}

	// @Override
	// protected void onResume() {
	// super.onResume();
	// if (mCountUpIntent == null && mIsServiceBound == false) {
	//
	// /*
	// * Start service
	// */
	// mCountUpIntent = new Intent(this, CountUpService.class);
	// bindService(mCountUpIntent, mCountUpConnection,
	// Context.BIND_AUTO_CREATE);
	// startService(mCountUpIntent);
	//
	// /*
	// * Register a local broadcast manager for updating UI
	// */
	// LocalBroadcastManager.getInstance(this).registerReceiver(
	// (mCounUpReceiver),
	// new IntentFilter(CountUpService.COUNTUP_RESULT));
	// }
	// }
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mCounUpReceiver);
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initData(savedInstanceState);

	}

	private void initData(Bundle savedInstanceState) {
		/*
		 * Restore last state when user start application from icon
		 */
		if (isMyServiceRunning(CountUpService.class)) {
			connectToService(true);
		} else if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(IS_BOUND, false)) {
				connectToService(true);
			}
		}

		/*
		 * Restore last state when user tap at notification
		 */
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			connectToService(true);
		}
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private void initView() {
		mTvHelloWorld = (TextView) findViewById(R.id.tvHelloWorld);
		mBtnSendMessage = (Button) findViewById(R.id.btnSendMessage);
		mBtnSendMessage.setOnClickListener(mBtnSendMessageOnClick);
		mBtnStartService = (Button) findViewById(R.id.btnStartService);
		mBtnStartService.setOnClickListener(mBtnStartServiceOnClick);
		mBtnStopService = (Button) findViewById(R.id.btnStopService);
		mBtnStopService.setOnClickListener(mBtnStopServiceOnClick);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private Context getContext() {
		return this;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_BOUND, mIsServiceBound);
	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
