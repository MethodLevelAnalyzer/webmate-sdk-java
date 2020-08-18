package com.testfabrik.webmate.javasdk.testmgmt;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.testfabrik.webmate.javasdk.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testfabrik.webmate.javasdk.commonutils.HttpHelpers;
import com.testfabrik.webmate.javasdk.jobs.WMValue;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Facade of TestMgmt subsystem.
 */
public class TestMgmtClient {

    private WebmateAPISession session;
    private TestMgmtApiClient apiClient;

    private static final Logger LOG = LoggerFactory.getLogger(TestMgmtClient.class);

    public static class SingleTestRunCreationSpec {

        private Map<String, WMValue> parameterAssignments;

        public SingleTestRunCreationSpec(Map<String, WMValue> parameterAssignments) {
            this.parameterAssignments = parameterAssignments;
        }

        @JsonValue
        public JsonNode asJson() {
            ObjectMapper om = new ObjectMapper();
            ObjectNode paramAssignments = ((ObjectNode)om.valueToTree(parameterAssignments));
            ObjectNode root = om.createObjectNode();
            return root
                    .put("type", "SingleTestRunCreationSpec")
                    .set("assignmentSpec", paramAssignments);
        }
    }

    private static class TestMgmtApiClient extends WebmateApiClient {

        private final static UriTemplate getTestsInProjectTemplate =
                new UriTemplate("/projects/${projectId}/testmgmt/tests");

        private final static UriTemplate getTestTemplate =
                new UriTemplate("/testmgmt/tests/${testId}");

        private final static UriTemplate getTestResultsTemplate =
                new UriTemplate("/testmgmt/testruns/${testRunId}/results");

        private final static UriTemplate createTestExecutionTemplate =
                new UriTemplate("/projects/${projectId}/testexecutions");

        private final static UriTemplate createTestSessionTemplate =
                new UriTemplate("/projects/${projectId}/testsessions");

        private final static UriTemplate startTestExecutionTemplate =
                new UriTemplate("/testmgmt/testexecutions/${testExecutionId}");

        private final static UriTemplate finishTestRunTemplate =
                new UriTemplate("/testmgmt/testruns/${testRunId}/finish");

        public TestMgmtApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment) {
            super(authInfo, environment);
        }

