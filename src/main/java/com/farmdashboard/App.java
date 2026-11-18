requires transitive javafx.controls;

package com.farmdashboard;
 
import com.farmdashboard.boundary.Dashboard;
 
import javafx.application.Application;
import javafx.stage.Stage;

import static javafx.application.Application.launch;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Dashboard.getInstance().show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

        /*
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
} */