package us.cuongpnh.study.localbroadcastmanager.service;

import java.util.ArrayList;
import java.util.Random;

import us.cuongpnh.study.localbroadcastmanager.MediaPlayerActivity;
import us.cuongpnh.study.localbroadcastmanager.PreventStopServiceActivity;
import us.cuongpnh.study.localbroadcastmanager.R;
import us.cuongpnh.study.localbroadcastmanager.model.Song;
import us.cuongpnh.study.localbroadcastmanager.receiver.MediaPlayerReceiver;
import us.cuongpnh.study.localbroadcastmanager.util.AudioFocusHelper;
import us.cuongpnh.study.localbroadcastmanager.util.MediaButtonHelper;
import us.cuongpnh.study.localbroadcastmanager.util.MusicFocusable;
import us.cuongpnh.study.localbroadcastmanager.util.RemoteControlClientCompat;
import us.cuongpnh.study.localbroadcastmanager.util.RemoteControlHelper;
import us.cuongpnh.study.localbroadcastmanager.util.Utils;
import us.cuongpnh.study.localbroadcastmanager.widget.MediaPlayerWidgetProviderSmall;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;

@SuppressWarnings("deprecation")
@SuppressLint("InlinedApi")
public class MediaPlayerService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener, MusicFocusable {
	public static String TAG = MediaPlayerService.class.toString();
	private static MediaPlayerService INSTANCE = null;

	public static final String NAVIGATION_SONG = "us.cuongpnh.study.localbroadcastmanager.NAVIGATION_SONG";
	public static final String NAVIGATION_SONG_TYPE = "us.cuongpnh.study.localbroadcastmanager.NAVIGATION_SONG_TYPE";
	public static final String SENDER_TYPE = "us.cuongpnh.study.localbroadcastmanager.SENDER_TYPE";
	public static final String PLAYER_LOG = "us.cuongpnh.study.localbroadcastmanager.PLAYER_LOG";
	public static final String PLAYER_UPDATE_UI = "us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService.PLAYER_UPDATE_UI";
	public static final String PLAYER_UPDATE_PLAY_PAUSE_UI = "us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService.PLAYER_UPDATE_PLAY_PAUSE_UI";
	public static final String PLAYER_UNBIND = "us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService.PLAYER_UNBIND";

	public static final String WIDGET = "widget";
	public static final String SONG_TITLE = "song_title";
	public static final String LOG_MESSAGE = "countup_message";
	public static final String MODE_REPEAT = "mode_repeat";
	public static final String FORCE_UPDATE = "force_update";
	public static final String MODE_SHUFFLE = "mode_shuffle";
	public static final String SONG_DURATION = "duration";
	public static final String SONG_CURRENT_POSTION = "current_position";
	public static final String IS_START_FROM_WIDGET = "is_start_from_widget";

	public static final int INIT_TIME = -1;
	public static final int PREVIOUS_SONG = 1;
	public static final int NEXT_SONG = 2;
	public static final int PLAY_PAUSE_SONG = 3;
	public static final int STOP_SONG = 4;
	public static final int SHUFFLE_SONG = 5;
	public static final int REPEAT_SONG = 6;
	public static final int NOTIFY_ID = 20052014;

	public static final String ACTION_TOGGLE_PLAYBACK = "us.cuongpnh.study.localbroadcastmanager.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "us.cuongpnh.study.localbroadcastmanager.action.PLAY";
	public static final String ACTION_PAUSE = "us.cuongpnh.study.localbroadcastmanager.action.PAUSE";
	public static final String ACTION_STOP = "us.cuongpnh.study.localbroadcastmanager.action.STOP";
	public static final String ACTION_SKIP = "us.cuongpnh.study.localbroadcastmanager.action.SKIP";
	public static final String ACTION_REWIND = "us.cuongpnh.study.localbroadcastmanager.action.REWIND";
	public static final String ACTION_URL = "us.cuongpnh.study.localbroadcastmanager.action.URL";

