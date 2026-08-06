package com.mycompany.cafe;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class LoginScreen {

    private VBox root;
    private Stage stage;

    private static final String DB_URL = "jdbc:sqlite:cafe_zerdak_db.db";

    public LoginScreen(Stage stage) {
        this.stage = stage;
        createUI();
    }

    private void createUI() {
        root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color:#F5F5F5;");

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setPrefWidth(350);
        card.setStyle(
                "-fx-background-color:white;"
                + "-fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,#999,15,0,0,5);"
        );

        Label logo = new Label("☕");
        logo.setFont(Font.font(50));

        Label title = new Label("Cafe زرده");
        title.setFont(Font.font("Arial", 30));
        title.setTextFill(Color.web("#8B5E3C"));

        Label welcome = new Label("Welcome Back");
        welcome.setFont(Font.font(18));

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setPrefHeight(40);
        username.setMaxWidth(250);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setPrefHeight(40);
        password.setMaxWidth(250);

        Button login = new Button("Login");
        login.setPrefWidth(250);
        login.setPrefHeight(40);
        login.setStyle(
                "-fx-background-color:#8B5E3C;"
                + "-fx-text-fill:white;"
                + "-fx-font-size:16;"
                + "-fx-background-radius:10;"
        );

        login.setOnAction(e -> {
            String user = username.getText().trim();
            String pass = password.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                showAlert("تنبيه", "يرجى إدخال اسم المستخدم وكلمة المرور");
                return;
            }

            String sql = "SELECT role FROM users WHERE username = ? AND password = ?";

            try {

                Class.forName("org.sqlite.JDBC");

                try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, user);
                    pstmt.setString(2, pass);

                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        String role = rs.getString("role");

                        Dashboard dashboard = new Dashboard(stage, role);

                        if ("admin".equalsIgnoreCase(role)) {
                            stage.setScene(new Scene(dashboard.getView(), 900, 600));
                        } else {
                            stage.setScene(new Scene(dashboard.getView(), 1000, 700));
                        }
                    } else {
                        showAlert("خطأ", "اسم المستخدم أو كلمة المرور غير صحيحة");
                    }

                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                showAlert("خطأ بالمكتبة", "لم يتم العثور على مكتبة SQLite JDBC بالمشروع!\nيرجى إضافتها لملف pom.xml أو الـ Libraries.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("خطأ في قاعدة البيانات", "تعذر الاتصال بقاعدة البيانات: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(
                logo,
                title,
                welcome,
                username,
                password,
                login
        );

        root.getChildren().add(card);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    public VBox getView() {
        return root;
    }
}
