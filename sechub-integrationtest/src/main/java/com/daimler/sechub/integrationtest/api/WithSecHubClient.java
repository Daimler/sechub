// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daimler.sechub.commons.model.TrafficLight;
import com.daimler.sechub.integrationtest.api.AsUser.ProjectFalsePositivesDefinition;
import com.daimler.sechub.integrationtest.internal.IntegrationTestFileSupport;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ClientAction;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.integrationtest.internal.TestJSONHelper;
import com.daimler.sechub.test.TestFileSupport;
import com.daimler.sechub.test.TestUtil;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This test class uses former build sechub client for execution.
 *
 * @author Albert Tregnaghi
 *
 */
public class WithSecHubClient {
    private static final Logger LOG = LoggerFactory.getLogger(WithSecHubClient.class);

    private AsUser asUser;
    private Path outputFolder;

    private boolean stopOnYellow;

    private String sechubClientBinaryPath;

    private boolean trustAll = true; // in tests we normally trust all - other usages, like dev tools should change
                                     // this

    WithSecHubClient(AsUser asUser) {
        this.asUser = asUser;
        try {
            this.outputFolder = Files.createTempDirectory("with-sechub-client-");
            this.outputFolder.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Can NOT create temp directory for tests!", e);
        }
    }

    public ProjectFalsePositivesDefinition startFalsePositiveDefinition(TestProject project, IntegrationTestJSONLocation location) {
        return asUser.new ProjectFalsePositivesDefinition(project, this, location);
    }

    public WithSecHubClient fromPath(String pathToSechubClientBinary) {
        this.sechubClientBinaryPath = pathToSechubClientBinary;
        return this;
    }

    public WithSecHubClient denyTrustAll() {
        this.trustAll = false;
        return this;
    }

    public WithSecHubClient enableStopOnYellow() {
        this.stopOnYellow = true;
        return this;
    }

    public AssertJobReport startDownloadJobReport(TestProject project, UUID jobUUID, IntegrationTestJSONLocation location) {
        return new AssertJobReport(project, jobUUID, location.getPath());
    }

    public class AssertJobReport {
        UUID jobUUID;
        TestProject project;
        String jsonConfigfile;
        TrafficLight trafficLight;

        public AssertJobReport(TestProject project, UUID jobUUID, String jsonConfigfile) {
            this.jobUUID = jobUUID;
            this.project = project;
            this.jsonConfigfile = jsonConfigfile;

            String path = executeReportDownloadAndGetPathOfFile();
            File file = new File(path);
            String report = TestFileSupport.loadTextFile(file, "\n");
            LOG.debug("loaded report:{}", report);
            JsonNode data = TestJSONHelper.get().readTree(report);
            JsonNode tl = data.get("trafficLight");
            String trafficLightText = tl.asText();
            this.trafficLight = TrafficLight.fromString(trafficLightText);
        }

        private String executeReportDownloadAndGetPathOfFile() {
            File file = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(jsonConfigfile);
            SecHubClientExecutor executor = createExecutor();
            List<String> list = buildEnvironmentAndBehaviourCommands(project);
            list.add("-jobUUID");
            list.add(jobUUID.toString());

            ExecutionResult result = doExecute(ClientAction.GET_REPORT, file, executor, list, null);
            if (result.getExitCode() != 0) {
                fail("Not exit code 0 but:" + result.getExitCode());
            }
            /* getReport returns always json in last line, no line separators */
            String lastOutputLine = result.getLastOutputLine();
            // Examnple:" SecHub report written to
            // /tmp/with-sechub-client-8935321107154972222/sechub_report_1d6b228f-74de-450f-9f81-0db6b8ad8be1.json"
            String identifier = "written to ";
            int index = lastOutputLine.indexOf(identifier);
            if (index == -1) {
                fail("Unexpected Output line:" + lastOutputLine);
            }
            String path = lastOutputLine.substring(index + identifier.length());
            return path;
        }

        public AssertJobReport hasTrafficLight(TrafficLight expected) {
            assertEquals(expected, trafficLight);
            return this;
        }

    }

