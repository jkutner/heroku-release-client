import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

class Release {

  public static final String BASE_URL = "https://api.heroku.com";

  protected String blobUrl;

  protected String appName;

  protected Map<String,String> headers;

  private String slugId;

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      System.out.println("ERROR: Must provide appName as first arg! For example:");
      System.out.println("");
      System.out.println("       java -cp target/classes:target/dependency/* Release sushi-123 path/to/app.tar.gz");
      System.out.println("");
      System.exit(1);
    } else if (args.length < 2) {
      System.out.println("ERROR: Must provide slugFile as second arg! For example:");
      System.out.println("");
      System.out.println("       java -cp target/classes:target/dependency/* Release sushi-123 path/to/app.tar.gz");
      System.out.println("");
      System.exit(1);
    }

    System.out.println("Releasing " + args[1] + " to " + args[0]);
    Release r = new Release(args[0], getEncodedApiKey());

    System.out.println("---> Creating release...");
    r.create();

    System.out.println("---> Uploading slug...");
    r.upload(new File(args[1]), new Logger());

    System.out.println("---> Releasing slug...");
    Map resp = r.release();

    for (Object key : resp.keySet()) {
      System.out.println("         " + key + "=" + resp.get(key));
    }
  }

  public Release(String appName, String encodedApiKey) {
    this.appName = appName;
    headers = new HashMap<>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");
  }

  public Map create() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/slugs";

    String createJson = "{\"process_types\":{}}";

    Map slugResponse = RestClient.post(urlStr, createJson, headers);

    Map blobJson = (Map)slugResponse.get("blob");
    blobUrl = (String)blobJson.get("url");

    slugId = (String)slugResponse.get("id");

    return slugResponse;
  }

  public void upload(File slugFile, Logger listener) throws IOException, InterruptedException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    RestClient.put(blobUrl, slugFile, listener);
  }

  public Map release() throws IOException {
    if (slugId == null) {
      throw new IllegalStateException("Slug must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/releases";

    String data = "{\"slug\":\"" + slugId + "\"}";

    return RestClient.post(urlStr, data, headers);
  }

  public static String getEncodedApiKey() throws IOException {
    String apiKey = System.getenv("HEROKU_API_TOKEN");
    if (null == apiKey || apiKey.isEmpty()) {
      try {
        apiKey = Toolbelt.getApiToken();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (apiKey == null || apiKey.isEmpty()) {
      throw new RuntimeException("Could not get API key! Please install the toolbelt and login with `heroku login` or set the HEROKU_API_KEY environment variable.");
    }

    return new BASE64Encoder().encode((":" + apiKey).getBytes());
  }
}
