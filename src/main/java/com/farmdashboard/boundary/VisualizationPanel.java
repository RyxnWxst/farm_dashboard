package com.farmdashboard.boundary;

import java.util.function.Consumer;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * The 800 x 600 map. 1 unit = 1 px, origin top-left, y grows downward.
 * length is drawn along x and width along y; height is not drawn (it is in the tooltip).
 * Parents are drawn before their children, so contents sit on top of their container.
 * The overlay is an empty layer above the map for P5's drone sprite.
 */
public class VisualizationPanel extends StackPane {

    public static final double MAP_WIDTH = 800;
    public static final double MAP_HEIGHT = 600;

    private static final Color CONTAINER_FILL = Color.web("#dbe9f6", 0.6);
    private static final Color ITEM_FILL = Color.web("#cfe8cf");
    private static final Color SELECTED_FILL = Color.web("#ffe08a", 0.85);
    private static final Color OUTLINE = Color.web("#4a6572");
    private static final Color WARNING = Color.web("#d62828");

    private final Pane mapLayer = new Pane();
    private final Pane overlay = new Pane();
    private ItemContainer root;
    private FarmComponent selected;
    private Consumer<FarmComponent> onSelect = node -> { };

    public VisualizationPanel() {
        for (Pane pane : new Pane[] {mapLayer, overlay}) {
            fixSize(pane);
        }
        fixSize(this);
        overlay.setMouseTransparent(true); // clicks go through to the map
        getChildren().addAll(mapLayer, overlay);
        setClip(new Rectangle(MAP_WIDTH, MAP_HEIGHT)); // anything outside Root is cut off
    }

    public void setRoot(ItemContainer root) {
        this.root = root;
        redraw();
    }

    /** Highlights a node. Dashboard calls this from select(); it doesn't fire the callback. */
    public void setSelected(FarmComponent node) {
        this.selected = node;
        redraw();
    }

    /** Called when the farmer clicks a node on the map (Dashboard.select). */
    public void setOnSelect(Consumer<FarmComponent> onSelect) {
        this.onSelect = onSelect;
    }

    /** P5 adds the DroneSprite here. */
    public Pane getOverlay() {
        return overlay;
    }

    /** Redraws the whole farm. Call after every command or file change (Dashboard.refresh). */
    public void redraw() {
        mapLayer.getChildren().clear();
        Rectangle background = new Rectangle(0, 0, MAP_WIDTH, MAP_HEIGHT);
        background.setFill(Color.WHITE);
        background.setStroke(Color.web("#999999"));
        background.setStrokeType(StrokeType.INSIDE);
        background.setOnMouseClicked(e -> choose(root));
        mapLayer.getChildren().add(background);
        if (root != null) {
            for (FarmComponent child : root.getChild()) {
                draw(child);
            }
        }
    }

    private void draw(FarmComponent node) {
        Rectangle box = new Rectangle(node.getLocationX(), node.getLocationY(),
                node.getLength(), node.getWidth());
        boolean outside = node.getParent() != null && !node.isInside(node.getParent());
        boolean isSelected = node == selected;

        box.setFill(isSelected ? SELECTED_FILL : node.isContainer() ? CONTAINER_FILL : ITEM_FILL);
        box.setStroke(outside ? WARNING : OUTLINE);
        box.setStrokeWidth(outside || isSelected ? 3 : 1);
        if (outside) {
            box.getStrokeDashArray().setAll(8.0, 4.0); // the out-of-bounds warning outline
        }
        box.setOnMouseClicked(e -> {
            choose(node);
            e.consume(); // don't also select the container underneath
        });
        Tooltip.install(box, new Tooltip(describe(node, outside)));

        Text label = new Text(node.getLocationX() + 4, node.getLocationY() + 14, node.getName());
        label.setFont(Font.font(11));
        label.setMouseTransparent(true);
        label.setClip(new Rectangle(node.getLocationX(), node.getLocationY(),
                node.getLength(), node.getWidth()));

        mapLayer.getChildren().addAll(box, label);
        for (FarmComponent child : node.getChild()) {
            draw(child);
        }
    }

    private void choose(FarmComponent node) {
        if (node == null) {
            return;
        }
        setSelected(node);
        onSelect.accept(node);
    }

    private static String describe(FarmComponent node, boolean outside) {
        String text = String.format("%s%n$%,.0f (with contents $%,.0f)%nat (%.0f, %.0f), %.0f x %.0f x %.0f",
                node.getName(), node.getPrice(), node.getTotal(),
                node.getLocationX(), node.getLocationY(),
                node.getLength(), node.getWidth(), node.getHeight());
        return outside ? text + "\nWARNING: outside " + node.getParent().getName() : text;
    }

    private static void fixSize(javafx.scene.layout.Region region) {
        region.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        region.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        region.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
    }
}
