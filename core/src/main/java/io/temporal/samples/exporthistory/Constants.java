package io.temporal.samples.exporthistory;

public final class Constants {

  public static final String QUERY = "CloseTime<=\"2025-09-30T19:43:00.000Z\"";
  public static final String ENDPOINT = "<your-namespace>.<your-account>.tmprl.cloud:7233";
  public static final String NAMESPACE = "<your-namespace>.<your-account>";
  public static final String CLIENT_CERT_PATH = "/Users/grantsmith/temporal-certs/client.pem";
  public static final String CLIENT_KEY_PATH = "/Users/grantsmith/temporal-certs/client.key";
  public static final String FILE_PATH =
      "./src/main/java/io/temporal/samples/exporthistory/workflow_history.proto";
  public static final String INPUT_FILE_NAME_FROM_CLOUD_EXPORT =
      "./src/test/java/io/temporal/samples/exporthistory/example-from-cloud.proto";
}
