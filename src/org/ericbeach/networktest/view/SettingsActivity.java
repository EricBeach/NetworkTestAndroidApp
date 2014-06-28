package org.ericbeach.networktest.view;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import org.ericbeach.networktest.R;
import org.ericbeach.networktest.R.xml;

/**
 * The activity (i.e., view) for the settings page.
 * @author Eric Beach (ebeach@google.com)
 */
public class SettingsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Display the fragment as the main content.
      getFragmentManager()
          .beginTransaction()
          .replace(android.R.id.content, new SettingsFragment())
          .commit();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case android.R.id.home:
          NavUtils.navigateUpFromSameTask(this);
          return true;
      }
      return super.onOptionsItemSelected(item);
  }
  
  /**
   * Settings view fragment that is loaded into the settings view and binds to the preferences
   * XML file to dynamically create the settings UI. 
   * @author Eric Beach (ebeach@google.com)
   */
  public static class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
  }
}
