package io.temporal.samples.exporthistory;

import static org.junit.Assert.assertTrue;

import com.google.protobuf.util.Timestamps;
import io.temporal.api.export.v1.WorkflowExecution;
import io.temporal.api.export.v1.WorkflowExecutions;
import io.temporal.api.history.v1.History;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.history.v1.StartChildWorkflowExecutionInitiatedEventAttributes;
import io.temporal.api.history.v1.UpsertWorkflowSearchAttributesEventAttributes;
import io.temporal.api.history.v1.WorkflowExecutionStartedEventAttributes;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Test;

public class ExportHistoryTest {

  @Test
  public void testExportEquality() {

    WorkflowExecutions executionsFromScript;
    String inputFileNameFromScript =
        "./src/main/java/io/temporal/samples/exporthistory/workflow_history.proto";
    try (FileInputStream fis = new FileInputStream(inputFileNameFromScript)) {
      executionsFromScript = WorkflowExecutions.parseFrom(fis);
    } catch (IOException e) {
      throw new RuntimeException("Error reading proto file: ", e);
    }

    WorkflowExecutions executionsFromCloudExport;
    String inputFileNameFromCloudExport =
        "./src/test/java/io/temporal/samples/exporthistory/example-from-cloud.proto";
    try (FileInputStream fis = new FileInputStream(inputFileNameFromCloudExport)) {
      executionsFromCloudExport = WorkflowExecutions.parseFrom(fis);
    } catch (IOException e) {
      throw new RuntimeException("Error reading proto file: ", e);
    }

    assertTrue(equalIgnoringSearchAttributes(executionsFromScript, executionsFromCloudExport));
  }

  /** Compare two WorkflowExecutions while ignoring all search_attributes. */
  public static boolean equalIgnoringSearchAttributes(WorkflowExecutions a, WorkflowExecutions b) {
    var aItems = new ArrayList<>(a.getItemsList());
    aItems.sort(
        (x, y) ->
            Timestamps.compare(
                x.getHistory().getEvents(0).getEventTime(),
                y.getHistory().getEvents(0).getEventTime()));
    var bItems = new ArrayList<>(b.getItemsList());
    bItems.sort(
        (x, y) ->
            Timestamps.compare(
                x.getHistory().getEvents(0).getEventTime(),
                y.getHistory().getEvents(0).getEventTime()));
    if (aItems.size() != bItems.size()) return false;

    for (int i = 0; i < aItems.size(); i++) {
      WorkflowExecution sa = stripSearchAttrs(aItems.get(i));
      WorkflowExecution sb = stripSearchAttrs(bItems.get(i));
      if (!sa.equals(sb)) {
        return false;
      }
    }
    return true;
  }

  private static WorkflowExecution stripSearchAttrs(WorkflowExecution exec) {
    if (!exec.hasHistory()) return exec;
    History sanitized = stripSearchAttrs(exec.getHistory());
    return exec.toBuilder().setHistory(sanitized).build();
  }

  /** Strip search_attributes from a History (per-event). */
  private static History stripSearchAttrs(History h) {
    History.Builder hb = h.toBuilder();
    for (int i = 0; i < hb.getEventsCount(); i++) {
      hb.setEvents(i, stripSearchAttrs(hb.getEvents(i)));
    }
    return hb.build();
  }

  /** Strip search_attributes from any event attributes that carry them. */
  private static HistoryEvent stripSearchAttrs(HistoryEvent e) {
    HistoryEvent.Builder eb = e.toBuilder();

    // 1) WorkflowExecutionStartedEventAttributes
    if (e.hasWorkflowExecutionStartedEventAttributes()) {
      WorkflowExecutionStartedEventAttributes.Builder a =
          e.getWorkflowExecutionStartedEventAttributes().toBuilder();
      a.clearSearchAttributes();
      eb.setWorkflowExecutionStartedEventAttributes(a);
    }

    // 2) UpsertWorkflowSearchAttributesEventAttributes
    if (e.hasUpsertWorkflowSearchAttributesEventAttributes()) {
      UpsertWorkflowSearchAttributesEventAttributes.Builder up =
          e.getUpsertWorkflowSearchAttributesEventAttributes().toBuilder();
      up.clearSearchAttributes();
      eb.setUpsertWorkflowSearchAttributesEventAttributes(up);
    }

    // 3) StartChildWorkflowExecutionInitiatedEventAttributes (often carries search_attributes)
    if (e.hasStartChildWorkflowExecutionInitiatedEventAttributes()) {
      StartChildWorkflowExecutionInitiatedEventAttributes.Builder child =
          e.getStartChildWorkflowExecutionInitiatedEventAttributes().toBuilder();
      // This field is optional in some server versions; clear if present.
      child.clearSearchAttributes();
      eb.setStartChildWorkflowExecutionInitiatedEventAttributes(child);
    }

    return eb.build();
  }
}
