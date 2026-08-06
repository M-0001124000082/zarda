package com.mycompany.cafe;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class cafee extends Application {

    @Override
    public void start(Stage primaryStage) {

        LoginScreen loginScreen = new LoginScreen(primaryStage);

        Scene scene = new Scene(loginScreen.getView(), 900, 600);

        primaryStage.setTitle("Cafe ZERDAK System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
