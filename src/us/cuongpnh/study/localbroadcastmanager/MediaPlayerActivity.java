package us.cuongpnh.study.localbroadcastmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import us.cuongpnh.study.localbroadcastmanager.adapter.SongAdapter;
import us.cuongpnh.study.localbroadcastmanager.model.Song;
import us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService;
import us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService.MediaPlayerBinder;
import us.cuongpnh.study.localbroadcastmanager.util.RemoteControlClientCompat;
import us.cuongpnh.study.localbroadcastmanager.util.Utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MediaPlayerActivity extends Activity {

	// song list variables
	private ArrayList<Song> mSongList;
	private SongAdapter mSongAdapter = null;
	private int mSongPosition = 0;
	// service
	private MediaPlayerService mMediaPlayerService = null;

	private static String TAG = MediaPlayerActivity.class.toString();
	private boolean mIsServiceBound = false;
	private boolean mForceConnect = false;
	private Intent mMediaPlayerIntent = null;
	@SuppressWarnings("unused")
	private NotificationManager mNotificationManager = null;
	private static String IS_BOUND = "is_bound";

	private ListView mLvSong = null;
	private ImageButton mIbPrevious = null;
	private ImageButton mIbPlay = null;
	@SuppressWarnings("unused")
	private ImageButton mIbStop = null;
	private ImageButton mIbNext = null;
	private ImageButton mIbShuffle = null;
	private ImageButton mIbRepeat = null;
	private TextView mTvDurationStartTime = null;
	private TextView mTvDurationEndTime = null;
	private TextView mTvSongTitle = null;
	private SeekBar mSbDuration = null;	

	ComponentName mMediaButtonReceiverComponent;
	AudioManager mAudioManager;
	RemoteControlClientCompat mRemoteControlClientCompat = null;

	private OnSeekBarChangeListener mSbDurationListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				Log.d(TAG, "Progress: " + progress);
				if (!mIsServiceBound) {
					return;
				}
				if (mMediaPlayerService.isPlaying()) {
					mMediaPlayerService.seekTo(progress);
				} else {
					if (mMediaPlayerService.isPause()) {
						mMediaPlayerService.resume();
					} else {
						mMediaPlayerService.play();
						mMediaPlayerService.seekTo(progress);
					}
				}
			}

		}
	};

	private BroadcastReceiver mMediaPlayerUnbindReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			unbindService(mMediaPlayerConnection);
			stopService(mMediaPlayerIntent);
			mIsServiceBound = false;
			resetUI();
		}
	};

	private BroadcastReceiver mMediaPlayerLogReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String s = intent.getStringExtra(MediaPlayerService.LOG_MESSAGE);
			Toast.makeText(getContext(), s.toString(), Toast.LENGTH_LONG).show();
		}
	};

	private BroadcastReceiver mMediaPlayerPlayPauseUIReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mMediaPlayerService.isPlaying()) {
				mIbPlay.setImageResource(R.drawable.ic_player_pause);
			} else {
				mIbPlay.setImageResource(R.drawable.ic_player_play);
			}
		}
	};

	private BroadcastReceiver mMediaPlayerUIReceiver = new BroadcastReceiver() {

		@SuppressLint("NewApi")
		@Override
		public void onReceive(Context context, Intent intent) {
			if(mIsServiceBound == false) {
				return;
			}
			int finalTime = intent.getIntExtra(MediaPlayerService.SONG_DURATION, 0);
			//Log.d(TAG, "Final time: " + finalTime);
			int startTime = intent.getIntExtra(MediaPlayerService.SONG_CURRENT_POSTION, 0);
			//Log.d(TAG, "Start time: " + startTime);
			if (finalTime == MediaPlayerService.INIT_TIME && startTime == MediaPlayerService.INIT_TIME) {
				/*
				 * Media play isn't playing => reset UI
				 */
				resetUI();
				return;
			}
			String title = intent.getStringExtra(MediaPlayerService.SONG_TITLE);
			boolean isForceUpdate = intent.getBooleanExtra(MediaPlayerService.FORCE_UPDATE, false);
			if (mMediaPlayerService.isInitUI() == false || isForceUpdate) {
				mSbDuration.setMax((int) finalTime);
				mTvSongTitle.setText(title);
				mTvDurationEndTime.setText(String.format(
						"%02d:%02d",
						TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
						TimeUnit.MILLISECONDS.toSeconds((long) finalTime)
								- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime))));
				mMediaPlayerService.setInitUI(true);
				/*
				 * Checking if media is playing or not
				 */
				if (mMediaPlayerService.isPlaying()) {
					mIbPlay.setImageResource(R.drawable.ic_player_pause);
				}

			}

			mTvDurationStartTime.setText(String.format(
					"%02d:%02d",
					TimeUnit.MILLISECONDS.toMinutes((long) startTime),
					TimeUnit.MILLISECONDS.toSeconds((long) startTime)
							- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime))));
			mSbDuration.setProgress((int) startTime);
		}
	};

	private OnClickListener mIbPreviousListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mIsServiceBound) {
				mMediaPlayerService.previous();
			}
		}
	};

	private OnClickListener mIbNextListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mIsServiceBound) {
				mMediaPlayerService.next();
			}
		}
	};
	private OnClickListener mIbPlayListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			connectToService(false);
		}
	};

	private OnClickListener mIbShuffleListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mIsServiceBound) {
				boolean isShuffle = mMediaPlayerService.isShuffle();
				Toast.makeText(getContext(), "Shuffle mode: " + (!isShuffle ? "on" : "off"), Toast.LENGTH_LONG).show();
				if (isShuffle) {
					mIbShuffle.setImageResource(R.drawable.ic_player_shuffle_off);
				} else {
					mIbShuffle.setImageResource(R.drawable.ic_player_shuffle);
				}
				mIbRepeat.setImageResource(R.drawable.ic_player_repeat_off);
				mMediaPlayerService.setShuffle(!isShuffle);
				mMediaPlayerService.setRepeat(false);
			}
		}
	};

	private OnClickListener mIbRepeatListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mIsServiceBound) {
				boolean isRepeat = mMediaPlayerService.isRepeat();
				Toast.makeText(getContext(), "Repeat mode: " + (!isRepeat ? "on" : "off"), Toast.LENGTH_LONG).show();
				if (isRepeat) {
					mIbRepeat.setImageResource(R.drawable.ic_player_repeat_off);
				} else {
					mIbRepeat.setImageResource(R.drawable.ic_player_repeat_all);
				}
				mIbShuffle.setImageResource(R.drawable.ic_player_shuffle_off);
				mMediaPlayerService.setRepeat(!isRepeat);
				mMediaPlayerService.setShuffle(false);
			}
		}
	};

	private void connectToService(boolean forceConnect) {
		mForceConnect = forceConnect;
		if (MediaPlayerService.getInstance() == null) {
			mForceConnect = false;
			mIsServiceBound = false;
		}
		if (mIsServiceBound == false || forceConnect) {
			/*
			 * Not connect yet
			 */
			if (mMediaPlayerIntent == null) {
				mMediaPlayerIntent = new Intent(getContext(), MediaPlayerService.class);
			}
			bindService(mMediaPlayerIntent, mMediaPlayerConnection, Context.BIND_AUTO_CREATE);
			startService(mMediaPlayerIntent);
		} else {
			/*
			 * Connected
			 */
			if (mMediaPlayerService.isPlaying()) {
				/*
				 * Media is playing
				 */
				mMediaPlayerService.pause();
				// mIbPlay.setImageResource(R.drawable.ic_player_play);

			} else {
				mMediaPlayerService.resume();
				// mIbPlay.setImageResource(R.drawable.ic_player_pause);
			}
		}
	}

	@SuppressWarnings("unused")
	private OnClickListener mIbStopListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mMediaPlayerIntent != null && mIsServiceBound == true) {
				//mNotificationManager.cancel(MediaPlayerService.NOTIFY_ID);
				mMediaPlayerService.stop();
			}
		}
	};
	private ServiceConnection mMediaPlayerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected");
			MediaPlayerBinder binder = (MediaPlayerBinder) service;
			// Get Service
			mMediaPlayerService = binder.getService();
			/*
			 * Binding old data from service here
			 */
			if (mForceConnect) {
				/*
				 * Checking if playlist from service is empty or not
				 */
				ArrayList<Song> tmpSongList = mMediaPlayerService.getSongList();
				if (tmpSongList == null || tmpSongList.isEmpty()) {
					/*
					 * Empty case => set service's playlist = playlist from
					 * activity
					 */
					mMediaPlayerService.setSongList(mSongList);
				} else {
					/*
					 * Else, sync playlist from service
					 */
					mSongList = tmpSongList;
				}

				if (!mMediaPlayerService.isPlaying()) {
					/*
					 * Force update UI just in case media is not playing
					 */
					mMediaPlayerService.forceUpdateUI();

				}

				Log.d(TAG, "Media player is playing");
				boolean isShuffle = mMediaPlayerService.isShuffle();
				boolean isRepeat = mMediaPlayerService.isRepeat();

				if (isRepeat) {
					mIbRepeat.setImageResource(R.drawable.ic_player_repeat_all);
				}

				if (isShuffle) {
					mIbShuffle.setImageResource(R.drawable.ic_player_shuffle);
				}

			} else {
				mMediaPlayerService.setSongList(mSongList);
				mMediaPlayerService.setSong(mSongPosition);
				mMediaPlayerService.play();
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
		Utils.init(this);
		setContentView(R.layout.activity_mediaplayer);
		initView();
		initData();
		LocalBroadcastManager.getInstance(this).registerReceiver((mMediaPlayerUIReceiver),
				new IntentFilter(MediaPlayerService.PLAYER_UPDATE_UI));
		LocalBroadcastManager.getInstance(this).registerReceiver((mMediaPlayerLogReceiver),
				new IntentFilter(MediaPlayerService.PLAYER_LOG));
		LocalBroadcastManager.getInstance(this).registerReceiver((mMediaPlayerUnbindReceiver),
				new IntentFilter(MediaPlayerService.PLAYER_UNBIND));
		LocalBroadcastManager.getInstance(this).registerReceiver((mMediaPlayerPlayPauseUIReceiver),
				new IntentFilter(MediaPlayerService.PLAYER_UPDATE_PLAY_PAUSE_UI));
		
		

	}

	// @Override
	// protected void onResume() {
	// super.onResume();
	// if (mMediaPlayerIntent == null && mIsServiceBound == false) {
	//
	// /*
	// * Start service
	// */
	// mMediaPlayerIntent = new Intent(this, CountUpService.class);
	// bindService(mMediaPlayerIntent, mCountUpConnection,
	// Context.BIND_AUTO_CREATE);
	// startService(mMediaPlayerIntent);
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
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMediaPlayerUIReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMediaPlayerLogReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMediaPlayerUnbindReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMediaPlayerPlayPauseUIReceiver);
		if (isMediaPlayerRunning()) {
			mMediaPlayerService.setInitUI(false);
		}
		if (mIsServiceBound == true && isMediaPlayerRunning() && mMediaPlayerConnection != null) {
			unbindService(mMediaPlayerConnection);
		}
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
	}

	private void initData() {
		initSongList();
		/*
		 * Restore last state when user start application from icon
		 */
		if (isMediaPlayerRunning()) {
			Log.d(TAG, "MediaPlayer is running");
			connectToService(true);
		}

		/*
		 * Restore last state when user tap at notification
		 */
		// Bundle extras = getIntent().getExtras();
		// if (extras != null) {
		// connectToService(true);
		// }
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private void initSongList() {
		getSongList();
		Collections.sort(mSongList, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.getTitle().compareTo(b.getTitle());
			}
		});
		// create and set adapter
		mSongAdapter = new SongAdapter(this, mSongList);
		mLvSong.setAdapter(mSongAdapter);
		mLvSong.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mSongPosition = position;
				if (mIsServiceBound == false || MediaPlayerService.getInstance() == null) {
					connectToService(false);
				} else {
					mMediaPlayerService.setSong(mSongPosition);
					mMediaPlayerService.play();
				}
			}
		});
	}

	private void initView() {

		/*
		 * Get view
		 */
		mLvSong = (ListView) findViewById(R.id.lvSong);
		mIbPlay = (ImageButton) findViewById(R.id.ibPlay);
		// mIbStop = (ImageButton) findViewById(R.id.ibStop);
		mIbNext = (ImageButton) findViewById(R.id.ibNext);
		mIbShuffle = (ImageButton) findViewById(R.id.ibShuffle);
		mIbRepeat = (ImageButton) findViewById(R.id.ibRepeat);
		mIbPrevious = (ImageButton) findViewById(R.id.ibPrevious);
		mTvSongTitle = (TextView) findViewById(R.id.tvSongTitle);
		mTvDurationEndTime = (TextView) findViewById(R.id.tvDurationEndTime);
		mTvDurationStartTime = (TextView) findViewById(R.id.tvDurationStartTime);
		mSbDuration = (SeekBar) findViewById(R.id.sbDuration);

		/*
		 * Listener here!
		 */
		mIbPlay.setOnClickListener(mIbPlayListener);
		// mIbStop.setOnClickListener(mIbStopListener);
		mIbNext.setOnClickListener(mIbNextListener);
		mIbPrevious.setOnClickListener(mIbPreviousListener);
		mIbShuffle.setOnClickListener(mIbShuffleListener);
		mIbRepeat.setOnClickListener(mIbRepeatListener);
		mSbDuration.setOnSeekBarChangeListener(mSbDurationListener);
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

	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isMediaPlayerRunning() {
		if (isServiceRunning(MediaPlayerService.class) && MediaPlayerService.getInstance() != null) {
			return true;
		}
		return false;
	}

	public void getSongList() {
		mSongList = new ArrayList<Song>();
		// query external audio
		ContentResolver musicResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
		// iterate over results if valid
		if (musicCursor != null && musicCursor.moveToFirst()) {
			// get columns
			int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
			int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
			int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
			int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
			int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

			// add songs to list
			do {
				long thisId = musicCursor.getLong(idColumn);
				String thisTitle = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				String thisAlbum = musicCursor.getString(albumColumn);
				long thisDuration = musicCursor.getLong(durationColumn);
				long thisAlbumId = musicCursor.getLong(albumIdColumn);
				mSongList
						.add(new Song(thisId, thisArtist, thisTitle, thisAlbum, thisDuration, thisAlbumId));
			} while (musicCursor.moveToNext());
		}
		musicCursor.close();
	}

	private void resetUI() {
		mTvDurationEndTime.setText(getString(R.string.inital_Time));
		mTvDurationStartTime.setText(getString(R.string.inital_Time));
		mTvSongTitle.setText(getString(R.string.app_name));
		mSbDuration.setProgress(0);
		/*
		 * Set image button from pause => play
		 */
		mIbPlay.setImageResource(R.drawable.ic_player_play);
		mIbShuffle.setImageResource(R.drawable.ic_player_shuffle_off);
		mIbRepeat.setImageResource(R.drawable.ic_player_repeat_off);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}
}
