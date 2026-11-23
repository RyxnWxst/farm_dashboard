package com.farmdashboard.boundary;

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
//import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Alert;

import com.farmdashboard.control.ComponentData;

public final class InputDialogs {

    private InputDialogs() {}

    public static ComponentData addItemDialog(){
        Dialog<ComponentData> dialog = new Dialog<>();
        dialog.setTitle("Add Item");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField();
        TextField price = new TextField();
        TextField x = new TextField();
        TextField y = new TextField();
        TextField length = new TextField();
        TextField width = new TextField();
        TextField height = new TextField();

        GridPane grid = form(
            "Name", name,
            "Price", price,
            "X", x,
            "Y", y,
            "Length", length,
            "Width", width,
            "Height", height
        );
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    return new ComponentData(name.getText(),
                        Double.parseDouble(price.getText()),
                        Double.parseDouble(x.getText()),
                        Double.parseDouble(y.getText()),
                        Double.parseDouble(length.getText()),
                        Double.parseDouble(width.getText()),
                        Double.parseDouble(height.getText()));   
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter valid numeric values for price, x, y, length, width, and height.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);

    }

    public static String renameDialog(String currentName) {
        Dialog<String> dialog = textDialog("Rename", "Name", currentName);
        return dialog.showAndWait().orElse(null);
    }

    public static Double priceDialog(double currentPrice) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Change Price");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField pf = new TextField(String.valueOf(currentPrice));
        dialog.getDialogPane().setContent(form("Price", pf));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                return Double.parseDouble(pf.getText());
            } catch (NumberFormatException ex) {
                showErrorDialog("Please enter a valid number for the price.");
                return null;
            }
        });
        return dialog.showAndWait().orElse(null);
    }

    public static Double[] locationDialog(double x, double y) {
        Dialog<Double[]> dialog = new Dialog<>();
        dialog.setTitle("Change Location");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField xf = new TextField(String.valueOf(x));
        TextField yf = new TextField(String.valueOf(y));
        dialog.getDialogPane().setContent(form("Location x", xf, "Location y", yf));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                return new Double[]{ Double.parseDouble(xf.getText()),
                                     Double.parseDouble(yf.getText()) };
            } catch (NumberFormatException ex) {
                showErrorDialog("Please enter numbers for x and y.");
                return null;
            }
        });
        return dialog.showAndWait().orElse(null);
    }

    public static Double[] dimensionsDialog(double length, double width, double height) {
        Dialog<Double[]> dialog = new Dialog<>();
        dialog.setTitle("Change Dimensions");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField lf = new TextField(String.valueOf(length));
        TextField wf = new TextField(String.valueOf(width));
        TextField hf = new TextField(String.valueOf(height));
        dialog.getDialogPane().setContent(
            form("Length", lf, "Width", wf, "Height", hf));

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                return new Double[]{ Double.parseDouble(lf.getText()),
                                     Double.parseDouble(wf.getText()),
                                     Double.parseDouble(hf.getText()) };
            } 
            catch (NumberFormatException ex) {
                showErrorDialog("Please enter numbers for length, width and height.");
                return null;
            }
        });
        return dialog.showAndWait().orElse(null);
    }

    public static boolean confirmDeleteDialog(String name, int contentsCount) {
        String msg = contentsCount == 0?
        "Delete \"" + name + "\"?" : "Delete \"" + name + "\" and its " + contentsCount + " contents?";
        return confirm("Delete", msg);
    }

    public static boolean confirmLoadTestFarm() {
        return confirm("Load Test Farm",
            "Load the test farm? This replaces the current farm and saves it.");
    }

    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(bt -> bt == ButtonType.OK).isPresent();
    }

    private static Dialog<String> textDialog(String title, String label, String initial) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField field = new TextField(initial);
        dialog.getDialogPane().setContent(form(label, field));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? field.getText() : null);
        return dialog;
    }

    private static GridPane form(Object... labelFieldPairs) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        for (int i = 0; i < labelFieldPairs.length; i += 2) {
            grid.add(new Label((String) labelFieldPairs[i]), 0, i / 2);
            grid.add((TextField) labelFieldPairs[i + 1], 1, i / 2);
        }
        return grid;
    }
}
