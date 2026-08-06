package com.mycompany.cafe;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MonthlyAccounts {

    private VBox root;
    private Stage stage;

    public MonthlyAccounts(Stage stage) {
        this.stage = stage;
        createUI();
    }

    private void createUI() {
        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("كافيه زردة - تقرير الحسابات الشهرية");
        title.setStyle("-fx-font-size:22px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

   
        double todayRevenue = DailyAccounts.getDailyTotalRevenue();
        double baseMonthlyRevenue = 0.0; 
        double totalMonthlyRevenue = baseMonthlyRevenue + todayRevenue;

        double salariesTotal = 12000.0;
        double utilitiesTotal = 0.0;
        double todayNet = DailyAccounts.getDailyNetProfit();

        double netMonthProfit = totalMonthlyRevenue - (salariesTotal + utilitiesTotal);

        Label revLbl = new Label("إجمالي إيرادات الشهر (شاملة اليوم): " + totalMonthlyRevenue + " جنيه");
        revLbl.setStyle("-fx-font-size:16px; -fx-text-fill:#2e7d32; -fx-font-weight: bold;");

        Label todayRevRef = new Label("دخل اليوم الحالي المُسجل من الصالة: " + todayRevenue + " جنيه (صافي اليوم: " + todayNet + " ج)");
        todayRevRef.setStyle("-fx-font-size:14px; -fx-text-fill:#555555;");

        Label salLbl = new Label("إجمالي رواتب وروزنامة الموظفين الشهرية: " + salariesTotal + " جنيه");
        salLbl.setStyle("-fx-font-size:16px; -fx-text-fill:#d9534f;");

        Label utilLbl = new Label("إجمالي المصروفات التشغيلية (إيجار/فواتير): " + utilitiesTotal + " جنيه");
        utilLbl.setStyle("-fx-font-size:16px; -fx-text-fill:#d9534f;");

        Label netMonthLbl = new Label("صافي أرباح الشهر الكلية: " + netMonthProfit + " جنيه");
        netMonthLbl.setStyle("-fx-font-size:20px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

        Button backBtn = new Button("رجوع للوحة التحكم");
        backBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white;");
        backBtn.setOnAction(e -> {
            Dashboard dashboard = new Dashboard(stage);
            stage.setScene(new javafx.scene.Scene(dashboard.getView(), 900, 600));
        });

        root.getChildren().addAll(
                title, new Separator(),
                revLbl, todayRevRef, salLbl, utilLbl,
                new Separator(),
                netMonthLbl,
                new Separator(),
                backBtn
        );
    }

    public VBox getView() {
        return root;
    }
}
