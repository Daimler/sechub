// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.daimler.sechub.developertools.admin.ui.action.ActionSupport;
import com.daimler.sechub.developertools.admin.ui.action.adapter.ShowProductExecutorTemplatesDialogAction;
import com.daimler.sechub.developertools.admin.ui.action.client.TriggerSecHubClientSynchronousScanAction;
import com.daimler.sechub.developertools.admin.ui.action.config.CreateExecutionProfileAction;
import com.daimler.sechub.developertools.admin.ui.action.config.CreateExecutorConfigAction;
import com.daimler.sechub.developertools.admin.ui.action.config.DeleteConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.config.DeleteProfileAction;
import com.daimler.sechub.developertools.admin.ui.action.config.EditConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.config.EditExecutionProfileAction;
import com.daimler.sechub.developertools.admin.ui.action.config.ListExecutionProfilesAction;
import com.daimler.sechub.developertools.admin.ui.action.config.ListExecutorConfigurationsAction;
import com.daimler.sechub.developertools.admin.ui.action.developerbatchops.DeveloperBatchCreateCheckmarxTestSetupAction;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.FetchMockMailsAction;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.CreateScenario2TestDataAction;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.CreateScenario3TestDataAction;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.TriggerMassiveNewJobsScenario3User1Action;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.TriggerNewCodeScanJobScenario3User1Action;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.TriggerNewInfraScanJobScenario3User1Action;
import com.daimler.sechub.developertools.admin.ui.action.integrationtestserver.testdata.TriggerNewWebScanJobScenario3User1Action;
import com.daimler.sechub.developertools.admin.ui.action.job.CancelJobAction;
import com.daimler.sechub.developertools.admin.ui.action.job.DownloadFullscanDataForJobAction;
import com.daimler.sechub.developertools.admin.ui.action.job.DownloadHTMLReportForJobAction;
import com.daimler.sechub.developertools.admin.ui.action.job.DownloadJSONReportForJobAction;
import com.daimler.sechub.developertools.admin.ui.action.job.GetJobStatusAction;
import com.daimler.sechub.developertools.admin.ui.action.job.RestartJobAction;
import com.daimler.sechub.developertools.admin.ui.action.job.RestartJobHardAction;
import com.daimler.sechub.developertools.admin.ui.action.job.ShowRunningBatchJobsListAction;
import com.daimler.sechub.developertools.admin.ui.action.other.CheckAliveAction;
import com.daimler.sechub.developertools.admin.ui.action.other.CheckVersionAction;
import com.daimler.sechub.developertools.admin.ui.action.other.FetchGlobalMappingAction;
import com.daimler.sechub.developertools.admin.ui.action.other.UpdateGlobalMappingAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.CheckPDSAliveAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.CheckPDSJobResultOrErrorAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.CheckPDSJobStatusAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.CreateNewPDSExecutionConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.CreatePDSJobAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.FetchPDSConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.FetchPDSJobParameterExampleAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.FetchPDSMonitoringStatusAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.MarkPDSJobReadyAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.ShowPDSConfigurationDialogAction;
import com.daimler.sechub.developertools.admin.ui.action.pds.UploadPDSJobFileAction;
import com.daimler.sechub.developertools.admin.ui.action.project.AssignOwnerToProjectAction;
import com.daimler.sechub.developertools.admin.ui.action.project.AssignProfileToAllProjectsAction;
import com.daimler.sechub.developertools.admin.ui.action.project.AssignProfileToProjectsAction;
import com.daimler.sechub.developertools.admin.ui.action.project.AssignUserToProjectAction;
import com.daimler.sechub.developertools.admin.ui.action.project.AssignUserToProjectMassCSVImportAction;
import com.daimler.sechub.developertools.admin.ui.action.project.CreateOverviewCSVExportAction;
import com.daimler.sechub.developertools.admin.ui.action.project.CreateProjectAction;
import com.daimler.sechub.developertools.admin.ui.action.project.CreateProjectMassCSVImportAction;
import com.daimler.sechub.developertools.admin.ui.action.project.DeleteProjectAction;
import com.daimler.sechub.developertools.admin.ui.action.project.DeleteProjectMassCSVImportAction;
import com.daimler.sechub.developertools.admin.ui.action.project.FetchProjectFalsePositiveConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.project.GetProjectMockConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.project.MarkProjectFalsePositiveAction;
import com.daimler.sechub.developertools.admin.ui.action.project.SetProjectMockDataConfigurationAction;
import com.daimler.sechub.developertools.admin.ui.action.project.ShowProjectDetailAction;
import com.daimler.sechub.developertools.admin.ui.action.project.ShowProjectListAction;
import com.daimler.sechub.developertools.admin.ui.action.project.ShowProjectsScanLogsAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UnassignProfileFromAllProjectsAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UnassignProfileFromProjectsAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UnassignUserFromProjectAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UnassignUserFromProjectMassCSVImportAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UnmarkProjectFalsePositiveAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UpdateProjectDescriptionAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UpdateProjectMetaDataAction;
import com.daimler.sechub.developertools.admin.ui.action.project.UpdateProjectWhitelistAction;
import com.daimler.sechub.developertools.admin.ui.action.scheduler.DisableSchedulerJobProcessingAction;
import com.daimler.sechub.developertools.admin.ui.action.scheduler.EnableSchedulerJobProcessingAction;
import com.daimler.sechub.developertools.admin.ui.action.scheduler.RefreshSchedulerStatusAction;
import com.daimler.sechub.developertools.admin.ui.action.status.CheckStatusAction;
import com.daimler.sechub.developertools.admin.ui.action.status.ListStatusEntriesAction;
import com.daimler.sechub.developertools.admin.ui.action.user.AcceptUserSignupAction;
import com.daimler.sechub.developertools.admin.ui.action.user.AnonymousRequestNewAPITokenUserAction;
import com.daimler.sechub.developertools.admin.ui.action.user.AnonymousSigninNewUserAction;
import com.daimler.sechub.developertools.admin.ui.action.user.CreateUserMassCSVImportAction;
import com.daimler.sechub.developertools.admin.ui.action.user.DeclineUserSignupAction;
import com.daimler.sechub.developertools.admin.ui.action.user.DeleteUserAction;
import com.daimler.sechub.developertools.admin.ui.action.user.ListSignupsAction;
import com.daimler.sechub.developertools.admin.ui.action.user.ShowAdminListAction;
import com.daimler.sechub.developertools.admin.ui.action.user.ShowUserDetailAction;
import com.daimler.sechub.developertools.admin.ui.action.user.ShowUserListAction;
import com.daimler.sechub.developertools.admin.ui.action.user.privileges.GrantAdminRightsToUserAction;
import com.daimler.sechub.developertools.admin.ui.action.user.privileges.RevokeAdminRightsFromAdminAction;
import com.daimler.sechub.domain.scan.product.ProductIdentifier;
import com.daimler.sechub.integrationtest.api.IntegrationTestMockMode;

