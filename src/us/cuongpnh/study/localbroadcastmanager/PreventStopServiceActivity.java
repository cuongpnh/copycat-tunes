package us.cuongpnh.study.localbroadcastmanager;

import android.app.Activity;
import android.os.Bundle;

public class PreventStopServiceActivity extends Activity{
	@Override
	public void onCreate(Bundle icicle ) {
		super.onCreate( icicle );
		finish();
	}
}