    public class AssertAsyncResult {
        private UUID jobUUID;
        public File configFile;

        public UUID getJobUUID() {
            return jobUUID;
        }

        private AssertAsyncResult() {

        }

        public AssertAsyncResult assertJobTriggered() {
            /* having a job uuid means it was done */
            assertNotNull(jobUUID);
            return this;
        }

        /**
         * Asserts file was uploaded for project
         *
         * @param project
         * @param sha256checksum
         * @return
         */
        public AssertZipFileUpload assertFileUploadedAsZip(TestProject project) {
            File file = assertFile(project);
            return new AssertZipFileUpload(file);
        }

        public AssertAsyncResult assertFileUploaded(TestProject project) {
            assertFile(project);
            return this;
        }

        private File assertFile(TestProject project) {
            /* the filename at upload is currently always sourcecode.zip! */
            File file = TestAPI.getFileUploaded(project, jobUUID, "sourcecode.zip");
            if (file == null) {
                fail("NO file upload for " + jobUUID + " in project +" + project);
            }

            LOG.info("Uploaded file for job {} was re-downloaded to {}", jobUUID, file);

            return file;
        }
    }

    public class AssertZipFileUpload {
        private File downloadedFile;
        Path unzipTo;

        private AssertZipFileUpload(File file) {
            if (file == null) {
                throw new IllegalArgumentException("Zip file may not be null");
            }
            if (!file.exists()) {
                throw new IllegalArgumentException("Zip file does not exist:" + file);
            }
            this.downloadedFile = file;

            try {
                unzipTo = Files.createTempDirectory("sechub-assertzip");
                if (TestUtil.isDeletingTempFiles()) {
                    unzipTo.toFile().deleteOnExit();
                }
                /* unzip */
                TestUtil.unzip(downloadedFile, unzipTo);
                LOG.info("Unzipped re-downloaded zipfile {} to {}", downloadedFile, unzipTo);

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

        }

        public AssertZipFileUpload zipContains(String pathToFile) {
            File f = new File(unzipTo.toFile(), pathToFile);
            if (!f.exists()) {
                fail("File does not exist:" + pathToFile + "\n - looked at :" + f.getAbsolutePath());
            }
            return this;
        }

        public AssertZipFileUpload zipNotContains(String pathToFile) {
            File f = new File(unzipTo.toFile(), pathToFile);
            if (f.exists()) {
                fail("File does exist:" + pathToFile + "\n - looked at :" + f.getAbsolutePath());
            }
            return this;
        }

    }

    /**
     * Starts asynchronous scan for test project - WILL NOT HIDE API token - so use
     * only in tests!
     * 
     * @param project
     * @param location
     * @return assert object
     */
    public AssertAsyncResult startAsynchronScanFor(TestProject project, IntegrationTestJSONLocation location) {
        return startAsynchronScanFor(project, location, null);
    }

    /**
     * Starts asynchronous scan for test project - WILL NOT HIDE API token - so use
     * only in tests!
     * 
     * @param project
     * @param location
     * @param environmentVariables
     * @return assert object
     */
    public AssertAsyncResult startAsynchronScanFor(TestProject project, IntegrationTestJSONLocation location, Map<String, String> environmentVariables) {
        return startAsynchronScanFor(project, location, environmentVariables, ApiTokenStrategy.VISIBLE_AS_ARGUMENT);
    }

    public AssertAsyncResult startAsynchronScanFor(TestProject project, IntegrationTestJSONLocation location, Map<String, String> environmentVariables,
            ApiTokenStrategy apiTokenStrategy) {
        File sechubConfigFile = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(location.getPath());
        SecHubClientExecutor executor = createExecutor();
        List<String> list = buildEnvironmentAndBehaviourCommands(project);
        ExecutionResult result = doExecute(ClientAction.START_ASYNC, apiTokenStrategy, sechubConfigFile, executor, list, environmentVariables);
        if (result.getExitCode() != 0) {
            fail("Not exit code 0 but:" + result.getExitCode() + " , last output line was:" + result.getLastOutputLine());
        }
        AssertAsyncResult asynchResult = new AssertAsyncResult();
        asynchResult.jobUUID = UUID.fromString(result.getLastOutputLine());
        asynchResult.configFile = sechubConfigFile;
        return asynchResult;
    }

