// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;

import com.daimler.sechub.commons.model.JSONConverterException;
import com.daimler.sechub.commons.model.TrafficLight;
import com.daimler.sechub.domain.scan.log.ProjectScanLogService;
import com.daimler.sechub.domain.scan.product.CodeScanProductExecutionService;
import com.daimler.sechub.domain.scan.product.InfrastructureScanProductExecutionService;
import com.daimler.sechub.domain.scan.product.WebScanProductExecutionService;
import com.daimler.sechub.domain.scan.project.ScanMockData;
import com.daimler.sechub.domain.scan.project.ScanProjectConfig;
import com.daimler.sechub.domain.scan.project.ScanProjectConfigID;
import com.daimler.sechub.domain.scan.project.ScanProjectConfigService;
import com.daimler.sechub.domain.scan.project.ScanProjectMockDataConfiguration;
import com.daimler.sechub.domain.scan.report.CreateScanReportService;
import com.daimler.sechub.domain.scan.report.ScanReport;
import com.daimler.sechub.sharedkernel.ProgressMonitor;
import com.daimler.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionException;
import com.daimler.sechub.sharedkernel.messaging.AsynchronMessageHandler;
import com.daimler.sechub.sharedkernel.messaging.BatchJobMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageService;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageSynchronousResult;
import com.daimler.sechub.sharedkernel.messaging.DummyEventInspector;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.messaging.SynchronMessageHandler;
import com.daimler.sechub.storage.core.JobStorage;
import com.daimler.sechub.storage.core.StorageService;

public class ScanServiceTest {

	private static final String TEST_PROJECT_ID1 = "test-project-id1";
	private static final String TRAFFIC_LIGHT = "someColor";
	private static final java.util.UUID UUID = java.util.UUID.randomUUID();
	private static final String SECHUB_CONFIG_VALID_MINIMUM = "{ \"projectId\" : \""+TEST_PROJECT_ID1+"\" }";
	private ScanService serviceToTest;
	private WebScanProductExecutionService webScanProductExecutionService;
	private CodeScanProductExecutionService codeScanProductExecutionService;
	private InfrastructureScanProductExecutionService infrastructureScanProductExecutionService;
	private CreateScanReportService reportService;
	private ScanReport report;
	private StorageService storageService;
	private JobStorage jobStorage;
	private ProjectScanLogService scanLogService;
	private ScanProjectConfigService scanProjectConfigService;
    private ScanJobListener scanJobRegistry;
    private ScanProgressMonitorFactory monitorFactory;
	private static final SecHubConfiguration SECHUB_CONFIG = new SecHubConfiguration();

	@Before
	public void before() throws Exception {
		storageService = mock(StorageService.class);
		jobStorage = mock(JobStorage.class);
		scanProjectConfigService = mock(ScanProjectConfigService.class);
		scanJobRegistry = mock(ScanJobListener.class);
		monitorFactory=mock(ScanProgressMonitorFactory.class);
		ProgressMonitor monitor = mock(ProgressMonitor.class);
		when(monitor.getId()).thenReturn("monitor-test-id");

		when(storageService.getJobStorage(any(), any())).thenReturn(jobStorage);
		when(monitorFactory.createProgressMonitor(any())).thenReturn(monitor);
		
		webScanProductExecutionService = mock(WebScanProductExecutionService.class);
		codeScanProductExecutionService = mock(CodeScanProductExecutionService.class);
		infrastructureScanProductExecutionService = mock(InfrastructureScanProductExecutionService.class);
		scanLogService = mock(ProjectScanLogService.class);

		reportService = mock(CreateScanReportService.class);
		report = mock(ScanReport.class);
		when(report.getTrafficLightAsString()).thenReturn(TRAFFIC_LIGHT);
		when(reportService.createReport(any())).thenReturn(report);

		serviceToTest = new ScanService();
		serviceToTest.webScanProductExecutionService = webScanProductExecutionService;
		serviceToTest.infraScanProductExecutionService = infrastructureScanProductExecutionService;
		serviceToTest.codeScanProductExecutionService = codeScanProductExecutionService;
		serviceToTest.reportService = reportService;
		serviceToTest.storageService = storageService;
		serviceToTest.scanLogService = scanLogService;
		serviceToTest.scanProjectConfigService = scanProjectConfigService;
		serviceToTest.scanJobListener=scanJobRegistry;
		serviceToTest.monitorFactory=monitorFactory;
	}

