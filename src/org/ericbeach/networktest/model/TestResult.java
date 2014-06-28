package org.ericbeach.networktest.model;


/**
 * Class to encapsulate the verdict of a network connectivity test.
 * @author Eric Beach (ebeach@google.com)
 */
public class TestResult {
  private TestId testId;
  private TestVerdict testVerdict;
  
  public TestResult(TestId testId) {
    this.testId = testId;
    this.testVerdict = TestVerdict.TEST_INCOMPLETE;
  }
  
  public void setTestVerdict(TestVerdict verdict) {
    testVerdict = verdict;
  }
  
  public TestVerdict getTestVerdict() {
    return testVerdict;
  }
  
  public TestId getTestId() {
    return testId;
  }
}
