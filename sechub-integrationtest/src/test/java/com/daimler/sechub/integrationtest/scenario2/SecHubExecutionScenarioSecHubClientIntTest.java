// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario2;

import static com.daimler.sechub.integrationtest.api.AssertExecutionResult.*;
import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static com.daimler.sechub.integrationtest.scenario2.Scenario2.*;
import static java.util.Arrays.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.daimler.sechub.integrationtest.api.IntegrationTestSetup;
import com.daimler.sechub.integrationtest.api.TestProject;
import com.daimler.sechub.integrationtest.api.TestUser;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.sharedkernel.type.TrafficLight;

public class SecHubExecutionScenarioSecHubClientIntTest {

	private static final String GREEN_RESULT_SOURCE_SCAN_IDENTIFIER = "../../../../src";

	@Rule
	public IntegrationTestSetup setup = IntegrationTestSetup.forScenario(Scenario2.class);

	@Rule
	public Timeout timeOut = Timeout.seconds(60 * 5);

	@Test
	public void sechub_client_is_able_to_trigger_sourcescan_asynchronous() {
		/* @formatter:off */

		/* prepare */
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1).
			updateWhiteListForProject(PROJECT_1, asList("https://fscan.intranet.example.org"));

		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute + test */
		as(USER_1).
			withSecHubClient().
				startAsynchronScanFor(PROJECT_1, "sechub-integrationtest-client-infrascan.json").
				assertJobTriggered();

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_trigger_infrascan_asynchronous() {
		/* @formatter:off */

		/* prepare */
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1).
			updateWhiteListForProject(PROJECT_1, asList("https://fscan.intranet.example.org"));

		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute + test */
		as(USER_1).
			withSecHubClient().
			startAsynchronScanFor(PROJECT_1, "sechub-integrationtest-client-sourcescan-green.json").
			assertJobTriggered();

