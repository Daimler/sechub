// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.daimler.sechub.integrationtest.internal.IntegrationTestContext;
import com.daimler.sechub.integrationtest.internal.IntegrationTestFileSupport;
import com.daimler.sechub.integrationtest.internal.TestJSONHelper;
import com.daimler.sechub.integrationtest.internal.TestRestHelper;
import com.daimler.sechub.test.JSONTestSupport;
import com.daimler.sechub.test.TestURLBuilder;
import com.daimler.sechub.test.TestUtil;
import com.fasterxml.jackson.databind.JsonNode;

public class AsUser {

	private static final Logger LOG = LoggerFactory.getLogger(AsUser.class);

	TestUser user;

	AsUser(TestUser user) {
		this.user = user;
	}

	public WithSecHubClient withSecHubClient() {
		return new WithSecHubClient(this);
	}

	/**
	 * Accept the user wanting to signup
	 *
	 * @param userWantingToSignup
	 * @return
	 */
	public AsUser upload(TestProject project, UUID jobUUID, File file, String checkSum) {
		/* @formatter:off */
		getRestHelper().upload(getUrlBuilder().
		    		buildUploadSourceCodeUrl(project.getProjectId(),jobUUID),file,checkSum);
		/* @formatter:on */
		return this;
	}

	/**
	 * Accept the user wanting to signup
	 *
	 * @param userWantingToSignup
	 * @return
	 */
	public AsUser acceptSignup(TestUser userWantingToSignup) {
		if (userWantingToSignup == null) {
			fail("user may not be null!");
			return null;
		}

		/* @formatter:off */
		getRestHelper().post(getUrlBuilder().
		    		buildAdminAcceptsUserSignUpUrl(userWantingToSignup.getUserId()));
		/* @formatter:on */
		return this;
	}

	private TestRestHelper getRestHelper() {
		return getContext().getRestHelper(user);
	}

	/**
	 * Signup given (new) user
	 *
	 * @param user
	 * @return this
	 */
	public AsUser signUpAs(TestUser user) {

		String json = "{\"apiVersion\":\"1.0\",\r\n" + "		\"userId\":\"" + user.getUserId() + "\",\r\n" + "		\"emailAdress\":\"" + user.getEmail()
				+ "\"}";
		getRestHelper().postJSon(getUrlBuilder().buildUserSignUpUrl(), json);
		return this;

	}

	public AsUser requestNewApiTokenFor(String emailAddress) {
		getRestHelper().postJSon(getUrlBuilder().buildAnonymousRequestNewApiToken(emailAddress), "");
		return this;
	}

	private TestURLBuilder getUrlBuilder() {
		return getContext().getUrlBuilder();
	}

	private IntegrationTestContext getContext() {
		return IntegrationTestContext.get();
	}

	/**
	 * Tries to create the project
	 *
	 * @param project
	 * @throws RestClientException
	 */
	public void createProject(TestProject project, String ownerName) {
		if (ownerName == null) {
			// we use always the user how creates the project as owner when not explicit set
			ownerName = this.user.getUserId();
		}
		/* @formatter:off */
		StringBuilder json = new StringBuilder();
		TestJSONHelper jsonHelper = TestJSONHelper.get();
		json.append("{\n" +
				" \"apiVersion\":\"1.0\",\n" +
				" \"name\":\""+project.getProjectId()+"\",\n" +
				" \"owner\":\""+ownerName+"\",\n" +
				" \"description\":\""+project.getDescription()+"\"");
		if (! project.getWhiteListUrls().isEmpty()) {
			json.append(",\n \"whiteList\" : {\"uris\":[");

			for (Iterator<String> it = project.getWhiteListUrls().iterator();it.hasNext();) {
				String url = it.next();
				json.append("\""+url+"\"");
				if (it.hasNext()){
					json.append(",");
				}
			}
			json.append("]\n");
			json.append("                 }\n");
		}

		json.append("}\n");
		jsonHelper.assertValidJson(json.toString());
		/* @formatter:on */
		getRestHelper().postJSon(getUrlBuilder().buildAdminCreatesProjectUrl(), json.toString());

	}

	public String getServerURL() {
		return getUrlBuilder().buildServerURL();
	}

	public String getStringFromURL(String link) {
		return getRestHelper().getStringFromURL(link);
	}

	/**
	 * Assigns user to a project
	 *
	 * @param targetUser
	 * @param project
	 * @return this
	 */
	public AsUser assignUserToProject(TestUser targetUser, TestProject project) {
		LOG.debug("assigning user:{} to project:{}", user.getUserId(), project.getProjectId());
		getRestHelper().postJSon(getUrlBuilder().buildAdminAssignsUserToProjectUrl(targetUser.getUserId(), project.getProjectId()), "");
		return this;
	}

