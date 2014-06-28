package org.ericbeach.networktest.model;

import android.content.Intent;
import android.util.Log;

import org.ericbeach.networktest.TestsRunnerService;
import org.ericbeach.networktest.view.MainActivity;

/**
 * Abstract class for all network tests. Any connectivity/network test must inherit from this.
 * @author Eric Beach (ebeach@google.com)
 */
public abstract class NetworkTest implements Runnable {
  public static final String TAG = NetworkTest.class.getName();
  
  protected TestResult testResult;
  public NetworkTest(TestId testId) {
    testResult = new TestResult(testId);
  }
  
  public TestResult getTestResult() {
    return testResult;
  }
  
  public void run() {
    Log.v(TAG, "Inside individual network test thread, about to begin " +
        "running network tests.");
    runTest();
  }
  
  protected void storeTestResult() {
    TestResultManager verdictManager = TestResultManager.getInstance();
    verdictManager.setTestResult(testResult);
    
    // We want to notify appropriate parties that a test has failed, so broadcast here.
    // Else, if a test didn't fail, we just want to broadcast that a new result has come in.
    if (testResult.getTestVerdict() == TestVerdict.PROBLEM) {
      Intent intent = new Intent(TestsRunnerService.INTENT_BROADCAST_TEST_PROBLEM_DETECTED);
      Log.v(TAG, "broadcasting tests results");
      MainActivity.getAppContext().sendBroadcast(intent);
    }
    broadcastTestCompleted();
  }
  
  protected abstract void runTest();
  protected abstract void analyzeResults();
  
  private void broadcastTestCompleted() {
    Intent intent = new Intent(TestsRunnerService.INTENT_BROADCAST_TEST_COMPLETED);
    Log.v(TAG, "broadcasting test result completed");
    MainActivity.getAppContext().sendBroadcast(intent);
  }
}
