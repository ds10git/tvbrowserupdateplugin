package org.tvbrowser.tvbrowserupdateplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import java.util.Calendar;

public class Settings extends AppCompatActivity {
  private BroadcastReceiver mUpdates;
  private static final int[] UPDATE_FREQUENCIES = {
      -1,
      7,
      30,
      180
  };

  private int mUpdateFequencyCurrent;
  private boolean mIncludeBetaVersions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    mIncludeBetaVersions = PrefUtils.isIncludeBetaVersions(Settings.this);
    mUpdateFequencyCurrent = PrefUtils.getUpdateFrequency(Settings.this);

    ((CheckBox)findViewById(R.id.settings_include_beta)).setChecked(mIncludeBetaVersions);
    ((CheckBox)findViewById(R.id.settings_include_beta)).setOnCheckedChangeListener((buttonView, isChecked) -> {
      if(mIncludeBetaVersions != isChecked) {
        mIncludeBetaVersions = isChecked;
        PrefUtils.setIncludeBetaVersions(Settings.this, isChecked);

        if (PrefUtils.isIncludeBetaVersions(Settings.this)) {
          PrefUtils.setVersionUpdateCurrent(Settings.this, PrefUtils.VALUE_PREF_DEFAULT);
          PrefUtils.setVersionUpdateUrl(Settings.this, "");
        }
      }
    });

    for(int i = 0; i < UPDATE_FREQUENCIES.length; i++) {
      if(UPDATE_FREQUENCIES[i] == mUpdateFequencyCurrent) {
        ((Spinner)findViewById(R.id.settings_frequency_value)).setSelection(i);
        break;
      }
    }

    ((Spinner)findViewById(R.id.settings_frequency_value)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(UPDATE_FREQUENCIES[position] != mUpdateFequencyCurrent) {
          long nextUpdate = PrefUtils.getUpdateSearchNext(Settings.this);

          if(nextUpdate != -1) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(nextUpdate);
            cal.add(Calendar.DAY_OF_YEAR, -mUpdateFequencyCurrent);
            nextUpdate = cal.getTimeInMillis();
          }
          else {
            nextUpdate = System.currentTimeMillis();
          }

          mUpdateFequencyCurrent = UPDATE_FREQUENCIES[position];
          PrefUtils.setUpdateFrequency(Settings.this, UPDATE_FREQUENCIES[position]);
          PrefUtils.setUpdateSearchNext(Settings.this, nextUpdate);
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

    mUpdates = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateVisibility();
      }
    };
  }

  @Override
  protected void onResume() {
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo("org.tvbrowser.tvbrowser", 0);

      if(PrefUtils.getVersionUpdateCurrent(Settings.this) < pInfo.versionCode) {
        PrefUtils.setVersionUpdateCurrent(Settings.this, PrefUtils.VALUE_PREF_DEFAULT);
        PrefUtils.setVersionUpdateUrl(Settings.this, "");
      }
    } catch (PackageManager.NameNotFoundException e) {}

    super.onResume();

    updateVisibility();

    final IntentFilter filter = new IntentFilter(PrefUtils.ACTION_INFO);

    LocalBroadcastManager.getInstance(Settings.this).registerReceiver(mUpdates, filter);
  }

  @Override
  protected void onPause() {
    LocalBroadcastManager.getInstance(Settings.this).unregisterReceiver(mUpdates);
    super.onPause();
  }

  private void updateVisibility() {
    boolean visible = PrefUtils.getVersionUpdateCurrent(Settings.this) != -1;

    findViewById(R.id.settings_info_update_text).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    findViewById(R.id.settings_info_update_button).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
  }

  public void updateFound(final View view) {
    String name = PrefUtils.getVersionUpdateUrl(Settings.this);

    final Intent startDownload = new Intent(getApplicationContext(), Info.class);
    startDownload.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startDownload.setData(Uri.parse(name));
    startDownload.setAction(PrefUtils.ACTION_INSTALL);
    startDownload.putExtra(PrefUtils.EXTRA_NAME_VERSION, name.substring(name.lastIndexOf("/")+1));
    startDownload.putExtra(PrefUtils.EXTRA_URL_DOWNLOAD, name);

    startActivity(startDownload);
  }

  public void searchNow(final View view) {
    final Intent search = new Intent(getApplicationContext(), ServiceCheckAndDownload.class);
    search.setAction(PrefUtils.ACTION_CHECK);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(search);
    }
    else {
      startService(search);
    }
  }
}
