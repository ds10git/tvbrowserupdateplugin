package org.tvbrowser.tvbrowserupdateplugin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public final class NetUtils {
  private static byte[] loadUrl(final String urlString, final AtomicInteger timeoutCount) throws MalformedURLException, IOException {
    BufferedInputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    URLConnection connection = null;
    try {
      connection = new URL(urlString).openConnection();
      setConnectionTimeout(connection,15000);

      if(urlString.toLowerCase(Locale.US).endsWith(".gz")) {
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      }

      in = new BufferedInputStream(connection.getInputStream());

      byte temp[] = new byte[1024];
      int count;

      while ((count = in.read(temp, 0, 1024)) != -1) {
        if(temp != null && count > 0) {
          out.write(temp, 0, count);

          if(timeoutCount != null) {
            timeoutCount.set(0);
          }
        }
      }
    }
    finally {
      if(in != null) {
        try {
          in.close();
        }catch(IOException ioe) {}
      }

      disconnect(connection);
    }

    return out.toByteArray();
  }

  /**
   * Save given URL to filename.
   * <p>
   * @param filename The file to save to.
   * @param urlString The URL to load from.
   * @param timeout The timeout of the download in milliseconds.
   * <p>
   * @return <code>true</code> if the file was downloaded successfully, <code>false</code> otherwise.
   */
  public static boolean saveUrl(final String filename, final String urlString, final int timeout) {
    final AtomicBoolean wasSaved = new AtomicBoolean(false);
    final AtomicInteger count = new AtomicInteger(0);

    new Thread("SAVE URL THREAD") {
      public void run() {
        FileOutputStream fout = null;

        try {
          byte[] byteArr = loadUrl(urlString, count);

          fout = new FileOutputStream(filename);
          fout.getChannel().truncate(0);
          fout.write(byteArr, 0, byteArr.length);
          fout.flush();

          wasSaved.set(true);
        }
        catch(IOException e) {
          Log.d("info5","",e);
        }
        finally {
          if(fout != null) {
            try {
              fout.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }.start();

    Thread wait = new Thread("SAVE URL WAITING THREAD") {
      public void run() {
        while(!wasSaved.get() && count.getAndIncrement() < (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
      }
    };
    wait.start();

    try {
      wait.join();
    } catch (InterruptedException e) {
      Log.d("info51", "INTERRUPTED", e);
    }

    return wasSaved.get();
  }

  /*
   * Copied from http://stackoverflow.com/questions/4818468/how-to-check-if-inputstream-is-gzipped and changed.
   * No license given on page.
   */
  public static InputStream decompressStream(InputStream input) throws IOException {
    PushbackInputStream pb = new PushbackInputStream( input, 2 ); //we need a pushbackstream to look ahead

    byte [] signature = new byte[2];
    int read = pb.read( signature ); //read the signature

    if(read == 2) {
      pb.unread( signature ); //push back the signature to the stream
    }
    else if(read == 1) {
      pb.unread(signature[0]);
    }

    if(signature[ 0 ] == (byte) (GZIPInputStream.GZIP_MAGIC & 0xFF) && signature[ 1 ] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8) ) {//check if matches standard gzip magic number
      return decompressStream(new GZIPInputStream(pb));
    }
    else {
      return pb;
    }
  }

  public static boolean isConnectedToServer(final String url, final int timeout) {
    final AtomicBoolean isConnected = new AtomicBoolean(false);

    new Thread("NETWORK CONNECTION CHECK THREAD") {
      public void run() {
        URLConnection connection = null;
        try {
          URL myUrl = new URL(url);

          connection = myUrl.openConnection();
          setConnectionTimeout(connection,timeout);

          HttpURLConnection httpConnection = (HttpURLConnection)connection;

          if(httpConnection != null) {
            int responseCode = httpConnection.getResponseCode();

            isConnected.set(responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpsURLConnection.HTTP_SEE_OTHER);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        finally  {
          disconnect(connection);
        }
      }
    }.start();

    Thread check = new Thread("WAITING FOR NETWORK CONNECTION THREAD") {
      @Override
      public void run() {
        int count = 0;
        while(!isConnected.get() && count++ <= (timeout / 100)) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
      }
    };
    check.start();

    try {
      check.join();
    } catch (InterruptedException e) {}

    return isConnected.get();
  }

  public static void setConnectionTimeoutDefault(URLConnection connection) {
    setConnectionTimeout(connection, 10000);
  }

  public static void setConnectionTimeout(URLConnection connection, int timeout) {
    connection.setReadTimeout(timeout);
    connection.setConnectTimeout(timeout);
  }

  /**
   * Disconnects the given connection and releases or reuses associated resources.
   * Calls are <code>null</code>-safe and idempotent.
   * <p/>
   * Note: the underlying connection must be inherited from {@link HttpURLConnection}.
   *
   * @param connection the connection to release.
   * @see HttpURLConnection#disconnect()
   */
  public static void disconnect(final URLConnection connection) {
    if (connection instanceof HttpURLConnection) {
      try {
        ((HttpURLConnection) connection).disconnect();
      } catch (final Exception ignored) {
        // intentionally ignored
      }
    }
  }

  public static File getDownloadDirectory(Context context) {
    File parent = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

    if(parent == null || !parent.isDirectory()) {
      parent = context.getDir(Environment.DIRECTORY_DOWNLOADS, Context.MODE_PRIVATE);
    }

    return parent;
  }

  public static boolean isOnline(final Context context) {
    boolean result = false;

    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();

    if(netInfo != null && netInfo.isConnectedOrConnecting()) {
      result = true;
    }

    return result;
  }
}
