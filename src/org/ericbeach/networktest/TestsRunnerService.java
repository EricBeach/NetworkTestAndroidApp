package org.ericbeach.networktest;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.ericbeach.networktest.model.DnsPoisoningTest;
import org.ericbeach.networktest.model.GoogleSiteTest;
import org.ericbeach.networktest.model.HttpFilteringTest;

/**
 * Android service to make a single run through each of the tests and, when finished broadcast
 * the test results.
 * @author Eric Beach (ebeach@google.com)
 */
public class TestsRunnerService extends IntentService {
  public static final String TAG = TestsRunnerService.class.getName();
  
  public static final String INTENT_BROADCAST_TEST_COMPLETED = "Broadcast test completed";
  public static final String INTENT_BROADCAST_TEST_PROBLEM_DETECTED = "Broadcast network problem";

  // USED FOR extending IntentService; Must be implemented.
  public TestsRunnerService() {
    super("TestsRunnerService");
  }
  
  // USED FOR extending IntentService; Must be implemented. Runs when the service starts.  
  @Override
  protected void onHandleIntent(Intent intent) {
    Log.v(TAG, "About to begin running tests");
    runTests();
  }
  
  @Override
  public void onDestroy() {
    Log.v(TAG, "Service being destroyed");
  }
  
  private void runTests() {
    Log.v(TAG, "Running network test checks");
    
    HttpFilteringTest httpFilteringTest = new HttpFilteringTest();
    Thread httpFilteringTestThread = new Thread(httpFilteringTest);
    httpFilteringTestThread.run();
    Log.v(TAG, "Started HTTP filtering test thread");
    
    DnsPoisoningTest dnsPoisoningTest = new DnsPoisoningTest();
    Thread dnsPoisoningTestThread = new Thread(dnsPoisoningTest);
    dnsPoisoningTestThread.run();
    Log.v(TAG, "Started DNS poisoning test thread");

    GoogleSiteTest googleSiteTest = new GoogleSiteTest();
    Thread googleSiteTestThread = new Thread(googleSiteTest);
    googleSiteTestThread.run();
    Log.v(TAG, "Started Google site test thread");
  }
}