    /**
     * Starts a synchronous scan for given project.
     *
     * @param project
     * @param location identifier for the config file which shall be used. Its
     *                 automatically resolved from test file support.
     * @return assert object
     */
    public ExecutionResult startSynchronScanFor(TestProject project, IntegrationTestJSONLocation location) {
        return startSynchronScanFor(project, location, null);
    }

    /**
     * Starts a synchronous scan for given project (WILL NOT HIDE API TOKEN! And
     * does also always use wait zero time. So use only inside tests!)
     *
     * @param project
     * @param location identifier for the config file which shall be used. Its
     *                 automatically resolved from test file support.
     * @return result
     */
    public ExecutionResult startSynchronScanFor(TestProject project, IntegrationTestJSONLocation location, Map<String, String> environmentVariables) {
        File file = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(location.getPath());
        return startSynchronScanFor(project, environmentVariables, file, ApiTokenStrategy.VISIBLE_AS_ARGUMENT, ClientWaitMode.WAIT_ZERO_TIME);
    }

    /**
     * Starts a synchronous scan for given project (WILL NOT HIDE API TOKEN! So use
     * only inside tests!)
     * 
     * @param project
     * @param environmentVariables
     * @param file
     * @return result
     */
    public ExecutionResult startSynchronScanFor(TestProject project, Map<String, String> environmentVariables, File file) {
        return startSynchronScanFor(project, environmentVariables, file, ApiTokenStrategy.VISIBLE_AS_ARGUMENT, ClientWaitMode.WAIT_ZERO_TIME);
    }

    public enum ClientWaitMode {
        WAIT_ZERO_TIME,

        WAIT_NORMAL,

        WAIT_WITH_ENV_SETTINGS,
    }

    public ExecutionResult startSynchronScanFor(TestProject project, Map<String, String> environmentVariables, File file, ApiTokenStrategy apiTokenStrategy,
            ClientWaitMode waitMode) {
        SecHubClientExecutor executor = createExecutor();

        List<String> list = buildEnvironmentAndBehaviourCommands(project, waitMode);
        if (waitMode == ClientWaitMode.WAIT_WITH_ENV_SETTINGS) {
            String value = System.getenv().get("SECHUB_WAITTIME_DEFAULT");
            if (value != null) {
                environmentVariables.put("SECHUB_WAITTIME_DEFAULT", value);
            }
        }

        return doExecute(ClientAction.START_SYNC, apiTokenStrategy, file, executor, list, environmentVariables);
    }

    private SecHubClientExecutor createExecutor() {
        SecHubClientExecutor executor = new SecHubClientExecutor();
        executor.setOutputFolder(outputFolder);
        executor.setSechubClientBinaryPath(sechubClientBinaryPath);
        executor.setTrustAll(trustAll);
        return executor;
    }

    public enum ApiTokenStrategy {
        HIDEN_BY_ENV,

        VISIBLE_AS_ARGUMENT
    }

    private ExecutionResult doExecute(ClientAction action, File file, SecHubClientExecutor executor, List<String> list,
            Map<String, String> environmentVariables) {
        return doExecute(action, ApiTokenStrategy.VISIBLE_AS_ARGUMENT, file, executor, list, environmentVariables);
    }

    private ExecutionResult doExecute(ClientAction action, ApiTokenStrategy hideAPIToken, File file, SecHubClientExecutor executor, List<String> list,
            Map<String, String> environmentVariables) {
        return executor.execute(file, hideAPIToken, asUser.user, action, environmentVariables, list.toArray(new String[list.size()]));
    }

    private List<String> buildEnvironmentAndBehaviourCommands(TestProject project) {
        return buildEnvironmentAndBehaviourCommands(project, ClientWaitMode.WAIT_NORMAL);
    }

