// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.administration.project;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.daimler.sechub.domain.administration.AdministrationAPIConstants;
import com.daimler.sechub.domain.administration.project.ProjectJsonInput.ProjectWhiteList;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.usecases.admin.project.UseCaseAdministratorCreatesProject;
import com.daimler.sechub.sharedkernel.usecases.admin.project.UseCaseAdministratorDeleteProject;
import com.daimler.sechub.sharedkernel.usecases.admin.project.UseCaseAdministratorListsAllProjects;
import com.daimler.sechub.sharedkernel.usecases.admin.project.UseCaseAdministratorShowsProjectDetails;
import com.daimler.sechub.sharedkernel.usecases.admin.user.UseCaseAdministratorAssignsUserToProject;
import com.daimler.sechub.sharedkernel.usecases.admin.user.UseCaseAdministratorUnassignsUserFromProject;
import com.daimler.sechub.sharedkernel.validation.URIValidation;

/**
 * The rest api for user administration done by a super admin.
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
public class ProjectAdministrationRestController {

	@Autowired
	ProjectCreationService creationService;

	@Autowired
	ProjectAssignUserService assignUserToProjectService;

	@Autowired
	ProjectUnassignUserService unassignUserToProjectService;

	@Autowired
	ProjectDeleteService deleteService;

	@Autowired
	ProjectDetailInformationService detailsService;

	@Autowired
	ProjectRepository repository;

	@Autowired
	URIValidation uriValidation;

	@Autowired
	CreateProjectInputValidator validator;

	/* @formatter:off */
	@UseCaseAdministratorCreatesProject(
			@Step(
				number=1,
				name="Rest call",
				needsRestDoc=true,
				description="Administrator creates a new project by calling rest api"))
	@RequestMapping(path = AdministrationAPIConstants.API_CREATE_PROJECT, method = RequestMethod.POST, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	@ResponseStatus(HttpStatus.CREATED)
	public void createProject(@RequestBody @Valid ProjectJsonInput input) {
		Set<URI> whiteListedURIs = new LinkedHashSet<>();
		Optional<ProjectWhiteList> whitelistOption = input.getWhiteList();

		if (whitelistOption.isPresent()) {
			ProjectWhiteList whiteList = whitelistOption.get();
			whiteListedURIs.addAll(whiteList.getUris());
		}

		/* @formatter:on */
		creationService.createProject(input.getName(), input.getDescription(), input.getOwner(), whiteListedURIs);
	}

	/* @formatter:off */
	@UseCaseAdministratorShowsProjectDetails(@Step(number=1,name="Rest call",description="Json returned containing details about project",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_SHOW_PROJECT_DETAILS, method = RequestMethod.GET, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	public ProjectDetailInformation showProjectDetails(@PathVariable(name="projectId") String userId) {
		/* @formatter:on */
		return detailsService.fetchDetails(userId);
	}

	/* @formatter:off */
	@UseCaseAdministratorListsAllProjects(@Step(number=1,name="Rest call",description="All project ids of sechub are returned as json", needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_LIST_ALL_PROJECTS, method = RequestMethod.GET, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	public List<String> listProjects() {
		/* @formatter:on */
		return repository.findAll().stream().map(Project::getId).collect(Collectors.toList());
	}

	/* @formatter:off */
	@UseCaseAdministratorAssignsUserToProject(@Step(number=1,name="Rest call",description="Administrator does call rest API to assign user",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_ASSIGN_USER_TO_PROJECT, method = RequestMethod.POST, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	@ResponseStatus(HttpStatus.CREATED)
	public void assignUserToProject(@PathVariable(name="projectId") String projectId, @PathVariable(name="userId") String userId) {
		/* @formatter:on */
		assignUserToProjectService.assignUserToProject(userId, projectId);
	}

	/* @formatter:off */
	@UseCaseAdministratorUnassignsUserFromProject(@Step(number=1,name="Rest call",description="Administrator does call rest API to unassign user",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_UNASSIGN_USER_TO_PROJECT, method = RequestMethod.DELETE, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	@ResponseStatus(HttpStatus.OK)
	public void unassignUserFromProject(@PathVariable(name="projectId") String projectId, @PathVariable(name="userId") String userId) {
		/* @formatter:on */
		unassignUserToProjectService.unassignUserFromProject(userId, projectId);
	}

	/* @formatter:off */
	@UseCaseAdministratorDeleteProject(@Step(number=1,name="Rest call",description="Project will be deleted",needsRestDoc=true))
	@RequestMapping(path = AdministrationAPIConstants.API_DELETE_PROJECT, method = RequestMethod.DELETE, produces= {MediaType.APPLICATION_JSON_UTF8_VALUE,MediaType.APPLICATION_JSON_VALUE})
	public void deleteProject(@PathVariable(name="projectId") String projectId) {
		/* @formatter:on */
		deleteService.deletProject(projectId);
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}
}
