package org.tvbrowser.tvbrowserupdateplugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceCheckAndDownload extends Service {
  private static final int ID_NOTIFICATION_CHECK_OR_DOWNLOAD = 14;
  public static final int ID_NOTIFICATION_INFO_UPDATE = 15;

  public ServiceCheckAndDownload() {
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    NotificationCompat.Builder notify = new NotificationCompat.Builder(ServiceCheckAndDownload.this, App.getInstance().getNotificationChannelId());
    notify.setContentTitle(getString(R.string.app_name));
    notify.setSmallIcon(R.drawable.ic_notify);
    startForeground(ID_NOTIFICATION_CHECK_OR_DOWNLOAD, notify.build());


    if(intent != null) {
      if (PrefUtils.ACTION_CHECK.equals(intent.getAction())) {
        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notify.setContentText(getString(R.string.notification_search));
        manager.notify(ID_NOTIFICATION_CHECK_OR_DOWNLOAD, notify.build());

        int currentVersion = intent.getIntExtra(PrefUtils.EXTRA_VERSION_CODE_CURRENT, 0);
Log.d("info22",""+currentVersion);
        if(currentVersion == 0) {
          try {
            PackageInfo pInfo = getPackageManager().getPackageInfo("org.tvbrowser.tvbrowser", 0);
            currentVersion = pInfo.versionCode;
          } catch (PackageManager.NameNotFoundException e) {
            Log.d("info5","",e);
          }
        }

        currentVersion = Math.max(currentVersion,PrefUtils.getVersionUpdateCurrent(ServiceCheckAndDownload.this));

        final Intent info = new Intent(PrefUtils.ACTION_INFO);

        if(NetUtils.isOnline(ServiceCheckAndDownload.this)) {
          File directory = NetUtils.getDownloadDirectory(ServiceCheckAndDownload.this);
          File target = new File(directory, PrefUtils.FILE_NAME_UPDATE);

          if (NetUtils.saveUrl(target.getAbsolutePath(), PrefUtils.URL_UPDATE_PATH + PrefUtils.FILE_NAME_UPDATE, 10000)) {
            BufferedReader in = null;

            try {
              in = new BufferedReader(new InputStreamReader(NetUtils.decompressStream(new FileInputStream(target))));

              String line = null;
              String[] parts = null;

              int index = 0;

              while ((line = in.readLine()) != null) {
                if (line.contains(";") && (index == 0 || PrefUtils.isIncludeBetaVersions(ServiceCheckAndDownload.this))) {
                  index++;
                  parts = line.split(";");

                  if (currentVersion >= Integer.parseInt(parts[0])) {
                    parts = null;
                  }
                }
              }

              PrefUtils.setUpdateSearchNext(ServiceCheckAndDownload.this, System.currentTimeMillis());

              if(parts != null) {
                final Intent startDownload = new Intent(getApplicationContext(), Info.class);
                startDownload.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startDownload.setData(Uri.parse(parts[3]));
                startDownload.setAction(PrefUtils.ACTION_INSTALL);
                startDownload.putExtra(PrefUtils.EXTRA_NAME_VERSION, parts[1]);
                startDownload.putExtra(PrefUtils.EXTRA_URL_DOWNLOAD, parts[3]);

                PrefUtils.setVersionUpdateCurrent(ServiceCheckAndDownload.this, Integer.parseInt(parts[0]));
                PrefUtils.setVersionUpdateUrl(ServiceCheckAndDownload.this, parts[3]);

                PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, startDownload, 0);

                NotificationCompat.Builder n2 = new NotificationCompat.Builder(ServiceCheckAndDownload.this, App.getInstance().getNotificationChannelId());
                n2.setPriority(NotificationCompat.PRIORITY_DEFAULT);

                n2.setAutoCancel(true);
                n2.setSmallIcon(R.drawable.ic_notify);
                n2.setContentTitle(getString(R.string.notification_found_title));
                n2.setContentText(getString(R.string.notification_found_message));
                n2.setContentIntent(pending);
                manager.notify(ID_NOTIFICATION_INFO_UPDATE, n2.build());

                info.putExtra(PrefUtils.EXTRA_NAME_VERSION, parts[1]);
                info.putExtra(PrefUtils.EXTRA_URL_DOWNLOAD, parts[3]);
              }
            } catch (IOException ioe) {

            } finally {
              if (in != null) {
                try {
                  in.close();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            }

            target.delete();
          }
        }

        LocalBroadcastManager.getInstance(ServiceCheckAndDownload.this).sendBroadcast(info);
      }

      stopSelf();
    }

    return Service.START_STICKY;
  }

  @Override
  public void onDestroy() {
    stopForeground(true);

    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) { return null; }
}