		/* @formatter:on */

	}

	@Test
	public void a_project_having_no_white_list_entries_but_no_problems_can_be_executed_as_codescan_and_results_green() {
		/* @formatter:off */

		/* prepare */
		TestProject project = PROJECT_3;
		TestUser user = USER_1;

		assertProject(project).hasNoWhiteListEntries();

		as(SUPER_ADMIN).
			assignUserToProject(user, project);

		/* execute */
		String jsonConfigFile = "sechub-integrationtest-client-sourcescan-green.json";
		UUID jobUUID =
	    as(user).
			withSecHubClient().
			startAsynchronScanFor(project, jsonConfigFile).
			assertFileUploaded(project).
			assertJobTriggered().
			getJobUUID();

		waitForJobDone(project, jobUUID);

		as(user).
			withSecHubClient().
			startDownloadJobReport(project, jobUUID, jsonConfigFile).
			hasTrafficLight(TrafficLight.GREEN)

			;
		/* @formatter:on */

	}

	@Test
	public void a_project_having_a_whitelist_containing_only_empty_string_but_no_problems_can_be_executed_as_codescan_and_results_green() {
		/* @formatter:off */

		/* prepare */
		TestProject project = PROJECT_3;
		TestUser user = USER_1;

		assertProject(project).hasNoWhiteListEntries();

		List<String> list = new ArrayList<>();
		list.add("");
		as(SUPER_ADMIN).
			updateWhiteListForProject(project, list).
			assignUserToProject(user, project);

		/* execute */
		String jsonConfigFile = "sechub-integrationtest-client-sourcescan-green.json";
		UUID jobUUID =
	    as(user).
			withSecHubClient().
			startAsynchronScanFor(project, jsonConfigFile).
			assertFileUploaded(project).
			assertJobTriggered().
			getJobUUID();

		waitForJobDone(project, jobUUID);

		as(user).
			withSecHubClient().
			startDownloadJobReport(project, jobUUID, jsonConfigFile).
			hasTrafficLight(TrafficLight.GREEN)

			;
		/* @formatter:on */

	}

	@Test
	public void sechub_client_can_execute_a_config_file_which_uses_template_variables_of_environment_entries() {
		/* @formatter:off */

		/* prepare */
		TestProject project = PROJECT_3;
		TestUser user = USER_1;

		assertProject(project).hasNoWhiteListEntries();

		List<String> list = new ArrayList<>();
		list.add("");
		as(SUPER_ADMIN).
			updateWhiteListForProject(project, list).
			assignUserToProject(user, project);

		Map<String, String> envEntries = new LinkedHashMap<>();
		envEntries.put("SHTEST_VERSION", "1.0");
		envEntries.put("SHTEST_FOLDERS1", GREEN_RESULT_SOURCE_SCAN_IDENTIFIER);

		/* execute */
		String jsonConfigFile = "sechub-integrationtest-client-sourcescan-generic-template.json";
		UUID jobUUID =
	    as(user).
			withSecHubClient().
			startAsynchronScanFor(project, jsonConfigFile,envEntries).
			assertFileUploaded(project).
			assertJobTriggered().
			getJobUUID();

		waitForJobDone(project, jobUUID);

		/* why test green result ? Bcause we set test folders in a way we
		 * will expect green traffic light - which is only the case when
		 * we have an explicit path set by the environment entry inside
		 * template... We could also rely on server validation of version
		 * but this way is better, because we rely on test environment /mocked
		 * adapter behavior which is well known.
		 */
		as(user).
			withSecHubClient().
			startDownloadJobReport(project, jobUUID, jsonConfigFile).
			hasTrafficLight(TrafficLight.GREEN)

			;
		/* @formatter:on */

	}

	@Test
	public void a_project_having_no_white_list_entries_but_some_problems_can_be_executed_as_codescan_and_results_yellow() {
		/* @formatter:off */

		/* prepare */
		TestProject project = PROJECT_3;
		TestUser user = USER_1;

		assertProject(project).hasNoWhiteListEntries();

		as(SUPER_ADMIN).
			assignUserToProject(user, project);


		/* execute */
		String jsonConfigFile = "sechub-integrationtest-client-sourcescan-yellow.json";
		UUID jobUUID =
	    as(user).
			withSecHubClient().
			startAsynchronScanFor(project, jsonConfigFile).
			assertFileUploaded(project).
			assertJobTriggered().
			getJobUUID();

		waitForJobDone(project, jobUUID);

		as(user).
			withSecHubClient().
			startDownloadJobReport(project, jobUUID, jsonConfigFile).
			hasTrafficLight(TrafficLight.YELLOW)

			;
		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_asynchronous_and_file_is_uploaded() {
		/* @formatter:off */

		/* prepare */
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1);

		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		as(USER_1).
			withSecHubClient().
			startAsynchronScanFor(PROJECT_1, "sechub-integrationtest-client-sourcescan-green.json").
			assertFileUploaded(PROJECT_1);

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_asynchronous_and_file_is_uploaded_and_excludes_handled() {
		/* @formatter:off */

		/* prepare */
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1);

		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */

		as(USER_1).
			withSecHubClient().
			// `sechub-integrationtest-client-sourcescan-excluded_some_files.json`
			// uses a mock with 5 seconds running job - enough to get access to
			// the uploaded content, download it full. Otherwise file could
			// be automated removed by cleanup actions on server!
			startAsynchronScanFor(PROJECT_1, "sechub-integrationtest-client-sourcescan-excluded_some_files.json").
				assertFileUploadedAsZip(PROJECT_1).
					zipContains("not-excluded.txt").
					zipContains("subfolder/not-excluded-2.txt").
					zipNotContains("exclude-me.txt").
					zipNotContains("subfolder/exclude-me.txt");

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_synchronous_and_result_has_trafficlight_green() {

		/* prepare */
		as(SUPER_ADMIN).assignUserToProject(USER_1, PROJECT_1);

		/* @formatter:off */
		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		ExecutionResult result = as(USER_1).withSecHubClient().startSynchronScanFor(PROJECT_1,
				"sechub-integrationtest-client-sourcescan-green.json");

		/* test */
		assertResult(result).
			isGreen().
			hasExitCode(0);

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_synchronous_and_result_has_trafficlight_yellow_pe_default_exitcode0() {

		/* prepare */
		as(SUPER_ADMIN).assignUserToProject(USER_1, PROJECT_1);

		/* @formatter:off */
		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		ExecutionResult result = as(USER_1).
				withSecHubClient().
				startSynchronScanFor(PROJECT_1, "sechub-integrationtest-client-sourcescan-yellow.json");

		/* test */
		assertResult(result).
			isYellow().
			hasExitCode(0);

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_synchronous_and_result_has_trafficlight_yellow_stop_on_yellow_active_so_exit_code1() {

		/* prepare */
		as(SUPER_ADMIN).assignUserToProject(USER_1, PROJECT_1);

		/* @formatter:off */
		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		ExecutionResult result = as(USER_1).
				withSecHubClient().
				enableStopOnYellow().
				startSynchronScanFor(PROJECT_1, "sechub-integrationtest-client-sourcescan-yellow.json");

		/* test */
		assertResult(result).
			isYellow().
			hasExitCode(1);

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_synchronous_and_result_has_trafficlight_red() {

		/* @formatter:off */
		/* prepare */
		as(SUPER_ADMIN).
			assignUserToProject(USER_1, PROJECT_1).
			updateWhiteListForProject(PROJECT_1, Collections.singletonList("https://vulnerable.demo.example.org"));

		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		ExecutionResult result = as(USER_1).
				withSecHubClient().
				startSynchronScanFor(PROJECT_1, "sechub-integrationtest-webscanconfig-red-result.json");

		/* test */
		assertResult(result).
			isRed().
			hasExitCode(1);

		/* @formatter:on */

	}

	@Test
	public void sechub_client_is_able_to_handle_synchronous_and_result_has_trafficlight_green_when_config_is_extreme_big() {

		/* prepare */
		as(SUPER_ADMIN).assignUserToProject(USER_1, PROJECT_1);

		/* @formatter:off */
		assertUser(USER_1).
			doesExist().
			isAssignedToProject(PROJECT_1);

		/* execute */
		ExecutionResult result = as(USER_1).withSecHubClient().startSynchronScanFor(PROJECT_1,
				"sechub-integrationtest-client-sourcescan-green-extreme-big.json");

		/* test */
		assertResult(result).
			isGreen().
			hasExitCode(0);

		/* @formatter:on */

	}

}
