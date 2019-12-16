// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.administration.project;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.domain.administration.user.User;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.SecHubEnvironment;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.UserContextService;
import com.daimler.sechub.sharedkernel.logging.AuditLogService;
import com.daimler.sechub.sharedkernel.logging.LogSanitizer;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageFactory;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageService;
import com.daimler.sechub.sharedkernel.messaging.IsSendingAsyncMessage;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.messaging.ProjectMessage;
import com.daimler.sechub.sharedkernel.usecases.admin.project.UseCaseAdministratorDeleteProject;
import com.daimler.sechub.sharedkernel.validation.UserInputAssertion;
@Service
@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
public class ProjectDeleteService {

	@Autowired
	DomainMessageService eventBusService;

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	UserContextService userContext;

	@Autowired
	LogSanitizer logSanitizer;

	@Autowired
	UserInputAssertion assertion;

	@Autowired
	SecHubEnvironment sechubEnvironment;

	@Autowired
	AuditLogService auditLogService;

	@Autowired
	ProjectTransactionService transactionService;

	private static final Logger LOG = LoggerFactory.getLogger(ProjectDeleteService.class);

	@UseCaseAdministratorDeleteProject(@Step(number = 2, name = "Service deletes projects.", next = { 3, 4,
			5, 6, 7}, description = "The service will delete the project with dependencies and triggers asynchronous events"))
	public void deleteProject(String projectId) {
		auditLogService.log("triggers delete of project {}", logSanitizer.sanitize(projectId, 30));

		assertion.isValidProjectId(projectId);

		Project project = projectRepository.findOrFailProject(projectId);

		/* create message containing data before project is deleted */
		ProjectMessage message = new ProjectMessage();
		message.setProjectId(project.getId());
		message.setProjectActionTriggeredBy(userContext.getUserId());

		User owner = project.getOwner();
		if (owner == null) {
			LOG.warn("No owner found for project {} while deleting", project.getId());
		} else {
			message.setProjectOwnerEmailAddress(owner.getEmailAdress());
		}

		for (User user : project.getUsers()) {
			message.addUserEmailAddress(user.getEmailAdress());
		}

		transactionService.deleteWithAssociationsInOwnTransaction(projectId);

		informProjectDeleted(message);
		if (owner != null) {
			sendRefreshUserAuth(owner);
		}

	}

	@IsSendingAsyncMessage(MessageID.PROJECT_DELETED)
	private void informProjectDeleted(ProjectMessage message) {
		DomainMessage infoRequest = new DomainMessage(MessageID.PROJECT_DELETED);
		infoRequest.set(MessageDataKeys.PROJECT_DELETE_DATA, message);
		infoRequest.set(MessageDataKeys.ENVIRONMENT_BASE_URL, sechubEnvironment.getServerBaseUrl());

		eventBusService.sendAsynchron(infoRequest);
	}

	@IsSendingAsyncMessage(MessageID.REQUEST_USER_ROLE_RECALCULATION)
	private void sendRefreshUserAuth(User ownerUser) {
		eventBusService.sendAsynchron(DomainMessageFactory.createRequestRoleCalculation(ownerUser.getName()));
	}
}
