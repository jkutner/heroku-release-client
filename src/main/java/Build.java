import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Joe Kutner on 9/6/16.
 *         Twitter: @codefinger
 */
public class Build {
  public static final String BASE_URL = "https://api.heroku.com";

  protected String blobUrl;

  private String blobGetUrl;

  protected String appName;

  protected Map<String,String> headers;

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      System.out.println("ERROR: Must provide appName as first arg! For example:");
      System.out.println("");
      System.out.println("       java -cp target/classes:target/dependency/* Build sushi-123 path/to/app.tar.gz");
      System.out.println("");
      System.exit(1);
    } else if (args.length < 2) {
      System.out.println("ERROR: Must provide tarball as second arg! For example:");
      System.out.println("");
      System.out.println("       java -cp target/classes:target/dependency/* Build sushi-123 path/to/app.tar.gz");
      System.out.println("");
      System.exit(1);
    }

    System.out.println("Building " + args[1] + " to " + args[0]);
    Build b = new Build(args[0], Release.getEncodedApiKey());

    System.out.println("---> Creating upload source...");
    b.createSource();

    System.out.println("---> Uploading tarball...");
    final Logger logger = new Logger();
    b.upload(new File(args[1]), logger);

    System.out.println("---> Building...");
    Map buildInfo = b.build(new RestClient.OutputLogger() {
      @Override
      public void log(String line) {
        logger.logInfo("remote: " + line);
      }
    });

    if (!"succeeded".equals(buildInfo.get("status"))) {
      Thread.sleep(4000);
      Map secondAttemptBuildInfo = b.getBuildInfo((String) buildInfo.get("id"));

      if (!"succeeded".equals(secondAttemptBuildInfo.get("status"))) {
        throw new RuntimeException("The build failed");
      }
    }

    System.out.println("---> Done.");
    System.exit(0);
  }

  public Build(String appName, String encodedApiKey) {
    this.appName = appName;
    headers = new HashMap<>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");
  }

  public Map createSource() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/sources";
    Map sourceResponse = RestClient.post(urlStr, headers);

    Map blobJson = (Map)sourceResponse.get("source_blob");
    blobUrl = (String)blobJson.get("put_url");
    blobGetUrl = (String)blobJson.get("get_url");

    return sourceResponse;
  }

  public void upload(File tarball, Logger listener) throws IOException, InterruptedException {
    if (blobUrl == null) {
      throw new IllegalStateException("Source must be created before uploading!");
    }

    RestClient.put(blobUrl, tarball, listener);
  }

  public Map build(RestClient.OutputLogger logger) throws IOException, InterruptedException {
    if (blobGetUrl == null) {
      throw new IllegalStateException("Source must be uploaded before building!");
    }

    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/builds";

    String data = "{"+
        "\"buildpacks\": [{\"url\": \"https://github.com/jkutner/heroku-buildpack-recompose\"}], " +
        "\"source_blob\":{\"url\":\"" + StringEscapeUtils.escapeJson(blobGetUrl) + "\", " +
        "\"version\":\"" + StringEscapeUtils.escapeJson(UUID.randomUUID().toString()) + "\"}}";

    Map buildResponse = RestClient.post(urlStr, data, headers);

    String outputUrl = (String)buildResponse.get("output_stream_url");

    if (outputUrl != null) {
      RestClient.get(outputUrl, headers, logger);
    }
    Thread.sleep(2000);

    return getBuildInfo((String)buildResponse.get("id"));
  }

  public Map getBuildInfo(String buildId) throws IOException {
    String buildStatusUrlStr = BASE_URL + "/apps/" + appName + "/builds/" + buildId;
    return RestClient.get(buildStatusUrlStr, headers);
  }
}

