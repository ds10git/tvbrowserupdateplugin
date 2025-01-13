package org.tvbrowser.tvbrowserupdateplugin;

import android.content.Context;
import androidx.preference.PreferenceManager;

import java.util.Calendar;
public final class PrefUtils {
  public static final String URL_UPDATE_PATH = "https://www.tvbrowser-app.de/download/";
  public static final String FILE_NAME_UPDATE = "updates.gz";

  public static final String ACTION_CHECK = "check";
  public static final String ACTION_INSTALL = "install";
  public static final String ACTION_INFO = "org.tvbrowser.tvbrowserupdateplugin.info";

  public static final String EXTRA_VERSION_CODE_CURRENT = "versionCodeCurrent";
  public static final String EXTRA_NAME_VERSION = "versionName";
  public static final String EXTRA_URL_DOWNLOAD = "urlDownload";
  private static final String KEY_PREF_VERSION_UPDATE_CURRENT = "prefUpdateCurrent";
  private static final String KEY_PREF_NO_REVOKE_PERMISSION_ASKED = "prefNoRevokePermissionsAsked";
  private static final String KEY_DOWNLOAD_FILE = "prefDownloadFile";
  private static final String KEY_PREF_VERSION_UPDATE_URL = "prefUpdateUrl";
  private static final String KEY_PREF_VERSION_UPDATE_NEXT = "prefUpdateNext";

  private static final String KEY_PREF_UPDATE_FREQUENCY = "prefUpdateFrequency";
  private static final String KEY_PREF_UPDATE_INCLUDE_BETA = "prefUpdateIncludeBeta";

  public static final int VALUE_PREF_DEFAULT = -1;
  public static final int VALUE_PREF_UPDATE_FREQEUNCY_DEFAULT = 30;
  public static final boolean VALUE_PREF_INCLUDE_BETA_VERSIONS = false;

  private static final boolean VALUE_PREF_NO_REVOKE_PERMISSION_ASKED_DEFAULT = false;

  public static long getUpdateSearchNext(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_PREF_VERSION_UPDATE_NEXT, VALUE_PREF_DEFAULT);
  }

  public static void setUpdateSearchNext(final Context context, long lastUpdate) {
    long nextUpdate = VALUE_PREF_DEFAULT;

    int frequency = getUpdateFrequency(context);

    if(frequency != VALUE_PREF_DEFAULT) {
      if(lastUpdate == -1) {
        lastUpdate = System.currentTimeMillis();
      }

      final Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastUpdate);
      cal.add(Calendar.DAY_OF_YEAR, frequency);

      nextUpdate = cal.getTimeInMillis();
    }

    PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(KEY_PREF_VERSION_UPDATE_NEXT, nextUpdate).apply();
  }

  public static int getVersionUpdateCurrent(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_PREF_VERSION_UPDATE_CURRENT, VALUE_PREF_DEFAULT);
  }

  public static void setVersionUpdateCurrent(final Context context, final int tvbVersion) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_PREF_VERSION_UPDATE_CURRENT, tvbVersion).apply();
  }

  public static String getVersionUpdateUrl(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PREF_VERSION_UPDATE_URL, null);
  }

  public static void setVersionUpdateUrl(final Context context, final String url) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PREF_VERSION_UPDATE_URL, url).apply();
  }

  public static void setNoRevokePermissionAsked(final Context context, final boolean value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_PREF_NO_REVOKE_PERMISSION_ASKED, value).apply();
  }

  public static boolean getNoRevokePermissionAsked(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_PREF_NO_REVOKE_PERMISSION_ASKED, VALUE_PREF_NO_REVOKE_PERMISSION_ASKED_DEFAULT);
  }

  public static int getUpdateFrequency(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_PREF_UPDATE_FREQUENCY, VALUE_PREF_UPDATE_FREQEUNCY_DEFAULT);
  }

  public static void setUpdateFrequency(final Context context, final int frequency) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_PREF_UPDATE_FREQUENCY, frequency).apply();
  }

  public static boolean isIncludeBetaVersions(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_PREF_UPDATE_INCLUDE_BETA, VALUE_PREF_INCLUDE_BETA_VERSIONS);
  }

  public static void setIncludeBetaVersions(final Context context, final boolean value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_PREF_UPDATE_INCLUDE_BETA, value).apply();
  }

  public static String getDownloadFile(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_DOWNLOAD_FILE, null);
  }

  public static void setDownloadFile(final Context context, final String downloadFile) {
    if(downloadFile == null) {
      PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_DOWNLOAD_FILE).apply();
    }
    else {
      PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_DOWNLOAD_FILE, downloadFile).apply();
    }
  }
}
