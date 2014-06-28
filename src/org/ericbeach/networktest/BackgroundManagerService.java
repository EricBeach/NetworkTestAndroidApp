package org.ericbeach.networktest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.ericbeach.networktest.model.TestId;
import org.ericbeach.networktest.model.TestResult;
import org.ericbeach.networktest.model.TestResultManager;
import org.ericbeach.networktest.model.TestVerdict;
import org.ericbeach.networktest.view.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Android service to run in the background and execute tests regularly.
 * @author Eric Beach (ebeach@google.com)
 */
public class BackgroundManagerService extends Service {
  public static final String TAG = BackgroundManagerService.class.getName();

  // TODO(ebeach): Look into ensuring this service is started 30 seconds after booting the app:
  // http://www.vogella.com/tutorials/AndroidServices/article.html
  private static final int NETWORK_PROBLEM_NOTIFICATION_ID = 100;
  private static final int BACKGROUND_MONITORING_SERVICE_STATUS_NOTIFICATION_ID = 102;
  
  private static volatile boolean isBackgroundManagerRunning = false; 
  private volatile BackgroundTestRunnerThread testsRunnerThread;
  
  private BroadcastReceiver testsCompletedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == TestsRunnerService.INTENT_BROADCAST_TEST_COMPLETED) {
        Log.v(this.getClass().getName(),
            "Received broadcast intent with test completed inside background service");
        // Check for the situation where we found a network problem but the network is disconnected
        // such as the following scenario: (1) user begins tests (2) user disconnects from Internet
        // (3) networkChangeReceiver detects network change [2] and shuts down (4) tests fail
        // and we set in motion showing a Network Problem error.
        // In other words, we don't want to display Network Problem when the user is totally
        // disconnected from the network.
        if (NetworkUtil.isWiFiOrCelluarNetworkOn()) {
          BackgroundManagerService.this.receivedNetworkTestCompletedBroadcast(intent);
        }
      }
    }
  };
  
  private BroadcastReceiver testFailureDetectedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == TestsRunnerService.INTENT_BROADCAST_TEST_PROBLEM_DETECTED) {
        Log.v(this.getClass().getName(),
            "Received broadcast intent that test found network problem");
        
        // Check for the situation where we found a network problem but the network is disconnected
        // such as the following scenario: (1) user begins tests (2) user disconnects from Internet
        // (3) networkChangeReceiver detects network change [2] and shuts down (4) tests fail
        // and we set in motion showing a Network Problem error.
        // In other words, we don't want to display Network Problem when the user is totally
        // disconnected from the network.
        if (NetworkUtil.isWiFiOrCelluarNetworkOn()) {
          BackgroundManagerService.this.receivedNetworkTestFailedBroadcast(intent); 
        }
      }
    }
  };
  
  private BroadcastReceiver networkStatusChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_CHANGED) {
        Log.v(this.getClass().getName(),
            "Received broadcast intent that network status changed");
        BackgroundManagerService.this.updateTestsRunnerThreadBasedUponNetworkStatus(); 
      }
    }
  };
  
  // Since onStartCommand() can be executed multiple times if the service is attempted to be
  // started multiple times. Therefore, we must have a mechanism to check and ensure we do not
  // spawn too many threads in this service / perform tasks that should only be done once.
  // @see "Service Lifecycles" http://developer.android.com/reference/android/app/Service.html
  // @see "2.3 Service start.." http://www.vogella.com/tutorials/AndroidServices/article.html
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand");

    // Check whether service is running in foreground.
    // If its not, register receivers and start in foreground.
    if (!isBackgroundManagerRunning) {
      Log.v(TAG,
          "Background service never started, so launch as foreground service. Register listeners.");
      
      // In order to (a) avoid Android killing the service (b) continually notify the user
      // when the Network Test application is being run, start the BackgroundManagerService
      // as a Foreground service, meaning it will show in the notification bar.
      Notification networkTestIsRunningNotification = getNetworkTestIsRunningNotification();
      startForeground(BACKGROUND_MONITORING_SERVICE_STATUS_NOTIFICATION_ID,
          networkTestIsRunningNotification);
    
      // Register the broadcast receiver to listen for test completed broadcasts.
      registerReceiver(testsCompletedReceiver,
          new IntentFilter(TestsRunnerService.INTENT_BROADCAST_TEST_COMPLETED));

      // Register the broadcast receiver to listen for tests failure broadcasts.
      registerReceiver(testFailureDetectedReceiver,
          new IntentFilter(TestsRunnerService.INTENT_BROADCAST_TEST_PROBLEM_DETECTED));
    
      // Register the broadcast receiver to listen for network changed receivers.
      registerReceiver(networkStatusChangedReceiver,
          new IntentFilter(NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_CHANGED));
      Log.v(TAG, "Registered multiple BroadcastReceiver");
    } else {
      Log.v(TAG, "Background service already started, so skip registering.");
    }
    isBackgroundManagerRunning = true;
    updateTestsRunnerThreadBasedUponNetworkStatus();
    
    return Service.START_REDELIVER_INTENT;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  @Override
  public void onDestroy() {
    Log.v(TAG, "Service being destroyed. Application no longer active.");
    
    // Ensure that the background process thread that continually executes tests
    // no longer runs. Degrade it gracefully.
    // @see http://stackoverflow.com/a/11387729/1783829
    stopTestsRunnerThread();
    displayNetworkTestsStoppedNotification();
  }
  
  @Override
  public void onLowMemory() {
    Log.v(TAG, "Service memory is low.");
  }
  
  private void updateTestsRunnerThreadBasedUponNetworkStatus() {
    /**
    A services runs in the same process as the application in which it is declared and in
    the main thread of that application, by default. So, if your service performs
    intensive or blocking operations while the user interacts with an activity from
    the same application, the service will slow down activity performance. To avoid
    impacting application performance, you should start a new thread inside the service.
    @see "Cauton" http://developer.android.com/guide/components/services.html
    */
   // Only start the network tests if we are on a network. If the phone is in full airplane
   // mode with no WiFi and no celluar network, it makes no sense to do network tests.
   // Just register the listeners (below) for when the network state changes.
   if (NetworkUtil.isWiFiOrCelluarNetworkOn()) {
     Log.v(TAG, "Internet is on...Starting network test runner thread");
     startTestsRunnerThread(60);
     displayNetworkTestsRunningNotification();
   } else {
     Log.v(TAG, "Internet is off...not starting network test runner thread");
     stopTestsRunnerThread();
     displayNetworkTestsStoppedNotification();
   }
  }
  
  private void displayNetworkTestsRunningNotification() {
    // Display notification that NetworkTest is running.
    Notification notification = getNetworkTestIsRunningNotification();
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(BACKGROUND_MONITORING_SERVICE_STATUS_NOTIFICATION_ID,
        notification);
  }
  
  private void displayNetworkTestsStoppedNotification() {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    mBuilder.setSmallIcon(R.drawable.notification_x);
    mBuilder.setContentTitle("Network Test Paused");
    mBuilder.setContentText("Applicatoin not running tests.");

    // Creates an explicit intent for an Activity in your app.
    Intent resultIntent = new Intent(this, MainActivity.class);

    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of
    // the Network Test application to the Home screen.
    // http://developer.android.com/guide/topics/ui/notifiers/notifications.html#SimpleNotification
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(MainActivity.class);
    // Adds the Intent that starts the Activity to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    // BACKGROUND_MONITORING_SERVICE_STATUS_NOTIFICATION_ID allows you to update the
    // notification later on.
    mNotificationManager.notify(BACKGROUND_MONITORING_SERVICE_STATUS_NOTIFICATION_ID,
        mBuilder.build());
    Log.v(TAG, "Launched network tests stopped notification");
  }
  
  private Notification getNetworkTestIsRunningNotification() {    
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    mBuilder.setSmallIcon(R.drawable.notification_refreshing);
    mBuilder.setContentTitle("Network Test Active");
    mBuilder.setContentText("Currently monitoring network connection.");

    // Creates an explicit intent for an Activity in your app.
    Intent resultIntent = new Intent(this, MainActivity.class);

    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of
    // the Network Test application to the Home screen.
    // http://developer.android.com/guide/topics/ui/notifiers/notifications.html#SimpleNotification
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(MainActivity.class);
    // Adds the Intent that starts the Activity to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    return mBuilder.build();
  }
  
  private void startTestsRunnerThread(int secondsDelay) {
    if (testsRunnerThread == null || !testsRunnerThread.isAlive()) {
      testsRunnerThread = new BackgroundTestRunnerThread();
      Log.v(TAG, "Sending start signal to BackgroundTestRunnerThread");
      testsRunnerThread.startDelayed(secondsDelay);
      Log.v(TAG, "Executed past start command");
    }
  }
  
  private void stopTestsRunnerThread() {
    if (testsRunnerThread != null) {
      Log.v(TAG, "Sending cancel signal to the thread runner thread");
      testsRunnerThread.cancel();
    }
  }
  
  private void receivedNetworkTestCompletedBroadcast(Intent intent) {
    Log.v(TAG, "Received broadcast that test completed");
    processNetworkTestsResults();
  }

  private void receivedNetworkTestFailedBroadcast(Intent intent) {
    Log.v(TAG, "Received broadcast that a network test failed");
    // Since we already know there is a network test failure, we can take actions on the system.
    takeSystemActionUponNetworkProblemDetected();
  }
  
  private void processNetworkTestsResults() {
    TestResultManager testResultManager = TestResultManager.getInstance();
    HashMap<TestId, TestResult> testResults = testResultManager.getTestResults();
    boolean isNetworkProblemFound = false;
    for (HashMap.Entry<TestId, TestResult> entry : testResults.entrySet()) {
        if (entry.getValue().getTestVerdict() != TestVerdict.NO_PROBLEM) {
          isNetworkProblemFound = true;
          break;
        }
    }
    
    if (!isNetworkProblemFound) {
      // In case a previous notification of a network problem existed, dismiss it now
      // that there were no network problems found.
      dismissNetworkProblemNotification();
      return;
    }
    
    takeSystemActionUponNetworkProblemDetected();
  }
  
  private void takeSystemActionUponNetworkProblemDetected() {
    // Check whether to disable Wi-Fi.
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        super.getApplicationContext());
    final boolean isProblemDisableWiFiEnabled =
        settings.getBoolean("pref_key_action_disconnect_wifi", false);
    if (isProblemDisableWiFiEnabled) {
      disableDeviceWiFi();
    }
    
    // Check whether to show a notification.
    final boolean isProblemNotificationEnabled =
        settings.getBoolean("pref_key_action_notify_me", false);
    if (isProblemNotificationEnabled) {
      displayNetworkProblemNotification(isProblemDisableWiFiEnabled);
    }
  }
  
  private void displayNetworkProblemNotification(final boolean isProblemDisableWiFiEnabled) {
    ArrayList<String> bigMessage = new ArrayList<String>();
    if (isProblemDisableWiFiEnabled) {
      bigMessage.add("Based upon your app settings, we");
      bigMessage.add("disabled your Wi-Fi connection");
      displayNetworkProblemNotification(bigMessage, true);  
    } else {
      displayNetworkProblemNotification(bigMessage, false);
    }
  }
  
  private void displayNetworkProblemNotification(final ArrayList<String> bigMessage,
                                                 final boolean showBigMessage) {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    mBuilder.setSmallIcon(R.drawable.notification_exclamation_mark);
    mBuilder.setContentTitle("Network Problem Detected");
    mBuilder.setContentText("Problem detected with your network connection.");
    // When the user clicks the notification, the notification will be dismissed/canceled.
    mBuilder.setAutoCancel(true);
    
    // Creates an explicit intent for an Activity in your app.
    Intent resultIntent = new Intent(this, MainActivity.class);

    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of
    // the Network Test application to the Home screen.
    // http://developer.android.com/guide/topics/ui/notifiers/notifications.html#SimpleNotification
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(MainActivity.class);
    // Adds the Intent that starts the Activity to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
 
    if (showBigMessage) {
      // Sets a title for the Inbox style big view
      NotificationCompat.InboxStyle inboxStyle =
          new NotificationCompat.InboxStyle();
      inboxStyle.setBigContentTitle("Network Problem Detected");
      for (int i = 0; i < bigMessage.size(); i++) {
        inboxStyle.addLine(bigMessage.get(i)); 
      }
      // Moves the big view style object into the notification object.
      mBuilder.setStyle(inboxStyle); 
    }
    
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    // NETWORK_PROBLEM_NOTIFICATION_ID allows you to update the notification later on.
    mNotificationManager.notify(NETWORK_PROBLEM_NOTIFICATION_ID, mBuilder.build());
    Log.v(TAG, "Launched network problem notification with ID " +  NETWORK_PROBLEM_NOTIFICATION_ID);
  }
  
  private void dismissNetworkProblemNotification() {
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.cancel(NETWORK_PROBLEM_NOTIFICATION_ID);
  }
  
  private void disableDeviceWiFi() {
    WifiManager wifiManager = (WifiManager) MainActivity.getAppContext().
        getSystemService(Context.WIFI_SERVICE);
    wifiManager.setWifiEnabled(false);
  }
}
