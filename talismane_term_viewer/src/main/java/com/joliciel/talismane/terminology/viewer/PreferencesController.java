/*
 * Copyright (c) 2011, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.joliciel.talismane.terminology.viewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@SuppressWarnings("restriction")
public class PreferencesController {
	@FXML
	private TextField txtEditor;
	@FXML
	private TextField txtArguments;
	@FXML
	private TextField txtDatabaseURL;
	@FXML
	private TextField txtDatabaseUsername;
	@FXML
	private PasswordField txtDatabasePassword;
	@FXML
	private TextField txtDatabaseProjectCode;
	@FXML
	private TextField txtCSVSeparator;

	Stage primaryStage = null;
	File iniFile = null;
	TerminologyViewerController primaryController = null;

	@FXML
	protected void initialize() throws Exception {
		txtCSVSeparator.setText(",");
		String currentDirPath = System.getProperty("user.dir");
		File confDir = new File(currentDirPath + "/conf/");
		confDir.mkdirs();
		iniFile = new File(confDir, "talismane_terminology_viewer.ini");
		if (!iniFile.exists())
			iniFile.createNewFile();
		else {
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(iniFile),
					"UTF-8")));

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!line.startsWith("#")) {
					int equalsPos = line.indexOf('=');
					String parameter = line.substring(0, equalsPos);
					String value = line.substring(equalsPos + 1);
					if (parameter.equals("editor")) {
						txtEditor.setText(value);
					} else if (parameter.equals("arguments")) {
						txtArguments.setText(value);
					} else if (parameter.equals("jdbc.url")) {
						txtDatabaseURL.setText(value);
					} else if (parameter.equals("jdbc.username")) {
						txtDatabaseUsername.setText(value);
					} else if (parameter.equals("jdbc.password")) {
						txtDatabasePassword.setText(value);
					} else if (parameter.equals("project.code")) {
						txtDatabaseProjectCode.setText(value);
					} else if (parameter.equals("csvSeparator")) {
						txtCSVSeparator.setText(value);
					}
				}
			}
			scanner.close();
		}
	}

	@FXML
	protected void btnBrowse_onClick(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Executable files (*.exe)", "*.exe");
		fileChooser.getExtensionFilters().add(extFilter);
		FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All files (*)", "*");
		fileChooser.getExtensionFilters().add(allFilter);

		String currentDir = System.getProperty("user.dir");
		fileChooser.setInitialDirectory(new File(currentDir));

		// Show save file dialog
		File file = fileChooser.showOpenDialog(primaryStage);
		if (file != null) {
			txtEditor.setText(file.getPath());
		}
	}

	@FXML
	protected void btnOK_onClick(ActionEvent event) throws Exception {
		iniFile.delete();
		iniFile.createNewFile();
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(iniFile), "UTF-8"));
		writer.write("editor=" + txtEditor.getText() + "\n");
		writer.write("arguments=" + txtArguments.getText() + "\n");
		writer.write("jdbc.url=" + txtDatabaseURL.getText() + "\n");
		writer.write("jdbc.username=" + txtDatabaseUsername.getText() + "\n");
		writer.write("jdbc.password=" + txtDatabasePassword.getText() + "\n");
		writer.write("project.code=" + txtDatabaseProjectCode.getText() + "\n");
		writer.write("csvSeparator=" + txtCSVSeparator.getText() + "\n");
		writer.flush();
		writer.close();
		primaryController.setEditor(txtEditor.getText());
		primaryController.setArguments(txtArguments.getText());
		primaryController.setDatabaseURL(txtDatabaseURL.getText());
		primaryController.setDatabaseUsername(txtDatabaseUsername.getText());
		primaryController.setDatabasePassword(txtDatabasePassword.getText());
		primaryController.setProjectCode(txtDatabaseProjectCode.getText());
		primaryController.setCsvSeparator(txtCSVSeparator.getText());
		primaryStage.close();
	}

	@FXML
	protected void btnCancel_onClick(ActionEvent event) {
		primaryStage.close();
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	public TerminologyViewerController getPrimaryController() {
		return primaryController;
	}

	public void setPrimaryController(TerminologyViewerController primaryController) {
		this.primaryController = primaryController;
	}

}