	@Test
	public void when_no_exception_is_thrown_result_has_not_failed() throws Exception {

		/* execute */
		DomainMessageSynchronousResult result = serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		assertFalse(result.hasFailed());
	}

	@Test
	public void scanservice_does_execute_webscan_execution_service() throws Exception {

		/* execute */
		serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		verify(webScanProductExecutionService).executeProductsAndStoreResults(any());
	}

	@Test
	public void scanservice_does_execute_infrascan_execution_service() throws Exception {

		/* execute */
		serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		verify(infrastructureScanProductExecutionService).executeProductsAndStoreResults(any());
	}

	@Test
	public void scanservice_does_execute_codescan_execution_service() throws Exception {

		/* execute */
		serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		verify(codeScanProductExecutionService).executeProductsAndStoreResults(any());
	}

	@Test
	public void scanservice_does_cleanup_storage_of_job__when_not_failed() throws Exception {

		/* execute */
		DomainMessageSynchronousResult result = serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		verify(jobStorage).deleteAll();
		assertFalse(result.hasFailed());
	}

	/**
	 * Here we test that on failure the storage is ALSO cleaned. Why? Because in
	 * future there should be the possiblity for a retry mechanism, but currently
	 * there is none. When this is implemented we must change the test so it will
	 * check there is NO cleaning. But having no retry mechanism implemented, we
	 * expect the cleanup process done even when failing.
	 *
	 * @throws Exception
	 */
	@Test
	public void scanservice_does_cleanup_storage_of_job__when_HAS_failed() throws Exception {

		/* prepare */
		DomainMessage request = prepareValidRequest();
		doThrow(new SecHubExecutionException("ups...", new RuntimeException())).when(webScanProductExecutionService).executeProductsAndStoreResults(any());

		/* execute */
		DomainMessageSynchronousResult result = serviceToTest.receiveSynchronMessage(request);

		/* test */
		assertTrue(result.hasFailed());
		verify(jobStorage/* when retry implemented:,never() */).deleteAll();

	}

	@Test
	public void scanservice_does_execute_report_service() throws Exception {

		/* execute */
		serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		verify(reportService).createReport(any());
	}

	@Test
	public void scanservice_set_result_traficlight_as_from_report() throws Exception {

		/* execute */
		DomainMessageSynchronousResult result = serviceToTest.receiveSynchronMessage(prepareValidRequest());

		/* test */
		assertEquals(TRAFFIC_LIGHT, result.get(MessageDataKeys.REPORT_TRAFFIC_LIGHT));
	}

	@Test
	public void scanservice_does_NOT_execute_reportservice_when_webscan_throws_sechubexception() throws Exception {

		/* prepare */
		DomainMessage request = prepareValidRequest();
		doThrow(new SecHubExecutionException("ups...", new RuntimeException())).when(webScanProductExecutionService).executeProductsAndStoreResults(any());

		/* execute */
		DomainMessageSynchronousResult result = serviceToTest.receiveSynchronMessage(request);

		/* test */
		assertTrue(result.hasFailed());

		verify(webScanProductExecutionService).executeProductsAndStoreResults(any());
		verify(reportService, never()).createReport(any());

	}

	@Test
	public void event_handling_works_as_expected_and_SCAN_DONE_is_returned_as_resulting_message_id() {
		/* prepare */
		DomainMessage request = prepareValidRequest();

		/* execute */
		DomainMessageSynchronousResult result = simulateEventSend(request, serviceToTest);

		/* test */
		assertEquals(MessageID.SCAN_DONE, result.getMessageId());
	}
	
	@Test
	public void event_handling_FAILED_when_configuration_is_not_set() {
		/* prepare */
		DomainMessage request = prepareValidRequest();
		request.set(MessageDataKeys.SECHUB_CONFIG, null);
		
		/* execute */
		DomainMessageSynchronousResult result = simulateEventSend(request, serviceToTest);

		/* test */
		assertEquals(MessageID.SCAN_FAILED, result.getMessageId());
	}
	
	@Test
	public void event_handling_FAILED_when_configuration_is_set_but_contains_no_projectId() {
		/* prepare */
		SecHubConfiguration configNoProjectId = prepareValidConfiguration();
		configNoProjectId.setProjectId(null);
		DomainMessage request = prepareRequest(configNoProjectId);
		
		/* execute */
		DomainMessageSynchronousResult result = simulateEventSend(request, serviceToTest);

		/* test */
		assertEquals(MessageID.SCAN_FAILED, result.getMessageId());
	}
	
