# Export History Sample

This sample shows how to get the execution history for some workflows in a namespace.

It can be used in situations in which the workflow export feature was turned on, and it
is desireable to get the history of the workflows that existed before the time it was turned
on.

It reads workflow executions from a cloud namespace and writes them to
`./src/main/java/io/temporal/samples/exporthistory/workflow_history.proto`.

## Instructions

1. In Constants.java, update the connection config (namespace, endpoint, file paths for the certs, etc), and
   modify the query to suit your needs.
2. Run the following command to run the code
   `./gradlew -q execute -PmainClass=io.temporal.samples.exporthistory.ExportCloudToProto`

## Caveats and Considerations

- Rate limits were not considered when writing this sample.
- Internal/system workflows (for example, schedules) will differ.
- If comparing to the cloud export feature, search attributes will differ. The cloud export
  feature exports the internal search attribute names, but this script retrieves the user facing names.