    private List<String> buildEnvironmentAndBehaviourCommands(TestProject project, ClientWaitMode waitMode) {
        List<String> list = new ArrayList<>();
        String serverURL = asUser.getServerURL();
        if (serverURL != null) {
            list.add("-server");
            list.add(serverURL);
        }
        if (project != null) {
            list.add("-project");
            list.add(project.getProjectId());
        }
        list.add("-output");
        list.add(outputFolder.toFile().getAbsolutePath());
        if (waitMode == ClientWaitMode.WAIT_ZERO_TIME) {
            list.add("-wait");
            list.add("0");
        }
        if (stopOnYellow) {
            list.add("-stop-on-yellow");
        }
        return list;
    }

    /**
     * Starts a code scan - result will be green and not long running
     * 
     * @param project
     * @return
     */
    public AssertExecutionResult startAndWaitForCodeScan(TestProject project) {
        return startAndWaitForCodeScan(project, IntegrationTestJSONLocation.CLIENT_JSON_SOURCESCAN_GREEN);
    }

    public AssertExecutionResult startAndWaitForCodeScan(TestProject project, IntegrationTestJSONLocation location) {
        ExecutionResult result = startSynchronScanFor(project, location);
        return AssertExecutionResult.assertResult(result);
    }

    /**
     * When not changed by project specific mock data setup this will result in a
     * RED traffic light result.<br>
     * <br>
     * Ensure that "https://fscan.intranet.example.org/" is in whitelist of project
     * to scan!
     * 
     * @param project
     * @return execution result
     */
    public AssertExecutionResult createInfraScanAndFetchScanData(TestProject project) {
        ExecutionResult result = startSynchronScanFor(project, IntegrationTestJSONLocation.CLIENT_JSON_INFRASCAN);
        return AssertExecutionResult.assertResult(result);
    }

    public void markAsFalsePositive(TestProject project, IntegrationTestJSONLocation location, String pathToJSONFile) {
        File sechubConfigFile = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(location.getPath());
        SecHubClientExecutor executor = createExecutor();
        List<String> list = buildEnvironmentAndBehaviourCommands(project);
        list.add("-file");
        list.add(pathToJSONFile);

        ExecutionResult result = doExecute(ClientAction.MARK_FALSE_POSITIVES, sechubConfigFile, executor, list, null);
        if (result.getExitCode() != 0) {
            fail("Not exit code 0 but:" + result.getExitCode() + " , last output line was:" + result.getLastOutputLine());
        }
    }

    public void unmarkAsFalsePositive(TestProject project, IntegrationTestJSONLocation location, String pathToJSONFile) {
        File sechubConfigFile = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(location.getPath());
        SecHubClientExecutor executor = createExecutor();
        List<String> list = buildEnvironmentAndBehaviourCommands(project);
        list.add("-file");
        list.add(pathToJSONFile);

        ExecutionResult result = doExecute(ClientAction.UNMARK_FALSE_POSITIVES, sechubConfigFile, executor, list, null);
        if (result.getExitCode() != 0) {
            fail("Not exit code 0 but:" + result.getExitCode() + " , last output line was:" + result.getLastOutputLine());
        }

    }

    public ProjectFalsePositivesDefinition getFalsePositiveConfigurationOfProject(TestProject project, IntegrationTestJSONLocation location) {
        File sechubConfigFile = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(location.getPath());
        SecHubClientExecutor executor = createExecutor();
        List<String> list = buildEnvironmentAndBehaviourCommands(project);

        ExecutionResult result = doExecute(ClientAction.GET_FALSE_POSITIVES, sechubConfigFile, executor, list, null);
        if (result.getExitCode() != 0) {
            fail("Not exit code 0 but:" + result.getExitCode() + " , last output line was:" + result.getLastOutputLine());
        }
        File file = result.getJSONFalsePositiveFile();
        String json = TestFileSupport.loadTextFile(file, "\n");

        return asUser.create(project, json);
    }
}
