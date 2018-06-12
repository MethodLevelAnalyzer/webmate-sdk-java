package com.testfabrik.webmate.javasdk.devices;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.testfabrik.webmate.javasdk.*;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Facade to the webmate Device subsystem.
 */
public class DeviceClient {

    private WebmateAPISession session;
    private DeviceApiClient apiClient;

    private static final Logger LOG = LoggerFactory.getLogger(DeviceClient.class);

    private static class DeviceApiClient extends WebmateApiClient {

        private final static UriTemplate getDeviceIdsForProjectTemplate = new UriTemplate("/project/${projectId}/device/devices");

        private final static UriTemplate synchronizeDeviceTemplate = new UriTemplate("/device/devices/${deviceId}/sync");

        private final static UriTemplate releaseDeviceTemplate = new UriTemplate("/device/devices/${deviceId}");



        public DeviceApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment) {
            super(authInfo, environment);
        }

        public DeviceApiClient(WebmateAuthInfo authInfo, WebmateEnvironment environment, HttpClientBuilder httpClientBuilder) {
            super (authInfo, environment, httpClientBuilder);
        }

        public Collection<DeviceId> getDeviceIdsForProject(ProjectId projectId) {
            ApiResponse response = sendGET(getDeviceIdsForProjectTemplate, ImmutableMap.of("projectId", projectId.toString()));

            Optional<HttpResponse> optHttpResponse = response.getOptHttpResponse();

            if (!optHttpResponse.isPresent()) {
                throw new WebmateApiClientException("Could not get device list. Got no response");
            }

            List<DeviceId> deviceIds = new ArrayList<>();
            try {
                String deviceIdsJson = EntityUtils.toString(optHttpResponse.get().getEntity());
                ObjectMapper mapper = new ObjectMapper();
                List<String> deviceIdList = mapper.readValue(deviceIdsJson, new TypeReference<List<String>>(){});
                for (String deviceIdStr : deviceIdList) {
                   deviceIds.add(new DeviceId(UUID.fromString(deviceIdStr)));
                }
            } catch (IOException e) {
                throw new WebmateApiClientException("Could not retrieve device list", e);
            }
            return deviceIds;
        }

        public void synchronizeDevice(DeviceId deviceId) {
            sendPOST(synchronizeDeviceTemplate, ImmutableMap.of("deviceId", deviceId.toString()));
        }

        public void releaseDevice(DeviceId deviceId) {
            sendDELETE(releaseDeviceTemplate, ImmutableMap.of("deviceId", deviceId.toString()));
        }

    }

    /**
     * Creates a DeviceClient based on a WebmateApiSession.
     * @param session The WebmateApiSession the DeviceClient is supposed to be based on.
     */
    public DeviceClient(WebmateAPISession session) {
        this.session = session;
        this.apiClient = new DeviceApiClient(session.authInfo, session.environment);
    }

    /**
     * Creates a DeviceClient based on a WebmateApiSession and a custom HttpClientBuilder.
     * @param session The WebmateApiSession the DeviceClient is supposed to be based on.
     * @param httpClientBuilder The HttpClientBuilder that is used for building the underlying connection.
     */
    public DeviceClient(WebmateAPISession session,  HttpClientBuilder httpClientBuilder) {
        this.session = session;
        this.apiClient = new DeviceApiClient(session.authInfo, session.environment, httpClientBuilder);
    }

    /**
     * Get all Device ids for a project.
     * @param projectId Id of Project (as found in dashboard), for which devices should be retrieved.
     *
     */
    public Collection<DeviceId> getDeviceIdsForProject(ProjectId projectId) {
        return this.apiClient.getDeviceIdsForProject(projectId);
    }

    /**
     * Synchronize webmate with device. (Usually not necessary)
     * @param deviceId DeviceId of device. Can be found in "Details" dialog of an item in webmate device overview.
     */
    public void synchronizeDevice(DeviceId deviceId) {
        this.apiClient.synchronizeDevice(deviceId);
    }

    /**
     * Release device. The device will not be deployed afterwards.
     * @param deviceId DeviceId of device. Can be found in "Details" dialog of an item in webmate device overview.
     */
    public void releaseDevice(DeviceId deviceId) {
        this.apiClient.releaseDevice(deviceId);
    }

}