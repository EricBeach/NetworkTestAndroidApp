package org.ericbeach.networktest.model;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Network connectivity test to check DNS Poisoning.
 * @author Eric Beach (ebeach@google.com)
 */
public class DnsPoisoningTest extends NetworkTest {
  public static final String TAG = DnsPoisoningTest.class.getName();
  
  HashMap<String, String> hostnameQueryResults = new HashMap<String, String>();
  HashMap<String, List<String>> hostnamesToTest = new HashMap<String, List<String>>();
  
  public DnsPoisoningTest() {
    super(TestId.DNS_POISONING);
    hostnamesToTest.put("ccd-testing-v4.gstatic.com",
        Arrays.asList("216.239.32.21", "216.239.34.21", "216.239.36.21", "216.239.38.21"));
  }
  
  @Override
  public void runTest() {
    for (Map.Entry<String, List<String>> entry : hostnamesToTest.entrySet()) {
      String hostToQuery = entry.getKey();
      Log.v(TAG, "About to query hostname " + hostToQuery);
      makeDnsQuery(hostToQuery);
    }
    analyzeResults();
  }

  @Override
  protected void analyzeResults() {
    testResult.setTestVerdict(TestVerdict.NO_PROBLEM);
    
    // A hostname we queried did not give us any results.
    if (hostnameQueryResults.size() < hostnamesToTest.size()) {
      Log.v(TAG, "Queried " + hostnamesToTest.size() + " hostnames and only received " +
          hostnameQueryResults.size() + " results. Noting a problem.");
      testResult.setTestVerdict(TestVerdict.PROBLEM);  
    }
    
    for (Map.Entry<String, String> entry : hostnameQueryResults.entrySet()) {
      String queryResultIp = entry.getValue();
      List<String> expectedResults = hostnamesToTest.get(entry.getKey());
      if (!expectedResults.contains(queryResultIp)) {
        testResult.setTestVerdict(TestVerdict.PROBLEM);
        Log.v(TAG, "Hostname " + entry.getKey() + " did not return valid IP");
        break;
      }
    }
    storeTestResult();
  }
  
  private void makeDnsQuery(String hostname) {
    try {
      String ip;
      InetAddress[] inetAddress = InetAddress.getAllByName(hostname);
      if (inetAddress.length > 0) {
        ip = inetAddress[0].getHostAddress();
        Log.v(TAG, "Query for hostname " + hostname + " returned IP " + ip);
      } else {
        ip = "";
        Log.v(TAG, "Query for hostname " + hostname + " returned IP " + ip);
      }
      hostnameQueryResults.put(hostname, ip);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      Log.v(TAG, "Query for hostname " + hostname + " returned UnknownHostException " +
          e.getMessage());
    }
  }
}
