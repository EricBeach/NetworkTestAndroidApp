package org.ericbeach.networktest.model;

import java.util.Date;
import java.util.HashMap;

import android.util.Log;

/**
 * Class to store, retrieve, and manage the results (i.e., verdicts) of tests
 * (i.e., network tests).
 * @author Eric Beach (ebeach@google.com)
 */
public class TestResultManager {
  public static final String TAG = TestResultManager.class.getName();
  
  private static volatile TestResultManager instance = null;
  private volatile HashMap<TestId, TestResult> latestTestResult;
  private Date lastTestCompleted;
  
  // Protected constructor to prevent instantiation by outsiders.
  protected TestResultManager() {
    Log.v(TAG, "Creating new instance");
    latestTestResult = new HashMap<TestId, TestResult>();
  }
  
  public static TestResultManager getInstance() {
    if (instance == null) {
      synchronized (TestResultManager.class) {
        if (instance == null) {
          instance = new TestResultManager();
        }
      }
    }
    return instance;
  }
  
  public synchronized void setTestResult(TestResult result) {
    Log.v(TAG, "Setting test verdict " + result.getTestVerdict() + " for Test ID " +
        result.getTestId());
    latestTestResult.put(result.getTestId(), result);
    lastTestCompleted = new Date();
  }
    
  public synchronized TestVerdict getTestVerdict(TestId testId) {
    TestResult latestResult = latestTestResult.get(testId);
    if (latestResult == null) {
        return TestVerdict.TEST_INCOMPLETE;
    } else {
      return latestResult.getTestVerdict();  
    }
  }
  
  public synchronized HashMap<TestId, TestResult> getTestResults() {
    return latestTestResult;
  }
  
  public synchronized String getTimeLastTestCompleted() {
    if (lastTestCompleted == null) {
      return "Test not yet finished running";
    } else {
      return "Last Run: " + lastTestCompleted.toString();  
    }
  }
  
  public synchronized void resetTestResults() {
    latestTestResult.clear(); 
  }
}
