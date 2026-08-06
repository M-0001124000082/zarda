package com.mycompany.cafe;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DailyAccounts {

    private VBox root;
    private Stage stage;

    private static double dailyExpenses = 0.0;
    private static double storeExpenses = 0.0; 
    private static double staffWages = 0.0; 

    private static double accumulatedMonthlyRevenue = 0.0;

    public DailyAccounts(Stage stage) {
        this.stage = stage;
        createUI();
    }

    private void createUI() {
        root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("كافيه زردة - الحسابات اليومية الشاملة");
        title.setStyle("-fx-font-size:22px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

        double totalRevenue = Hall.getDailyTotalIncome();
        double drinksRev = Hall.getDailyDrinksIncome();
        double shishaRev = Hall.getDailyShishaIncome();
        double foodRev = Hall.getDailyFoodIncome();
        double psRev = Hall.getDailyPlaystationIncome();

        Label revenueLabel = new Label("إجمالي المبيعات اليومية: " + totalRevenue + " جنيه");
        revenueLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#2e7d32; -fx-font-weight: bold;");

        Label breakdownLabel = new Label(String.format("☕ مشاريب: %.1f ج | 💨 شيشة: %.1f ج | 🍔 أكل: %.1f ج | 🎮 بلايستيشن: %.1f ج", drinksRev, shishaRev, foodRev, psRev));
        breakdownLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#555555; -fx-font-weight: bold;");

        double totalExpensesAll = dailyExpenses + storeExpenses + staffWages;
        Label expensesLabel = new Label("إجمالي المصروفات اليومية (نثريات + مخزن + رواتب): " + totalExpensesAll + " جنيه");
        expensesLabel.setStyle("-fx-font-size:15px; -fx-text-fill:#d9534f; -fx-font-weight: bold;");

        double netProfit = totalRevenue - totalExpensesAll;
        Label netLabel = new Label("صافي الدخل اليومي: " + netProfit + " جنيه");
        netLabel.setStyle("-fx-font-size:18px; -fx-text-fill:#333333; -fx-font-weight: bold;");

        TextField staffNameField = new TextField();
        staffNameField.setPromptText("اسم الموظف (مثلاً: أحمد - شيف)");
        staffNameField.setMaxWidth(250);

        TextField staffWageField = new TextField();
        staffWageField.setPromptText("مبلغ اليومية");
        staffWageField.setMaxWidth(250);

        Button addStaffBtn = new Button("تسجيل يومية موظف");
        addStaffBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold;");

        TextField expenseNameField = new TextField();
        expenseNameField.setPromptText("بيان المصروف (مثلاً: فاتورة كهرباء)");
        expenseNameField.setMaxWidth(250);

        TextField expenseAmountField = new TextField();
        expenseAmountField.setPromptText("المبلغ");
        expenseAmountField.setMaxWidth(250);

        Button addExpenseBtn = new Button("إضافة مصروف جديد");
        addExpenseBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold;");
        
        addStaffBtn.setOnAction(e -> {
            try {
                double wage = Double.parseDouble(staffWageField.getText());
                staffWages += wage;
                
                double updatedTotalExp = dailyExpenses + storeExpenses + staffWages;
                expensesLabel.setText("إجمالي المصروفات اليومية (نثريات + مخزن + رواتب): " + updatedTotalExp + " جنيه");
                
                double updatedNet = Hall.getDailyTotalIncome() - updatedTotalExp;
                netLabel.setText("صافي الدخل اليومي: " + updatedNet + " جنيه");
                
                staffNameField.clear();
                staffWageField.clear();
                showAlert("تم الحفظ", "تمت إضافة يومية الموظف وخصمها من الدخل بنجاح!");
            } catch (Exception ex) {
                showAlert("خطأ", "يرجى إدخال مبلغ صحيح ليومية الموظف!");
            }
        });

        addExpenseBtn.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(expenseAmountField.getText());
                dailyExpenses += amt;
                
                double updatedTotalExp = dailyExpenses + storeExpenses + staffWages;
                expensesLabel.setText("إجمالي المصروفات اليومية (نثريات + مخزن + رواتب): " + updatedTotalExp + " جنيه");
                
                double updatedNet = Hall.getDailyTotalIncome() - updatedTotalExp;
                netLabel.setText("صافي الدخل اليومي: " + updatedNet + " جنيه");
                
                expenseNameField.clear();
                expenseAmountField.clear();
                showAlert("تم الحفظ", "تمت إضافة المصروف العارض بنجاح!");
            } catch (Exception ex) {
                showAlert("خطأ", "يرجى إدخال مبلغ صحيح بالمصروفات!");
            }
        });

        Button backBtn = new Button("رجوع للوحة التحكم");
        backBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        backBtn.setOnAction(e -> {
            Dashboard dashboard = new Dashboard(stage, "admin");
            stage.setScene(new javafx.scene.Scene(dashboard.getView(), 1000, 700));
        });

        root.getChildren().addAll(
                title, new Separator(),
                revenueLabel, breakdownLabel, expensesLabel, netLabel,
                new Separator(),
                new Label("تسجيل يومية الموظفين اليومية:"),
                staffNameField, staffWageField, addStaffBtn,
                new Separator(),
                new Label("إضافة مصروفات يومية عارضة:"),
                expenseNameField, expenseAmountField, addExpenseBtn,
                new Separator(),
                backBtn
        );
    }

    public static void addStoreExpense(double amount) {
        storeExpenses += amount;
    }

    public static double getDailyNetProfit() {
        return Hall.getDailyTotalIncome() - (dailyExpenses + storeExpenses + staffWages);
    }

    public static double getDailyTotalRevenue() {
        return Hall.getDailyTotalIncome();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getView() {
        return root;
    }
}