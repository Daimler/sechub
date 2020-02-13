// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daimler.sechub.domain.scan.access.ScanDeleteAnyAccessToProjectAtAllService;
import com.daimler.sechub.domain.scan.access.ScanGrantUserAccessToProjectService;
import com.daimler.sechub.domain.scan.access.ScanRevokeUserAccessAtAllService;
import com.daimler.sechub.domain.scan.access.ScanRevokeUserAccessFromProjectService;
import com.daimler.sechub.domain.scan.config.UpdateScanMappingService;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.messaging.AsynchronMessageHandler;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.IsReceivingAsyncMessage;
import com.daimler.sechub.sharedkernel.messaging.MappingMessage;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.messaging.ProjectMessage;
import com.daimler.sechub.sharedkernel.messaging.UserMessage;
import com.daimler.sechub.sharedkernel.usecases.admin.config.UseCaseAdministratorUpdatesMappingConfiguration;

@Component
public class ScanMessageHandler implements AsynchronMessageHandler{


	private static final Logger LOG = LoggerFactory.getLogger(ScanMessageHandler.class);


	@Autowired
	ScanGrantUserAccessToProjectService grantService;

	@Autowired
	ScanRevokeUserAccessFromProjectService revokeUserFromProjectService;

	@Autowired
	ScanRevokeUserAccessAtAllService revokeUserService;

	@Autowired
	ScanDeleteAnyAccessToProjectAtAllService deleteAllProjectAccessService;

	@Autowired
	ProjectDataDeleteService projectDataDeleteService;

	@Autowired
	UpdateScanMappingService updateScanMappingService;

	@Override
	public void receiveAsyncMessage(DomainMessage request) {
		MessageID messageId = request.getMessageId();
		LOG.debug("received domain request: {}", request);

		switch (messageId) {
		case USER_ADDED_TO_PROJECT:
			handleUserAddedToProject(request);
			break;
		case USER_REMOVED_FROM_PROJECT:
			handleUserRemovedFromProject(request);
			break;
		case USER_DELETED:
			handleUserDeleted(request);
			break;
		case PROJECT_DELETED:
			handleProjectDeleted(request);
			break;
		case MAPPING_CONFIGURATION_CHANGED:
            handleMappingConfigurationChanged(request);
            break;
		default:
			throw new IllegalStateException("unhandled message id:"+messageId);
		}
	}

	@IsReceivingAsyncMessage(MessageID.MAPPING_CONFIGURATION_CHANGED)
	@UseCaseAdministratorUpdatesMappingConfiguration(@Step(number=3,name="Event handler",description="Receives mapping configuration change event"))
	private void handleMappingConfigurationChanged(DomainMessage request) {
	    MappingMessage data = request.get(MessageDataKeys.CONFIG_MAPPING_DATA);
	    updateScanMappingService.updateScanMapping(data.getMappingId(),data.getMappingData());
    }

    @IsReceivingAsyncMessage(MessageID.USER_ADDED_TO_PROJECT)
	private void handleUserAddedToProject(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.PROJECT_TO_USER_DATA);
		grantService.grantUserAccessToProject(data.getUserId(),data.getProjectId());
	}

	@IsReceivingAsyncMessage(MessageID.USER_REMOVED_FROM_PROJECT)
	private void handleUserRemovedFromProject(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.PROJECT_TO_USER_DATA);
		revokeUserFromProjectService.revokeUserAccessFromProject(data.getUserId(), data.getProjectId());
	}

	@IsReceivingAsyncMessage(MessageID.USER_DELETED)
	private void handleUserDeleted(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.USER_DELETE_DATA);
		revokeUserService.revokeUserAccess(data.getUserId());
	}

	@IsReceivingAsyncMessage(MessageID.PROJECT_DELETED)
	private void handleProjectDeleted(DomainMessage request) {
		ProjectMessage data = request.get(MessageDataKeys.PROJECT_DELETE_DATA);
		/* first cut access */
		deleteAllProjectAccessService.deleteAnyAccessDataForProject(data.getProjectId());
		/* now delete data */
		projectDataDeleteService.deleteAllDataForProject(data.getProjectId());
	}

}
