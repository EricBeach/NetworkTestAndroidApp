package org.ericbeach.networktest.model;

import org.apache.http.HttpStatus;

/**
 * Network connectivity test to determine whether HTTP connections are being filtered.
 * @author Eric Beach (ebeach@google.com)
 */
public class HttpFilteringTest extends HttpTest {  
  public HttpFilteringTest() {
    super(TestId.HTTP_FILTERING);
    urlsToTest.put("http://www.facebook.com", HttpStatus.SC_OK);
    urlsToTest.put("http://www.nytimes.com", HttpStatus.SC_OK);
    urlsToTest.put("http://www.stanford.edu", HttpStatus.SC_OK);
    runTest();
  }
}
