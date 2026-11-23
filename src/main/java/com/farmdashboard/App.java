package com.farmdashboard;
 
import com.farmdashboard.boundary.Dashboard;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Dashboard.getInstance().show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
