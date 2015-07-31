package us.cuongpnh.study.localbroadcastmanager.service;

import us.cuongpnh.study.localbroadcastmanager.MainActivity;
import us.cuongpnh.study.localbroadcastmanager.R;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CountUpService extends Service {

	public static String TAG = CountUpService.class.toString();
	public static final String COUNTUP_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
	public static final String COUNTUP_MESSAGE = "countup_message";
	private final IBinder mCountUpBinder = new CountUpBinder();
	private LocalBroadcastManager mBroadcaster = null;
	public static final int NOTIFY_ID = 1;
	private String mMessage = null;
	private Intent mResultIntent = null;
	private NotificationManager mNotificationManager = null;
	private NotificationCompat.Builder mNotifyBuilder = null;
	private final Handler mHandler = new Handler();

	private Runnable mSendUpdatesToUI = new Runnable() {
		public void run() {
			DisplayLoggingInfo();
			mHandler.postDelayed(this, 5000); // Interval 5 seconds
		}
	};

	// @Override
	// public int onStartCommand(Intent intent, int flags, int startId) {
	// return START_STICKY;
	// }

	@Override
	public void onCreate() {
		Log.d(TAG, (mMessage != null) ? mMessage : "Empty");
		super.onCreate();
		mBroadcaster = LocalBroadcastManager.getInstance(this);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mResultIntent = new Intent(this, MainActivity.class);
	}

	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		mHandler.removeCallbacks(mSendUpdatesToUI);
		mHandler.postDelayed(mSendUpdatesToUI, 1000);
	}

	private void DisplayLoggingInfo() {
		Log.d(TAG, "entered DisplayLoggingInfo");
		logMessage("Countup to: " + System.currentTimeMillis() + "");
	}

	@SuppressLint("NewApi")
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		createNotification();
		return mCountUpBinder;
	}

	public void sendResult(String message) {
		Intent intent = new Intent(COUNTUP_RESULT);
		if (message != null)
			intent.putExtra(COUNTUP_MESSAGE, message);
		mBroadcaster.sendBroadcast(intent);
	}

	public class CountUpBinder extends Binder {
		public CountUpService getService() {
			return CountUpService.this;
		}
	}

	// @Override
	// public boolean onUnbind(Intent intent) {
	// Log.d(TAG, "onUnbind");
	// return super.onUnbind(intent);
	// }

	public void logMessage(String message) {
		Log.d(TAG, message);
		this.mMessage = message;
		updateNotification();
		sendResult(message);
	}

	@SuppressLint("NewApi")
	private void updateNotification() {
		// Sets an ID for the notification, so it can be updated
		mNotifyBuilder.setContentTitle("New Message").setContentText(mMessage);
		fillNotificationContent();
	}

	@SuppressLint("NewApi")
	private void updateNotification(String message) {
		// Sets an ID for the notification, so it can be updated
		mNotifyBuilder.setContentTitle("New Message").setContentText(message);
		fillNotificationContent();
	}

	@SuppressLint("NewApi")
	private void createNotification() {
		mNotifyBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(mMessage != null ? mMessage : "Empty")
				.setContentText("Hello World!").setOngoing(true);

		fillNotificationContent();
	}

	@SuppressLint("NewApi")
	private void fillNotificationContent() {

		// // Creates an explicit intent for an Activity in your app
		mResultIntent.putExtra(COUNTUP_MESSAGE, (mMessage != null) ? mMessage
				: "Empty");
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// // Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// // Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(mResultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mNotifyBuilder.setContentIntent(resultPendingIntent);
		// mNotifyBuilder.setContentIntent(resultPendingIntent);
		// int numMessages = 0;
		// Start of a loop that processes data and then notifies the user
		// mNotifyBuilder.setContentText(mMessage).setNumber(++numMessages);
		// Because the ID remains unchanged, the existing notification is
		// updated.
		mNotificationManager.notify(NOTIFY_ID, mNotifyBuilder.build());
	}

	public String getMessage() {
		return mMessage != null ? mMessage : "Empty";
	}

	public void stopService() {
		stopSelf();
		mHandler.removeCallbacks(mSendUpdatesToUI);
	}
}
