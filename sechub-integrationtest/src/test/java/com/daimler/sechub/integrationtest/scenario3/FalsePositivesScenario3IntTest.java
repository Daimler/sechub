// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario3;

import static com.daimler.sechub.integrationtest.api.AssertSecHubReport.*;
import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static com.daimler.sechub.integrationtest.scenario3.Scenario3.*;
import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.daimler.sechub.integrationtest.api.AsUser.ProjectFalsePositivesDefinition;
import com.daimler.sechub.integrationtest.api.IntegrationTestJSONLocation;
import com.daimler.sechub.integrationtest.api.IntegrationTestSetup;
import com.daimler.sechub.integrationtest.api.TestProject;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.sharedkernel.type.TrafficLight;
public class FalsePositivesScenario3IntTest {

    @Rule
    public IntegrationTestSetup setup = IntegrationTestSetup.forScenario(Scenario3.class);

    @Rule
    public Timeout timeOut = Timeout.seconds(600);

    TestProject project = PROJECT_1;

    @Test
    public void mark_falsepositives_of_only_existing_medium_will_result_in_report_without_defined__And_trafficlight_changes_from_yellow_to_green() throws Exception {
        /* @formatter:off */
        /***********/
        /* prepare */
        /***********/
        IntegrationTestJSONLocation location = IntegrationTestJSONLocation.CLIENT_JSON_SOURCESCAN_YELLOW;
        ExecutionResult result = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        assertSecHubReport(result).
            containsFinding(1, "Absolute Path Traversal").
            hasTrafficLight(TrafficLight.YELLOW);

        UUID jobUUID = result.getSechubJobUUD();

        /***********/
        /* execute */
        /***********/
        as(USER_1).startFalsePositiveDefinition(project).add(1, jobUUID).markAsFalsePositive();

        /********/
        /* test */
        /********/
        ExecutionResult result2 = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        assertSecHubReport(result2).
            containsNotFinding(1, "Absolute Path Traversal").
            hasTrafficLight(TrafficLight.GREEN);

        /* @formatter:on */
    }
    
    @Test
    public void unmark_falsepositives_of_only_existing_medium_will_result_in_report_without_defined__And_trafficlight_changes_from_gren_to_yellow() throws Exception {
        /* @formatter:off */
        /***********/
        /* prepare */
        /***********/
        IntegrationTestJSONLocation location = IntegrationTestJSONLocation.CLIENT_JSON_SOURCESCAN_YELLOW;
        ExecutionResult result = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        UUID jobUUID = result.getSechubJobUUD();

        as(USER_1).startFalsePositiveDefinition(project).add(1, jobUUID).markAsFalsePositive();

        // create scan + fetch report again (check filtering of false positive works as a precondition */
        ExecutionResult result2 = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        assertSecHubReport(result2).
            containsNotFinding(1, "Absolute Path Traversal").
            hasTrafficLight(TrafficLight.GREEN);
        
        /***********/
        /* execute */
        /***********/
        as(USER_1).startFalsePositiveDefinition(project).add(1, jobUUID).unmarkFalsePositive();

        /********/
        /* test */
        /********/

        // create scan + fetch report again
        ExecutionResult result3 = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        assertSecHubReport(result3).
            containsFinding(1, "Absolute Path Traversal").
            hasTrafficLight(TrafficLight.YELLOW);

        /* @formatter:on */
    }

    @Test
    public void fetch_fp_config_when_one_entry_added() throws Exception {
        /* @formatter:off */
        /***********/
        /* prepare */
        /***********/
        IntegrationTestJSONLocation location = IntegrationTestJSONLocation.CLIENT_JSON_SOURCESCAN_YELLOW;
        ExecutionResult result = as(USER_1).withSecHubClient().startSynchronScanFor(project, location);
        assertSecHubReport(result).
            containsFinding(1, "Absolute Path Traversal").
            hasTrafficLight(TrafficLight.YELLOW);

        UUID jobUUID = result.getSechubJobUUD();

        /***********/
        /* execute */
        /***********/
        as(USER_1).startFalsePositiveDefinition(project).add(1, jobUUID).markAsFalsePositive();

        /********/
        /* test */
        /********/
        ProjectFalsePositivesDefinition configuration = as(USER_1).getFalsePositiveConfigurationOfProject(project);
        assertTrue(configuration.isContaining(1, jobUUID));
        
        /* @formatter:on */
    }
    

}
