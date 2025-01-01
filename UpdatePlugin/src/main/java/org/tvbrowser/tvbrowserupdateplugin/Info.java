package org.tvbrowser.tvbrowserupdateplugin;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

public class Info extends AppCompatActivity {
  private static final int REQUEST_CODE_PERMISSION_GRANT = 52;
  private static final int INSTALL_TVBROWSER = 1;

  private Runnable mInstallRunnable;
  private File mDownloadFile;

  @Override
  protected void onResume() {
    super.onResume();
    Log.d("info22","Info");
    if(getIntent() != null && PrefUtils.ACTION_INSTALL.equals(getIntent().getAction())) {
      final String versionName = getIntent().getStringExtra(PrefUtils.EXTRA_NAME_VERSION);
      final String url = getIntent().getStringExtra(PrefUtils.EXTRA_URL_DOWNLOAD);

      final AlertDialog.Builder b = new AlertDialog.Builder(Info.this);
      b.setCancelable(false);
      b.setTitle(R.string.app_name);
      b.setMessage(getString(R.string.dialog_info_update_available).replace("{0}",versionName));
      b.setPositiveButton(R.string.dialog_info_update_ok, (dialog,which) -> {downloadTvb(url,versionName); });
      b.setNegativeButton(android.R.string.cancel, (dialog,which) -> { finish(); });
      b.show();
    }
  }

  private void downloadTvb(final String downloadUrl, final String versionName) {
    AsyncTask<String, Void, Boolean> async = new AsyncTask<String, Void, Boolean>() {
      private ProgressDialog mProgress;
      private File mPluginFile;
      private String mDownloadURL;

      protected void onPreExecute() {
        mProgress = new ProgressDialog(Info.this);
        mProgress.setCancelable(false);
        mProgress.setMessage(getString(R.string.dialog_info_update_download).replace("{0}", versionName));
        mProgress.show();
      }

      @Override
      protected Boolean doInBackground(String... params) {
        mPluginFile = new File(params[0]);

        if(mPluginFile.isFile()) {
          mPluginFile.delete();
        }

        mDownloadURL = params[1];

        return NetUtils.saveUrl(params[0], params[1], 15000);
      }

      protected void onPostExecute(Boolean result) {
        mProgress.dismiss();
        if (result) {
          mDownloadFile = mPluginFile;
          mInstallRunnable = new Runnable() {
            @Override
            public void run() {
              final Uri apkUri = FileProvider.getUriForFile(Info.this, Info.this.getString(R.string.authority_file_provider), mPluginFile);

              Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
              install.setData(apkUri);
              install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
              Info.this.startActivityForResult(install, INSTALL_TVBROWSER);
            }
          };

          if (isAtLeastAndroidN()) {
            if(!canRequestPackageInstalls(Info.this)) {
              final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Info.this);
              builder.setTitle(R.string.dialog_permission_title);
              builder.setCancelable(false);
              builder.setMessage(R.string.dialog_permission_message);
              builder.setPositiveButton(R.string.dialog_permission_ok, (dialog, which) -> Info.this.startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", Info.this.getPackageName()))), REQUEST_CODE_PERMISSION_GRANT));
              builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> finish());
              builder.show();
            }
            else {
              mInstallRunnable.run();
            }
          }
          else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(mPluginFile), "application/vnd.android.package-archive");
            Info.this.startActivityForResult(intent, INSTALL_TVBROWSER);
          }
        }
        else {
          final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Info.this);
          builder.setCancelable(false);
          builder.setTitle(R.string.dialog_info_update_error);

          final SpannableString message = new SpannableString(getString(R.string.dialog_info_update_error_message).replace("{0}", mDownloadURL));
          Linkify.addLinks(message, Linkify.WEB_URLS);
          builder.setMessage(message);

          builder.setPositiveButton(android.R.string.ok, (dialog,which) -> finish());
          android.app.AlertDialog d = builder.create();
          d.show();

          ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }
      }
    };

    final File downloadDirectory = NetUtils.getDownloadDirectory(Info.this);
    final File downloadFile = new File(downloadDirectory,"tvbrowser-update-"+versionName+".apk");

    async.execute(downloadFile.getAbsolutePath(), downloadUrl);
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode == REQUEST_CODE_PERMISSION_GRANT) {
      if (mInstallRunnable != null && canRequestPackageInstalls(Info.this)) {
        mInstallRunnable.run();
      } else {
        finish();
      }
    }
    else {
      finish();
    }
  }

  @Override
  public void finish() {
    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    if(manager != null) {
      manager.cancel(ServiceCheckAndDownload.ID_NOTIFICATION_INFO_UPDATE);
    }

    PrefUtils.setVersionUpdateCurrent(Info.this, PrefUtils.VALUE_PREF_DEFAULT);
    PrefUtils.setVersionUpdateUrl(Info.this, "");

    if(mDownloadFile != null && mDownloadFile.isFile() && !mDownloadFile.delete()) {
      mDownloadFile.deleteOnExit();
    }

    super.finish();
  }
}
