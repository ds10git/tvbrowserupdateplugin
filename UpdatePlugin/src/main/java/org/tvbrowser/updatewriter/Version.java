package org.tvbrowser.updatewriter;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Version {
  public static final int MIN_SDK_DEFAULT = 14;
  
  private static final String URL_BETA = "https://www.tvbrowser-app.de/download/test";
  private static final String URL_STABLE = "https://github.com/ds10git/tvbrowserandroid/releases/download/release_";
  
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String SEPARATOR = ";";
  
  private int versionCode;
  private int minSdkVersion;
  private String versionName;
  private String date;
  private String signature;
  private String url;
  
  public Version() {}
  
  public Version(String encoded) throws NumberFormatException {
    String[] parts = encoded.split(SEPARATOR);
    
    if(parts.length == 4) {
      minSdkVersion = MIN_SDK_DEFAULT;
      versionCode = Integer.parseInt(parts[0]);
      versionName = parts[1];
      date = parts[2];
      url = parts[3];
    }
    else if(parts.length == 6) {
      minSdkVersion = Integer.parseInt(parts[4]);
      versionCode = Integer.parseInt(parts[0]);
      versionName = parts[1];
      date = parts[2];
      url = parts[3];
      signature = parts[5];
    }
    System.out.println(date);
  }
  
  public int getMinSdkVersion() {
    return minSdkVersion;
  }
  
  public String getVersionName() {
    return versionName;
  }
  
  public void setSignature(String signature) {
    this.signature = signature;
  }
  
  public boolean isBeta() {
    return versionName != null && versionName.toLowerCase().contains("beta");
  }
  
  public boolean isValid() {
    return versionCode > 0 && minSdkVersion > 0 && versionName != null;
  }
  
  public void setVersionCode(int versionCode) {
    this.versionCode = versionCode;
  }
  
  public void setMinSdkVersion(int minSdkVersion) {
    this.minSdkVersion = minSdkVersion;
  }
  
  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }
  
  public String getUrl() {
    return url;
  }
  
  @Override
  public String toString() {
    if(date == null) {
      date = DATE_FORMAT.format(new Date());
    }
    
    StringBuilder b = new StringBuilder();
    
    b.append(versionCode).append(SEPARATOR).append(versionName).append(SEPARATOR).append(date).append(SEPARATOR);
    
    if(versionName != null && versionName.toLowerCase().contains("beta")) {
      b.append(URL_BETA);
    }
    else {
      b.append(URL_STABLE).append(versionName);
    }
    
    b.append("/TV-Browser-v").append(versionName).append(".apk");
    b.append(SEPARATOR).append(minSdkVersion); 
    
    if(signature != null) {
      b.append(SEPARATOR).append(signature);
    }
    
    return b.toString();
  }
}