// SPDX-License-Identifier: MIT
package com.daimler.sechub.pds.monitoring;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.daimler.sechub.pds.config.PDSServerConfigurationService;
import com.daimler.sechub.pds.job.PDSJobRepository;
import com.daimler.sechub.pds.job.PDSJobStatusState;

public class PDSMonitoringStatusServiceTest {

    private static final String SERVER_ID = "server_id";
    private static final String SERVER_ID_FROM_OTHER_SERVER = "other_server_id";
    private PDSMonitoringStatusService serviceToTest;
    PDSServerConfigurationService serverConfiguratonService;
    PDSHeartBeatRepository heartBeatRepository;
    PDSJobRepository jobRepository;
    
    @Before
    public void before() throws Exception {
        jobRepository = mock(PDSJobRepository.class);
        heartBeatRepository = mock(PDSHeartBeatRepository.class);
        serverConfiguratonService = mock(PDSServerConfigurationService.class);
        
        serviceToTest = new PDSMonitoringStatusService();
        serviceToTest.jobRepository=jobRepository;
        serviceToTest.heartBeatRepository=heartBeatRepository;
        serviceToTest.serverConfiguratonService=serverConfiguratonService;
        
        
        when(serverConfiguratonService.getServerId()).thenReturn(SERVER_ID);
    }

    @Test
    public void monitoringStatus_contains_information_about_two_members_when_two_members_defined() {
        /* prepare */
        PDSMonitoring testData = PDSMonitoringTestDataUtil.createTestMonitoringWith2ClusterMembers();
        List<PDSHeartBeat> heartBeatsForServerId = new ArrayList<>();
        
        // heartbeat1
        PDSHeartBeat heartbeat1 = new PDSHeartBeat();
        heartbeat1.setClusterMemberData(testData.getMembers().get(0).toJSON());
        heartBeatsForServerId.add(heartbeat1);
        // heartbeat2
        PDSHeartBeat heartbeat2 = new PDSHeartBeat();
        heartbeat2.setClusterMemberData(testData.getMembers().get(1).toJSON());
        heartBeatsForServerId.add(heartbeat2);
        
        when(heartBeatRepository.findAllByServerId(SERVER_ID)).thenReturn(heartBeatsForServerId);
        
        /* execute */
        PDSMonitoring monitoring = serviceToTest.getMonitoringStatus();
        
        /* test */
        assertEquals(2,  monitoring.getMembers().size());
        // now we just check that we got member data with given timestamp:
        assertEquals(testData.getMembers().get(0).getHeartBeatTimestamp(),  monitoring.getMembers().get(0).getHeartBeatTimestamp());
        assertEquals(testData.getMembers().get(1).getHeartBeatTimestamp(),  monitoring.getMembers().get(1).getHeartBeatTimestamp());
        
    }
    
    @Test
    public void monitoringStatus_contains_information_about_running_jobs() {
        addJob(SERVER_ID, PDSJobStatusState.RUNNING, 1);
        
        /* execute */
        PDSMonitoring monitoring = serviceToTest.getMonitoringStatus();
        
        /* test */
        assertFound(monitoring, PDSJobStatusState.RUNNING, 1);
    }
    
    @Test
    public void monitoringStatus_contains_information_about_multiple_job_states_but_only_for_configured_server() {
        addJob(SERVER_ID, PDSJobStatusState.CREATED, 1);
        addJob(SERVER_ID, PDSJobStatusState.QUEUED, 2);
        addJob(SERVER_ID, PDSJobStatusState.RUNNING, 3);
        addJob(SERVER_ID, PDSJobStatusState.DONE, 4);
        addJob(SERVER_ID, PDSJobStatusState.FAILED, 5);
        
        addJob(SERVER_ID_FROM_OTHER_SERVER, PDSJobStatusState.QUEUED, 10);
        addJob(SERVER_ID_FROM_OTHER_SERVER, PDSJobStatusState.CREATED, 20);
        addJob(SERVER_ID_FROM_OTHER_SERVER, PDSJobStatusState.RUNNING, 30);
        addJob(SERVER_ID_FROM_OTHER_SERVER, PDSJobStatusState.DONE, 40);
        addJob(SERVER_ID_FROM_OTHER_SERVER, PDSJobStatusState.FAILED, 50);
        
        /* execute */
        PDSMonitoring monitoring = serviceToTest.getMonitoringStatus();
        
        /* test */
        assertFound(monitoring, PDSJobStatusState.CREATED,1);
        assertFound(monitoring, PDSJobStatusState.QUEUED,2);
        assertFound(monitoring, PDSJobStatusState.RUNNING,3);
        assertFound(monitoring, PDSJobStatusState.DONE,4);
        assertFound(monitoring, PDSJobStatusState.FAILED,5);
    }
    
    private void assertFound(PDSMonitoring monitoring, PDSJobStatusState state, long amount) {
        assertEquals(Long.valueOf(amount), monitoring.getJobs().get(state));
    }

    private void addJob(String serverId, PDSJobStatusState state, long amountOfStates) {
        when(jobRepository.countJobsOfServerInState(serverId,state)).thenReturn(amountOfStates);
    }

}