	/**
	 * Unassigns user from project
	 *
	 * @param targetUser
	 * @param project
	 * @return this
	 */
	public AsUser unassignUserFromProject(TestUser targetUser, TestProject project) {
		LOG.debug("unassigning user:{} from project:{}", user.getUserId(), project.getProjectId());
		getRestHelper().delete(getUrlBuilder().buildAdminUnassignsUserFromProjectUrl(targetUser.getUserId(), project.getProjectId()));
		return this;
	}

	private String createCodeScanJob(TestProject project, IntegrationTestMockMode runMode) {
		String folder = null;
		if (runMode != null) {
			folder = runMode.getTarget();
		}
		if (folder == null) {
			folder = "notexisting";
		}
		String testfile = "sechub-integrationtest-sourcescanconfig1.json";
		String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile(testfile);
		String projectId = project.getProjectId();

		json = json.replaceAll("__projectId__", projectId);

		json = json.replaceAll("__folder__", folder);
		String url = getUrlBuilder().buildAddJobUrl(projectId);
		return getRestHelper().postJSon(url, json);
	}

	private String createWebScanJob(TestProject project, IntegrationTestMockMode runMode) {
		String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile("sechub-integrationtest-webscanconfig1.json");
		String projectId = project.getProjectId();

		json = json.replaceAll("__projectId__", projectId);
		List<String> whites = project.getWhiteListUrls();
		String acceptedURI1 = createTargetURIForSechubConfiguration(runMode, whites);

		json = json.replaceAll("__acceptedUri1__", acceptedURI1);
		String url = getUrlBuilder().buildAddJobUrl(projectId);
		return getRestHelper().postJSon(url, json);
	}

	/**
	 * Create taget uri - will either use
	 *
	 * @param runMode
	 * @param whites
	 * @return
	 */
	private String createTargetURIForSechubConfiguration(IntegrationTestMockMode runMode, List<String> whites) {
		String acceptedURI1 = null;
		if (runMode != null) {
			acceptedURI1 = runMode.getTarget();
		}
		if (acceptedURI1 != null) {
			return acceptedURI1;
		}
		if (whites == null || whites.isEmpty()) {
			return "https://undefined.com";
		}
		/* okay, no runmode used having whitelist entry*/
		List<String> copy = new ArrayList<>(whites);
		for (IntegrationTestMockMode mode : IntegrationTestMockMode.values()) {
			String target = mode.getTarget();
			if (target != null) {
				/* we drop all existing run mode parts here - to avoid side effects */
				copy.remove(target);
			}
		}
		return copy.iterator().next();
	}

	public void approveJob(TestProject project, UUID jobUUID) {
		getRestHelper().put(getUrlBuilder().buildApproveJobUrl(project.getProjectId(), jobUUID.toString()));
	}

