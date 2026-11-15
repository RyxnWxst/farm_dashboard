module com.farmdashboard {
    requires javafx.controls;
    requires com.google.gson;

    exports com.farmdashboard;

    // Gson fills NodeData's private fields by reflection
    opens com.farmdashboard.data to com.google.gson;


}
