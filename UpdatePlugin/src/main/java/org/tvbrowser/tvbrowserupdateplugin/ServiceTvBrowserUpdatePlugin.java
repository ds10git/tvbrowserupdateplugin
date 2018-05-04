package org.tvbrowser.tvbrowserupdateplugin;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ReceiveTarget;

import java.util.List;

public class ServiceTvBrowserUpdatePlugin extends Service {
  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }

  private Plugin.Stub getBinder = new Plugin.Stub() {
    private int mVersionCodeCurrent = -1;

    @Override
    public String getVersion() throws RemoteException {
      String version = "UNKONW";

      try {
        PackageInfo pInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
        version = pInfo.versionName;
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }

      return version;
    }

    @Override
    public String getName() throws RemoteException {
      return getString(R.string.app_name);
    }

    @Override
    public String getDescription() throws RemoteException {
      return getString(R.string.service_update_description);
    }

    @Override
    public String getAuthor() throws RemoteException {
      return "René Mach";
    }

    @Override
    public String getLicense() throws RemoteException {
      return getString(R.string.license);
    }

    @Override
    public byte[] getMarkIcon() throws RemoteException {
      return null;
    }

    @Override
    public PluginMenu[] getContextMenuActionsForProgram(Program program) throws RemoteException {
      return null;
    }

    @Override
    public boolean onProgramContextMenuSelected(Program program, PluginMenu pluginMenu) throws RemoteException {
      return false;
    }

    @Override
    public boolean hasPreferences() throws RemoteException {
      return true;
    }

    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {
      final Intent preferences = new Intent(getApplicationContext(), Settings.class);
      preferences.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(preferences);
    }

    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      return null;
    }

    @Override
    public boolean isMarked(long programId) throws RemoteException {
      return false;
    }

    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {

    }

    @Override
    public ReceiveTarget[] getAvailableProgramReceiveTargets() throws RemoteException {
      return new ReceiveTarget[0];
    }

    @Override
    public void receivePrograms(Program[] programs, ReceiveTarget target) throws RemoteException {

    }

    @Override
    public void onActivation(PluginManager pluginManager) throws RemoteException {
      mVersionCodeCurrent = pluginManager.getTvBrowserSettings().getTvbVersionCode();

      if(PrefUtils.getUpdateSearchNext(ServiceTvBrowserUpdatePlugin.this) < System.currentTimeMillis()) {
        final Intent search = new Intent(getApplicationContext(), ServiceCheckAndDownload.class);
        search.setAction(PrefUtils.ACTION_CHECK);
        search.putExtra(PrefUtils.EXTRA_VERSION_CODE_CURRENT, mVersionCodeCurrent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          startForegroundService(search);
        } else {
          startService(search);
        }
      }
      else if(PrefUtils.getUpdateSearchNext(ServiceTvBrowserUpdatePlugin.this) == PrefUtils.VALUE_PREF_DEFAULT && PrefUtils.getUpdateFrequency(ServiceTvBrowserUpdatePlugin.this) != PrefUtils.VALUE_PREF_DEFAULT) {
        PrefUtils.setUpdateSearchNext(ServiceTvBrowserUpdatePlugin.this, System.currentTimeMillis());
      }
    }

    @Override
    public void onDeactivation() throws RemoteException {

    }
  };
}
