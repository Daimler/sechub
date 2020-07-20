// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.product.checkmarx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.daimler.sechub.adapter.AbstractAdapterConfigBuilder;
import com.daimler.sechub.adapter.AdapterMetaData;
import com.daimler.sechub.adapter.checkmarx.CheckmarxAdapter;
import com.daimler.sechub.adapter.checkmarx.CheckmarxAdapterConfig;
import com.daimler.sechub.adapter.checkmarx.CheckmarxConfig;
import com.daimler.sechub.adapter.checkmarx.CheckmarxMetaDataID;
import com.daimler.sechub.domain.scan.OneInstallSetupConfigBuilderStrategy;
import com.daimler.sechub.domain.scan.TargetRegistry.TargetRegistryInfo;
import com.daimler.sechub.domain.scan.product.AbstractCodeScanProductExecutor;
import com.daimler.sechub.domain.scan.product.ProductExecutorContext;
import com.daimler.sechub.domain.scan.product.ProductIdentifier;
import com.daimler.sechub.domain.scan.product.ProductResult;
import com.daimler.sechub.sharedkernel.MustBeDocumented;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;
import com.daimler.sechub.sharedkernel.metadata.MetaDataInspection;
import com.daimler.sechub.sharedkernel.metadata.MetaDataInspector;
import com.daimler.sechub.sharedkernel.resilience.ResilientActionExecutor;
import com.daimler.sechub.sharedkernel.storage.StorageService;
import com.daimler.sechub.storage.core.JobStorage;

@Service
public class CheckmarxProductExecutor extends AbstractCodeScanProductExecutor<CheckmarxInstallSetup> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckmarxProductExecutor.class);

    @Value("${sechub.adapter.checkmarx.scanresultcheck.period.minutes:-1}")
    @MustBeDocumented(AbstractAdapterConfigBuilder.DOCUMENT_INFO_TIMEOUT)
    private int scanResultCheckPeriodInMinutes;

    @Value("${sechub.adapter.checkmarx.scanresultcheck.timeout.minutes:-1}")
    @MustBeDocumented(AbstractAdapterConfigBuilder.DOCUMENT_INFO_TIMEOUT)
    private int scanResultCheckTimeOutInMinutes;

    @Autowired
    CheckmarxAdapter checkmarxAdapter;

    @Autowired
    CheckmarxInstallSetup installSetup;

    @Autowired
    StorageService storageService;

    @Autowired
    MetaDataInspector scanMetaDataCollector;

    @Autowired
    CheckmarxResilienceConsultant checkmarxResilienceConsultant;

    ResilientActionExecutor<ProductResult> resilientActionExecutor;

    public CheckmarxProductExecutor() {
        /* we create here our own instance - only for this service! */
        this.resilientActionExecutor = new ResilientActionExecutor<>();

    }

    @PostConstruct
    protected void postConstruct() {
        this.resilientActionExecutor.add(checkmarxResilienceConsultant);
    }

    @Override
    protected List<ProductResult> executeWithAdapter(SecHubExecutionContext context, ProductExecutorContext executorContext, CheckmarxInstallSetup setup,
            TargetRegistryInfo data) throws Exception {
        LOG.debug("Trigger checkmarx adapter execution");

        UUID jobUUID = context.getSechubJobUUID();
        String projectId = context.getConfiguration().getProjectId();

        JobStorage storage = storageService.getJobStorage(projectId, jobUUID);

        ProductResult result = resilientActionExecutor.executeResilient(() -> {

            AdapterMetaData metaDataOrNull = executorContext.getCurrentMetaDataOrNull();
            try (InputStream sourceCodeZipFileInputStream = fetchInputStreamIfNecessary(storage, metaDataOrNull)) {

                /* @formatter:off */

					CheckmarxAdapterConfig checkMarxConfig =CheckmarxConfig.builder().
							configure(createAdapterOptionsStrategy(context)).
							configure(new OneInstallSetupConfigBuilderStrategy(setup)).
							setTimeToWaitForNextCheckOperationInMinutes(scanResultCheckPeriodInMinutes).
							setScanResultTimeOutInMinutes(scanResultCheckTimeOutInMinutes).
							setFileSystemSourceFolders(data.getCodeUploadFileSystemFolders()).
							setSourceCodeZipFileInputStream(sourceCodeZipFileInputStream).
							setTeamIdForNewProjects(setup.getTeamIdForNewProjects(projectId)).
							setClientSecret(setup.getClientSecret()).
							setPresetIdForNewProjects(setup.getPresetIdForNewProjects(projectId)).
							setProjectId(projectId).
							setTraceID(context.getTraceLogIdAsString()).
							build();
					/* @formatter:on */

                /* inspect */
                MetaDataInspection inspection = scanMetaDataCollector.inspect(ProductIdentifier.CHECKMARX.name());
                inspection.notice(MetaDataInspection.TRACE_ID, checkMarxConfig.getTraceID());
                inspection.notice("presetid", checkMarxConfig.getPresetIdForNewProjectsOrNull());
                inspection.notice("teamid", checkMarxConfig.getTeamIdForNewProjects());

                /* execute checkmarx by adapter and update product result */
                String xml = checkmarxAdapter.start(checkMarxConfig, executorContext.getCallBack());
                
                ProductResult productResult = executorContext.getCurrentProductResult(); // product result is set by callback
                productResult.setResult(xml);
                
                return productResult;
            }
        });
        return Collections.singletonList(result);

    }

    private InputStream fetchInputStreamIfNecessary(JobStorage storage, AdapterMetaData metaData) throws IOException {
        if (metaData != null && metaData.hasValue(CheckmarxMetaDataID.KEY_FILEUPLOAD_DONE, true)) {
            return null;
        }
        return storage.fetch("sourcecode.zip");
    }

    @Override
    public ProductIdentifier getIdentifier() {
        return ProductIdentifier.CHECKMARX;
    }

    @Override
    protected CheckmarxInstallSetup getInstallSetup() {
        return installSetup;
    }

}
