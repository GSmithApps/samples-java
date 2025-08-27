package io.temporal.samples.exporthistory;

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ExportCloudToProto {

  public static void main(String[] args) {

    // #1 change these
    String endpoint = "grant-test-mtls.a2dd6.tmprl.cloud:7233";
    String namespace = "grant-test-mtls.a2dd6";
    String clientCertPath = "/Users/grantsmith/temporal-certs/client.pem";
    String clientKeyPath = "/Users/grantsmith/temporal-certs/client.key";

    InputStream clientCert = null;
    InputStream clientKey = null;
    SslContext sslContext = null;

    try {
      clientCert = new FileInputStream(clientCertPath);
      clientKey = new FileInputStream(clientKeyPath);
      sslContext = SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build();
    } catch (Exception e) {
      throw new RuntimeException("Error resolving file paths for mTLS: ", e);
    }
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setSslContext(sslContext)
                .setTarget(endpoint)
                .build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    List<io.temporal.api.export.v1.WorkflowExecution> allExecutions =
        client
            // #2 change your query
            .listExecutions("CloseTime>=\"2025-09-30T19:43:00.000Z\"")
            .map(
                executionMetadata -> {
                  var exec = executionMetadata.getExecution();
                  var wfid = exec.getWorkflowId();
                  var rid = exec.getRunId();
                  WorkflowExecutionHistory history = client.fetchHistory(wfid, rid);
                  io.temporal.api.history.v1.History protoHistory = history.getHistory();

                  return io.temporal.api.export.v1.WorkflowExecution.newBuilder()
                      .setHistory(protoHistory)
                      .build();
                })
            .collect(Collectors.toList());

    io.temporal.api.export.v1.WorkflowExecutions executions =
        io.temporal.api.export.v1.WorkflowExecutions.newBuilder()
            .addAllItems(allExecutions)
            .build();

    byte[] binary = executions.toByteArray();

    FileOutputStream fos = null;
    try {

      String filePath = "./src/main/java/io/temporal/samples/exporthistory/workflow_history.proto";

      fos = new FileOutputStream(filePath);

      fos.write(binary);

      System.out.println("Workflow history saved to: " + filePath);

    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
      } catch (IOException e) {
        System.err.println("Error closing the file stream: " + e.getMessage());
        e.printStackTrace();
      }
    }

    System.exit(0);
  }
}