public class CommandUI {
    private static final ImageIcon EDIT_ROAD_BLACK_ICON = new ImageIcon(CommandUI.class.getResource("/icons/material-io/twotone_edit_road_black_18dp.png"));
    private JPanel panel;
    private JMenuBar menuBar;
    private TrafficLightComponent statusTrafficLight;

    private JProgressBar progressBar;
    private Queue<String> queue = new ConcurrentLinkedQueue<>();
    UIContext context;
    private JToolBar toolBar;
    private CheckStatusAction checkStatusAction;
    private Set<ShowProductExecutorTemplatesDialogAction> showProductExecutorTemplatesDialogActions = new HashSet<>();

    public JPanel getPanel() {
        return panel;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public CommandUI(UIContext context) {
        this.context = context;

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setPreferredSize(new Dimension(400, 30));

        // register product executor template actions
        register(ShowProductExecutorTemplatesDialogActionFactory.createCheckmarxV1Action(context));
        register(ShowProductExecutorTemplatesDialogActionFactory.createPDS_CODESCAN_V1Action(context));
        register(ShowProductExecutorTemplatesDialogActionFactory.createPDS_WEBSCAN_V1Action(context));
        register(ShowProductExecutorTemplatesDialogActionFactory.createPDS_INFRASCAN_V1Action(context));

        panel = new JPanel(new BorderLayout());

        panel.add(progressBar, BorderLayout.EAST);

        checkStatusAction = new CheckStatusAction(context);
        createMainMenu();

        createToolBar();

        /* auto execute check status on startup */
        if (ConfigurationSetup.isCheckOnStartupEnabled()) {

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    checkStatusAction.checkStatusWithoutEvent();
                }
            });
        }
    }

    public TrafficLightComponent getStatusTrafficLight() {
        return statusTrafficLight;
    }

    private void createToolBar() {
        statusTrafficLight = new TrafficLightComponent();

        toolBar = new JToolBar();

        toolBar.add(statusTrafficLight);
        toolBar.add(checkStatusAction);
        toolBar.addSeparator();
        toolBar.add(new CreateProjectAction(context).tooltipUseText());
        toolBar.add(new AcceptUserSignupAction(context).tooltipUseText());
        toolBar.addSeparator();
        toolBar.add(new EnableSchedulerJobProcessingAction(context).tooltipUseText());
        toolBar.add(new DisableSchedulerJobProcessingAction(context).tooltipUseText());
        toolBar.addSeparator();
        toolBar.add(new TriggerSecHubClientSynchronousScanAction(context).tooltipUseText());
        toolBar.addSeparator();
        toolBar.add(new EditExecutionProfileAction(context).tooltipUseText());
        toolBar.add(new EditConfigurationAction(context).tooltipUseText());

    }

    private void createMainMenu() {
        menuBar = new JMenuBar();
        createStatusMenu();
        createJobMenu();
        createProjectMenu();
        createUserMenu();
        createEditMenu();
        createSchedulerMenu();
        createIntegrationTestServerMenu();
        createMassOperationsMenu();

        createConfigMenu();
        createPDSMenu();
        createSecHubClientMenu();
    }

    public void createEditMenu() {
        JMenu mainMenu = new JMenu("Edit");
        ActionSupport support = ActionSupport.getInstance();
        support.apply(mainMenu, support.createDefaultCutCopyAndPastActions());
        menuBar.add(mainMenu);
    }

    public void createConfigMenu() {
        JMenu menu = new JMenu("Config");
        menuBar.add(menu);

        JMenu executorMenu = new JMenu("Executors");
        menu.add(executorMenu);

        add(executorMenu, new CreateExecutorConfigAction(context));
        add(executorMenu, new EditConfigurationAction(context));
        add(executorMenu, new DeleteConfigurationAction(context));
        add(executorMenu, new ListExecutorConfigurationsAction(context));

        menu.addSeparator();
        JMenu profileMenu = new JMenu("Profiles");

        profileMenu.setIcon(EDIT_ROAD_BLACK_ICON);
        menu.add(profileMenu);

        add(profileMenu, new CreateExecutionProfileAction(context));
        add(profileMenu, new EditExecutionProfileAction(context));
        add(profileMenu, new DeleteProfileAction(context));
        add(profileMenu, new ListExecutionProfilesAction(context));
        menu.addSeparator();

        JMenu mappingsMenu = new JMenu("Global mappings");
        menu.add(mappingsMenu);
        add(mappingsMenu,new FetchGlobalMappingAction(context));
        add(mappingsMenu,new UpdateGlobalMappingAction(context));
    }

    private ShowProductExecutorTemplatesDialogAction register(ShowProductExecutorTemplatesDialogAction action) {
        showProductExecutorTemplatesDialogActions.add(action);
        return action;
    }

    public ShowProductExecutorTemplatesDialogAction resolveShowProductExecutorMappingDialogActionOrNull(ProductIdentifier productId, int version) {
        for (ShowProductExecutorTemplatesDialogAction action : showProductExecutorTemplatesDialogActions) {
            if (action.getProductIdentifier().equals(productId) && action.getVersion() == version) {
                return action;
            }
        }
        return null;
    }

    public void createPDSMenu() {
        JMenu menu = new JMenu("PDS");
        menuBar.add(menu);

        add(menu, new ShowPDSConfigurationDialogAction(context));
        menu.addSeparator();
        add(menu, new FetchPDSConfigurationAction(context));
        add(menu, new FetchPDSJobParameterExampleAction(context));
        menu.addSeparator();
        add(menu, new CreateNewPDSExecutionConfigurationAction(context));
        menu.addSeparator();
        add(menu, new CheckPDSAliveAction(context));
        add(menu, new FetchPDSMonitoringStatusAction(context));
        menu.addSeparator();
        add(menu, new CreatePDSJobAction(context));
        add(menu, new UploadPDSJobFileAction(context));
        add(menu, new MarkPDSJobReadyAction(context));
        menu.addSeparator();
        add(menu, new CheckPDSJobStatusAction(context));
        add(menu, new CheckPDSJobResultOrErrorAction(context));
    }

    private void createUserMenu() {
        JMenu menu = new JMenu("User");
        menuBar.add(menu);

        add(menu, new AnonymousSigninNewUserAction(context));
        add(menu, new AcceptUserSignupAction(context));
        menu.addSeparator();
        add(menu, new DeclineUserSignupAction(context));
        add(menu, new DeleteUserAction(context));
        menu.addSeparator();
        add(menu, new ShowUserDetailAction(context));
        add(menu, new AnonymousRequestNewAPITokenUserAction(context));
        menu.addSeparator();
        add(menu, new ListSignupsAction(context));
        add(menu, new ShowUserListAction(context));
        menu.addSeparator();
        add(menu, new AssignOwnerToProjectAction(context));
        menu.addSeparator();
        add(menu, new AssignUserToProjectAction(context));
        add(menu, new UnassignUserFromProjectAction(context));
        menu.addSeparator();

        JMenu adminMenu = new JMenu("Admin rights");
        add(adminMenu, new GrantAdminRightsToUserAction(context));
        add(adminMenu, new RevokeAdminRightsFromAdminAction(context));
        adminMenu.addSeparator();
        add(adminMenu, new ShowAdminListAction(context));
        menu.add(adminMenu);
    }

    private void createProjectMenu() {
        JMenu menu = new JMenu("Project");
        menuBar.add(menu);

        add(menu, new CreateProjectAction(context));
        add(menu, new DeleteProjectAction(context));
        add(menu, new ShowProjectDetailAction(context));
        add(menu, new ShowProjectsScanLogsAction(context));

        menu.addSeparator();
        add(menu, new AssignOwnerToProjectAction(context));
        
        menu.addSeparator();
        add(menu, new AssignUserToProjectAction(context));
        add(menu, new UnassignUserFromProjectAction(context));

        menu.addSeparator();
        add(menu, new ShowProjectListAction(context));

        menu.addSeparator();
        add(menu, new UpdateProjectWhitelistAction(context));
        add(menu, new UpdateProjectMetaDataAction(context));
        
        menu.addSeparator();
        add(menu, new UpdateProjectDescriptionAction(context));

        menu.addSeparator();
        JMenu profiles = new JMenu("Execution profiles");
        profiles.setIcon(EDIT_ROAD_BLACK_ICON);
        add(profiles, new AssignProfileToProjectsAction(context));
        add(profiles, new UnassignProfileFromProjectsAction(context));
        profiles.addSeparator();
        add(profiles, new UnassignProfileFromAllProjectsAction(context));
        add(profiles, new AssignProfileToAllProjectsAction(context));
        menu.add(profiles);

        menu.addSeparator();
        JMenu falsePositives = new JMenu("False positives");
        add(falsePositives, new FetchProjectFalsePositiveConfigurationAction(context));
        add(falsePositives, new UnmarkProjectFalsePositiveAction(context));
        add(falsePositives, new MarkProjectFalsePositiveAction(context));
        menu.add(falsePositives);

        JMenu projectMockData = new JMenu("Mockdata");
        menu.add(projectMockData);

        add(projectMockData, new SetProjectMockDataConfigurationAction(context));
        add(projectMockData, new GetProjectMockConfigurationAction(context));

    }

    private void createStatusMenu() {
        JMenu menu = new JMenu("Status");
        menuBar.add(menu);

        add(menu, new ShowRunningBatchJobsListAction(context));
        menu.addSeparator();
        add(menu, new CheckAliveAction(context));
        add(menu, new CheckVersionAction(context));
        add(menu, new ListStatusEntriesAction(context));
        menu.addSeparator();
        add(menu, new ShowAdminListAction(context));
        add(menu, new CreateOverviewCSVExportAction(context));
    }

    private void createSchedulerMenu() {
        JMenu menu = new JMenu("Scheduler");
        menuBar.add(menu);

        add(menu, new DisableSchedulerJobProcessingAction(context));
        add(menu, new EnableSchedulerJobProcessingAction(context));
        add(menu, new RefreshSchedulerStatusAction(context));
    }

    private void createJobMenu() {
        JMenu menu = new JMenu("Job");
        menuBar.add(menu);
        add(menu, new GetJobStatusAction(context));
        add(menu, new CancelJobAction(context));
        add(menu, new RestartJobAction(context));
        add(menu, new RestartJobHardAction(context));
        menu.addSeparator();
        add(menu, new ShowRunningBatchJobsListAction(context));
        menu.addSeparator();
        add(menu, new DownloadJSONReportForJobAction(context));
        add(menu, new DownloadHTMLReportForJobAction(context));
        menu.addSeparator();
        add(menu, new DownloadFullscanDataForJobAction(context));
    }

    private void createSecHubClientMenu() {
        JMenu menu = new JMenu("Client");
        menuBar.add(menu);
        add(menu, new TriggerSecHubClientSynchronousScanAction(context));
    }

    private void createIntegrationTestServerMenu() {
        JMenu menu = new JMenu("Integration TestServer");
        menuBar.add(menu);
        if (!ConfigurationSetup.isIntegrationTestServerMenuEnabled()) {
            menu.setEnabled(false);
            menu.setToolTipText("Not enabled, use \"-D" + ConfigurationSetup.SECHUB_ENABLE_INTEGRATION_TESTSERVER_MENU.getSystemPropertyid()
                    + "=true\" to enable it and run an integration test server!");
        }
        add(menu, new FetchMockMailsAction(context));
        menu.addSeparator();
        add(menu, new SetProjectMockDataConfigurationAction(context));
        add(menu, new GetProjectMockConfigurationAction(context));
        menu.addSeparator();

        JMenu testDataMenu = new JMenu("Testdata");
        menu.add(testDataMenu);
        add(testDataMenu, new CreateScenario2TestDataAction(context));
        add(testDataMenu, new CreateScenario3TestDataAction(context));
        testDataMenu.addSeparator();
        add(testDataMenu, new TriggerNewInfraScanJobScenario3User1Action(context));
        testDataMenu.addSeparator();
        add(testDataMenu, new TriggerNewWebScanJobScenario3User1Action(context, IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_GREEN__FAST));
        add(testDataMenu, new TriggerNewWebScanJobScenario3User1Action(context, IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_GREEN__LONG_RUNNING));
        
        add(testDataMenu, new TriggerNewWebScanJobScenario3User1Action(context, IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_GREEN__LONG_RUNNING));
        
        
        add(testDataMenu, new TriggerNewWebScanJobScenario3User1Action(context, IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_ONE_FINDING__FAST));
        add(testDataMenu, new TriggerNewWebScanJobScenario3User1Action(context, IntegrationTestMockMode.WEBSCAN__NETSPARKER_MANY_RESULTS__FAST));
        testDataMenu.addSeparator();
        add(testDataMenu, new TriggerNewCodeScanJobScenario3User1Action(context, IntegrationTestMockMode.CODE_SCAN__CHECKMARX__YELLOW__FAST));
        add(testDataMenu, new TriggerNewCodeScanJobScenario3User1Action(context, IntegrationTestMockMode.CODE_SCAN__CHECKMARX__GREEN__FAST));
        testDataMenu.addSeparator();
        add(testDataMenu, new TriggerMassiveNewJobsScenario3User1Action(context));

    }

    private void createMassOperationsMenu() {
        JMenu massOperationsMenu = new JMenu("Mass operations");
        menuBar.add(massOperationsMenu);
        add(massOperationsMenu, new CreateUserMassCSVImportAction(context));
        add(massOperationsMenu, new CreateProjectMassCSVImportAction(context));
        add(massOperationsMenu, new AssignUserToProjectMassCSVImportAction(context));
        massOperationsMenu.addSeparator();
        add(massOperationsMenu, new DeleteProjectMassCSVImportAction(context));
        add(massOperationsMenu, new UnassignUserFromProjectMassCSVImportAction(context));
        massOperationsMenu.addSeparator();

        JMenu developerBatchOperations = new JMenu("Developer batch ops");
        massOperationsMenu.add(developerBatchOperations);
        developerBatchOperations.add(new DeveloperBatchCreateCheckmarxTestSetupAction(context));

    }

    private void add(JMenu menu, AbstractAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        menu.add(menuItem);

    }

    public void startActionProgress(String threadName) {
        queue.add(threadName);
        handleQueueState();
    }

    public void stopActionProgress(String threadName) {
        queue.remove(threadName);
        handleQueueState();
    }

    private void handleQueueState() {
        boolean showProgress = !queue.isEmpty();
        progressBar.setIndeterminate(showProgress);
        context.getGlassPaneUI().block(showProgress);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

}
