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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.Term;
import com.joliciel.talismane.terminology.TerminologyBase;
import com.joliciel.talismane.terminology.TerminologyService;
import com.joliciel.talismane.terminology.TerminologyServiceLocator;
import com.joliciel.talismane.utils.CSVFormatter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
 
public class TerminologyViewerController {
	private static final Log LOG = LogFactory.getLog(TerminologyViewerController.class);
	private static CSVFormatter CSV = new CSVFormatter();
	
    @FXML private TableView<Term> tblTerms;
    @FXML private TableView<Context> tblContexts;
    @FXML private TextField txtMinFrequency;
    @FXML private TextField txtSearch;
    @FXML private TextField txtTop;
    @FXML private TextField txtMaxLexicalWords;
    @FXML private Button btnReload;
    @FXML private CheckBox chkMarked;
    @FXML private CheckBox chkExpansions;
    @FXML private Label lblTermCount;
   
    TerminologyBase terminologyBase = null;
    Stage primaryStage = null;
    String editor = null;
    String arguments = null;
    String databaseURL = null;
    String databaseUsername = null;
    String databasePassword = null;
    String projectCode = null;
    String csvSeparator = ",";
    
    LinkedList<TermTableDefinition> termTableHistory = new LinkedList<TermTableDefinition>();
    int currentHistoryIndex = -1;
    
