// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.administration.project;

import static com.daimler.sechub.test.TestURLBuilder.*;
import static com.daimler.sechub.test.TestURLBuilder.RestDocPathParameter.*;
//import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;

import com.daimler.sechub.domain.administration.project.ProjectJsonInput.ProjectMetaData;
import com.daimler.sechub.sharedkernel.Profiles;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.configuration.AbstractAllowSecHubAPISecurityConfiguration;
import com.daimler.sechub.test.TestPortProvider;

@RunWith(SpringRunner.class)
@WebMvcTest(ProjectAdministrationRestController.class)
@ContextConfiguration(classes = { ProjectAdministrationRestController.class,
		ProjectAdministrationRestControllerMockTest.SimpleTestConfiguration.class })
@WithMockUser(authorities = RoleConstants.ROLE_SUPERADMIN)
@ActiveProfiles({Profiles.TEST, Profiles.ADMIN_ACCESS})
public class ProjectAdministrationRestControllerMockTest {

	private static final int PORT_USED = TestPortProvider.DEFAULT_INSTANCE.getWebMVCTestHTTPSPort();

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	ProjectCreationService creationService;

	@MockBean
	ProjectAssignUserService assignUserService;

	@MockBean
	ProjectDeleteService projectDeleteService;

	@MockBean
	ProjectUnassignUserService unassignUserService;

	@MockBean
	ProjectDetailInformationService detailService;

	@MockBean
	ProjectRepository mockedProjectRepository;

	@MockBean
	CreateProjectInputValidator createProjectInputvalidator;

	@Before
	public void before() {
		when(createProjectInputvalidator.supports(ProjectJsonInput.class)).thenReturn(true);
	}

	@Test
	public void when_admin_tries_to_list_all_projects_all_2_projects_from_repo_are_returned_in_string_array() throws Exception {
		/* prepare */
		List<Project> list = new ArrayList<>();
		Project project1 = new Project();
		project1.id="project1";
		Project project2 = new Project();
		project2.id="project2";
		list.add(project1);
		list.add(project2);
		when(mockedProjectRepository.findAll()).thenReturn(list);

		/* execute + test @formatter:off */
        this.mockMvc.perform(
        		get(https(PORT_USED).buildAdminListsProjectsUrl()).
        		contentType(MediaType.APPLICATION_JSON_VALUE)
        		).
        			andExpect(status().isOk()).
        			andExpect(jsonPath("$.[0]", CoreMatchers.equalTo("project1"))).
        			andExpect(jsonPath("$.[1]", CoreMatchers.equalTo("project2"))
        		);

		/* @formatter:on */
	}

	@Test
	public void when_validator_marks_no_errors___calling_create_project_url_calls_create_service_and_returns_http_200() throws Exception {

		/* execute + test @formatter:off */
        this.mockMvc.perform(
        		post(https(PORT_USED).buildAdminCreatesProjectUrl()).
        		contentType(MediaType.APPLICATION_JSON_VALUE).

        		content("{\"name\":\"projectId1\",\"description\":\"description1\",\"owner\":\"ownerName1\",\"whiteList\":{\"uris\":[\"192.168.1.1\",\"192.168.1.2\"]}}")
        		).
        			andExpect(status().isCreated()
        		);

		verify(creationService).
			createProject("projectId1","description1","ownerName1", new LinkedHashSet<>(Arrays.asList(new URI("192.168.1.1"), new URI("192.168.1.2"))), new ProjectMetaData());
		/* @formatter:on */
	}

	@Test
	public void when_validator_marks_errors___calling_create_project_url_never_calls_create_service_but_returns_http_400() throws Exception {
		/* prepare */
		doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Errors errors = invocation.getArgument(1);
                errors.reject("testerror");
                return null;
            }
        }).when(createProjectInputvalidator).validate(any(ProjectJsonInput.class), any(Errors.class));


		/* execute + test @formatter:off */
		  this.mockMvc.perform(
	        		post(https(PORT_USED).buildAdminCreatesProjectUrl()).
	        		contentType(MediaType.APPLICATION_JSON_VALUE)
	        		).
	        			andExpect(status().isBadRequest()
	        		);


		  verifyNoInteractions(creationService);
		/* @formatter:on */
	}

	@Test
	public void delete_project_calls_delete_service() throws Exception {

		/* execute + test @formatter:off */
		this.mockMvc.perform(
				delete(https(PORT_USED).buildAdminDeletesProject(PROJECT_ID.pathElement()),"projectId1").
				contentType(MediaType.APPLICATION_JSON_VALUE)
				).
		andExpect(status().isOk());

		/* @formatter:on */
		verify(projectDeleteService).deleteProject("projectId1");
	}

	@TestConfiguration
	@Profile(Profiles.TEST)
	@EnableAutoConfiguration
	public static class SimpleTestConfiguration extends AbstractAllowSecHubAPISecurityConfiguration {

	}

}
