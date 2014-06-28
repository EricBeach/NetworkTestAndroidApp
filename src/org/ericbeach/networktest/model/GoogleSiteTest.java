package org.ericbeach.networktest.model;

import org.apache.http.HttpStatus;

/**
 * Network connectivity test to check for whether Google sites are available.
 * @author Eric Beach (ebeach@google.com)
 */
public class GoogleSiteTest extends HttpTest {  
  public GoogleSiteTest() {
    super(TestId.GOOGLE_AVAILABILITY);
    urlsToTest.put("http://www.google.com/generate_204", HttpStatus.SC_NO_CONTENT);
    urlsToTest.put("http://plus.google.com", HttpStatus.SC_OK);
    urlsToTest.put("http://drive.google.com", HttpStatus.SC_OK);
    urlsToTest.put("http://mail.google.com", HttpStatus.SC_OK);
    runTest();
  }
}
