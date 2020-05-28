// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario4;

import com.daimler.sechub.integrationtest.api.TestProject;
import com.daimler.sechub.integrationtest.api.TestUser;
import com.daimler.sechub.integrationtest.internal.AbstractTestScenario;
import com.daimler.sechub.integrationtest.internal.StaticTestScenario;

/**
 * This is a {@link StaticTestScenario} - please look into details and contract.
 * <br>.
 * In this scenario following is automatically <b>ONE TIME</b> initialized:
 *
 * <pre>
 * PROJECT_1_ is automatically created
 * USER_1, is automatically registered, created and assigned to project1
 * </pre>
 * 
 * <br>
 * Data will NOT be destroyed but reused in all tests!
 *
 * @author Albert Tregnaghi
 *
 */
public class Scenario4 extends AbstractTestScenario implements StaticTestScenario {

	/**
	 * User 1 is registered on startup, also owner and user of {@link #PROJECT_1}
	 */
	public static final TestUser USER_1 = createTestUser(Scenario4.class, "user1");

	/**
	 * Project 1 is created on startup, and has {@link #USER_1} assigned
	 */
	public static final TestProject PROJECT_1 = createTestProject(Scenario4.class, "project1");

	private static boolean initialized;

	@Override
	protected void initializeTestData() {
		/* @formatter:off */
		initializer().
			createUser(USER_1).
			createProject(PROJECT_1, USER_1).
			assignUserToProject(PROJECT_1,USER_1)
			;
		/* @formatter:on */
	}
	
	public boolean isInitializationNecessary() {
	    return ! initialized;
	}
	
	@Override
	protected void waitForTestDataAvailable() {
		/* @formatter:off */
		initializer().
			waitUntilProjectExists(PROJECT_1).

			waitUntilUserExists(USER_1).

			waitUntilUserCanLogin(USER_1)

			;
		
		initialized=true;
		/* @formatter:on */
	}

}
