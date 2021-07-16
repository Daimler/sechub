// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.daimler.sechub.commons.model.SecHubResult;
import com.daimler.sechub.commons.model.TrafficLight;
import com.daimler.sechub.domain.scan.SecHubResultService;
import com.daimler.sechub.domain.scan.product.ReportProductExecutionService;
import com.daimler.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;

public class ReportServiceTest {

    private CreateScanReportService serviceToTest;
    private ReportProductExecutionService reportProductExecutionService;
    private SecHubResultService secHubResultService;
    private ScanReportTrafficLightCalculator trafficLightCalculator;
    private SecHubExecutionContext context;
    private SecHubResult secHubResult;
    private ScanReportRepository reportRepository;
    private UUID secHubJobUUID;
    private SecHubConfiguration configuration;
    private ScanReportTransactionService scanReportTransactionService;

    @Before
    public void before() throws Exception {
        serviceToTest = new CreateScanReportService();

        secHubJobUUID = UUID.randomUUID();
        context = mock(SecHubExecutionContext.class);
        configuration = mock(SecHubConfiguration.class);
        when(context.getConfiguration()).thenReturn(configuration);
        when(context.getSechubJobUUID()).thenReturn(secHubJobUUID);
        when(configuration.getProjectId()).thenReturn("project1");

        reportRepository = mock(ScanReportRepository.class);
        /* just return report as given to save method... */
        when(reportRepository.save(any(ScanReport.class))).thenAnswer(new Answer<ScanReport>() {

            @Override
            public ScanReport answer(InvocationOnMock invocation) throws Throwable {
                return (ScanReport) invocation.getArguments()[0];
            }
        });
        scanReportTransactionService = mock(ScanReportTransactionService.class);
        reportProductExecutionService = mock(ReportProductExecutionService.class);

        secHubResult = mock(SecHubResult.class);
        secHubResultService = mock(SecHubResultService.class);
        when(secHubResultService.createResult(context)).thenReturn(secHubResult);

        trafficLightCalculator = mock(ScanReportTrafficLightCalculator.class);

        serviceToTest.reportProductExecutionService = reportProductExecutionService;
        serviceToTest.secHubResultService = secHubResultService;
        serviceToTest.trafficLightCalculator = trafficLightCalculator;
        serviceToTest.reportRepository = reportRepository;
        serviceToTest.scanReportTransactionService = scanReportTransactionService;

    }

    @Test
    public void createReport_returns_not_null() throws Exception {

        /* execute */
        ScanReport report = serviceToTest.createReport(context);

        /* test */
        assertNotNull(report);

    }

    @Test
    public void createReport_set_report_traffic_light_red_name_when_defined_by_trafficlight_calculator() throws Exception {
        /* prepare */
        when(trafficLightCalculator.calculateTrafficLight(secHubResult)).thenReturn(TrafficLight.RED);

        /* execute */
        ScanReport report = serviceToTest.createReport(context);

        /* test */
        assertNotNull(report);
        assertEquals(TrafficLight.RED.name(), report.getTrafficLightAsString());

    }

    @Test
    public void createReport_set_report_traffic_light_yellow_name_when_defined_by_trafficlight_calculator() throws Exception {
        /* prepare */
        when(trafficLightCalculator.calculateTrafficLight(secHubResult)).thenReturn(TrafficLight.YELLOW);

        /* execute */
        ScanReport report = serviceToTest.createReport(context);

        /* test */
        assertNotNull(report);
        assertEquals(TrafficLight.YELLOW.name(), report.getTrafficLightAsString());

    }

    @Test
    public void createReport_set_report_traffic_green_yellow_name_when_defined_by_trafficlight_calculator() throws Exception {
        /* prepare */
        when(trafficLightCalculator.calculateTrafficLight(secHubResult)).thenReturn(TrafficLight.GREEN);

        /* execute */
        ScanReport report = serviceToTest.createReport(context);

        /* test */
        assertNotNull(report);
        assertEquals(TrafficLight.GREEN.name(), report.getTrafficLightAsString());

    }

    @Test
    public void createReport_set_sechub_jobuuid_to_returneed_report() throws Exception {
        /* execute */
        ScanReport report = serviceToTest.createReport(context);

        /* test */
        assertNotNull(report);
        assertEquals(secHubJobUUID, report.getSecHubJobUUID());

    }

    @Test
    public void createReport_calls_sechub_result_service() throws Exception {

        /* execute */
        serviceToTest.createReport(context);

        /* test */
        verify(secHubResultService).createResult(context);
    }

    @Test
    public void createReport_calls_execution_service() throws Exception {

        /* execute */
        serviceToTest.createReport(context);

        /* test */
        verify(reportProductExecutionService).executeProductsAndStoreResults(context);
    }

    @Test
    public void createReport_saves_created_report_by_repository() throws Exception {

        /* execute */
        serviceToTest.createReport(context);

        /* test */
        verify(reportRepository).save(any(ScanReport.class));
    }

    @Test
    public void createReport_returns_saved_report_by_repository() throws Exception {

        /* execute */
        serviceToTest.createReport(context);

        /* test */
        verify(reportRepository).save(any(ScanReport.class));
    }

    @Test
    public void createReport_calls_trafficlight_calculator_with_result() throws Exception {

        /* execute */
        serviceToTest.createReport(context);

        /* test */
        verify(trafficLightCalculator).calculateTrafficLight(secHubResult);
    }

}
