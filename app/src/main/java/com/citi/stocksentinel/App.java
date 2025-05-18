package com.citi.stocksentinel;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class App extends Application {

   
    private static final String API_KEY;

    static {
        API_KEY = System.getenv("API_KEY");
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Environment variable ALPHA_VANTAGE_API_KEY is not set. Please set it to your Alpha Vantage API key.");
        }
    }
     // Your Alpha Vantage API key
    private static final String DEFAULT_SYMBOL = "DIA"; // DIA ETF for DJIA
    private static final int POLLING_INTERVAL_SECONDS = 15; // 15-second polling
    private static final int MAX_QUEUE_SIZE = 100; // Limit stored data points

    private final Queue<ArrayList<Object>> stockDataQueue = new LinkedList<>(); // [timestamp (Date), price, symbol]
    private XYChart.Series<Number, Number> priceSeries; // For LineChart
    private long startTime; // For x-axis time
    private String currentSymbol = DEFAULT_SYMBOL; // Current symbol
    private Label resultLabel; // For displaying results
    private Timeline pollingTimeline; // For periodic queries
    private Stage chartStage; // Pop-up chart window

    @Override
    public void start(Stage primaryStage) {
        // Initialize start time for x-axis
        startTime = System.currentTimeMillis();

        // Title
        Label title = new Label("Stock Sentinel Dashboard");
        title.getStyleClass().add("title");

        // Search input
        TextField searchField = new TextField();
        searchField.setPromptText("Enter symbol (e.g., DIA for DJIA, IBM)");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        Tooltip searchTooltip = new Tooltip("Use DIA to track the Dow Jones Industrial Average (DJIA). DIA is an ETF that closely follows the DJIA.");
        searchField.setTooltip(searchTooltip);

        // Search button
        Button searchButton = new Button("Search");
        searchButton.setPrefWidth(120);
        searchButton.getStyleClass().add("search-button");

        // Show chart button
        Button showChartButton = new Button("Show Price Chart");
        showChartButton.setPrefWidth(220);
        showChartButton.getStyleClass().add("search-button");

        // Info label
        Label infoLabel = new Label("DIA is an ETF tracking the DJIA. Prices are in $ per share; multiply by ~100 for approximate DJIA index.");
        infoLabel.getStyleClass().add("info-label");
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(300);

        // Results area
        Pane resultsArea = new Pane();
        resultsArea.setPrefSize(600, 350);
        resultsArea.getStyleClass().add("results-area");
        Tooltip resultsTooltip = new Tooltip("Latest global quote for DIA (DJIA proxy). Chart shows live or historical prices every 15 seconds.");
        Tooltip.install(resultsArea, resultsTooltip);
        

        // Result label for latest stock data
        resultLabel = new Label("Waiting for first data update...");
        resultLabel.getStyleClass().add("result-label");
        resultLabel.setLayoutX(10);
        resultLabel.setLayoutY(50);
        resultLabel.setWrapText(true);
        resultLabel.setMaxWidth(580);
        resultsArea.getChildren().add(resultLabel);

        // Search and chart buttons layout
        HBox buttonBox = new HBox(10, searchField, searchButton, showChartButton);
        buttonBox.setAlignment(Pos.CENTER);

        // Main layout
        VBox root = new VBox(20, title, buttonBox, infoLabel, resultsArea);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("root");

        // Polling setup
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(POLLING_INTERVAL_SECONDS), event -> fetchAndUpdateData(currentSymbol)));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();

        // Event handler for search button
        searchButton.setOnAction(event -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                query = DEFAULT_SYMBOL;
            }
            currentSymbol = query;
            stockDataQueue.clear();
            if (priceSeries != null) {
                priceSeries.getData().clear();
            }
            fetchAndUpdateData(query); // Immediate fetch on symbol change
        });

        // Event handler for show chart button
        showChartButton.setOnAction(event -> showChartPopup());

        // Scene and stage setup
        Scene scene = new Scene(root, 800, 500); // Reduced height since chart is in pop-up
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("Stock Sentinel Dashboard");
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showChartPopup() {
        if (chartStage == null || !chartStage.isShowing()) {
            chartStage = new Stage();
            chartStage.initStyle(StageStyle.DECORATED);
            chartStage.setTitle("DIA Price Trend");

            // Line chart setup
            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel("Time (seconds since start)");
            xAxis.setStyle("-fx-tick-label-fill: #ffffff;");

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("DIA Price (USD)");
            yAxis.setStyle("-fx-tick-label-fill: #ffffff;");

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Live DIA Price Trend (Proxy for DJIA)");
            lineChart.getStyleClass().add("line-chart");
            lineChart.setPrefSize(600, 400);
            lineChart.setCreateSymbols(false); // Smooth line
            Tooltip chartTooltip = new Tooltip("Shows live DIA prices or historical data when market is closed, updated every 15 seconds.");
            Tooltip.install(lineChart, chartTooltip);

            priceSeries = new XYChart.Series<>();
            priceSeries.setName(currentSymbol + " Price");
            lineChart.getData().add(priceSeries);

            // Close button
            Button closeButton = new Button("Close");
            closeButton.setStyle("-fx-background-color: #ff5555; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-padding: 8px 16px; -fx-background-radius: 6px;");
            closeButton.setOnAction(e -> chartStage.close());

            // Chart layout
            VBox chartRoot = new VBox(10, lineChart, closeButton);
            chartRoot.setAlignment(Pos.CENTER);
            chartRoot.setPadding(new Insets(10));
            chartRoot.setStyle("-fx-background-color: #1e1e1e;");

            Scene chartScene = new Scene(chartRoot, 600, 450);
            chartScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            chartStage.setScene(chartScene);
            chartStage.setResizable(false);
            chartStage.show();

            // Update chart with current data
            updateChart();
        } else {
            chartStage.requestFocus();
        }
    }

    private void fetchAndUpdateData(String symbol) {
        try {
            // Check market status
            LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
            boolean isMarketOpen = now.getDayOfWeek().getValue() <= 5 && // Monday–Friday
                    now.getHour() >= 9 && now.getHour() < 16; // 9 AM–4 PM EDT

            if (!isMarketOpen) {
                resultLabel.setText("Market is closed. Showing historical intraday data for " + symbol + ".");
                fetchIntradayData(symbol);
                return;
            }

            // Fetch GLOBAL_QUOTE
            JsonNode globalQuoteData = fetchGlobalQuote(symbol);
            if (globalQuoteData == null || !globalQuoteData.has("Global Quote")) {
                resultLabel.setText("Invalid symbol or no data: " + symbol);
                return;
            }

            // Parse Global Quote data
            JsonNode quote = globalQuoteData.get("Global Quote");
            String stockSymbol = quote.get("01. symbol").asText("N/A");
            String currentPrice = quote.get("05. price").asText("N/A");
            String stockPriceHigh = quote.get("03. high").asText("N/A");
            String stockPriceLow = quote.get("04. low").asText("N/A");
            String stockVolume = quote.get("06. volume").asText("N/A");
            String stockTimestamp = quote.get("07. latest trading day").asText("N/A");
            String stockExchange = stockSymbol.equals("DIA") ? "NYSE Arca" : "N/A";
            String stockCurrency = "USD";

            // Verify market status with latest trading day
            LocalDate latestTradingDay = LocalDate.parse(stockTimestamp, DateTimeFormatter.ISO_LOCAL_DATE);
            if (!latestTradingDay.equals(now.toLocalDate())) {
                resultLabel.setText("Market is closed. Showing historical intraday data for " + symbol + ".");
                fetchIntradayData(symbol);
                return;
            }

            // Approximate DJIA index
            double diaPrice = Double.parseDouble(currentPrice);
            String approxIndexPoints = String.format("%.0f", diaPrice * 100);

            // Store data in queue
            ArrayList<Object> stockData = new ArrayList<>();
            stockData.add(new Date());
            stockData.add(new BigDecimal(currentPrice));
            stockData.add(stockSymbol);
            stockDataQueue.add(stockData);

            // Limit queue size
            if (stockDataQueue.size() > MAX_QUEUE_SIZE) {
                stockDataQueue.poll();
            }

            // Update chart
            updateChart();

            // Update HBox with basic info
            Label sSymbol = new Label(stockSymbol);
            Label sCurrency = new Label(stockCurrency);
            Label sExchange = new Label(stockExchange);
            Label sExchangeTimeZone = new Label(stockSymbol.equals("DIA") ? "America/New_York" : "N/A");

            sSymbol.getStyleClass().addAll("item", "nth-child-1");
            sCurrency.getStyleClass().add("item");
            sExchange.getStyleClass().add("item");
            sExchangeTimeZone.getStyleClass().add("item");

            HBox basicStockInfo = new HBox(10, sSymbol, sCurrency, sExchange, sExchangeTimeZone);
            basicStockInfo.getStyleClass().add("item-box");
            basicStockInfo.setLayoutX(10);
            basicStockInfo.setLayoutY(10);

            Pane resultsArea = (Pane) resultLabel.getParent();
            resultsArea.getChildren().removeIf(node -> node instanceof HBox);
            resultsArea.getChildren().add(basicStockInfo);

            // Update resultLabel
            String resultText = String.format(
                    "Price: %s USD\nApprox. DJIA Index: %s points\nHigh: %s\nLow: %s\nVolume: %s\nTimestamp: %s",
                    currentPrice, approxIndexPoints, stockPriceHigh, stockPriceLow, stockVolume, stockTimestamp);
            resultLabel.setText(resultText);

            System.out.println("Updated data for " + stockSymbol + " at " + new Date());

        } catch (IOException | InterruptedException e) {
            if (e.getMessage().contains("429")) {
                resultLabel.setText("Too many requests for " + symbol + ". Pausing updates for 60 seconds.");
                pollingTimeline.pause();
                new Timeline(new KeyFrame(Duration.seconds(60), evt -> pollingTimeline.play())).play();
            } else {
                resultLabel.setText("Failed to fetch data for " + symbol + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private JsonNode fetchGlobalQuote(String symbol) throws IOException, InterruptedException {
        String url = String.format("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", symbol, API_KEY);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            throw new IOException("HTTP 429 Too Many Requests");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonRes = mapper.readTree(response.body());
        if (jsonRes.has("Error Message") || !jsonRes.has("Global Quote")) {
            return null;
        }
        return jsonRes;
    }

    private void fetchIntradayData(String symbol) {
        try {
            String url = String.format("https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=5min&outputsize=compact&apikey=%s", symbol, API_KEY);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                resultLabel.setText("Rate limit reached for historical data. Try again later.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonRes = mapper.readTree(response.body());
            if (jsonRes.has("Time Series (5min)")) {
                stockDataQueue.clear();
                if (priceSeries != null) {
                    priceSeries.getData().clear();
                }
                JsonNode timeSeries = jsonRes.get("Time Series (5min)");
                timeSeries.fields().forEachRemaining(entry -> {
                    String timestampStr = entry.getKey();
                    String price = entry.getValue().get("4. close").asText();
                    try {
                        Date timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timestampStr);
                        ArrayList<Object> stockData = new ArrayList<>();
                        stockData.add(timestamp);
                        stockData.add(new BigDecimal(price));
                        stockData.add(symbol);
                        stockDataQueue.add(stockData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                updateChart();
            } else {
                resultLabel.setText("No historical data available for " + symbol);
            }
        } catch (IOException | InterruptedException e) {
            resultLabel.setText("Failed to fetch historical data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateChart() {
        if (priceSeries == null) return;
        priceSeries.getData().clear();
        long currentTime = System.currentTimeMillis();

        for (ArrayList<Object> stockData : stockDataQueue) {
            Date timestamp = (Date) stockData.get(0);
            BigDecimal price = (BigDecimal) stockData.get(1);
            String symbol = (String) stockData.get(2);

            if (symbol.equals(currentSymbol)) {
                double secondsSinceStart = (timestamp.getTime() - startTime) / 1000.0;
                priceSeries.getData().add(new XYChart.Data<>(secondsSinceStart, price.doubleValue()));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}