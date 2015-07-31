package us.cuongpnh.study.localbroadcastmanager.receiver;

import us.cuongpnh.study.localbroadcastmanager.service.MediaPlayerService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class MediaPlayerReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("MediaPlayerReceiver", "MediaPlayerReceiver");
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return;
		}
		/*
		 * Checking special case: start media player from widget after stopping
		 * it in notification
		 */
		if (isStartNewPlayerFromWidget(context, intent)) {
			startMediaPlayerFromReceiver(context);
			return;
		}
		if (MediaPlayerService.getInstance() == null) {
			return;
		}

		if (intent.getAction().equals(
				android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			Toast.makeText(context, "Headphones disconnected.",
					Toast.LENGTH_SHORT).show();

			// send an intent to our MusicService to telling it to pause the
			// audio
			MediaPlayerService.getInstance().pause();

		} else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {

			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(
					Intent.EXTRA_KEY_EVENT);
			if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
				return;

			switch (keyEvent.getKeyCode()) {
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if (MediaPlayerService.getInstance().isPlaying()) {
					Log.d(MediaPlayerService.TAG, "PAUSE_SONG");
					MediaPlayerService.getInstance().pause();
				} else {
					if (MediaPlayerService.getInstance().isPause()) {
						Log.d(MediaPlayerService.TAG, "RESUME_SONG");
						MediaPlayerService.getInstance().resume();
					} else {
						/*
						 * Media player isn't play yet
						 */
						Log.d(MediaPlayerService.TAG, "PLAY_SONG (first time)");
						MediaPlayerService.getInstance().play();
					}
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				MediaPlayerService.getInstance().play();
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				MediaPlayerService.getInstance().pause();
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				MediaPlayerService.getInstance().stop();
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				MediaPlayerService.getInstance().next();
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				MediaPlayerService.getInstance().previous();
				break;
			}
		} else if (intent.getAction()
				.equals(MediaPlayerService.NAVIGATION_SONG)) {

			int navigationSong = bundle
					.getInt(MediaPlayerService.NAVIGATION_SONG_TYPE);
			switch (navigationSong) {
			case MediaPlayerService.NEXT_SONG:
				Log.d(MediaPlayerService.TAG, "NEXT_SONG");
				MediaPlayerService.getInstance().next();
				break;
			case MediaPlayerService.PREVIOUS_SONG:
				Log.d(MediaPlayerService.TAG, "PREVIOUS_SONG");
				MediaPlayerService.getInstance().previous();
				break;
			case MediaPlayerService.PLAY_PAUSE_SONG:
				if (MediaPlayerService.getInstance().isPlaying()) {
					Log.d(MediaPlayerService.TAG, "PAUSE_SONG");
					MediaPlayerService.getInstance().pause();
				} else {
					if (MediaPlayerService.getInstance().isPause()) {
						Log.d(MediaPlayerService.TAG, "RESUME_SONG");
						MediaPlayerService.getInstance().resume();
					} else {
						/*
						 * Media player isn't play yet
						 */
						Log.d(MediaPlayerService.TAG, "PLAY_SONG (first time)");
						MediaPlayerService.getInstance().play();
					}
				}
				break;
			case MediaPlayerService.STOP_SONG:
				Log.d(MediaPlayerService.TAG, "STOP_SONG");
				MediaPlayerService.getInstance().stop();
				break;
			case MediaPlayerService.SHUFFLE_SONG:
				Log.d(MediaPlayerService.TAG, "SHUFFLE_SONG");
				MediaPlayerService.getInstance().toggleShuffle();
				break;
			case MediaPlayerService.REPEAT_SONG:
				Log.d(MediaPlayerService.TAG, "REPEAT_SONG");
				MediaPlayerService.getInstance().toggleRepeat();
				break;
			default:
				break;
			}
		}

	}

	private boolean isStartNewPlayerFromWidget(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (MediaPlayerService.getInstance() != null) {
			return false;
		}
		boolean action = intent.getAction().equals(
				MediaPlayerService.NAVIGATION_SONG);
		if (!action) {
			return false;
		}

		
		int navigationSong = bundle
				.getInt(MediaPlayerService.NAVIGATION_SONG_TYPE);
		if (navigationSong != MediaPlayerService.PLAY_PAUSE_SONG) {
			return false;
		}
		String sender = bundle.getString(MediaPlayerService.SENDER_TYPE);		
		if (sender == null) {
			return false;
		}

		if (!sender.equalsIgnoreCase(MediaPlayerService.WIDGET)) {
			return false;
		}
		return true;
	}
	
	private void startMediaPlayerFromReceiver(Context context)
	{
		Intent intent = new Intent(context, MediaPlayerService.class);
		intent.putExtra(MediaPlayerService.IS_START_FROM_WIDGET, true);
		context.startService(intent);
	}
}
