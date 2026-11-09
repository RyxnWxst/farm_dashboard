package com.farmdashboard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

//simple skeleton
public class App extends Application {

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new BorderPane(), 1320, 900);
        scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        stage.setTitle("Farm Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
