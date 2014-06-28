package org.ericbeach.networktest.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.TextView;

import org.ericbeach.networktest.R;
import org.ericbeach.networktest.R.id;
import org.ericbeach.networktest.R.layout;

/**
 * The activity (i.e., view) that represents the About page.
 * @author Eric Beach (ebeach@google.com)
 */
public class AboutActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      // Set the text view as the activity layout
      setContentView(R.layout.activity_about);
      
      displayBuildName();
  }
  
  private void displayBuildName() {
    Context context = getApplicationContext();      
    String myVersionName = "";
    try {
      myVersionName = context.getPackageManager()
          .getPackageInfo(context.getPackageName(), 0).versionName;
      
      TextView textView = new TextView(this); 
      textView = (TextView) findViewById(R.id.textViewAboutBuild); 
      textView.setText(myVersionName);
    } catch (Exception e) {
    }
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
}