	private final Handler mHandler = new Handler();
	private final IBinder mMediaPlayerBinder = new MediaPlayerBinder();

	private String mMessage = null;
	public boolean mIsInitUI = false;
	public boolean mIsStartFromWidget = false;
	private Intent mResultIntent = null;
	private LocalBroadcastManager mBroadcaster = null;
	private RemoteControlClientCompat mRemoteControlClientCompat = null;
	/*
	 * 26-02-2015 23:17 I decided change from notification manager to
	 * startForeground. This is the reason:
	 * http://stackoverflow.com/questions/3538728
	 * /what-is-the-difference-between-a-background-and-foreground-service
	 */

	@SuppressWarnings("unused")
	private NotificationManager mNotificationManager = null;
	private NotificationCompat.Builder mNotifyBuilder = null;
	private RemoteViews mCustomNotificationView = null;
	private RemoteViews mCustomNotificationBigView = null;
	private Notification mNotification = null;
	private MediaPlayer mPlayer = null;
	private ArrayList<Song> mSongs = null;
	public static ArrayList<Song> mSongsBackup = null;
	public static int mSongPositionBackup = 0;
	private ArrayList<Integer> mShuffleHolder = null;
	private int mShuffleIndex = -1;
	private int mSongPosition = 0;

	private boolean mIsShuffle = false;
	private boolean mIsRepeat = false;
	private static boolean mIsShuffleBackup = false;
	private static boolean mIsRepeatBackup = false;
	private boolean mIsNeedSeek = false;
	private int mSeekProgress = 0;
	private AudioManager mAudioManager = null;
	private ComponentName mMediaButtonReceiverComponent = null;
	private AudioFocusHelper mAudioFocusHelper = null;
	public static final float DUCK_VOLUME = 0.1f;

	// do we have audio focus?
	enum AudioFocus {
		NoFocusNoDuck, // we don't have audio focus, and can't duck
		NoFocusCanDuck, // we don't have focus, but can play at a low volume
						// ("ducking")
		Focused // we have full audio focus
	}

	private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

	private Runnable mSendUpdatesToUI = new Runnable() {
		public void run() {
			updateUI();
			mHandler.postDelayed(this, 1000);
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		// if (intent == null) {
		// /*
		// * Using when user force stop media player by removing application
		// * from recent task. If you return START_REDELIVER_INTENT instead of
		// START_STICKY, then you can delete this code block
		// */
		// stopSelf();
		// return START_NOT_STICKY;
		// }
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			mIsStartFromWidget = bundle.getBoolean(IS_START_FROM_WIDGET, false);
			if (mIsStartFromWidget) {
				Log.d(TAG, "mIsStartFromWidget");
				restoreSongDataFromStaticField();
				if (mSongs == null || mSongs.isEmpty()) {
					/*
					 * In case no song list stored in static field
					 */
					restoreSongDataFromPrefs();
					if (mSongs == null || mSongs.isEmpty()) {
						/*
						 * In case no song list stored in shared preference We
						 * should load a default song list here!
						 */
						getDefaultSongList();
					}
				}
				createNotification();
				play();
				mIsStartFromWidget = false;
			}
		}
		return START_REDELIVER_INTENT;
		/*
		 * Change from START_STICKY to START_REDELIVER_INTENT to prevent error
		 * when start app from widget after remove app from recent list
		 */
		// return START_STICKY;
	}

	public static MediaPlayerService getInstance() {
		return INSTANCE;
	}

	public MediaPlayer getPlayer() {
		return mPlayer;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		super.onCreate();
		if (INSTANCE == null) {
			Log.d(TAG, "Singleton");
			INSTANCE = this;
		}
		initData();
	}

	@SuppressLint("NewApi")
	private void initData() {
		mBroadcaster = LocalBroadcastManager.getInstance(this);
		mCustomNotificationView = new RemoteViews(getPackageName(),
				R.layout.custom_notification);
		mCustomNotificationBigView = new RemoteViews(getPackageName(),
				R.layout.custom_notification_big);
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		mMediaButtonReceiverComponent = new ComponentName(this,
				MediaPlayerReceiver.class);

		mResultIntent = new Intent(this, MediaPlayerActivity.class);

		mNotifyBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat_action_notification)
				.setContentTitle(mMessage != null ? mMessage : "Empty")
				.setContentText("Hello World!").setOngoing(true);