	@Test
	public void scan_service_fetches_configuration_without_accesscheck() throws Exception{
		/* prepare */
		SecHubConfiguration configNoProjectId = prepareValidConfiguration();
		DomainMessage request = prepareRequest(configNoProjectId);
		
		/* execute */
		simulateEventSend(request, serviceToTest);

		/* test */
		verify(scanProjectConfigService).get(TEST_PROJECT_ID1, ScanProjectConfigID.MOCK_CONFIGURATION,false);
	}
	
	
	@Test
	public void scan_service_fetches_mock_configuration_and_puts_mock_project_configuration_complete_in_execution_context() throws Exception{
		/* prepare */
		SecHubConfiguration configNoProjectId = prepareValidConfiguration();
		DomainMessage request = prepareRequest(configNoProjectId);
		
		ScanProjectMockDataConfiguration projectMockDataConfig = new ScanProjectMockDataConfiguration();
		projectMockDataConfig.setCodeScan(new ScanMockData(TrafficLight.YELLOW));
		
		ScanProjectConfig projectConfig = new ScanProjectConfig(ScanProjectConfigID.MOCK_CONFIGURATION, TEST_PROJECT_ID1);
		projectConfig.setData(projectMockDataConfig.toJSON());
		
		when(scanProjectConfigService.get("test-project-id1", ScanProjectConfigID.MOCK_CONFIGURATION,false)).thenReturn(projectConfig);
		
		/* execute */
		simulateEventSend(request, serviceToTest);

		/* test */
		ArgumentCaptor<SecHubExecutionContext> contextCaptor = ArgumentCaptor.forClass(SecHubExecutionContext.class);
		verify(codeScanProductExecutionService).executeProductsAndStoreResults(contextCaptor.capture());
		SecHubExecutionContext context = contextCaptor.getValue();
		assertEquals(projectMockDataConfig, context.getData(ScanKey.PROJECT_MOCKDATA_CONFIGURATION));
	}

	private DomainMessage prepareValidRequest() {

		SecHubConfiguration configMin = prepareValidConfiguration();

		return prepareRequest(configMin);
	}

	private DomainMessage prepareRequest(SecHubConfiguration configMin) {
		DomainMessage request = new DomainMessage(MessageID.START_SCAN);
		request.set(MessageDataKeys.SECHUB_UUID, UUID);
		request.set(MessageDataKeys.SECHUB_CONFIG, configMin);
		BatchJobMessage batchJobMessage = new BatchJobMessage();
		batchJobMessage.setSecHubJobUUID(UUID);
		batchJobMessage.setBatchJobId(42);
        request.set(MessageDataKeys.BATCH_JOB_ID, batchJobMessage);

		return request;
	}

	private SecHubConfiguration prepareValidConfiguration() {
		SecHubConfiguration configMin;
		try {
			configMin = SECHUB_CONFIG.fromJSON(SECHUB_CONFIG_VALID_MINIMUM);
		} catch (JSONConverterException e) {
			throw new IllegalStateException("testcase invalid!");
		}
		return configMin;
	}

	private DomainMessageSynchronousResult simulateEventSend(DomainMessage request, SynchronMessageHandler handler) {
		List<AsynchronMessageHandler> injectedAsynchronousHandlers = new ArrayList<>();
		List<SynchronMessageHandler> injectedSynchronousHandlers = new ArrayList<>();
		injectedSynchronousHandlers.add(handler);

		FakeDomainMessageService fakeDomainMessageService = new FakeDomainMessageService(injectedSynchronousHandlers, injectedAsynchronousHandlers);
		return fakeDomainMessageService.sendSynchron(request);
	}

	private class FakeDomainMessageService extends DomainMessageService {

		public FakeDomainMessageService(List<SynchronMessageHandler> injectedSynchronousHandlers, List<AsynchronMessageHandler> injectedAsynchronousHandlers) {
			super(injectedSynchronousHandlers, injectedAsynchronousHandlers);
			this.taskExecutor = new TestTaskExecutor();
			this.eventInspector=new DummyEventInspector();
		}

	}

	private class TestTaskExecutor implements TaskExecutor {

		@Override
		public void execute(Runnable task) {
			task.run();
		}

	}

}
