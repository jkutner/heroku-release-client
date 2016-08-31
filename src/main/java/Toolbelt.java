import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Toolbelt {
  public static String getApiToken() throws IOException, InterruptedException {
    return readNetrcFile().get("api.heroku.com").get("password");
  }

  private static Map<String,Map<String,String>> readNetrcFile() throws IOException {
    String homeDir = System.getProperty("user.home");
    String netrcFilename = isWindows() ? "_netrc" : ".netrc";
    File netrcFile = new File(new File(homeDir), netrcFilename);

    if (!netrcFile.exists()) {
      throw new FileNotFoundException(netrcFile.toString());
    }

    Map<String,Map<String,String>> netrcMap = new HashMap<>();

    String machine = null;
    Map<String,String> entry = new HashMap<>();
    for (String line : FileUtils.readLines(netrcFile)) {
      if (line != null && !line.trim().isEmpty()) {
        if (line.startsWith("machine")) {
          if (null != machine) {
            netrcMap.put(machine, entry);
            entry = new HashMap<>();
          }
          machine = line.trim().split(" ")[1];
        } else {
          String[] keyValue = line.trim().split(" ");
          entry.put(keyValue[0], keyValue[1]);
        }
      }
    }

    if (null != machine) {
      netrcMap.put(machine, entry);
    }

    return netrcMap;
  }

  private static Boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}