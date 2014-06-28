package org.ericbeach.networktest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import org.ericbeach.networktest.view.MainActivity;

/**
 * Utility class to provide network information.
 * @author Eric Beach (ebeach@google.com)
 */
public class NetworkUtil {
  public static final String TAG = NetworkUtil.class.getName();
  
  public static synchronized boolean isWiFiOrCelluarNetworkOn() {
    final ConnectivityManager connMgr = (ConnectivityManager) MainActivity.getAppContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE);

    final android.net.NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    
    // "A more concise way of checking whether a network interface is available is as follows.
    // The method getActiveNetworkInfo() returns a NetworkInfo instance representing the first
    // connected network interface it can find, or null if none of the interfaces is connected
    // (meaning that an internet connection is not available):
    // http://developer.android.com/training/basics/network-ops/managing.html
    // On Nexus Tablet, getNetworkInfo() returns null if not connected whereas on
    // Nexus phone, it is not null.
    if (networkInfo != null && networkInfo.isConnected()) {
      Log.v(TAG, "Active network detected");
      return true;
    } else {
      Log.v(TAG, "NULL network manager returned or offline");
      return false;
    }
  }
}
