package org.tvbrowser.tvbrowserupdateplugin;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.Executors;

public class Info extends AppCompatActivity {
  private Runnable mInstallRunnable;
  private File mDownloadFile;

  private boolean mCleanNot;

  private final ActivityResultLauncher<Intent> mRequestPermission = registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          r -> {
            if (mInstallRunnable != null && canRequestPackageInstalls(Info.this)) {
              mInstallRunnable.run();
            } else {
              finish();
            }
          }
  );

  private final ActivityResultLauncher<Intent> mStartInstall = registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(), r -> finish()
  );

  @Override
  protected void onResume() {
    super.onResume();
    if(getIntent() != null && PrefUtils.ACTION_INSTALL.equals(getIntent().getAction())) {
      final String versionName = getIntent().getStringExtra(PrefUtils.EXTRA_NAME_VERSION);
      final String url = getIntent().getStringExtra(PrefUtils.EXTRA_URL_DOWNLOAD);

      final AlertDialog.Builder b = new AlertDialog.Builder(Info.this);
      b.setCancelable(false);
      b.setTitle(R.string.app_name);
      b.setMessage(getString(R.string.dialog_info_update_available).replace("{0}",versionName));
      b.setPositiveButton(R.string.dialog_info_update_ok, null);
      b.setNegativeButton(android.R.string.cancel, (dialog,which) -> finish());

      final AlertDialog dialog = b.create();
      dialog.show();

      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
        if(!NetUtils.isOnline(Info.this)) {
          NetUtils.showNoInternetConnectionDialog(Info.this);
        }
        else {
          dialog.dismiss();
          downloadTvb(url,versionName);
        }
      });
    }
  }

  private boolean testDownload(final File mPluginFile, final String downloadUrl) {
    final String downloadFile = PrefUtils.getDownloadFile(Info.this);
    if(downloadFile != null) {
      final File dFile = new File(downloadFile);

      if(dFile.isFile() && dFile.getAbsolutePath().equals(mPluginFile.getAbsolutePath())) {
        return true;
      }
      else {
        if(mPluginFile.isFile()) {
          mPluginFile.delete();
        }
        return NetUtils.saveUrl(mPluginFile.getAbsolutePath(), downloadUrl, 15000);
      }
    }

    return NetUtils.saveUrl(mPluginFile.getAbsolutePath(), downloadUrl, 15000);
  }

  private void downloadTvb(final String downloadUrl, final String versionName) {
    final Handler handler = new Handler(Looper.getMainLooper());
    final ProgressDialog mProgress = new ProgressDialog(Info.this);
    final File mPluginFile = new File(NetUtils.getDownloadDirectory(Info.this),"tvbrowser-update-"+versionName);

    mProgress.setCancelable(false);
    mProgress.setMessage(getString(R.string.dialog_info_update_download).replace("{0}", versionName));

    Executors.newSingleThreadExecutor().execute(() -> {

      handler.post(mProgress::show);

      boolean result = testDownload(mPluginFile, downloadUrl);

      mProgress.dismiss();
      
      if (result) {
        mDownloadFile = mPluginFile;
        mInstallRunnable = () -> {
          final Uri apkUri = FileProvider.getUriForFile(Info.this, Info.this.getString(R.string.authority_file_provider), mPluginFile);

          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

          mStartInstall.launch(intent);
        };

        if (isAtLeastAndroidN()) {
          if(!canRequestPackageInstalls(Info.this)) {
            final android.app.AlertDialog.Builder builder = getBuilder();
            handler.post(builder::show);
          }
          else {
            mInstallRunnable.run();
          }
        }
        else {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setDataAndType(Uri.fromFile(mPluginFile), "application/vnd.android.package-archive");
          mStartInstall.launch(intent);
        }
      }
      else {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Info.this);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_info_update_error);
        builder.setMessage(R.string.dialog_info_update_error_message);
        builder.setNegativeButton(android.R.string.cancel, ((dialog, which) -> finish()));
        builder.setPositiveButton(R.string.dialog_info_update_error_ok, (dialog,which) -> {
          Intent openBrowser = new Intent(Intent.ACTION_VIEW);
          openBrowser.setData(Uri.parse(downloadUrl));
          startActivity(openBrowser);
          finish();
        });
        handler.post(builder::show);
      }
    });
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private android.app.AlertDialog.Builder getBuilder() {
    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Info.this);
    builder.setTitle(R.string.dialog_permission_title);
    builder.setCancelable(false);
    builder.setMessage(R.string.dialog_permission_message);
    builder.setPositiveButton(R.string.dialog_permission_ok, (dialog, which) -> mRequestPermission.launch(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", Info.this.getPackageName())))));
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> finish());
    return builder;
  }

  public static boolean isAtLeastAndroidN() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
  }

  public static boolean isAtLeastAndroidO() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
  }

  public static boolean canRequestPackageInstalls(final Context context) {
    return !isAtLeastAndroidO() || context.getPackageManager().canRequestPackageInstalls();
  }

  @Override
  public void finish() {
    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    if(manager != null) {
      manager.cancel(ServiceCheckAndDownload.ID_NOTIFICATION_INFO_UPDATE);
    }

    if(!mCleanNot) {
      PrefUtils.setVersionUpdateCurrent(Info.this, PrefUtils.VALUE_PREF_DEFAULT);
      PrefUtils.setVersionUpdateUrl(Info.this, "");
      PrefUtils.setDownloadFile(Info.this, null);

      if (mDownloadFile != null && mDownloadFile.isFile() && !mDownloadFile.delete()) {
        mDownloadFile.deleteOnExit();
      }
    }
    else {
      mCleanNot = false;

      if(mDownloadFile != null) {
        PrefUtils.setDownloadFile(Info.this, mDownloadFile.getAbsolutePath());
      }
    }

    super.finish();
  }
}
