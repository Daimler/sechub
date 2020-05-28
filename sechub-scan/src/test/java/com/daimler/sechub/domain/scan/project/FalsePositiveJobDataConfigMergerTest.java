package com.daimler.sechub.domain.scan.project;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.daimler.sechub.domain.scan.ScanDomainTestFileSupport;
import com.daimler.sechub.domain.scan.Severity;
import com.daimler.sechub.domain.scan.report.ScanReportResult;
import com.daimler.sechub.sharedkernel.type.ScanType;

public class FalsePositiveJobDataConfigMergerTest {

    private static final String TEST_AUTHOR = "author1";
    private FalsePositiveJobDataConfigMerger toTest;

    private FalsePositiveProjectConfiguration config;

    @Before
    public void before() throws Exception {
        toTest = new FalsePositiveJobDataConfigMerger();

        config = new FalsePositiveProjectConfiguration();

    }

    @Test
    public void report_example1_add_one_jobdata_results_in_one_entry_in_config() {
        /* prepare */
        UUID jobUUID = UUID.fromString("f1d02a9d-5e1b-4f52-99e5-401854ccf936");
        
        ScanReportResult scanReportResult = loadScanReport("sechub_result/sechub-report-example1-noscantype.json");

        FalsePositiveJobData falsePositiveJobData = new FalsePositiveJobData();
        falsePositiveJobData.setComment("comment1");
        falsePositiveJobData.setFindingId(2);
        falsePositiveJobData.setJobUUID(jobUUID);
        
        /* execute */
        toTest.addJobDataWithMetaDataToConfig(scanReportResult, config, falsePositiveJobData, TEST_AUTHOR);
        
        /* test */
        List<FalsePositiveEntry> falsePositives = config.getFalsePositives();
        assertNotNull(falsePositives);
        assertEquals(1,falsePositives.size());
        
        FalsePositiveEntry fp1 = falsePositives.iterator().next();
        
        // check given job data contained
        FalsePositiveJobData jobData = fp1.getJobData();
        assertEquals("comment1", jobData.getComment());
        assertEquals(2, jobData.getFindingId());
        assertEquals(jobUUID, jobData.getJobUUID());
        assertEquals(TEST_AUTHOR,  fp1.getAuthor());

        // check meta data fetched and added 
        FalsePositiveMetaData metaData = fp1.getMetaData();
        assertNotNull(metaData);
        assertEquals("SSRF", metaData.getName());
        assertEquals(Severity.MEDIUM, metaData.getSeverity());

        assertEquals(ScanType.CODE_SCAN, metaData.getScanType()); // no scan type defined, but fallback to code scan must work
        FalsePositiveCodeMetaData code = metaData.getCode();
        assertNotNull(code);
        
    }
    
    
    @Test
    public void report_example1_add_job_data_already_contained_does_not_change() {
        /* prepare */
        UUID jobUUID = UUID.fromString("f1d02a9d-5e1b-4f52-99e5-401854ccf936");
        
        ScanReportResult scanReportResult = loadScanReport("sechub_result/sechub-report-example1-noscantype.json");

        FalsePositiveJobData falsePositiveJobData = new FalsePositiveJobData();
        falsePositiveJobData.setComment("comment1");
        falsePositiveJobData.setFindingId(2);
        falsePositiveJobData.setJobUUID(jobUUID);
        
        // first call does setup configuration
        toTest.addJobDataWithMetaDataToConfig(scanReportResult, config, falsePositiveJobData, TEST_AUTHOR);
      
        // now we change the false positive job data
        FalsePositiveJobData falsePositiveJobData2 = new FalsePositiveJobData();
        falsePositiveJobData2.setComment("comment2");
        falsePositiveJobData2.setFindingId(2);
        falsePositiveJobData2.setJobUUID(jobUUID);
        
        /* execute */
        toTest.addJobDataWithMetaDataToConfig(scanReportResult, config, falsePositiveJobData2, TEST_AUTHOR);
        
        /* test */
        List<FalsePositiveEntry> falsePositives = config.getFalsePositives();
        assertNotNull(falsePositives);
        assertEquals(1,falsePositives.size());
        
        FalsePositiveEntry fp1 = falsePositives.iterator().next();
        
        // check given job data contained
        FalsePositiveJobData jobData = fp1.getJobData();
        assertEquals("comment1", jobData.getComment()); // we still have comment1, so no changes
        
    }

    private ScanReportResult loadScanReport(String path) {
        String reportJSON = ScanDomainTestFileSupport.getTestfileSupport().loadTestFile(path);
        return ScanReportResult.fromJSONString(reportJSON);
    }

}
