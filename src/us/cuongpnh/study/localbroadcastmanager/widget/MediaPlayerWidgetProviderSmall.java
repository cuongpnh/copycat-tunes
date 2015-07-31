package us.cuongpnh.study.localbroadcastmanager.widget;

import us.cuongpnh.study.localbroadcastmanager.MediaPlayerActivity;
import us.cuongpnh.study.localbroadcastmanager.R;
import us.cuongpnh.study.localbroadcastmanager.model.Song;
import us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class MediaPlayerWidgetProviderSmall extends AppWidgetProvider {
	public static final String TAG = MediaPlayerWidgetProviderSmall.class
			.getName();

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		Log.d(TAG, "onUpdate");
		// Get all ids
		ComponentName thisWidget = new ComponentName(context,
				MediaPlayerWidgetProviderSmall.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		// Build the intent to call the service
		Intent intent = new Intent(context.getApplicationContext(),
				UpdateMediaPlayerWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

		// Update the widgets via the service
		context.startService(intent);
	}

	public static class UpdateMediaPlayerWidgetService extends Service {

		public static final String TAG = UpdateMediaPlayerWidgetService.class
				.getName();
		private AppWidgetManager mAppWidgetManager = null;
		private RemoteViews mRemoteViews = null;
		private Bitmap mCoverArt = null;
		private Song mSong = null;
		private int[] mAllWidgetIds = null;

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			// if (intent == null) {
			// /*
			// * Using when user force stop media player by removing app from
			// * recent task. Change return from START_NOT_STICKY to
			// START_REDELIVER_INTENT, then you can delete this code block
			// */
			// stopSelf();
			// return START_NOT_STICKY;
			// }
			initData(intent);
			if (mSong == null) {
				/*
				 * No song found, so we will change layout to
				 * widget_mediaplayer_no_song here!
				 */
				setLayoutToNoSongSelected();
				stopSelf();
				return START_NOT_STICKY;
			}
			initContentWidget();

			// else {
			// initContentWidget();
			// }
			// ComponentName thisWidget = new ComponentName(
			// getApplicationContext(),
			// MediaPlayerWidgetProviderSmall.class);
			//
			// int[] allWidgetIds2 =
			// appWidgetManager.getAppWidgetIds(thisWidget);

			// for (int widgetId : mAllWidgetIds) {
			//
			// // Intent clickSongDisplayIntent = new
			// // Intent(this.getApplicationContext(),
			// // MediaPlayerWidgetProviderSmall.class);
			// //
			// clickSongDisplayIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			// //
			// clickSongDisplayIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
			// // allWidgetIds);
			// //
			// // PendingIntent pendingIntent = PendingIntent.getBroadcast(
			// // getApplicationContext(), 0, clickSongDisplayIntent,
			// // PendingIntent.FLAG_UPDATE_CURRENT);
			//
			// updateUI();
			// updateUIEvent();
			// mAppWidgetManager.updateAppWidget(widgetId, mRemoteViews);
			// }
			stopSelf();
			return START_REDELIVER_INTENT;
			// return START_NOT_STICKY;
		}

		private void setButtonPlayPauseToPlayState() {
			mRemoteViews.setImageViewResource(R.id.ivWidgetSongPlayPauseSmall,
					R.drawable.ic_widget_play);
			// for (int widgetId : mAllWidgetIds) {
			// mAppWidgetManager.updateAppWidget(widgetId, mRemoteViews);
			// }
		}

		private void setLayoutToNoSongSelected() {
			Intent openActivityIntent = new Intent(
					this.getApplicationContext(), MediaPlayerActivity.class);
			PendingIntent piOpenActivity = PendingIntent.getActivity(
					getApplicationContext(), 0, openActivityIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			mRemoteViews = new RemoteViews(this.getApplicationContext()
					.getPackageName(), R.layout.widget_mediaplayer_no_song);

			mRemoteViews.setOnClickPendingIntent(R.id.tvWidgetNoSongSelected,
					piOpenActivity);

			for (int widgetId : mAllWidgetIds) {
				mAppWidgetManager.updateAppWidget(widgetId, mRemoteViews);
			}

		}

		private void initContentWidget() {
			for (int widgetId : mAllWidgetIds) {
				updateUI();
				updateUIEvent();
				mAppWidgetManager.updateAppWidget(widgetId, mRemoteViews);
			}
		}

		private void initData(Intent intent) {
			/*
			 * Get current song from playing media player
			 */
			if (MediaPlayerService.getInstance() != null) {
				mSong = MediaPlayerService.getInstance().getCurrentSong();
			}
			/*
			 * If there is no song from media player then we will get song from
			 * backup (static field and shared preference)
			 */
			if (mSong == null) {
				mSong = MediaPlayerService
						.getCurrentSongFromBackup(getApplicationContext());
			}

			if (mSong != null) {
				mCoverArt = MediaPlayerService.getCoverArtBitmap(this,
						mSong.getAlbumId());
			}
			mAppWidgetManager = AppWidgetManager.getInstance(this
					.getApplicationContext());
			mAllWidgetIds = intent
					.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			mRemoteViews = new RemoteViews(this.getApplicationContext()
					.getPackageName(), R.layout.widget_mediaplayer_small);

		}

		private void updateUI() {

			/*
			 * Update widget's UI
			 */
			mRemoteViews.setTextViewText(R.id.tvWidgeSongTitleSmall,
					mSong.getTitle());
			mRemoteViews.setTextViewText(R.id.tvWidgetSongArtistSmall,
					mSong.getArtist());
			mRemoteViews.setImageViewBitmap(R.id.ivWidgetSongDisplaySmall,
					mCoverArt);
			if (MediaPlayerService.getInstance() == null) {
				Log.d(TAG, "setButtonPlayPauseToPlayState");
				setButtonPlayPauseToPlayState();
			} else {

				/*
				 * Update button play/pause
				 */
				if (MediaPlayerService.getInstance().isPlaying()) {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongPlayPauseSmall,
							R.drawable.ic_widget_pause);
				} else {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongPlayPauseSmall,
							R.drawable.ic_widget_play);
				}
				/*
				 * Update button shuffle
				 */
				if (MediaPlayerService.getInstance().isShuffle()) {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongShuffleSmall,
							R.drawable.ic_widget_shuffle_on);
				} else {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongShuffleSmall,
							R.drawable.ic_widget_shuffle_off);
				}

				/*
				 * Update button repeat
				 */
				if (MediaPlayerService.getInstance().isRepeat()) {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongRepeatSmall,
							R.drawable.ic_widget_repeat_all);
				} else {
					mRemoteViews.setImageViewResource(
							R.id.ivWidgetSongRepeatSmall,
							R.drawable.ic_widget_repeat_off);
				}
			}
		}

		@SuppressWarnings("unused")
		private void updateUIEvent() {
			/*
			 * Using FLAG_ONE_SHOT is extremely lag. We should have static field
			 * for request code in PendingIntent.get*(Activity, Service,
			 * Broadcast).
			 */
			Intent openActivityIntent = new Intent(
					this.getApplicationContext(), MediaPlayerActivity.class);
			PendingIntent piOpenActivity = PendingIntent.getActivity(
					getApplicationContext(), 0, openActivityIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			Intent previousSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			previousSongIntent.putExtra(
					MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.PREVIOUS_SONG);
			PendingIntent piPreviousSong = PendingIntent.getBroadcast(this, 1,
					previousSongIntent, 0);

			Intent nextSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			nextSongIntent.putExtra(MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.NEXT_SONG);
			PendingIntent piNextSong = PendingIntent.getBroadcast(this, 2,
					nextSongIntent, 0);

			Intent stopSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			stopSongIntent.putExtra(MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.STOP_SONG);
			PendingIntent piStopSong = PendingIntent.getBroadcast(this, 3,
					stopSongIntent, 0);

			Intent playPauseSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			playPauseSongIntent.putExtra(
					MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.PLAY_PAUSE_SONG);
			/*
			 * Important! For playing song from widget after stopping media
			 * player in notification
			 */
			playPauseSongIntent.putExtra(MediaPlayerService.SENDER_TYPE,
					MediaPlayerService.WIDGET);
			PendingIntent piPlayPauseSong = PendingIntent.getBroadcast(this, 7,
					playPauseSongIntent, 0);

			Intent shuffleSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			shuffleSongIntent.putExtra(MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.SHUFFLE_SONG);
			PendingIntent piShuffleSong = PendingIntent.getBroadcast(this, 5,
					shuffleSongIntent, 0);

			Intent repeatSongIntent = new Intent(
					MediaPlayerService.NAVIGATION_SONG);
			repeatSongIntent.putExtra(MediaPlayerService.NAVIGATION_SONG_TYPE,
					MediaPlayerService.REPEAT_SONG);
			PendingIntent piRepeatSong = PendingIntent.getBroadcast(this, 6,
					repeatSongIntent, 0);

			mRemoteViews.setOnClickPendingIntent(R.id.ivWidgetSongDisplaySmall,
					piOpenActivity);
			mRemoteViews.setOnClickPendingIntent(
					R.id.ivWidgetSongPlayPauseSmall, piPlayPauseSong);
			mRemoteViews.setOnClickPendingIntent(R.id.ivWidgetSongNextSmall,
					piNextSong);
			mRemoteViews.setOnClickPendingIntent(R.id.ivWidgetSongPrevSmall,
					piPreviousSong);
			mRemoteViews.setOnClickPendingIntent(R.id.ivWidgetSongShuffleSmall,
					piShuffleSong);
			mRemoteViews.setOnClickPendingIntent(R.id.ivWidgetSongRepeatSmall,
					piRepeatSong);
		}

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}

	}
}