		if (android.os.Build.VERSION.SDK_INT >= 8) {
			mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(),
					this);
		} else {
			mAudioFocus = AudioFocus.Focused; // no focus feature, so we always
												// "have" audio focus
		}

		initMusicPlayer();
		// getSongList2();
		// createNotification();
		// play();
	}

	public void initMusicPlayer() {
		mSongs = new ArrayList<Song>();
		mShuffleHolder = new ArrayList<Integer>();
		mSongPosition = 0;

		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
	}

	public void getDefaultSongList() {
		mSongs = new ArrayList<Song>();
		// query external audio
		ContentResolver musicResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null,
				null);
		// iterate over results if valid
		if (musicCursor != null && musicCursor.moveToFirst()) {
			// get columns
			int titleColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int idColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
			int artistColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
			int albumColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM);
			int durationColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION);
			int albumIdColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

			// add songs to list
			do {
				long thisId = musicCursor.getLong(idColumn);
				String thisTitle = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				String thisAlbum = musicCursor.getString(albumColumn);
				long thisDuration = musicCursor.getLong(durationColumn);
				long thisAlbumId = musicCursor.getLong(albumIdColumn);
				mSongs.add(new Song(thisId, thisArtist, thisTitle, thisAlbum,
						thisDuration, thisAlbumId));
			} while (musicCursor.moveToNext());
		}
		musicCursor.close();
	}

	private boolean canChanageSong() {
		if (mSongs == null || mSongs.isEmpty()
				|| (!mSongs.isEmpty() && mSongs.size() == 1)) {
			return false;
		}
		return true;
	}

	public void previous() {
		if (!canChanageSong()) {
			return;
		}
		mSongPosition = Math.abs(mSongPosition - 1 + mSongs.size())
				% mSongs.size();
		play();
	}

	public void next() {
		if (!canChanageSong()) {
			return;
		}
		mSongPosition = Math.abs(mSongPosition + 1) % mSongs.size();
		play();
	}

	public void play() {
		Log.d(TAG, "Play");
		if (mSongs == null || mSongs.isEmpty()) {
			Log.d(TAG, "Playlist is empty");
			sendResult("Playlist is empty");
			return;
		}
		if (mShuffleHolder.isEmpty()) {
			makeShuffleHolder();
		}
		setShuffleIndex();
		mPlayer.reset();
		mIsInitUI = false;
		Song playSong = mSongs.get(mSongPosition);
		long currSong = playSong.getId();
		sendResult("Play song: " + playSong.getTitle());
		Log.d(TAG, "Play song: " + playSong.getTitle());
		Uri trackUri = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				currSong);
		try {
			mPlayer.setDataSource(this, trackUri);
		} catch (Exception e) {
			Log.e(TAG, "Song not found", e);
			sendResult("Song not found");
			/*
			 * Remote song which not found and play next song.
			 */
			mSongs.remove(mSongPosition);
			mShuffleHolder.clear();
			play();
		}
		tryToGetAudioFocus();
		lockScreenControls();
		if (mRemoteControlClientCompat != null) {
			mRemoteControlClientCompat
					.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		}

		mPlayer.prepareAsync();
	}

	private void makeShuffleHolder() {
		for (int i = 0; i < mSongs.size(); i++) {
			mShuffleHolder.add(i);
		}
	}

	private void setShuffleIndex() {
		for (int i = 0; i < mShuffleHolder.size(); i++) {
			if (mShuffleHolder.get(i) == mSongPosition) {
				mShuffleIndex = i;
			}
		}
	}

	public void setSong(int songIndex) {
		mSongPosition = songIndex;
	}

	@SuppressLint("NewApi")
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		createNotification();
		return mMediaPlayerBinder;
	}

	public void sendResult(String message) {
		Intent intent = new Intent(PLAYER_LOG);
		if (message != null)
			intent.putExtra(LOG_MESSAGE, message);
		mBroadcaster.sendBroadcast(intent);
	}

	public void updateUI() {
		if (mPlayer == null) {
			return;
		}
		if (mPlayer.isPlaying() == false) {
			return;
		}
		/*
		 * Update activity's UI
		 */
		updateActivityUI(false);

		/*
		 * Update Widget's UI
		 */
		updateWidgetUI();
	}

	private void updateWidgetUI() {
		updateWidgetUI4x1();
	}

	private void updateWidgetUI4x1() {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());
		Intent intent = new Intent(this, MediaPlayerWidgetProviderSmall.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		ComponentName thisWidget = new ComponentName(getApplicationContext(),
				MediaPlayerWidgetProviderSmall.class);

		int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(intent);
	}

	private void updateActivityUI(boolean force) {
		Intent intent = new Intent(PLAYER_UPDATE_UI);
		intent.putExtra(SONG_TITLE, mSongs.get(mSongPosition).getTitle());
		intent.putExtra(SONG_DURATION, mPlayer.getDuration());
		intent.putExtra(SONG_CURRENT_POSTION, mPlayer.getCurrentPosition());
		if (force) {
			intent.putExtra(FORCE_UPDATE, true);
		}
		mBroadcaster.sendBroadcast(intent);
	}

	public void forceUpdateUI() {
		if (mPlayer == null) {
			return;
		}
		updateActivityUI(true);
	}

	private void updatePlayPauseUI() {
		/*
		 * For activity
		 */
		Intent intent = new Intent(PLAYER_UPDATE_PLAY_PAUSE_UI);
		mBroadcaster.sendBroadcast(intent);
		/*
		 * For widget
		 */
		updateWidgetUI();
	}

	private void unbindConnection() {
		Intent intent = new Intent(PLAYER_UNBIND);
		mBroadcaster.sendBroadcast(intent);
	}

	public boolean isPause() {
		return mPlayer.getCurrentPosition() > 0 ? true : false;
	}

	public void resetUI() {
		Intent intent = new Intent(PLAYER_UPDATE_UI);
		intent.putExtra(SONG_DURATION, INIT_TIME);
		intent.putExtra(SONG_CURRENT_POSTION, INIT_TIME);
		mBroadcaster.sendBroadcast(intent);
	}

	public class MediaPlayerBinder extends Binder {
		public MediaPlayerService getService() {
			return MediaPlayerService.this;
		}
	}

	public void logMessage(String message) {
		this.mMessage = message;
		sendResult(message);
	}

	@SuppressLint("NewApi")
	private void updateNotification(String title, String content, long albumId) {
		Bitmap bitmap = getCoverArtBitmap(this, albumId);

		mNotifyBuilder.setContentTitle(title).setContentText(content);
		/*
		 * Update normal notification
		 */
		mCustomNotificationView.setTextViewText(R.id.tvCustomNotificationTitle,
				title);
		mCustomNotificationView.setTextViewText(
				R.id.tvCustomNotificationContent, content);
		mCustomNotificationView.setImageViewBitmap(R.id.ivSongDisplay, bitmap);

		/*
		 * Update big notification
		 */
		mCustomNotificationBigView.setTextViewText(
				R.id.tvCustomNotificationBigTitle, title);
		mCustomNotificationBigView.setTextViewText(
				R.id.tvCustomNotificationBigContent, content);
		mCustomNotificationBigView.setImageViewBitmap(R.id.ivSongDisplayBig,
				bitmap);
		/*
		 * Update play/pause UI in notification
		 */
		mCustomNotificationView.setImageViewResource(R.id.ivPlayPauseSong,
				R.drawable.pb_pause);
		mCustomNotificationBigView.setImageViewResource(
				R.id.ivPlayPauseSongBig, R.drawable.pb_pause);

		fillNotificationContent();
	}

	@SuppressLint("NewApi")
	private void createNotification() {
		Intent previousSongIntent = new Intent(NAVIGATION_SONG);
		previousSongIntent.putExtra(NAVIGATION_SONG_TYPE, PREVIOUS_SONG);
		PendingIntent piPreviousSong = PendingIntent.getBroadcast(this, 1,
				previousSongIntent, 0);

		Intent nextSongIntent = new Intent(NAVIGATION_SONG);
		nextSongIntent.putExtra(NAVIGATION_SONG_TYPE, NEXT_SONG);
		PendingIntent piNextSong = PendingIntent.getBroadcast(this, 2,
				nextSongIntent, 0);

		Intent stopSongIntent = new Intent(NAVIGATION_SONG);
		stopSongIntent.putExtra(NAVIGATION_SONG_TYPE, STOP_SONG);
		PendingIntent piStopSong = PendingIntent.getBroadcast(this, 3,
				stopSongIntent, 0);

		Intent playPauseSongIntent = new Intent(NAVIGATION_SONG);
		playPauseSongIntent.putExtra(NAVIGATION_SONG_TYPE, PLAY_PAUSE_SONG);
		PendingIntent piPlayPauseSong = PendingIntent.getBroadcast(this, 4,
				playPauseSongIntent, 0);
		/*
		 * Setup pending intent for normal notification
		 */
		mCustomNotificationView.setOnClickPendingIntent(R.id.ivNextSong,
				piNextSong);
		mCustomNotificationView.setOnClickPendingIntent(R.id.ivStopSong,
				piStopSong);
		mCustomNotificationView.setOnClickPendingIntent(R.id.ivPlayPauseSong,
				piPlayPauseSong);
		/*
		 * Setup pending intent for big notification
		 */

		mCustomNotificationBigView.setOnClickPendingIntent(R.id.ivNextSongBig,
				piNextSong);
		mCustomNotificationBigView.setOnClickPendingIntent(R.id.ivStopSongBig,
				piStopSong);
		mCustomNotificationBigView.setOnClickPendingIntent(
				R.id.ivPlayPauseSongBig, piPlayPauseSong);
		mCustomNotificationBigView.setOnClickPendingIntent(R.id.ivPrevSongBig,
				piPreviousSong);
		fillNotificationContent();
	}

	@SuppressLint("NewApi")
	private void fillNotificationContent() {
		// Creates an explicit intent for an Activity in your app
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// // Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MediaPlayerActivity.class);
		// // Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(mResultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mNotifyBuilder.setContentIntent(resultPendingIntent);
		// int numMessages = 0;
		// Start of a loop that processes data and then notifies the user
		// mNotifyBuilder.setContentText(mMessage).setNumber(++numMessages);
		// Because the ID remains unchanged, the existing notification is
		// updated.

		mNotification = mNotifyBuilder.build();
		mNotification.bigContentView = mCustomNotificationBigView;
		mNotification.contentView = mCustomNotificationView;
		startForeground(NOTIFY_ID, mNotification);

		// mNotifyBuilder.setContent(mCustomNotificationView);
		// startForeground(NOTIFY_ID, mNotifyBuilder.build());
	}

	public String getMessage() {
		return mMessage != null ? mMessage : "Empty";
	}

	public void stop() {
		/*
		 * Backup for playing from widget after stopping media player in
		 * notification
		 */
		// backupSongDataToStaticField();
		backupSongData();

		if (mRemoteControlClientCompat != null) {
			mRemoteControlClientCompat
					.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		}
		giveUpAudioFocus();
		/*
		 * Send broadcast to notify all binder to unbind the media player
		 */
		unbindConnection();
		stopSelf();
		stopForeground(true);
		mHandler.removeCallbacks(mSendUpdatesToUI);
		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
		mSongs = null;
		mShuffleHolder = null;
		mSongPosition = 0;
		mShuffleIndex = 0;
		INSTANCE = null;

		/*
		 * For updating button play/pause
		 */
		updateWidgetUI();
	}

	private void restoreSongDataFromStaticField() {
		mSongs = mSongsBackup;
		mSongPosition = mSongPositionBackup;
		mIsShuffle = mIsShuffleBackup;
		mIsRepeat = mIsRepeatBackup;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		/*
		 * Handler repeat mode here
		 */
		sendResult("onCompletion");
		Log.d(TAG, "onCompletion " + mPlayer.getCurrentPosition());

		if (!isRepeat() && !isShuffle()) {
			updatePlayPauseUI(false);
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
			}
			// resetUI();
		}
		if (isRepeat()) {
			play();
		}
		if (isShuffle()) {
			if (canChanageSong()) {
				getShuffleSong();
			}
			// setSong(getRandomNumber(0, mSongs.size() - 1));
			play();
		}
	}

	private void getShuffleSong() {
		if (mShuffleHolder.size() == 0) {
			makeShuffleHolder();
		}
		int randIndex = getRandomNumber(0, mShuffleHolder.size() - 1);
		// mShuffleIndex = getRandomNumber(0, mShuffleHolder.size() - 1);
		mSongPosition = mShuffleHolder.get(randIndex);
	}

	private int getRandomNumber(int min, int max) {
		Random r = new Random();
		return r.nextInt(max - min + 1) + min;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		sendResult("onError");
		giveUpAudioFocus();
		return false;
	}

	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onPrepared(MediaPlayer mp) {
		Song playingSong = mSongs.get(mSongPosition);
		mp.start();
		if (mIsNeedSeek) {
			mp.seekTo(mSeekProgress);
			mIsNeedSeek = false;
		}
		if (isShuffle() && mShuffleIndex != -1
				&& mShuffleIndex < mShuffleHolder.size()) {
			mShuffleHolder.remove(mShuffleIndex);
		}

		updateNotification(playingSong.getTitle(), playingSong.getArtist(),
				playingSong.getAlbumId());
		/*
		 * Storing song list to shared preference and static so that we can
		 * restore it when user clear recent app or start from widget. Static
		 * field use to boost up speed.
		 */
		backupSongData();
		mHandler.removeCallbacks(mSendUpdatesToUI);
		mHandler.postDelayed(mSendUpdatesToUI, 100);
	}

	private void backupSongData() {
		backupSongDataToStaticField();
		backupSongDataToPrefs();
	}

	private void backupSongDataToStaticField() {
		mSongsBackup = mSongs;
		mSongPositionBackup = mSongPosition;
		mIsShuffleBackup = mIsShuffle;
		mIsRepeatBackup = mIsRepeat;
	}

	private void backupSongDataToPrefs() {
		Utils.storeInt(this, Utils.PREFS_SONG_INDEX, mSongPosition);
		Utils.storeBoolean(this, Utils.PREFS_SONG_SHUFFLE, mIsShuffle);
		Utils.storeBoolean(this, Utils.PREFS_SONG_REPEAT, mIsRepeat);
	}

	@SuppressWarnings("unchecked")
	private void restoreSongDataFromPrefs() {
		mSongs = (ArrayList<Song>) Utils.getObject(this, Utils.PREFS_SONG_LIST,
				new TypeToken<ArrayList<Song>>() {
				}.getType(), new ArrayList<Song>());
		mSongPosition = Utils.getInt(this, Utils.PREFS_SONG_INDEX, 0);
		mIsShuffle = Utils.getBoolean(this, Utils.PREFS_SONG_SHUFFLE, false);
		mIsRepeat = Utils.getBoolean(this, Utils.PREFS_SONG_REPEAT, false);
	}

	public void pause() {
		if (isPlaying()) {
			mPlayer.pause();
			updatePlayPauseUI(false);
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
			}
		}
	}

	private void updatePlayPauseUI(boolean isPlaying) {
		/*
		 * For notification
		 */
		if (isPlaying) {
			mCustomNotificationView.setImageViewResource(R.id.ivPlayPauseSong,
					R.drawable.pb_pause);
			mCustomNotificationBigView.setImageViewResource(
					R.id.ivPlayPauseSongBig, R.drawable.pb_pause);
		} else {
			mCustomNotificationView.setImageViewResource(R.id.ivPlayPauseSong,
					R.drawable.pb_play);
			mCustomNotificationBigView.setImageViewResource(
					R.id.ivPlayPauseSongBig, R.drawable.pb_play);
		}
		fillNotificationContent();
		/*
		 * For activity and widget
		 */
		updatePlayPauseUI();
	}

	public void resume() {
		if (mPlayer != null && !mPlayer.isPlaying()) {
			mPlayer.start();
			updatePlayPauseUI(true);
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			}
		}
	}

	public void setSongList(ArrayList<Song> songList) {
		mSongs = songList;
		/*
		 * Storing song list to shared preference so that we can restore it when
		 * user clear recent app or start from widget
		 */
		Utils.storeObject(this, Utils.PREFS_SONG_LIST, mSongs);
	}

	public ArrayList<Song> getSongList() {
		return mSongs;
	}

	public boolean isShuffle() {
		return mIsShuffle;
	}

	public void setShuffle(boolean mIsShuffle) {
		this.mIsShuffle = mIsShuffle;
	}

	public void toggleShuffle() {
		this.mIsShuffle = !this.mIsShuffle;
		if (mIsShuffle) {
			mIsRepeat = false;
		}
		updateWidgetUI();
	}

	public void toggleRepeat() {
		this.mIsRepeat = !this.mIsRepeat;
		if (mIsRepeat) {
			mIsShuffle = false;
		}
		updateWidgetUI();
	}

	public boolean isRepeat() {
		return mIsRepeat;
	}

	public void setRepeat(boolean mIsRepeat) {
		this.mIsRepeat = mIsRepeat;
	}

	public boolean isInitUI() {
		return mIsInitUI;
	}

	public void setInitUI(boolean mIsInitUI) {
		this.mIsInitUI = mIsInitUI;
	}

	public void seekTo(int position) {
		if (mPlayer == null) {
			return;
		}
		if (isPlaying()) {
			mPlayer.seekTo(position);
		} else {
			mSeekProgress = position;
			mIsNeedSeek = true;
		}

	}

	private void lockScreenControls() {
		Song playingItem = mSongs.get(mSongPosition);
		Bitmap bitmap = getCoverArtBitmap(this, playingItem.getAlbumId());
		MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager,
				mMediaButtonReceiverComponent);

		// Use the remote control APIs (if available) to set the playback
		// state

		if (mRemoteControlClientCompat == null) {
			Log.d("mRemoteControlClientCompat", "mRemoteControlClientCompat");
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(mMediaButtonReceiverComponent);
			mRemoteControlClientCompat = new RemoteControlClientCompat(
					PendingIntent.getBroadcast(this /* context */, 0 /*
																	 * requestCode
																	 * , ignored
																	 */,
							intent /* intent */, 0 /* flags */));
			RemoteControlHelper.registerRemoteControlClient(mAudioManager,
					mRemoteControlClientCompat);
		}

		mRemoteControlClientCompat
				.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY
						| RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
						| RemoteControlClient.FLAG_KEY_MEDIA_NEXT
						| RemoteControlClient.FLAG_KEY_MEDIA_STOP);

		// Update the remote controls
		mRemoteControlClientCompat
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
						playingItem.getArtist())
				.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
						playingItem.getAlbum())
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
						playingItem.getTitle())
				.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
						playingItem.getDuration())
				// TODO: fetch real item artwork
				.putBitmap(
						RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
						bitmap).apply();
	}

	@Override
	public void onGainedAudioFocus() {
		Toast.makeText(getApplicationContext(), "gained audio focus.",
				Toast.LENGTH_SHORT).show();
		mAudioFocus = AudioFocus.Focused;

		// restart media player with new focus settings
		if (mPlayer != null) {
			configAndStartMediaPlayer();
		}
	}

	@Override
	public void onLostAudioFocus(boolean canDuck) {
		Toast.makeText(getApplicationContext(),
				"lost audio focus." + (canDuck ? "can duck" : "no duck"),
				Toast.LENGTH_SHORT).show();
		mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck
				: AudioFocus.NoFocusNoDuck;

		// start/restart/pause media player with new focus settings
		if (mPlayer != null && mPlayer.isPlaying()) {
			configAndStartMediaPlayer();
		}
	}

	private void configAndStartMediaPlayer() {
		if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
			// If we don't have audio focus and can't duck, we have to pause,
			// even if mState
			// is State.Playing. But we stay in the Playing state so that we
			// know we have to resume
			// playback once we get the focus back.
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				updatePlayPauseUI(false);
			}
			return;
		} else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
			if (mPlayer.isPlaying()) {
				mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME); // we'll be
																// relatively
				// quiet
			}
		} else {
			mPlayer.setVolume(1.0f, 1.0f); // we can be loud
			/*
			 * We should update lock-screen media button after gaining focus
			 * (again)
			 */
			lockScreenControls();
			if (mPlayer.isPlaying()) {
				if (mRemoteControlClientCompat != null) {
					mRemoteControlClientCompat
							.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
				}
			} else {
				if (mRemoteControlClientCompat != null) {
					mRemoteControlClientCompat
							.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
				}
			}
		}

		// if (!mPlayer.isPlaying()) {
		// mPlayer.start();
		// }
	}

	private void tryToGetAudioFocus() {
		if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.requestFocus())
			mAudioFocus = AudioFocus.Focused;
	}

	private void giveUpAudioFocus() {
		if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.abandonFocus())
			mAudioFocus = AudioFocus.NoFocusNoDuck;
	}

	public Song getCurrentSong() {
		if (getInstance() == null) {
			return null;
		}
		if (mSongs == null || mSongs.isEmpty()) {
			return null;
		}
		return mSongs.get(mSongPosition);
	}

	public static Song getCurrentSongFromBackup(Context context) {
		Song song = getCurrentSongFromStaticField();
		if (song == null) {
			song = getCurrentSongFromPrefs(context);
		}
		return song;
	}

	@SuppressWarnings("unchecked")
	public static Song getCurrentSongFromPrefs(Context context) {
		ArrayList<Song> songs = (ArrayList<Song>) Utils.getObject(context,
				Utils.PREFS_SONG_LIST, new TypeToken<ArrayList<Song>>() {
				}.getType(), null);
		int songPosition = Utils.getInt(context, Utils.PREFS_SONG_INDEX, 0);
		if (songs == null) {
			return null;
		}
		return songs.get(songPosition);
	}

	public static Song getCurrentSongFromStaticField() {
		if (mSongsBackup == null) {
			return null;
		}
		return mSongsBackup.get(mSongPositionBackup);
	}

	public static String getCoverArtPath(Context context, long androidAlbumId) {
		String path = null;
		Cursor c = context.getContentResolver().query(
				MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Albums.ALBUM_ART },
				MediaStore.Audio.Albums._ID + "=?",
				new String[] { Long.toString(androidAlbumId) }, null);
		if (c != null) {
			if (c.moveToFirst()) {
				path = c.getString(0);
			}
			c.close();
		}
		return path;
	}

	public static Bitmap getCoverArtBitmap(Context context, long androidAlbumId) {
		String albumArtwordPath = getCoverArtPath(context, androidAlbumId);
		Bitmap bitmap = null;
		if (albumArtwordPath == null) {
			bitmap = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.miniplayer_default_album_art);
		} else {
			bitmap = BitmapFactory.decodeFile(albumArtwordPath);
		}
		return bitmap;
	}

	@SuppressLint("NewApi")
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			Log.d(TAG, "onTaskRemoved");
			/*
			 * Prevent stop service when swipe application from recent list
			 */
			Intent intent = new Intent(this, PreventStopServiceActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}

	}

}
