package org.ericbeach.networktest.model;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract intermediary class to perform different forms of HTTP test.
 * @author Eric Beach (ebeach@google.com)
 */
public abstract class HttpTest extends NetworkTest {
  public static final String TAG = HttpTest.class.getName();
  
  private static final int HTTP_TIMEOUT_SECONDS = 6;
  
  protected HashMap<String, Integer> urlResultCodes = new HashMap<String, Integer>();
  protected HashMap<String, String> urlResultContents = new HashMap<String, String>();
  
  protected HashMap<String, Integer> urlsToTest = new HashMap<String, Integer>();
  
  public HttpTest(TestId testId) {
    super(testId);
  }
  
  @Override
  public void runTest() {
    Log.v(TAG, "Running network http test, querying " + urlsToTest.size() + " URLs");
    for (Map.Entry<String, Integer> entry : urlsToTest.entrySet()) {
      makeHttpRequest(entry.getKey());
    }
    analyzeResults();
  }
  
  @Override
  protected void analyzeResults() {
    TestVerdict verdict;
    if (urlsToTest.size() < 1) {
      verdict = TestVerdict.TEST_INCOMPLETE;
    } else {
      verdict = TestVerdict.NO_PROBLEM;
    }
    
    for (Map.Entry<String, Integer> entry : urlResultCodes.entrySet()) {
      int actualStatusCode = entry.getValue();
      int expectedStatusCode = urlsToTest.get(entry.getKey());
      if (actualStatusCode != expectedStatusCode) {
        verdict = TestVerdict.PROBLEM;
        Log.v(TAG, "Expected status code " + expectedStatusCode + " but got " +
            actualStatusCode);
      }
    }
    testResult.setTestVerdict(verdict);
    storeTestResult();
  }
  
  private void makeHttpRequest(String url) {
    final HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_SECONDS * 1000);
    
    HttpClient httpclient = new DefaultHttpClient(httpParams);
    HttpResponse response;
    String responseString = "";
    int httpStatusResutCode = -1;
    try {
        Log.v(TAG, "About to query URL: " + url);
        response = httpclient.execute(new HttpGet(url));
        StatusLine statusLine = response.getStatusLine();
        httpStatusResutCode = statusLine.getStatusCode();
        if (httpStatusResutCode == HttpStatus.SC_OK) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          response.getEntity().writeTo(out);
          out.close();
          responseString = out.toString();
        }
    } catch (ClientProtocolException e) {
    } catch (IOException e) {
      Log.v(TAG, url + " IOException " + e.getMessage());
    }
    urlResultContents.put(url, responseString);
    urlResultCodes.put(url, httpStatusResutCode);
    Log.v(TAG, "HTTP Status Code for " + url + " was " + httpStatusResutCode);
  }
  
}
