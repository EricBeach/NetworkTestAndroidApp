package org.ericbeach.networktest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.ericbeach.networktest.model.TestResultManager;
import org.ericbeach.networktest.view.MainActivity;

/**
 * Thread that runs in the background to execute connectivity tests.
 * @author Eric Beach (ebeach@google.com)
 */
public class BackgroundTestRunnerThread extends Thread {
  public static final String TAG = BackgroundTestRunnerThread.class.getName();
  
  private volatile int startDelayMilliseconds;
  
  @Override
  public void run() {
    try {
      // Since the test results will be loaded into the UI from different threads
      // asynchronously, we do not want to render the UI with a stale
      // network problem error, so before we run the suite of tests we need to clear out
      // the currently running tests.
      TestResultManager.getInstance().resetTestResults();
      
      if (startDelayMilliseconds > 0) {
        Log.v(TAG, "Sleeping thread for " + startDelayMilliseconds +
            " milliseconds before beginning work (i.e., delayed launch)");
        BackgroundTestRunnerThread.sleep(startDelayMilliseconds);
      }
      
      while (!BackgroundTestRunnerThread.currentThread().isInterrupted()) {
        Intent intent = new Intent(MainActivity.getAppContext(), TestsRunnerService.class);
        Log.v(TAG, "About to start TestsRunnerService ");
        MainActivity.getAppContext().startService(intent);
          
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
            MainActivity.getAppContext());
        int sleepTime;
        // Since Android saves preferences as strings, even with 'android:numeric="integer"',
        // we need to convert to an integer.
        sleepTime = Integer.parseInt(settings.getString("pref_key_frequency", "60"));
        Log.v(TAG, "Sleep thread for " + sleepTime + " seconds");
        BackgroundTestRunnerThread.sleep(sleepTime * 1000);
      }
    } catch (InterruptedException e) {
      Log.v(TAG, "Thread interrupted ");
    } catch (Exception e) {
      Log.v(TAG, "Thread exception: " + e.getMessage());
    }
  }
    
  /**
   * @see http://stackoverflow.com/a/11387729/1783829
   */
  public void cancel() {
    Log.v(TAG, "About to cancel thread");
    this.interrupt();
  }
    
  @Override
  public void start() {
    startDelayMilliseconds = 0;
    Log.v(TAG, "About to start new thread with delay of 0");
    super.start();
  }
    
  public void startDelayed(int secondsToDelay) {
    startDelayMilliseconds = secondsToDelay * 1000;
    Log.v(TAG, "About to start new thread with delay of " +
      secondsToDelay + " seconds delay");
    super.start();
  }
}
