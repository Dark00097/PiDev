package com.nexora.bank.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class StatisticsController implements Initializable {

    @FXML private Label lblTotalRevenu;
    @FXML private Label lblTotalClients;
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblCreditsApprouves;
    
    @FXML private ComboBox<String> cmbPeriode;
    @FXML private LineChart<String, Number> chartTransactions;
    @FXML private PieChart chartComptes;
    @FXML private BarChart<String, Number> chartCredits;
    @FXML private ListView<String> listActivites;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadLineChart();
        loadPieChart();
        loadBarChart();
        loadRecentActivities();
    }

    private void loadLineChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Transactions 2025");
        series.getData().add(new XYChart.Data<>("Jan", 132000));
        series.getData().add(new XYChart.Data<>("FÃ©v", 148000));
        series.getData().add(new XYChart.Data<>("Mar", 162000));
        series.getData().add(new XYChart.Data<>("Avr", 175000));
        series.getData().add(new XYChart.Data<>("Mai", 190000));
        series.getData().add(new XYChart.Data<>("Juin", 205000));
        series.getData().add(new XYChart.Data<>("Juil", 198000));
        series.getData().add(new XYChart.Data<>("AoÃ»t", 210000));
        series.getData().add(new XYChart.Data<>("Sep", 225000));
        series.getData().add(new XYChart.Data<>("Oct", 240000));
        series.getData().add(new XYChart.Data<>("Nov", 255000));
        series.getData().add(new XYChart.Data<>("DÃ©c", 275000));
        
        chartTransactions.getData().add(series);
    }

    private void loadPieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Courant (45%)", 45),
            new PieChart.Data("Ã‰pargne (35%)", 35),
            new PieChart.Data("Professionnel (20%)", 20)
        );
        chartComptes.setData(pieChartData);
    }

    private void loadBarChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Montant accordÃ©");
        series.getData().add(new XYChart.Data<>("Immobilier", 850000));
        series.getData().add(new XYChart.Data<>("Automobile", 320000));
        series.getData().add(new XYChart.Data<>("Personnel", 180000));
        series.getData().add(new XYChart.Data<>("Professionnel", 520000));
        
        chartCredits.getData().add(series);
    }

    private void loadRecentActivities() {
        ObservableList<String> activities = FXCollections.observableArrayList(
            "Nouveau compte crÃ©Ã© - CB-2024-156",
            "Virement de 5 000 DT effectuÃ©",
            "CrÃ©dit immobilier approuvÃ©",
            "Transaction validÃ©e - 1 250 DT",
            "Nouveau client inscrit",
            "Cashback crÃ©ditÃ© - 45 DT",
            "Rapport mensuel gÃ©nÃ©rÃ©",
            "Compte vÃ©rifiÃ© - CB-2024-089"
        );
        listActivites.setItems(activities);
    }

    @FXML
    private void refreshStats() {
        // Refresh statistics
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Actualisation");
        alert.setHeaderText(null);
        alert.setContentText("Les statistiques ont Ã©tÃ© actualisÃ©es.");
        alert.showAndWait();
    }
}
