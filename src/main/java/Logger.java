public class Logger {

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
    logDebug("Uploaded " + uploaded + "/" + contentLength);
  }

  public Boolean isUploadProgressEnabled() {
    return true;
  }
}
