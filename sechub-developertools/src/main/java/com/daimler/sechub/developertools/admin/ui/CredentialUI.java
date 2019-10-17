// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.daimler.sechub.integrationtest.api.TestAPI;

public class CredentialUI {
	JPasswordField passwordField;
	JTextField useridField;
	JTextField serverField;
	JSpinner serverPortSpinner;
	private JPanel panel;

	public JPanel getPanel() {
		return panel;
	}

	public CredentialUI() {
		String port = ConfigurationSetup.SECHUB_ADMIN_SERVER_PORT.getStringValue("443");
		String server = ConfigurationSetup.SECHUB_ADMIN_SERVER.getStringValueOrFail();
		String userId = ConfigurationSetup.SECHUB_ADMIN_USERID.getStringValueOrFail();
		String apiToken = ConfigurationSetup.SECHUB_ADMIN_APITOKEN.getStringValueOrFail();

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		useridField= new JTextField(userId);
		passwordField= new JPasswordField(apiToken);
		serverField = new JTextField(server);

		serverPortSpinner = new JSpinner(new SpinnerNumberModel());
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(serverPortSpinner);
		editor.getFormat().setGroupingUsed(false);
		serverPortSpinner.setEditor(editor);
		serverPortSpinner.setValue(new Integer(port));

		/* when we run integration test server mode, we use the passwords from integration test super admin */
		if (ConfigurationSetup.isIntegrationTestServerMenuEnabled()) {
			useridField.setText(TestAPI.SUPER_ADMIN.getUserId());
			passwordField.setText(TestAPI.SUPER_ADMIN.getApiToken());
		}

		serverField.setPreferredSize(new Dimension(300,30));
		useridField.setPreferredSize(new Dimension(200,30));
		passwordField.setPreferredSize(new Dimension(200,30));
		serverPortSpinner.setPreferredSize(new Dimension(100,30));

		panel.add(new JLabel("Server:"));
		panel.add(serverField);
		panel.add(new JLabel("Port:"));
		panel.add(serverPortSpinner);
		panel.add(new JLabel("User:"));
		panel.add(useridField);
		panel.add(new JLabel("API-Token:"));
		panel.add(passwordField);

		/* currently there is a bug - changes are not handled . So we disable edit fields etc.*/
		serverField.setEnabled(false);
		useridField.setEnabled(false);
		passwordField.setEnabled(false);
		serverPortSpinner.setEnabled(false);

		serverField.setToolTipText(ConfigurationSetup.SECHUB_ADMIN_SERVER.getSystemPropertyid());
		useridField.setToolTipText(ConfigurationSetup.SECHUB_ADMIN_USERID.getSystemPropertyid());
		passwordField.setToolTipText(ConfigurationSetup.SECHUB_ADMIN_APITOKEN.getSystemPropertyid());
		serverPortSpinner.setToolTipText(ConfigurationSetup.SECHUB_ADMIN_SERVER_PORT.getSystemPropertyid());
	}

	public int getPortNumber() {
		return ((Integer)serverPortSpinner.getValue()).intValue();
	}
}
