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

    InputStream clientCert = null;
    InputStream clientKey = null;
    SslContext sslContext = null;

    try {
      clientCert = new FileInputStream(Constants.CLIENT_CERT_PATH);
      clientKey = new FileInputStream(Constants.CLIENT_KEY_PATH);
      sslContext = SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build();
    } catch (Exception e) {
      throw new RuntimeException("Error resolving file paths for mTLS: ", e);
    }
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setSslContext(sslContext)
                .setTarget(Constants.ENDPOINT)
                .build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(Constants.NAMESPACE).build());

    List<io.temporal.api.export.v1.WorkflowExecution> allExecutions =
        client
            // #2 change your query
            .listExecutions(Constants.QUERY)
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

      fos = new FileOutputStream(Constants.FILE_PATH);

      fos.write(binary);

      System.out.println("Workflow history saved to: " + Constants.FILE_PATH);

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
