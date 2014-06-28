package org.ericbeach.networktest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.ericbeach.networktest.view.MainActivity;

/**
 * Broadcast receiver to listen for when the status of the network (e.g., network connection
 * active) changes (e.g., WiFi is turned off).
 * @author Eric Beach (ebeach@google.com)
 */
public class NetworkChangeBroadcastReceiver extends BroadcastReceiver {
  public static final String TAG = NetworkChangeBroadcastReceiver.class.getName();

  public static final String INTENT_NETWORK_STATUS_CHANGED = "0";
  public static final String INTENT_NETWORK_STATUS_EXTRA_NAME = "network";
  
  @Override
  public void onReceive(Context context, Intent intent) {
    final boolean isNetworkOn = NetworkUtil.isWiFiOrCelluarNetworkOn();
    Intent i = new Intent(NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_CHANGED);
    i.putExtra(NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_EXTRA_NAME, isNetworkOn);
    Log.v(TAG, "broadcasting network status change");
    MainActivity.getAppContext().sendBroadcast(i);
  }
}
