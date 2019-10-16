package com.testfabrik.webmate.javasdk.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.testfabrik.webmate.javasdk.*;
import com.testfabrik.webmate.javasdk.browsersession.BrowserSessionId;
import com.testfabrik.webmate.javasdk.testmgmt.*;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Facade of TestMgmt subsystem.
 */
public class ArtifactClient {

    private WebmateAPISession session;
    private ArtifactApiClient apiClient;

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactClient.class);

    private static class ArtifactApiClient extends WebmateApiClient {

        private final static UriTemplate queryArtifactsTemplate =
                new UriTemplate("/projects/${projectId}/artifacts");

        private final static UriTemplate getArtifactTemplate =
                new UriTemplate("/artifact/artifacts/${artifactId}");


        public ArtifactApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment) {
            super(authInfo, environment);
        }

        public ArtifactApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment, HttpClientBuilder clientBuilder) {
            super(authInfo, environment, clientBuilder);
        }

        public Optional<List<ArtifactInfo>> queryArtifacts(ProjectId id, TestRunId associatedTestRun, Set<ArtifactType> artifactTypes) {


            List<NameValuePair> params = Lists.newArrayList();
            params.add(new BasicNameValuePair("testRunId", associatedTestRun.toString()));

            if (!artifactTypes.isEmpty()) {
                StringBuilder typesParam = new StringBuilder();
                for (ArtifactType artifactType : artifactTypes) {
                    String typeName = artifactType.getTypeName();
                    typesParam = typesParam.length() > 0 ? typesParam.append(",").append(typeName) : typesParam.append(typeName);
                }
                params.add(new BasicNameValuePair("types", typesParam.toString()));
            }

            Optional<HttpResponse> optHttpResponse = sendGET(queryArtifactsTemplate, ImmutableMap.of("projectId", id.toString()), params).getOptHttpResponse();
            if (!optHttpResponse.isPresent()) {
                return Optional.absent();
            }

            ArtifactInfo[] artifactInfos;
            try {
                String testInfosJson = EntityUtils.toString(optHttpResponse.get().getEntity());
                ObjectMapper mapper = JacksonMapper.getInstance();
                artifactInfos = mapper.readValue(testInfosJson, ArtifactInfo[].class);
            } catch (IOException e) {
                throw new WebmateApiClientException("Error reading data: " + e.getMessage(), e);
            }
            return Optional.of(Arrays.asList(artifactInfos));
        }

        public Optional<Artifact> getArtifact(ArtifactId id) {
            Optional<HttpResponse> optHttpResponse = sendGET(getArtifactTemplate, ImmutableMap.of("artifactId", id.toString())).getOptHttpResponse();
            if (!optHttpResponse.isPresent()) {
                return Optional.absent();
            }

            Artifact artifact;
            try {
                String artifactJson = EntityUtils.toString(optHttpResponse.get().getEntity());

                ObjectMapper mapper = JacksonMapper.getInstance();
                artifact = mapper.readValue(artifactJson, Artifact.class);
            } catch (IOException e) {
                throw new WebmateApiClientException("Error reading Artifact data: " + e.getMessage(), e);
            }
            return Optional.fromNullable(artifact);
        }
    }

    /**
     * Creates a TestMgmtClient based on a WebmateApiSession
     * @param session The WebmateApiSession used by the TestMgmtClient
     */
    public ArtifactClient(WebmateAPISession session) {
        this.session = session;
        this.apiClient = new ArtifactApiClient(session.authInfo, session.environment);
    }

    /**
     * Creates a TestMgmtClient based on a WebmateApiSession and a custom HttpClientBuilder.
     * @param session The WebmateApiSession used by the TestMgmtClient
     * @param httpClientBuilder The HttpClientBuilder that is used for building the underlying connection.
     */
    public ArtifactClient(WebmateAPISession session, HttpClientBuilder httpClientBuilder) {
        this.session = session;
        this.apiClient = new ArtifactApiClient(session.authInfo, session.environment, httpClientBuilder);
    }

    /**
     * Retrieve Artifact infos associated with testrun in project
     *
     * @param projectId project id
     * @param associatedTestRun testRunId associated with artifacts. TODO other association types
     * @return artifactInfo list
     */
    public List<ArtifactInfo> queryArtifacts(ProjectId projectId, TestRunId associatedTestRun, Set<ArtifactType> types) {
        return this.apiClient.queryArtifacts(projectId, associatedTestRun, types).get();
    }

    /**
     * Retrieve Artifact with id
     * @param id Id of Artifact.
     * @return Artifact
     */
    public Optional<Artifact> getArtifact(ArtifactId id) {
        return this.apiClient.getArtifact(id);
    }
}
