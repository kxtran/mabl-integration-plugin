package com.mabl.integration.jenkins;

import com.mabl.integration.jenkins.domain.CreateDeploymentResult;
import com.mabl.integration.jenkins.domain.ExecutionResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.io.PrintStream;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test runner
 */
public class MablStepDeploymentRunnerTest {


    private static final long TEST_TIMEOUT_SECONDS = 10;

    private final String environmentId = "foo-env-e";
    private final String applicationId = "foo-app-a";
    private final String eventId = "foo-event-id";

    private MablRestApiClient client;
    private PrintStream outputStream;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(TEST_TIMEOUT_SECONDS);

    @Before
    public void setup() {
        client = mock(MablRestApiClient.class);
        outputStream = mock(PrintStream.class);
    }

    @Test
    public void runTestsHappyPath() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenReturn(new CreateDeploymentResult(eventId));

        when(client.getExecutionResults(eventId))
                .thenReturn(createExecutionResult("succeeded", true));

        assertTrue("successful outcome expected", runner.call());

        verify(client).close();
    }

    @Test
    public void runTestsHappyPathManyPollings() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenReturn(new CreateDeploymentResult(eventId));

        when(client.getExecutionResults(eventId))
                .thenReturn(createExecutionResult("queued", true))
                .thenReturn(createExecutionResult("pre-execution", true))
                .thenReturn(createExecutionResult("scheduling", true))
                .thenReturn(createExecutionResult("scheduled", true))
                .thenReturn(createExecutionResult("running", true))
                .thenReturn(createExecutionResult("post-execution", true))
                .thenReturn(createExecutionResult("completed", true));

        assertTrue("successful outcome expected", runner.call());

        verify(client).close();
    }

    @Test
    public void runTestsMablErrorOnCreateDeployment() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenThrow(new MablSystemError("mabl error"));

        assertFalse("failure outcome expected", runner.call());

        verify(client).close();
    }

    @Test
    public void runTestsMablErrorDeploymentResultsNotFound() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenThrow(new MablSystemError("mabl error"));

        when(client.getExecutionResults(eventId)).thenReturn(null);

        assertFalse("failure outcome expected", runner.call());

        verify(client).close();
    }

    @Test
    public void runTestsPlanFailure() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenReturn(new CreateDeploymentResult(eventId));

        when(client.getExecutionResults(eventId))
                .thenReturn(createExecutionResult("failed", false));

        assertFalse("failure outcome expected", runner.call());

        verify(client).close();
    }

    @Test
    public void continueOnMablError() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                false,
                true
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenThrow(new MablSystemError("mabl error"));

        assertTrue("failure override expected", runner.call());

        verify(client).close();
    }

    @Test
    public void continueOnPlanFailure() throws IOException, MablSystemError {
        MablStepDeploymentRunner runner = new MablStepDeploymentRunner(
                client,
                outputStream,
                environmentId,
                applicationId,
                true,
                false
        );

        when(client.createDeploymentEvent(environmentId, applicationId))
                .thenReturn(new CreateDeploymentResult(eventId));

        when(client.getExecutionResults(eventId))
                .thenReturn(createExecutionResult("queued", true))
                .thenReturn(createExecutionResult("terminated", false));

        assertTrue("failure override expected", runner.call());

        verify(client).close();
    }

    private ExecutionResult createExecutionResult(
            final String status,
            final boolean success
    ) {
        return new ExecutionResult(
                singletonList(
                        new ExecutionResult.ExecutionSummary
                                (status, "all is well",
                                        success, 0L, 0L,
                                        null, null)));
    }
}
