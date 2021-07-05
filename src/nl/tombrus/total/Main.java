package nl.tombrus.total;

import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    private static final Preferences PREFS   = Preferences.userNodeForPackage(Main.class);
    private static final String      PREFS_X = "window.position.x";
    private static final String      PREFS_Y = "window.position.y";

    public static void main(String[] args) {
        launch(args);
    }

    private static double xOffset;
    private static double yOffset;

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = new FXMLLoader(getClass().getResource("sample.fxml")).load();

        root.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
            savePosition(stage);
        });

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root, 122, 22));
        stage.setAlwaysOnTop(true);
        stage.show();
        retrievePosition(stage);
    }

    private static void savePosition(Stage stage) {
        PREFS.putDouble(PREFS_X, stage.getX());
        PREFS.putDouble(PREFS_Y, stage.getY());
    }

    private static void retrievePosition(Stage stage) {
        stage.setX(PREFS.getDouble(PREFS_X, stage.getX()));
        stage.setY(PREFS.getDouble(PREFS_Y, stage.getY()));

        if (Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())).isEmpty()) {
            stage.centerOnScreen();
        }
    }
}
