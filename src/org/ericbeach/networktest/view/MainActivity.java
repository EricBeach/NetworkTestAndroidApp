package org.ericbeach.networktest.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.ericbeach.networktest.BackgroundManagerService;
import org.ericbeach.networktest.NetworkChangeBroadcastReceiver;
import org.ericbeach.networktest.NetworkUtil;
import org.ericbeach.networktest.R;
import org.ericbeach.networktest.R.id;
import org.ericbeach.networktest.R.layout;
import org.ericbeach.networktest.R.menu;
import org.ericbeach.networktest.R.string;
import org.ericbeach.networktest.R.xml;
import org.ericbeach.networktest.TestsRunnerService;
import org.ericbeach.networktest.model.TestId;
import org.ericbeach.networktest.model.TestResultManager;
import org.ericbeach.networktest.model.TestVerdict;

/**
 * The main activity (i.e., view) for the application. It is the home screen.
 * @author Eric Beach (ebeach@google.com)
 */
public class MainActivity extends Activity {
  public static final String TAG = MainActivity.class.getName();
  
  private static volatile Context applicationContext;
  private BroadcastReceiver testsStatusReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == TestsRunnerService.INTENT_BROADCAST_TEST_COMPLETED) {
        Log.v(this.getClass().getName(), "Received broadcast that test completed");
        MainActivity.this.receivedTestCompletedBroadcast(intent);
      }
    }
  };
  
  private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_CHANGED) {
        Log.v(this.getClass().getName(),
            "Received broadcast intent that network status changed");
        MainActivity.this.receivedTestCompletedBroadcast(intent); 
      }
    }
  };
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Must always call super.
    // http://developer.android.com/guide/components/activities.html#ImplementingLifecycleCallbacks
    super.onCreate(savedInstanceState);
    
    // Set default settings for users who are loading application for the first time.
    // @see http://developer.android.com/reference/android/preference/PreferenceManager.html
    // @see http://developer.android.com/guide/topics/ui/settings.html#Defaults
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    
    // Set this activity's layout.
    setContentView(R.layout.activity_main);

    // Set singleton application context which can be used from other components.
    applicationContext = getApplicationContext();
  }
  
  @Override
  protected void onResume() {
    // Must always call super.
    // http://developer.android.com/guide/components/activities.html#ImplementingLifecycleCallbacks
    super.onResume();
    
    // Since the MainActivity is paused() then resumed() when the device changes
    // from horizontal to vertical (e.g., you rotate it), it re-draws the screen to the default
    // layout. The default layout, which is what is contained in the .xml file in the layout,
    // folder does not contain the test verdicts (only the empty placeholder textview outline).
    // onResume() is called when onCreate() is invoked, so this takes care of the initial
    // first time run as well.
    updateActivityLayoutAndContents();
    
    // Start the background manager service to ensure its running.
    startBackgroundManagerService();
    
    // Register the broadcast receiver to listen for tests completed broadcasts.
    registerReceiver(testsStatusReceiver,
        new IntentFilter(TestsRunnerService.INTENT_BROADCAST_TEST_COMPLETED));

    // Register the broadcast receiver to listen for network status changed broadcasts.
    registerReceiver(networkChangeReceiver,
        new IntentFilter(NetworkChangeBroadcastReceiver.INTENT_NETWORK_STATUS_CHANGED));
  
  }
  
  @Override
  protected void onPause() {
    // Must always call super.
    // http://developer.android.com/guide/components/activities.html#ImplementingLifecycleCallbacks
    super.onPause();
    
    // Unregister the receiver so it doesn't leak.
    // @see http://stackoverflow.com/a/21136523, http://stackoverflow.com/a/7887459
    unregisterReceiver(testsStatusReceiver);
    unregisterReceiver(networkChangeReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // @see http://developer.android.com/guide/topics/ui/menus.html
    // Inflate the menu items for use in the action bar.
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_activity_actions, menu);
    return super.onCreateOptionsMenu(menu);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Handle presses on the action bar items.
      switch (item.getItemId()) {
          case R.id.action_about:
            Intent intentAbout = new Intent(this, AboutActivity.class);
            startActivity(intentAbout);
              return true;
          case R.id.action_settings:
            Intent intentSettings = new Intent(this, SettingsActivity.class);
            startActivity(intentSettings);
              return true;
          default:
              return super.onOptionsItemSelected(item);
      }
  } 
 
  public static synchronized Context getAppContext() {
    if (applicationContext == null) {
      Log.v(MainActivity.class.getName(), "About to return null application context");  
    }
    return applicationContext;
  }
  
  private void startBackgroundManagerService() {
    Intent intent = new Intent(getAppContext(), BackgroundManagerService.class);
    Log.v(TAG, "Start background manager service");
    getAppContext().startService(intent);
  }
  
  private void receivedTestCompletedBroadcast(Intent intent) {
    Log.v(TAG, "Received broadcast that tests completed successfully");
    updateActivityLayoutAndContents();
  }
  
  private void updateActivityLayoutAndContents() {
    // Check for the situation where we found a network problem but the network is disconnected
    // such as the following scenario: (1) user begins tests (2) user disconnects from Internet
    // (3) networkChangeReceiver detects network change [2] and shuts down (4) tests fail
    // and we set in motion showing Problem Detected test results.
    // In other words, we don't want to display Problem Detected when the user is totally
    // disconnected from the network.
    if (NetworkUtil.isWiFiOrCelluarNetworkOn()) {
      displayUpdatedNetworkTestResults();   
    } else {
      displayNetworkTestsPaused(); 
    }
  }
  
  private void displayNetworkTestsPaused() {
    setupDisplayForConnectivityTestsPaused();
    Log.v(TAG, "Display that network tests are paused");
  }
  
  private void displayUpdatedNetworkTestResults() {
    setupDisplayForConnectivityTestsRunning();
    hideUserDisableNetworkTests();
    paintNetworkTestResults();
    paintLastTestTimestamp();
    
    Log.v(TAG, "Repaint network tests results with most recent results inside view");
  }
  
  private void setupDisplayForConnectivityTestsPaused() {
    TableLayout resultsContainer = (TableLayout) findViewById(R.id.main_test_results_container);
    resultsContainer.setVisibility(View.INVISIBLE);
    
    TextView testsPausedContainer = (TextView) findViewById(R.id.main_tests_paused);
    testsPausedContainer.setVisibility(View.VISIBLE);
  }
  
  private void setupDisplayForConnectivityTestsRunning() {
    TableLayout resultsContainer = (TableLayout) findViewById(R.id.main_test_results_container);
    resultsContainer.setVisibility(View.VISIBLE);
    
    TextView testsPausedContainer = (TextView) findViewById(R.id.main_tests_paused);
    testsPausedContainer.setVisibility(View.INVISIBLE);
  }
  
  /**
   * Check the user's settings/preferences to determine which network/connectivity checks he
   * wants run and hide the checks that he does not want run.
   */
  private void hideUserDisableNetworkTests() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        super.getApplicationContext());
    final boolean isDnsPoisoningCheckEnabled =
        settings.getBoolean("pref_key_check_dns_poisoning", false);
    final boolean isHttpFilteringCheckEnabled =
        settings.getBoolean("pref_key_check_http_filtering", false);
    final boolean isGoogleFilteringCheckEnabled =
        settings.getBoolean("pref_key_check_google_availability", false);

    TableRow dnsPoisoningTableRow = (TableRow) findViewById(R.id.main_test_row_dns_check);
    if (isDnsPoisoningCheckEnabled) {
      dnsPoisoningTableRow.setVisibility(View.VISIBLE);  
    } else {
      dnsPoisoningTableRow.setVisibility(View.INVISIBLE);
    }
    
    TableRow httpFilteringTableRow = (TableRow) findViewById(R.id.main_test_row_http_check);
    if (isHttpFilteringCheckEnabled) {
      httpFilteringTableRow.setVisibility(View.VISIBLE);  
    } else {
      httpFilteringTableRow.setVisibility(View.INVISIBLE);
    }
    
    TableRow googleFilteringTableRow = (TableRow) findViewById(R.id.main_test_row_google_check);
    if (isGoogleFilteringCheckEnabled) {
      googleFilteringTableRow.setVisibility(View.VISIBLE);  
    } else {
      googleFilteringTableRow.setVisibility(View.INVISIBLE);
    }
  }
  
  private void paintLastTestTimestamp() {
    TestResultManager resultManager = TestResultManager.getInstance();
    TextView lastTestTimestampText = (TextView) findViewById(R.id.main_test_last_run_time);
    lastTestTimestampText.setText(resultManager.getTimeLastTestCompleted());
  }
  
  private void paintNetworkTestResults() {
    TestResultManager resultManager = TestResultManager.getInstance();
    
    TestVerdict dnsPoisoningVerdict = resultManager.getTestVerdict(TestId.DNS_POISONING);
    TextView dnsPoisoningVerdictView = (TextView) findViewById(
        R.id.main_test_row_dns_check_verdict);
    setVerdictViewText(dnsPoisoningVerdictView, dnsPoisoningVerdict);
    
    TestVerdict httpFilteringVerdict = resultManager.getTestVerdict(TestId.HTTP_FILTERING);
    TextView httpFilteringVerdictView = (TextView) findViewById(
        R.id.main_test_row_http_check_verdict);
    setVerdictViewText(httpFilteringVerdictView, httpFilteringVerdict);

    TestVerdict googleFilteringVerdict = resultManager.getTestVerdict(TestId.GOOGLE_AVAILABILITY);
    TextView googleFilteringVerdictView = (TextView) findViewById(
        R.id.main_test_row_google_check_verdict);
    setVerdictViewText(googleFilteringVerdictView, googleFilteringVerdict);
  }
  
  private void setVerdictViewText(TextView view, TestVerdict verdict) {
    switch (verdict) {
      case NO_PROBLEM:
        view.setText(R.string.main_check_result_no_filtering);
        break;
      case PROBLEM:
        view.setText(R.string.main_check_result_filtering);
        break;
      default:
        view.setText("Test Not Finished");  
    }
  }
}