	public AsUser updateWhiteListForProject(TestProject project, List<String> uris) {
		String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile("sechub-integrationtest-updatewhitelist1.json");
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = uris.iterator(); it.hasNext();) {
			sb.append("\\\"");
			sb.append(it.next());
			sb.append("\\\"");
			if (it.hasNext()) {
				sb.append(" , ");
			}
		}
		json = json.replaceAll("__acceptedUris__", sb.toString());
		getRestHelper().postJSon(getUrlBuilder().buildUpdateProjectWhiteListUrl(project.getProjectId()), json);
		return this;

	}

	public String getJobStatus(String projectId, UUID jobUUID) {
		return getRestHelper().getJSon(getUrlBuilder().buildGetJobStatusUrl(projectId, jobUUID.toString()));
	}

	public String getJobReport(String projectId, UUID jobUUID) {
		int count = 0;
		boolean jobEnded = false;
		while (count < 10) {
			String jobstatus = getJobStatus(projectId, jobUUID);
			if (jobstatus.indexOf("ENDED") != -1) {
				jobEnded = true;
				break;
			}
			TestUtil.waitMilliseconds(200);
		}
		if (!jobEnded) {
			throw new IllegalStateException("Even after some retries no job report state was accessible!");
		}
		/* okay report is available - so do downooad */
		return getRestHelper().getJSon(getUrlBuilder().buildGetJobReportUrl(projectId, jobUUID));
	}

	/**
	 * Starts a webscan job for project (but job is not started)
	 *
	 * @param project
	 * @return uuid for created job
	 */
	public UUID createWebScan(TestProject project) {
		return createWebScan(project, null);
	}

	/**
	 *
	 * @param project
	 * @param useLongRunningButGreen
	 * @return
	 */
	public UUID createWebScan(TestProject project, IntegrationTestMockMode runMode) {
		assertProject(project).doesExist();
		if (runMode == null) {
			runMode = IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_GREEN__FAST;
		}
		String response = createWebScanJob(project, runMode);
		try {
			JsonNode jsonNode = JSONTestSupport.DEFAULT.fromJson(response);
			JsonNode jobId = jsonNode.get("jobId");
			if (jobId == null) {
				fail("No jobID entry found in json:\n" + response);
				return null;
			}
			return UUID.fromString(jobId.textValue());
		} catch (IllegalArgumentException e) {
			fail("Job did not return with a valid UUID!:" + response);
			throw new IllegalStateException("fail not working");
		} catch (IOException e) {
			throw new IllegalStateException("io failure, should not occure", e);
		}

	}

	/**
	 *
	 * @param project
	 * @param useLongRunningButGreen
	 * @return
	 */
	public UUID createCodeScan(TestProject project, IntegrationTestMockMode runMode) {
		assertProject(project).doesExist();
		if (runMode == null) {
			runMode = IntegrationTestMockMode.CODE_SCAN__CHECKMARX__YELLOW__FAST;
		}
		String response = createCodeScanJob(project, runMode);
		try {
			JsonNode jsonNode = JSONTestSupport.DEFAULT.fromJson(response);
			JsonNode jobId = jsonNode.get("jobId");
			if (jobId == null) {
				fail("No jobID entry found in json:\n" + response);
				return null;
			}
			return UUID.fromString(jobId.textValue());
		} catch (IllegalArgumentException e) {
			fail("Job did not return with a valid UUID!:" + response);
			throw new IllegalStateException("fail not working");
		} catch (IOException e) {
			throw new IllegalStateException("io failure, should not occure", e);
		}

	}

	public File downloadAsTempFileFromURL(String url, UUID jobUUID) {
		String fileName = "sechub-file-redownload-" + jobUUID.toString();
		String fileEnding = ".zip";
		return downloadAsTempFileFromURL(url, jobUUID, fileName, fileEnding);
	}

	public File downloadAsTempFileFromURL(String url, UUID jobUUID, String fileName, String fileEnding) {

		// Optional Accept header
		RequestCallback requestCallback = request -> request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

		ResponseExtractor<File> responseExtractor = response -> {
			Path path = Files.createTempFile(fileName, fileEnding);
			Files.copy(response.getBody(), path, StandardCopyOption.REPLACE_EXISTING);
			if (TestUtil.isDeletingTempFiles()) {
				path.toFile().deleteOnExit();
			}
			return path.toFile();
		};
		RestTemplate template = getRestHelper().getTemplate();
		File x = template.execute(url, HttpMethod.GET, requestCallback, responseExtractor);
		return x;
	}

	public String getServerVersion() {
		return getRestHelper().getJSon(getUrlBuilder().buildGetServerVersionUrl());
	}

	public AssertFullScanData downloadFullScanDataFor(UUID sechubJobUUID) {
		String url = getUrlBuilder().buildAdminDownloadsZipFileContainingFullScanDataFor(sechubJobUUID);
		File file = downloadAsTempFileFromURL(url, sechubJobUUID, "download-fullscan", ".zip");
		return new AssertFullScanData(file);
	}

	public AsUser grantSuperAdminRightsTo(TestUser targetUser) {
		String url = getUrlBuilder().buildAdminGrantsSuperAdminRightsTo(targetUser.getUserId());
		getRestHelper().post(url);
		return this;
	}

	public AsUser revokeSuperAdminRightsFrom(TestUser targetUser) {
		String url = getUrlBuilder().buildAdminRevokesSuperAdminRightsFrom(targetUser.getUserId());
		getRestHelper().post(url);
		return this;
	}

	public String getScanLogsForProject(TestProject project1) {
		String url = getUrlBuilder().buildAdminFetchesScanLogsForProject(project1.getProjectId());
		return getRestHelper().getJSon(url);
	}

	/**
	 * Disbles job processing by scheduler.<br>
	 * <br>
	 * <b> WARNING:</b> You must ensure that your test will do a
	 * <code>as(SUPER_ADMIN).enableSchedulerJobProcessing();</code> at the end of
	 * your test (no matter if test fails somewhere in your test case), otherwise
	 * you got a extreme side effect to your other integration tests...
	 *
	 * @return
	 */
	public AsUser disableSchedulerJobProcessing() {
		String url = getUrlBuilder().buildAdminDisablesSchedulerJobProcessing();
		getRestHelper().post(url);
		return this;
	}

	public AsUser enableSchedulerJobProcessing() {
		String url = getUrlBuilder().buildAdminEnablesSchedulerJobProcessing();
		getRestHelper().post(url);
		return this;
	}

}
