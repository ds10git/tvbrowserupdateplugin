package org.tvbrowser.tvbrowserupdateplugin;

import static androidx.core.content.UnusedAppRestrictionsConstants.*;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class Settings extends AppCompatActivity {
  private static final int[] UPDATE_FREQUENCIES = {
      -1,
      7,
      30,
      180
  };

  private final ActivityResultLauncher<Intent> mStartActivityIntent = registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          r -> {
            PrefUtils.setNoRevokePermissionAsked(getApplicationContext(),true);
          }
  );
  // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
  private final ActivityResultLauncher<String> requestPermissionLauncher =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            //handled by system
          });
  private int mUpdateFequencyCurrent;
  private boolean mIncludeBetaVersions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    try{
      String versionName = getPackageManager()
              .getPackageInfo(getPackageName(), 0).versionName;
      setTitle(getTitle()+" "+ versionName);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

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
  }
  @Subscribe
  public void onMessageEvent(ServiceCheckAndDownload.MessageEvent event) {
    updateVisibility();

    if(event.mVersionName == null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
      builder.setCancelable(false);
      builder.setTitle(R.string.dialog_no_update_title);
      builder.setMessage(R.string.dialog_no_update_message);
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }
  }

  @Override
  protected void onResume() {
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo("org.tvbrowser.tvbrowser", 0);

      if(PrefUtils.getVersionUpdateCurrent(Settings.this) < pInfo.versionCode) {
        PrefUtils.setVersionUpdateCurrent(Settings.this, PrefUtils.VALUE_PREF_DEFAULT);
        PrefUtils.setVersionUpdateUrl(Settings.this, "");
      }
    } catch (PackageManager.NameNotFoundException e) {
      //ignore
    }

    super.onResume();

    updateVisibility();

    final IntentFilter filter = new IntentFilter(PrefUtils.ACTION_INFO);

    EventBus.getDefault().register(this);

    if(!PrefUtils.getNoRevokePermissionAsked(getApplicationContext())) {
      ListenableFuture<Integer> future = PackageManagerCompat.getUnusedAppRestrictionsStatus(getApplicationContext());

      future.addListener(() -> {
        try {
          onResult(future.get());
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }, ContextCompat.getMainExecutor(getApplicationContext()));
    }
  }

  void onResult(int appRestrictionsStatus) {
    switch (appRestrictionsStatus) {
      // Couldn't fetch status. Check logs for details.
      case ERROR: {
        break;
      }
      // Restrictions don't apply to your app on this device.
      case FEATURE_NOT_AVAILABLE: {
        break;
      }
      // The user has disabled restrictions for your app.
      case DISABLED: {
        break;
      }
      // If the user doesn't start your app for a few months, the system will
      // place restrictions on it. See the API_* constants for details.
      case API_30_BACKPORT:
      case API_30:
      case API_31: {
        handleRestrictions(appRestrictionsStatus);
        break;
      }
    }
  }

  void handleRestrictions(int appRestrictionsStatus) {
    AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
    builder.setCancelable(false);
    builder.setTitle(R.string.dialog_no_revoke_title);
    builder.setMessage(R.string.dialog_no_revoke_message);
    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
      AlertDialog.Builder builder1 = new AlertDialog.Builder(Settings.this);
      builder1.setCancelable(false);
      builder1.setTitle(R.string.dialog_no_revoke_info_title);
      builder1.setMessage(R.string.dialog_no_revoke_info_message);
      builder1.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
        // If your app works primarily in the background, you can ask the user
        // to disable these restrictions. Check if you have already asked the
        // user to disable these restrictions. If not, you can show a message to the
        // user explaining why permission auto-reset or app hibernation should be
        // disabled. Then, redirect the user to the page in system settings where they
        // can disable the feature.

        Intent intent = IntentCompat.createManageUnusedAppRestrictionsIntent(Settings.this, getPackageName());

        // You must use startActivityForResult(), not startActivity(), even if
        // you don't use the result code returned in onActivityResult().
        mStartActivityIntent.launch(intent);
      });

      builder1.show();
    });
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
      PrefUtils.setNoRevokePermissionAsked(Settings.this, true);
    });
    builder.show();
  }

  @Override
  protected void onPause() {
    EventBus.getDefault().unregister(this);
    super.onPause();
  }

  private void updateVisibility() {
    boolean visible = PrefUtils.getVersionUpdateCurrent(Settings.this) != -1;

    findViewById(R.id.settings_info_update_text).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    findViewById(R.id.settings_info_update_button).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    findViewById(R.id.settings_info_note_text).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo("com.google.android.gms", 0);

      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);

      return true;
    } catch (PackageManager.NameNotFoundException e) {
      Log.d("info5","",e);
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.open_play_protect) {
      final Intent i = new Intent();
      i.setClassName("com.google.android.gms", "com.google.android.gms.security.settings.VerifyAppsSettingsActivity" );
      try {
        startActivity(i);
      } catch (android.content.ActivityNotFoundException ex) {
        Toast.makeText(getApplicationContext(), getString(R.string.open_play_protect_error), Toast.LENGTH_LONG).show();
      }
    }

    return super.onOptionsItemSelected(item);
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
      builder.setCancelable(false);
      builder.setTitle(R.string.dialog_notification_permission_title);
      builder.setMessage(R.string.dialog_notification_permission_message);
      builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
      });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.show();
    }

    if(!NetUtils.isOnline(Settings.this)) {
      NetUtils.showNoInternetConnectionDialog(Settings.this);
    }
    else {
      final Intent search = new Intent(getApplicationContext(), ServiceCheckAndDownload.class);
      search.setAction(PrefUtils.ACTION_CHECK);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(search);
      } else {
        startService(search);
      }
    }
  }
}