    @FXML protected void initialize() throws Exception {
        String currentDirPath = System.getProperty("user.dir");
        File confDir = new File(currentDirPath + "/conf/");
        confDir.mkdirs();
        File iniFile = new File(confDir, "talismane_terminology_viewer.ini");
        if (!iniFile.exists())
        	iniFile.createNewFile();
        else {
    		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(iniFile), "UTF-8")));

        	while (scanner.hasNextLine()) {
        		String line = scanner.nextLine();
        		if (!line.startsWith("#")) {
        			int equalsPos = line.indexOf('=');
        			String parameter = line.substring(0, equalsPos);
        			String value = line.substring(equalsPos+1);
        			if (parameter.equals("editor")) {
        				editor = value;
        			} else if (parameter.equals("arguments")) {
        				arguments = value;
        			} else if (parameter.equals("jdbc.url")) {
        				databaseURL = value;
        			} else if (parameter.equals("jdbc.username")) {
        				databaseUsername = value;
        			} else if (parameter.equals("jdbc.password")) {
        				databasePassword = value;
        			} else if (parameter.equals("project.code")) {
        				projectCode = value;
        			} else if (parameter.equals("csvSeparator")) {
        				csvSeparator = value;
        			}
        		}
        	}
        	scanner.close();
        }
    }
    
    @FXML protected void handleMenuFileDatabaseAction(ActionEvent event) {
    	String sessionId="";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);

    	TerminologyServiceLocator terminologyServiceLocator = TerminologyServiceLocator.getInstance(locator);
    	TerminologyService terminologyService = terminologyServiceLocator.getTerminologyService();
    	Properties props = new Properties();
    	props.put("jdbc.driverClassName", "org.postgresql.Driver");
    	props.put("jdbc.url", databaseURL);
    	props.put("jdbc.username", databaseUsername);
    	props.put("jdbc.password", databasePassword);
    	terminologyBase = terminologyService.getPostGresTerminologyBase(projectCode, props);
    	this.onNewTermingologyBase();
    }
    
    private void onNewTermingologyBase() {
    	int minFrequency = 5;
    	try {
    		minFrequency = Integer.parseInt(txtMinFrequency.getText());
    	} catch (NumberFormatException nfe) {
    		// do nothing
    	}
    	List<Term> terms = terminologyBase.findTerms(minFrequency, null, 0, null, null);
 
    	tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(terms)));
		this.pushTermTable();
    	btnReload.setDisable(false);
    }
    
    private Collection<Term> wrapTerms(Collection<Term> terms) {
       	List<Term> termWrappers = new ArrayList<Term>(terms.size());
    	for (Term term : terms) {
    		TermWrapper termWrapper = new TermWrapper(term);
    		termWrappers.add(termWrapper);
    	}
    	return termWrappers;
    }
    
    @FXML protected void handleLimitButtonAction(ActionEvent event) {
       	int maxCount = -1;
       	String topStr = txtTop.getText();
       	try {
       		maxCount = Integer.parseInt(topStr);
       	} catch (NumberFormatException e) {
    		this.showAlert("Top must be a number.");
       	}
       	
		this.setSelectedTerm();
		ObservableList<Term> terms = tblTerms.getItems();
		
		List<Term> limitedTerms = new ArrayList<Term>(maxCount);
		int i = 0;
		for (Term term : terms) {
			if (i==maxCount)
				break;
			limitedTerms.add(term);
			i++;
		}
		tblTerms.setItems(FXCollections.observableArrayList(limitedTerms));
		this.pushTermTable();
   }
    
    @FXML protected void handleReloadButtonAction(ActionEvent event) {
    	boolean haveCriteria = false;
    	int minFrequency = -1;
    	if (txtMinFrequency.getText().trim().length()>0) {
	    	try {
	    		minFrequency = Integer.parseInt(txtMinFrequency.getText());
	    		haveCriteria = true;
	    	} catch (NumberFormatException nfe) {
	    		this.showAlert("Min Frequency must be a number.");
	    		return;
	    	}
    	}
    	
    	int maxLexicalWords = -1;
    	if (txtMaxLexicalWords.getText().trim().length()>0) {
	    	try {
	    		maxLexicalWords = Integer.parseInt(txtMaxLexicalWords.getText());
	    		haveCriteria = true;
	    	} catch (NumberFormatException nfe) {
	    		this.showAlert("Max lex words must be a number.");
	    		return;
	    	}
    	}
    	
    	String searchText = txtSearch.getText();

    	boolean marked = chkMarked.isSelected();
    	boolean markedExpansions = chkExpansions.isSelected();
    	
    	haveCriteria = minFrequency>0 || maxLexicalWords>0 || searchText.length()>0 || marked;

    	if (haveCriteria) {
			this.setSelectedTerm();
	    	List<Term> terms = terminologyBase.findTerms(minFrequency, searchText, maxLexicalWords, marked, markedExpansions);    	
	    	tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(terms)));
			this.pushTermTable();
    	} else {
    		this.showAlert("No selection criteria entered.");
    	}

    }
    
    @FXML protected void handleSearchButtonAction(ActionEvent event) {
    	String searchText = txtSearch.getText();
    	if (searchText.length()>0) {
    		this.setSelectedTerm();
    		List<Term> terms = terminologyBase.findTerms(0, searchText, 0, null, null);  	
	    	tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(terms)));
    		this.pushTermTable();
    	} else {
    		this.showAlert("Search text is too short.");
    	}
    }
    
    @FXML protected void handleMarkedButtonAction(ActionEvent event) {
		this.setSelectedTerm();
		List<Term> terms = terminologyBase.findTerms(0, null, 0, true, false);
    	tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(terms)));
		this.pushTermTable();
    }
    
    @FXML protected void handleHeadsButtonAction(ActionEvent event) {
    	Term term = tblTerms.getSelectionModel().getSelectedItem();
    	if (term!=null) {
    		this.setSelectedTerm();
        	if (term instanceof TermWrapper) {
        		term = ((TermWrapper) term).getWrappedTerm();
        	}
        	Set<Term> heads = terminologyBase.getHeads(term);
    		tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(heads)));
    		this.pushTermTable();
    	}
    }
    
    @FXML protected void handleExpansionsButtonAction(ActionEvent event) {
    	this.doExpansions();
    }
    
    void doExpansions() {
       	Term term = tblTerms.getSelectionModel().getSelectedItem();
    	if (term!=null) {
    		this.setSelectedTerm();
        	if (term instanceof TermWrapper) {
        		term = ((TermWrapper) term).getWrappedTerm();
        	}
    		Set<Term> expansions = terminologyBase.getExpansions(term);
    		tblTerms.setItems(FXCollections.observableArrayList(this.wrapTerms(expansions)));
    		this.pushTermTable();
    	}   	
    }
    
    @FXML protected void handleMarkButtonAction(ActionEvent event) {
    	this.markTerm();
    }
    
    void markTerm() {
      	Term term = tblTerms.getSelectionModel().getSelectedItem();
    	if (term!=null) {
    		boolean marked = !term.isMarked();
    		term.setMarked(marked);
    		if (term instanceof TermWrapper) {
    			terminologyBase.storeTerm(((TermWrapper) term).getWrappedTerm());
    		} else {
    			terminologyBase.storeTerm(term);
    		}
    	}    	   	
    }
    
 
    @FXML protected void handleExportButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        
        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All files (*)", "*");
        fileChooser.getExtensionFilters().add(allFilter);
        
        String currentDir = System.getProperty("user.dir");
        fileChooser.setInitialDirectory(new File(currentDir));
        
        //Show save file dialog
        File file = fileChooser.showSaveDialog(primaryStage);
        if(file != null){
        	try {
        		CSV.setAddQuotesAlways(true);
        		CSV.setCsvSeparator(csvSeparator);
	        	Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
	           	ObservableList<Term> terms = tblTerms.getItems();
	           	writer.append(CSV.format("Term")
	           			+ CSV.format("Frequency")
	           			+ CSV.format("Expansions")
	           			+ CSV.format("Contexts")
	           			+ "\n");
	           	for (Term term : terms) {
	               	if (term instanceof TermWrapper) {
	            		term = ((TermWrapper) term).getWrappedTerm();
	            	}
	               	writer.append(CSV.format(term.getText()));
	               	writer.append(CSV.format(term.getFrequency()));
	               	writer.append(CSV.format(term.getExpansionCount()));
	               	StringBuilder sb = new StringBuilder();
	               	for (Context context : terminologyBase.getContexts(term)) {
	               		sb.append(context.getTextSegment() + "|");
	               	}
	               	writer.append(CSV.format(sb.toString()));
	               	writer.append("\n");
	               	writer.flush();
	           	}
	           	writer.close();
        	} catch (IOException ioe) {
        		throw new RuntimeException(ioe);
        	}
        }

    }
    @FXML protected void tblTerms_OnMouseClicked(MouseEvent mouseEvent) {
    	if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
    		this.refreshContexts();
        }
    }
    
    @FXML protected void tblTerms_OnKeyPressed(KeyEvent keyEvent) {
    	if (keyEvent.isControlDown()) {
//    		LOG.debug("keyEvent.getCharacter(): " + keyEvent.getCharacter());
//    		LOG.debug("keyEvent.getCode().getName(): " + keyEvent.getCode().getName());
    		if (keyEvent.getCode().getName().equals("C")) {
    			final Clipboard clipboard = Clipboard.getSystemClipboard();
    			final ClipboardContent content = new ClipboardContent();
    			Term term = tblTerms.getSelectionModel().getSelectedItem();
    			content.putString(term.getText());
    			clipboard.setContent(content);
    		}
    	} else if (keyEvent.getCode().getName().equalsIgnoreCase("M")) {
			this.markTerm();
    	} else if (keyEvent.getCode().getName().equalsIgnoreCase("E")) {
        	this.doExpansions();   		
    	} else if (keyEvent.getCode().getName().equalsIgnoreCase("B")) {
    		this.doBack();
    	} else if (keyEvent.getCode().getName().equalsIgnoreCase("F")) {
    		this.doForward();
    	}
    }
    
    @FXML protected void tblContexts_OnKeyPressed(KeyEvent keyEvent) {
    	if (keyEvent.isControlDown()) {
    		if (keyEvent.getCode().getName().equals("C")) {
    			final Clipboard clipboard = Clipboard.getSystemClipboard();
    			final ClipboardContent content = new ClipboardContent();
    			Context context = tblContexts.getSelectionModel().getSelectedItem();
    			content.putString(context.getTextSegment());
    			clipboard.setContent(content);
    		}
    	}
    }
    
    @FXML protected void tblContexts_OnMouseClicked(MouseEvent mouseEvent) throws Exception {
      	if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
            if(mouseEvent.getClickCount() == 2){
            	if (editor!=null && arguments!=null) {
                   	Context context = tblContexts.getSelectionModel().getSelectedItem();
                   	if (context!=null) {
                   		String argumentString = arguments;
                   		argumentString = argumentString.replace("%file", context.getFileName());
                   		argumentString = argumentString.replace("%line", "" + context.getLineNumber());
                   		argumentString = argumentString.replace("%column", "" + context.getColumnNumber());
                   		String command = "\"" + editor + "\"" + argumentString;
                   		LOG.debug(command);
                   		Runtime.getRuntime().exec(command);
                   	}
            	}
            }
        }
    }

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}
    
	private void showAlert(String text) {
  		Stage dialogStage = new Stage();
		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.setScene(new Scene(VBoxBuilder.create().
		    children(new Text(text), new Button("Ok")).
		    alignment(Pos.CENTER).padding(new Insets(5)).build()));
		dialogStage.show();
	}
	
    @FXML protected void handleMenuSettingsPreferences(ActionEvent event) throws Exception {
  		Stage preferencesStage = new Stage();
		preferencesStage.initModality(Modality.WINDOW_MODAL);
    	FXMLLoader fxmlLoader = new FXMLLoader();
    	URL fxmlURL = TerminologyViewerController.class.getResource("resources/preferences.fxml");
        Parent root = (Parent) fxmlLoader.load(fxmlURL.openStream());
        
        PreferencesController controller = fxmlLoader.getController();
        controller.setPrimaryStage(preferencesStage);
        controller.setPrimaryController(this);
        
        preferencesStage.setTitle("Talismane Terminology Viewer Preferences");
        preferencesStage.setScene(new Scene(root, 400, 300));
        preferencesStage.show();
    }
    
	public String getEditor() {
		return editor;
	}
	public void setEditor(String editor) {
		this.editor = editor;
	}
	public String getArguments() {
		return arguments;
	}
	public void setArguments(String arguments) {
		this.arguments = arguments;
	}
	public String getDatabaseURL() {
		return databaseURL;
	}
	public void setDatabaseURL(String databaseURL) {
		this.databaseURL = databaseURL;
	}
	public String getDatabaseUsername() {
		return databaseUsername;
	}
	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
	}
	public String getDatabasePassword() {
		return databasePassword;
	}
	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
	}
	public String getProjectCode() {
		return projectCode;
	}
	public void setProjectCode(String projectCode) {
		this.projectCode = projectCode;
	}

    public String getCsvSeparator() {
		return csvSeparator;
	}

	public void setCsvSeparator(String csvSeparator) {
		this.csvSeparator = csvSeparator;
	}

	@FXML protected void handleBackButtonAction(ActionEvent event) {
		this.doBack();
    }
	
	void doBack() {
	   	if (currentHistoryIndex>0) {
    		this.setSelectedTerm();
    		currentHistoryIndex--;
    		TermTableDefinition def = termTableHistory.get(currentHistoryIndex);
    		def.apply(tblTerms);
    		lblTermCount.setText("" + tblTerms.getItems().size());
    		this.refreshContexts();
    	}
	}

    @FXML protected void handleForwardButtonAction(ActionEvent event) {
    	this.doForward();
    }
    
    void doForward() {
    	if (currentHistoryIndex<termTableHistory.size()-1) {
    		this.setSelectedTerm();
    		currentHistoryIndex++;
       		TermTableDefinition def = termTableHistory.get(currentHistoryIndex);
    		def.apply(tblTerms);
    		lblTermCount.setText("" + tblTerms.getItems().size());
    		this.refreshContexts();
    	}
    }
    
    @FXML protected void handleScrollButtonAction(ActionEvent event) {
    	int selectedIndex = tblTerms.getSelectionModel().getSelectedIndex();
    	tblTerms.scrollTo(selectedIndex);
    	tblTerms.requestFocus();
    }
    
    void refreshContexts() {
    	Term term = tblTerms.getSelectionModel().getSelectedItem();
    	if (term!=null) {
        	if (term instanceof TermWrapper) {
        		term = ((TermWrapper) term).getWrappedTerm();
        	}
        	List<Context> contexts = new ArrayList<Context>();
        	if (term!=null) {
    	    	contexts = terminologyBase.getContexts(term);
        	}
    		tblContexts.setItems(FXCollections.observableArrayList(contexts));
    	}
    }
    
	void pushTermTable() {
		lblTermCount.setText("" + tblTerms.getItems().size());
		for (int i = currentHistoryIndex + 1; i<termTableHistory.size(); i++) {
			termTableHistory.removeLast();
		}
		
		TermTableDefinition def = new TermTableDefinition(tblTerms);
		termTableHistory.addLast(def);
		if (termTableHistory.size()>10)
			termTableHistory.removeFirst();
		currentHistoryIndex = termTableHistory.size() - 1;
	}
	
	void setSelectedTerm() {
		if (currentHistoryIndex>=0 && currentHistoryIndex<termTableHistory.size()) {
			TermTableDefinition def = termTableHistory.get(currentHistoryIndex);
			def.update(tblTerms);
		}
//		tblTerms.getSelectionModel().select(-1);
//		tblTerms.scrollTo(0);
	}
	
	private static final class TermTableDefinition {
		private ObservableList<Term> terms;
		private int selectedIndex;
		
		public TermTableDefinition(TableView<Term> tblTerms) {
			this.terms = tblTerms.getItems();
			this.selectedIndex = tblTerms.getSelectionModel().getSelectedIndex();
		}
		
		public void update(TableView<Term> tblTerms) {
			this.selectedIndex = tblTerms.getSelectionModel().getSelectedIndex();
		}
		
		public void apply(final TableView<Term> tblTerms) {
			tblTerms.setItems(null);
	       	tblTerms.layout();
			tblTerms.setItems(terms);
			
			tblTerms.getSelectionModel().select(selectedIndex);
        	
        	tblTerms.requestFocus();
        	tblTerms.layout();

			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	if (LOG.isTraceEnabled())
                		LOG.trace("Scrolling to " + selectedIndex);
                	tblTerms.scrollTo(selectedIndex);	
                }
			});
		}

		@Override
		public String toString() {
			return "TermTableDef[" + terms.size() + "," + selectedIndex + "]";
		}

	}
    
}
