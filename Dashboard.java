package com.mycompany.cafe;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Dashboard {

    private VBox root;
    private Stage stage;
    private String userRole;


    public Dashboard(Stage stage, String userRole){
        this.stage = stage;
        this.userRole = userRole;
        createUI();
    }

    Dashboard(Stage stage) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void createUI(){
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color:#f5f5f5;"
        );

        Label title = new Label("كافيه زردة - لوحة التحكم (" + (userRole.equals("admin") ? "المدير" : "الكابتن") + ")");
        title.setStyle(
                "-fx-font-size:26px;" +
                "-fx-text-fill:#8B5E3C;" +
                "-fx-font-weight: bold;"
        );


        Button hallBtn = new Button("الصالة (المطبخ)");
        styleDashboardButton(hallBtn, "#8B5E3C");

        
        Button storeBtn = new Button("المخزن");
        styleDashboardButton(storeBtn, "#333333");

    
        Button employeesBtn = new Button("حسابات الموظفين");
        styleDashboardButton(employeesBtn, "#337ab7");


        Button dailyAccountsBtn = new Button("الحسابات اليومية");
        styleDashboardButton(dailyAccountsBtn, "#5cb85c");


        Button monthlyAccountsBtn = new Button("الحسابات الشهرية");
        styleDashboardButton(monthlyAccountsBtn, "#f0ad4e");


        Button logoutBtn = new Button("تسجيل الخروج");
        styleDashboardButton(logoutBtn, "#d9534f");

       
        hallBtn.setOnAction(e -> {
            Hall hall = new Hall(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            hall.getView(),
                            1000,
                            700
                    )
            );
        });

       
        storeBtn.setOnAction(e -> {
            Store store = new Store(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            store.getView(),
                            1000,
                            700
                    )
            );
        });

  
        employeesBtn.setOnAction(e -> {
            EmployeesAccounts emp = new EmployeesAccounts(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            emp.getView(),
                            1000,
                            700
                    )
            );
        });

       
        dailyAccountsBtn.setOnAction(e -> {
            DailyAccounts daily = new DailyAccounts(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            daily.getView(),
                            1000,
                            700
                    )
            );
        });

       
        monthlyAccountsBtn.setOnAction(e -> {
            MonthlyAccounts monthly = new MonthlyAccounts(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            monthly.getView(),
                            1000,
                            700
                    )
            );
        });

        // ربط زر تسجيل الخروج للعودة لصفحة اللوج إن
        logoutBtn.setOnAction(e -> {
            LoginScreen loginScreen = new LoginScreen(stage);
            stage.setScene(
                    new javafx.scene.Scene(
                            loginScreen.getView(),
                            1000,
                            700
                    )
            );
        });

      
        root.getChildren().add(title);

        if (userRole.equals("admin")) {
          
            root.getChildren().addAll(
                    hallBtn,
                    storeBtn,
                    employeesBtn,
                    dailyAccountsBtn,
                    monthlyAccountsBtn
            );
        } else if (userRole.equals("captain")) {
           
            root.getChildren().addAll(
                    hallBtn,
                    storeBtn
            );
        }

      
        root.getChildren().add(logoutBtn);
    }

    
    private void styleDashboardButton(Button btn, String bgColor) {
        btn.setPrefWidth(250);
        btn.setPrefHeight(45);
        btn.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-text-fill:white;" +
                "-fx-font-size:16px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
    }

    public VBox getView(){
        return root;
    }
}