package com.farmdashboard;

import com.farmdashboard.boundary.VisualizationPanel;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.entity.ItemContainer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ItemContainer root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        FarmJson.copyInto(root, FarmDataStore.getInstance().get(DataFile.TEST_FARM, NodeData.class));

        // root.findByName("Cow").move(700, 100); // uncomment to see the red out-of-bounds outline

        VisualizationPanel map = new VisualizationPanel();
        map.setRoot(root);
        map.setOnSelect(node -> System.out.println("Selected: " + node.getName()));

        Scene scene = new Scene(new BorderPane(map), 1320, 900);
        scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        stage.setTitle("Farm Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}