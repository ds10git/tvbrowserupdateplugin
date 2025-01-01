package org.tvbrowser.tvbrowserupdateplugin;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class App extends Application {
  private static App INSTANCE;

  @Override
  public void onCreate() {
    super.onCreate();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel();
    }

    INSTANCE = this;
  }

  public static App getInstance() {
    return INSTANCE;
  }

  /**
   * Returns the apps's appropriate priority global notification channel identifier
   */
  public String getNotificationChannelId() {
    return getPackageName();
  }

  /**
   * Returns the app's global high priority notification channel name
   */
  private String getNotificationChannelName() {
    return getString(R.string.app_name);
  }

  /**
   * Each foreground service or notification requires a {@link NotificationChannel} starting with
   * Android 8.0 Oreo (SDK 26). For categorization or grouping, additional channels are possible.
   *
   * @see <a href="https://developer.android.com/guide/topics/ui/notifiers/notifications.html#ManageChannels">Notifications Overview</a>
   * @see <a href="https://developer.android.com/about/versions/oreo/android-8.0.html#notifications">Android 8.0 Features and APIs</a>
   */
  @RequiresApi(Build.VERSION_CODES.O)
  private void createNotificationChannel() {
    //final long[] vibrationPattern = new long[] {1000,200,1000,400,1000,600};

    final NotificationChannel notificationChannelDefault = new NotificationChannel(getNotificationChannelId(),getNotificationChannelName(), NotificationManager.IMPORTANCE_DEFAULT);
    //notificationChannelDefault.setVibrationPattern(vibrationPattern);
    notificationChannelDefault.setSound(null,null);

    final NotificationManager service = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    if (service != null) {
      service.createNotificationChannel(notificationChannelDefault);
    }
  }
}