        public TestMgmtApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment, HttpClientBuilder clientBuilder) {
            super(authInfo, environment, clientBuilder);
        }

        private TestExecutionId handleCreateTestExecutionResponse(Optional<HttpResponse> response) {
            if (!response.isPresent()) {
                throw new WebmateApiClientException("Could not create TestExecution. Got no response");
            }

            return HttpHelpers.getObjectFromJsonEntity(response.get(), TestExecutionId.class);
        }

        public TestExecutionId createTestExecution(ProjectId projectId, TestExecutionSpec spec) {
            Optional<HttpResponse> optHttpResponse = sendPOST(createTestExecutionTemplate, ImmutableMap.of(
                    "projectId", projectId.toString()), spec.asJson()).getOptHttpResponse();

            return handleCreateTestExecutionResponse(optHttpResponse);
        }

        public TestSessionId createTestSession(ProjectId projectId, String name) {
            ObjectMapper om = JacksonMapper.getInstance();
            ObjectNode root = om.createObjectNode();
            root.put("name", name);

            Optional<HttpResponse> optHttpResponse = sendPOST(createTestSessionTemplate, ImmutableMap.of(
                    "projectId", projectId.toString()), root).getOptHttpResponse();

            if (!optHttpResponse.isPresent()) {
                throw new WebmateApiClientException("Could not create TestSession. Got no response");
            }

            return HttpHelpers.getObjectFromJsonEntity(optHttpResponse.get(), TestSessionId.class);
        }


        public TestRunId startTestExecution(TestExecutionId id) {
            Optional<HttpResponse> optHttpResponse = sendPOST(startTestExecutionTemplate, ImmutableMap.of(
                    "testExecutionId", id.toString())).getOptHttpResponse();

            if (!optHttpResponse.isPresent()) {
                throw new WebmateApiClientException("Could not start TestExecution. Got no response");
            }

            return HttpHelpers.getObjectFromJsonEntity(optHttpResponse.get(), TestRunId.class);
        }

        public void finishTestRun(TestRunId id, TestRunFinishData data) {
            ObjectMapper mapper = JacksonMapper.getInstance();
            JsonNode dataJson = mapper.valueToTree(data);

            Optional<HttpResponse> optHttpResponse = sendPOST(finishTestRunTemplate, ImmutableMap.of(
                    "testRunId", id.toString()), dataJson).getOptHttpResponse();

            if (!optHttpResponse.isPresent()) {
                throw new WebmateApiClientException("Could not finish TestRun. Got no response");
            }
        }

        public Optional<List<TestInfo>> getTestsInProject(ProjectId id) {
            Optional<HttpResponse> optHttpResponse = sendGET(getTestsInProjectTemplate, ImmutableMap.of("projectId", id.toString())).getOptHttpResponse();
            if (!optHttpResponse.isPresent()) {
                return Optional.absent();
            }

            TestInfo[] testInfos;
            try {
                String testInfosJson = EntityUtils.toString(optHttpResponse.get().getEntity());
                ObjectMapper mapper = JacksonMapper.getInstance();
                testInfos = mapper.readValue(testInfosJson, TestInfo[].class);
            } catch (IOException e) {
                throw new WebmateApiClientException("Error reading data: " + e.getMessage(), e);
            }
            return Optional.of(Arrays.asList(testInfos));
        }

        public Optional<Test> getTest(TestId id) {
            Optional<HttpResponse> optHttpResponse = sendGET(getTestTemplate, ImmutableMap.of("testId", id.toString())).getOptHttpResponse();
            if (!optHttpResponse.isPresent()) {
                return Optional.absent();
            }

            Test test;
            try {
                String testJson = EntityUtils.toString(optHttpResponse.get().getEntity());

                ObjectMapper mapper = JacksonMapper.getInstance();
                test = mapper.readValue(testJson, Test.class);
            } catch (IOException e) {
                throw new WebmateApiClientException("Error reading Test data: " + e.getMessage(), e);
            }
            return Optional.fromNullable(test);
        }

        /**
         * Get TestResults of Test with given id and testrun index.
         * @param id Id of TestRun.
         * @return Optional with list of TestResults or Optional.absent if there was no such Test or TestRun. If there were no TestResults, the result is Optional of empty list.
         */
        public Optional<List<TestResult>> getTestResults(TestRunId id) {
            Optional<HttpResponse> optHttpResponse = sendGET(getTestResultsTemplate, ImmutableMap.of("testRunId", id.toString())).getOptHttpResponse();
            if (!optHttpResponse.isPresent()) {
                return Optional.absent();
            }

            List<TestResult> result;
            try {
                String testJson = EntityUtils.toString(optHttpResponse.get().getEntity());

                ObjectMapper mapper = JacksonMapper.getInstance();
                ApiResult<TestResult[]> testResults = mapper.readValue(testJson, new TypeReference<ApiResult<TestResult[]>>() {});
                result = Arrays.asList(testResults.value);
            } catch (IOException e) {
                throw new WebmateApiClientException("Error reading TestResult data: " + e.getMessage(), e);
            }
            return Optional.fromNullable(result);
        }
    }

    /**
     * Creates a TestMgmtClient based on a WebmateApiSession
     * @param session The WebmateApiSession used by the TestMgmtClient
     */
    public TestMgmtClient(WebmateAPISession session) {
        this.session = session;
        this.apiClient = new TestMgmtApiClient(session.authInfo, session.environment);
    }

    /**
     * Creates a TestMgmtClient based on a WebmateApiSession and a custom HttpClientBuilder.
     * @param session The WebmateApiSession used by the TestMgmtClient
     * @param httpClientBuilder The HttpClientBuilder that is used for building the underlying connection.
     */
    public TestMgmtClient(WebmateAPISession session,  HttpClientBuilder httpClientBuilder) {
        this.session = session;
        this.apiClient = new TestMgmtApiClient(session.authInfo, session.environment, httpClientBuilder);
    }

    /**
     * Retrieve Tests in project with id
     * @param id Id of Project.
     * @return Test
     */
    public Optional<List<TestInfo>> getTestsInProject(ProjectId id) {
        return this.apiClient.getTestsInProject(id);
    }

    /**
     * Retrieve Test with id
     * @param id Id of Test.
     * @return Test
     */
    public Optional<Test> getTest(TestId id) {
        return this.apiClient.getTest(id);
    }

    /**
     * Retrieve list of TestResults for given test and test run.
     * @param id Id of TestRun.
     * @return List of TestResults. Optional.absent if there was no such Test or TestRun.
     */
    public Optional<List<TestResult>> getTestResults(TestRunId id) {
        return this.apiClient.getTestResults(id);
    }


    /**
     * Get Id of TestRun associated with a Selenium session.
     * @param opaqueSeleniumSessionIdString selenium session id
     * @return test run id
     */
    public TestRunId getTestRunIdForSessionId(String opaqueSeleniumSessionIdString) {
        return new TestRunId(UUID.randomUUID());
    }

    public TestRunId startExecution(TestExecutionSpec spec, ProjectId projectId) {
       TestExecutionId executionId = apiClient.createTestExecution(projectId, spec);
       return apiClient.startTestExecution(executionId);
    }

    /**
     * Create and start a TestExecution.
     * @param specBuilder A builder providing the required information for that test type, e.g. {@code Story}
     * @return
     */
    public TestRun startExecution(TestExecutionSpecBuilder specBuilder) {
        if (!session.getProjectId().isPresent()) {
            throw new WebmateApiClientException("A TestExecution must be associated with a project and none is provided or associated with the API session");
        }
        TestExecutionSpec spec = specBuilder.setApiSession(this.session).build();
        return new TestRun(startExecution(spec, session.getProjectId().get()), this.session);
    }

    /**
     * Create new TestSession in current project with given name.
     */
    public TestSession createTestSession(String name) {
        if (!session.getProjectId().isPresent()) {
            throw new WebmateApiClientException("A TestSession must be associated with a project and none is provided or associated with the API session");
        }
        return new TestSession(apiClient.createTestSession(session.getProjectId().get(), name), this.session);
    }

    /**
     * Finish a running TestRun.
     */
    public void finishTestRun(TestRunId id, TestRunEvaluationStatus status) {
       apiClient.finishTestRun(id, new TestRunFinishData(status));
    }

    /**
     * Finish a running TestRun with message.
     */
    public void finishTestRun(TestRunId id, TestRunEvaluationStatus status, String msg) {
        apiClient.finishTestRun(id, new TestRunFinishData(status, msg));
    }

    /**
     * Finish a running TestRun with message and detail information.
     */
    public void finishTestRun(TestRunId id, TestRunEvaluationStatus status, String msg, String detail) {
        apiClient.finishTestRun(id, new TestRunFinishData(status, msg, detail));
    }

}
