public class Logger {

  private Long lastUploaded = 0l;

  public void logInfo(String message) {
    System.out.println(message);
  }

  public void logDebug(String message) {
    System.out.println(message);
  }

  public void logWarn(String message) {
    System.out.println(message);
  }

  public void logUploadProgress(Long uploaded, Long contentLength) {
    if (uploaded - lastUploaded > 200000) {
      logDebug("Uploaded " + uploaded + "/" + contentLength);
      lastUploaded = uploaded;
    }
  }

  public Boolean isUploadProgressEnabled() {
    return true;
  }
}
