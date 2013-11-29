
package com.joliciel.talismane.terminology.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TerminologyViewer extends Application {
    
    public static void main(String[] args) {
        Application.launch(TerminologyViewer.class, args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
    	FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = (Parent) fxmlLoader.load(getClass().getResource("terminology_viewer.fxml").openStream());
        
        TerminologyViewerController controller = fxmlLoader.getController();
        controller.setPrimaryStage(stage);
        
        stage.setTitle("Talismane Terminology Viewer");
        stage.setScene(new Scene(root, 800, 400));
        stage.show();
    }
}
