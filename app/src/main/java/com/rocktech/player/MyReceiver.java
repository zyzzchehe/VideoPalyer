package com.rocktech.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;


public class MyReceiver extends BroadcastReceiver {
	private static final String TAG = "YaZhou-Receiver";
	
	@Override
    public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.e(TAG, "onReceive----" + action);
		if(action.equals("android.intent.action.BOOT_COMPLETED")){
			Log.e(TAG, "onReceive BOOT COMPLETED----");

			Intent i = new Intent(context,TestActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
    }
}
