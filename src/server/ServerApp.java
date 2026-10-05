package server;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerApp extends Application {

    private static final String BG        = "#0B1220";
    private static final String BG_PANEL  = "#111827";
    private static final String BG_CARD   = "#1E293B";
    private static final String BG_INPUT  = "#0F172A";
    private static final String BORDER    = "#1E293B";
    private static final String BORDER_SOFT = "#334155";
    private static final String PRIMARY   = "#3B82F6";
    private static final String CYAN      = "#06B6D4";
    private static final String GREEN     = "#10B981";
    private static final String YELLOW    = "#F59E0B";
    private static final String RED       = "#EF4444";
    private static final String PURPLE    = "#8B5CF6";
    private static final String FG        = "#E2E8F0";
    private static final String FG_DIM    = "#94A3B8";
    private static final String FG_DIMMER = "#64748B";

    private ServerCore core;
    private TextArea   logArea;

    private int  totalRequests  = 0;
    private int  totalRegisters = 0;
    private int  totalLogins    = 0;
    private int  totalSends     = 0;
    private long startTime      = 0;

    private Label lbReqPerSec, lbClients, lbData, lbLatency;
    private Label lbTotalReq, lbUptime, lbClientsSide;
    private Label lbReg, lbLogin, lbSend;
    private Label statusBadge;
    private XYChart.Series<Number, Number> series;
    private PieChart pieChart;
    private StackPane contentArea;
    private VBox pageDashboard, pageAccounts, pageMailbox, pageOnline, pageLog;
    private Button currentTabBtn = null;
    private ObservableList<AccountRow> accountsData = FXCollections.observableArrayList();
    private ObservableList<MailRow>    mailsData    = FXCollections.observableArrayList();
    private ObservableList<OnlineRow>  onlineData   = FXCollections.observableArrayList();
    private TableView<AccountRow> accountsTable;
    private TableView<MailRow>    mailsTable;
    private TableView<OnlineRow>  onlineTable;
    private VBox toastBox;
    private Timeline onlineRefresher;

    public static class AccountRow {
        private final String username, created, mailCount;
        public AccountRow(String u, String c, String m) { username = u; created = c; mailCount = m; }
        public String getUsername() { return username; }
        public String getCreated()  { return created; }
        public String getMailCount(){ return mailCount; }
    }

    public static class MailRow {
        private final String from, to, subject, time;
        public MailRow(String f, String t, String s, String tm) { from = f; to = t; subject = s; time = tm; }
        public String getFrom()    { return from; }
        public String getTo()      { return to; }
        public String getSubject() { return subject; }
        public String getTime()    { return time; }
    }

    public static class OnlineRow {
        private final String username, ip, lastSeen, status;
        public OnlineRow(String u, String i, String ls, String st) { username = u; ip = i; lastSeen = ls; status = st; }
        public String getUsername() { return username; }
        public String getIp()       { return ip; }
        public String getLastSeen() { return lastSeen; }
        public String getStatus()   { return status; }
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());

        toastBox = new VBox(8);
        toastBox.setAlignment(Pos.TOP_RIGHT);
        toastBox.setMouseTransparent(true);
        toastBox.setPadding(new Insets(20));

        StackPane stack = new StackPane(root, toastBox);
        StackPane.setAlignment(toastBox, Pos.TOP_RIGHT);

        Scene scene = new Scene(stack, 1400, 860);
        stage.setTitle("Mail Server — Dashboard");
        stage.setScene(scene);
        stage.show();

        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateUptime();
            updateChart();
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();

        onlineRefresher = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshOnline()));
        onlineRefresher.setCycleCount(Animation.INDEFINITE);
        onlineRefresher.play();
    }

    private HBox buildHeader() {
        FontIcon logo = new FontIcon(FontAwesomeSolid.SERVER);
        logo.setIconSize(20);
        logo.setIconColor(Color.WHITE);
        StackPane logoBox = new StackPane(logo);
        logoBox.setStyle("-fx-background-color: linear-gradient(to bottom right, " + PRIMARY + ", " + CYAN + "); -fx-background-radius:10; -fx-padding:10;");

        Label title = new Label("MAIL SERVER");
        title.setStyle("-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold;");
        Label sub = new Label("UDP Socket • Port 2023");
        sub.setStyle("-fx-text-fill:" + FG_DIMMER + "; -fx-font-size:10px;");
        VBox titleBox = new VBox(1, title, sub);

        HBox logoSection = new HBox(12, logoBox, titleBox);
        logoSection.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon dirIcon = new FontIcon(FontAwesomeSolid.FOLDER);
        dirIcon.setIconSize(11);
        dirIcon.setIconColor(Color.web(GREEN));
        Label dirLbl = new Label("  F:\\server_mail");
        dirLbl.setGraphic(dirIcon);
        dirLbl.setStyle("-fx-text-fill:" + FG_DIM + "; -fx-font-size:11px; -fx-background-color:" + BG_PANEL + "; -fx-padding:8 14; -fx-background-radius:20; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:20;");

        statusBadge = new Label("● OFFLINE");
        statusBadge.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-text-fill:" + FG_DIM + "; -fx-padding:8 16; -fx-background-radius:20; -fx-font-size:11px; -fx-font-weight:bold; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:20;");

        HBox header = new HBox(12, logoSection, spacer, dirLbl, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-border-color:" + BORDER + "; -fx-border-width:0 0 1 0;");
        return header;
    }

    private VBox buildSidebar() {
        VBox box = new VBox(16);
        box.setPrefWidth(260);
        box.setPadding(new Insets(20, 14, 20, 14));
        box.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-border-color:" + BORDER + "; -fx-border-width:0 1 0 0;");

        Label ctlTitle = new Label("ĐIỀU KHIỂN");
        ctlTitle.setStyle("-fx-text-fill:" + FG_DIMMER + "; -fx-font-size:11px; -fx-font-weight:bold;");

        TextField portField = new TextField("2023");
        portField.setPrefHeight(38);
        portField.setStyle("-fx-background-color:" + BG + "; -fx-text-fill:white; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:8; -fx-background-radius:8; -fx-padding:0 10; -fx-font-size:13px;");

        Button startBtn = new Button("▶   START SERVER");
        startBtn.setPrefHeight(42);
        startBtn.setPrefWidth(230);
        startBtn.setStyle("-fx-background-color:" + GREEN + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px; -fx-background-radius:8; -fx-cursor:hand;");

        Button stopBtn = new Button("■   STOP SERVER");
        stopBtn.setPrefHeight(42);
        stopBtn.setPrefWidth(230);
        stopBtn.setDisable(true);
        stopBtn.setStyle("-fx-background-color:" + RED + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px; -fx-background-radius:8; -fx-cursor:hand;");

        VBox ctlBox = new VBox(10, ctlTitle, portField, startBtn, stopBtn);
        ctlBox.setPadding(new Insets(14));
        ctlBox.setStyle("-fx-background-color:" + BG + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");

        Label stTitle = new Label("THỐNG KÊ");
        stTitle.setStyle("-fx-text-fill:" + FG_DIMMER + "; -fx-font-size:11px; -fx-font-weight:bold;");

        lbClientsSide = new Label("0");
        lbTotalReq    = new Label("0");
        lbUptime      = new Label("00:00:00");

        VBox stBox = new VBox(10, stTitle,
                sideStatRow(FontAwesomeSolid.USERS, "Client online", lbClientsSide, PRIMARY),
                sideStatRow(FontAwesomeSolid.HASHTAG, "Requests", lbTotalReq, CYAN),
                sideStatRow(FontAwesomeSolid.CLOCK, "Uptime", lbUptime, YELLOW));
        stBox.setPadding(new Insets(14));
        stBox.setStyle("-fx-background-color:" + BG + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");

        Label menuTitle = new Label("MENU");
        menuTitle.setStyle("-fx-text-fill:" + FG_DIMMER + "; -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:8 0 4 6;");

        Button btnDash     = sideMenuBtn(FontAwesomeSolid.CHART_LINE, "Dashboard");
        Button btnOnline   = sideMenuBtn(FontAwesomeSolid.USER_FRIENDS, "Clients Online");
        Button btnAccounts = sideMenuBtn(FontAwesomeSolid.USERS, "Accounts");
        Button btnMailbox  = sideMenuBtn(FontAwesomeSolid.INBOX, "Mailbox");
        Button btnLog      = sideMenuBtn(FontAwesomeSolid.TERMINAL, "Live Log");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(ctlBox, stBox, menuTitle, btnDash, btnOnline, btnAccounts, btnMailbox, btnLog, spacer);

        startBtn.setOnAction(e -> {
            int port;
            try { port = Integer.parseInt(portField.getText().trim()); }
            catch (Exception ex) { return; }

            core = new ServerCore(this::log);
            core.start(port);
            startTime = System.currentTimeMillis();

            startBtn.setDisable(true);
            stopBtn.setDisable(false);
            portField.setDisable(true);
            statusBadge.setText("● RUNNING");
            statusBadge.setStyle("-fx-background-color:rgba(16,185,129,0.2); -fx-text-fill:" + GREEN + "; -fx-padding:8 16; -fx-background-radius:20; -fx-font-size:11px; -fx-font-weight:bold; -fx-border-color:" + GREEN + "; -fx-border-radius:20;");
            log("[OK] Server started on port " + port);
            log("[DIR] F:\\server_mail");
            showToast("Server đã khởi động ở cổng " + port, GREEN);
        });

        stopBtn.setOnAction(e -> {
            if (core != null) core.stop();
            log("[STOP] Server stopped");
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
            portField.setDisable(false);
            statusBadge.setText("● OFFLINE");
            statusBadge.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-text-fill:" + FG_DIM + "; -fx-padding:8 16; -fx-background-radius:20; -fx-font-size:11px; -fx-font-weight:bold;");
            showToast("Server đã dừng", RED);
        });

        btnDash.setOnAction(e -> switchTab(btnDash, pageDashboard));
        btnOnline.setOnAction(e -> { refreshOnline(); switchTab(btnOnline, pageOnline); });
        btnAccounts.setOnAction(e -> { refreshAccounts(); switchTab(btnAccounts, pageAccounts); });
        btnMailbox.setOnAction(e -> { refreshMails(); switchTab(btnMailbox, pageMailbox); });
        btnLog.setOnAction(e -> switchTab(btnLog, pageLog));

        currentTabBtn = btnDash;
        btnDash.setStyle(menuBtnActive());

        return box;
    }

    private HBox sideStatRow(FontAwesomeSolid ic, String label, Label val, String color) {
        FontIcon fi = new FontIcon(ic);
        fi.setIconSize(11);
        fi.setIconColor(Color.web(color));
        StackPane ib = new StackPane(fi);
        ib.setStyle("-fx-background-color:" + color + "30; -fx-background-radius:6; -fx-padding:5;");

        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + FG_DIM + "; -fx-font-size:11px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        val.setStyle("-fx-text-fill:white; -fx-font-size:12px; -fx-font-weight:bold;");

        HBox h = new HBox(8, ib, l, sp, val);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Button sideMenuBtn(FontAwesomeSolid ic, String text) {
        FontIcon fi = new FontIcon(ic);
        fi.setIconSize(14);
        fi.setIconColor(Color.web(FG_DIM));
        Label l = new Label("  " + text);
        l.setStyle("-fx-text-fill:" + FG + "; -fx-font-size:13px;");
        HBox h = new HBox(fi, l);
        h.setAlignment(Pos.CENTER_LEFT);

        Button b = new Button();
        b.setGraphic(h);
        b.setPrefWidth(230);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(11, 14, 11, 14));
        b.setStyle(menuBtnStyle(false));
        b.setOnMouseEntered(e -> { if (b != currentTabBtn) b.setStyle(menuBtnStyle(true)); });
        b.setOnMouseExited (e -> { if (b != currentTabBtn) b.setStyle(menuBtnStyle(false)); });
        return b;
    }

    private String menuBtnStyle(boolean hover) {
        String bg = hover ? "rgba(59,130,246,0.15)" : "transparent";
        return "-fx-background-color:" + bg + "; -fx-background-radius:8; -fx-cursor:hand;";
    }

    private String menuBtnActive() {
        return "-fx-background-color:" + PRIMARY + "; -fx-background-radius:8; -fx-cursor:hand;";
    }

    private VBox buildMainContent() {
        pageDashboard = buildDashboardPage();
        pageOnline    = buildOnlinePage();
        pageAccounts  = buildAccountsPage();
        pageMailbox   = buildMailboxPage();
        pageLog       = buildLogPage();

        contentArea = new StackPane(pageDashboard);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        VBox main = new VBox(contentArea);
        main.setPadding(new Insets(24, 28, 24, 28));
        main.setStyle("-fx-background-color:" + BG + ";");
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private void switchTab(Button btn, VBox page) {
        if (currentTabBtn != null) currentTabBtn.setStyle(menuBtnStyle(false));
        btn.setStyle(menuBtnActive());
        currentTabBtn = btn;

        page.setOpacity(0);
        page.setTranslateY(15);
        contentArea.getChildren().setAll(page);

        FadeTransition ft = new FadeTransition(Duration.millis(250), page);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), page);
        tt.setFromY(15);
        tt.setToY(0);

        new ParallelTransition(ft, tt).play();
    }

    private VBox buildDashboardPage() {
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        Label sub = new Label("Tổng quan hoạt động mail server theo thời gian thực");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");
        VBox titleBox = new VBox(2, title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label liveBadge = new Label("● LIVE");
        liveBadge.setStyle("-fx-background-color:rgba(16,185,129,0.15); -fx-text-fill:" + GREEN + "; -fx-padding:6 14; -fx-background-radius:20; -fx-font-size:11px; -fx-font-weight:bold;");
        HBox sectionTitle = new HBox(titleBox, sp, liveBadge);
        sectionTitle.setAlignment(Pos.CENTER_LEFT);

        lbReqPerSec = new Label("0");
        lbClients   = new Label("0");
        lbData      = new Label("0 KB");
        lbLatency   = new Label("0 ms");

        HBox metrics = new HBox(16,
                bigMetricCard(FontAwesomeSolid.EXCHANGE_ALT, "Requests/sec", lbReqPerSec, PRIMARY),
                bigMetricCard(FontAwesomeSolid.USERS, "Clients Online", lbClients, GREEN),
                bigMetricCard(FontAwesomeSolid.DATABASE, "Data Transfer", lbData, CYAN),
                bigMetricCard(FontAwesomeSolid.TACHOMETER_ALT, "Avg Latency", lbLatency, YELLOW));
        metrics.setPrefHeight(120);

        NumberAxis xAxis = new NumberAxis(0, 30, 5);
        NumberAxis yAxis = new NumberAxis(0, 10, 1);
        xAxis.setStyle("-fx-tick-label-fill:" + FG_DIM + ";");
        yAxis.setStyle("-fx-tick-label-fill:" + FG_DIM + ";");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        lineChart.setPrefHeight(300);
        lineChart.setStyle("-fx-background-color:transparent; -fx-plot-background:transparent;");

        series = new XYChart.Series<>();
        lineChart.getData().add(series);

        VBox lineCard = chartCard("📈  Realtime Traffic", lineChart);
        HBox.setHgrow(lineCard, Priority.ALWAYS);

        pieChart = new PieChart();
        pieChart.setTitle("");
        pieChart.setLegendVisible(true);
        pieChart.setPrefHeight(300);
        pieChart.setStyle("-fx-background-color:transparent;");
        pieChart.getData().addAll(
                new PieChart.Data("REGISTER", 1),
                new PieChart.Data("LOGIN", 1),
                new PieChart.Data("SEND", 1));

        VBox pieCard = chartCard("🍩  Command Distribution", pieChart);
        pieCard.setPrefWidth(400);

        HBox charts = new HBox(16, lineCard, pieCard);

        lbReg   = new Label("0");
        lbLogin = new Label("0");
        lbSend  = new Label("0");
        HBox cmdBar = new HBox(16,
                cmdBadge("REGISTER", lbReg, PRIMARY),
                cmdBadge("LOGIN", lbLogin, GREEN),
                cmdBadge("SEND", lbSend, YELLOW));
        cmdBar.setPadding(new Insets(16));
        cmdBar.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");

        VBox page = new VBox(18, sectionTitle, metrics, charts, cmdBar);
        page.setStyle("-fx-background-color:" + BG + ";");
        return page;
    }

    private VBox bigMetricCard(FontAwesomeSolid ic, String label, Label valueLbl, String color) {
        FontIcon fi = new FontIcon(ic);
        fi.setIconSize(18);
        fi.setIconColor(Color.web(color));
        StackPane iconBox = new StackPane(fi);
        iconBox.setStyle("-fx-background-color:" + color + "20; -fx-background-radius:10; -fx-padding:10;");

        Label l = new Label(label);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");

        HBox titleRow = new HBox(10, iconBox, l);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        valueLbl.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");

        VBox card = new VBox(10, titleRow, valueLbl);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox chartCard(String title, Node chart) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        VBox v = new VBox(10, t, chart);
        v.setPadding(new Insets(16));
        v.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        return v;
    }

    private HBox cmdBadge(String name, Label valueLbl, String color) {
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:" + color + "; -fx-background-color:" + color + "20; -fx-padding:5 12; -fx-background-radius:6;");
        valueLbl.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        VBox v = new VBox(8, nameLbl, valueLbl);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(10, 24, 10, 24));
        HBox.setHgrow(v, Priority.ALWAYS);
        return new HBox(v);
    }

    private VBox buildOnlinePage() {
        Label title = new Label("Clients Online");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        Label sub = new Label("Danh sách client đang hoạt động (cập nhật mỗi 2 giây)");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");
        VBox titleBox = new VBox(2, title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color:" + PRIMARY + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> refreshOnline());

        HBox sectionTitle = new HBox(titleBox, sp, refreshBtn);
        sectionTitle.setAlignment(Pos.CENTER_LEFT);

        onlineTable = new TableView<>();
        onlineTable.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14; -fx-font-size:13px;");
        onlineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        onlineTable.setPlaceholder(new Label("Chưa có client nào online"));

        TableColumn<OnlineRow, String> cUser = new TableColumn<>("Username");
        cUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        cUser.setPrefWidth(200);

        TableColumn<OnlineRow, String> cIp = new TableColumn<>("Địa chỉ IP");
        cIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        cIp.setPrefWidth(200);

        TableColumn<OnlineRow, String> cSeen = new TableColumn<>("Hoạt động cuối");
        cSeen.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));
        cSeen.setPrefWidth(200);

        TableColumn<OnlineRow, String> cStat = new TableColumn<>("Trạng thái");
        cStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStat.setPrefWidth(150);

        onlineTable.getColumns().addAll(cUser, cIp, cSeen, cStat);
        onlineTable.setItems(onlineData);
        VBox.setVgrow(onlineTable, Priority.ALWAYS);

        VBox card = new VBox(onlineTable);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        VBox.setVgrow(card, Priority.ALWAYS);

        VBox page = new VBox(18, sectionTitle, card);
        VBox.setVgrow(page, Priority.ALWAYS);
        page.setStyle("-fx-background-color:" + BG + ";");
        return page;
    }

    private void refreshOnline() {
        if (core == null) return;
        Platform.runLater(() -> {
            onlineData.clear();
            Map<String, ServerCore.ClientInfo> on = core.getOnlineClients();
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
            long now = System.currentTimeMillis();

            for (Map.Entry<String, ServerCore.ClientInfo> e : on.entrySet()) {
                ServerCore.ClientInfo ci = e.getValue();
                long delta = (now - ci.lastSeen) / 1000;
                String status = delta < 5 ? "● Online" : "● Idle";
                onlineData.add(new OnlineRow(ci.username, ci.ip, fmt.format(new Date(ci.lastSeen)) + " (" + delta + "s trước)", status));
            }

            lbClientsSide.setText(String.valueOf(on.size()));
            lbClients.setText(String.valueOf(on.size()));
        });
    }

    private VBox buildAccountsPage() {
        Label title = new Label("Accounts");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        Label sub = new Label("Danh sách tất cả tài khoản đã đăng ký trên server");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");
        VBox titleBox = new VBox(2, title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color:" + PRIMARY + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> refreshAccounts());

        HBox sectionTitle = new HBox(titleBox, sp, refreshBtn);
        sectionTitle.setAlignment(Pos.CENTER_LEFT);

        accountsTable = new TableView<>();
        accountsTable.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14; -fx-font-size:13px;");
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountsTable.setPlaceholder(new Label("Chưa có account nào"));

        TableColumn<AccountRow, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUser.setPrefWidth(250);

        TableColumn<AccountRow, String> colCreated = new TableColumn<>("Ngày tạo");
        colCreated.setCellValueFactory(new PropertyValueFactory<>("created"));
        colCreated.setPrefWidth(250);

        TableColumn<AccountRow, String> colCount = new TableColumn<>("Số mail");
        colCount.setCellValueFactory(new PropertyValueFactory<>("mailCount"));
        colCount.setPrefWidth(150);

        accountsTable.getColumns().addAll(colUser, colCreated, colCount);
        accountsTable.setItems(accountsData);
        VBox.setVgrow(accountsTable, Priority.ALWAYS);

        VBox card = new VBox(accountsTable);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        VBox.setVgrow(card, Priority.ALWAYS);

        VBox page = new VBox(18, sectionTitle, card);
        VBox.setVgrow(page, Priority.ALWAYS);
        page.setStyle("-fx-background-color:" + BG + ";");
        return page;
    }

    private void refreshAccounts() {
        accountsData.clear();
        File dir = new File(ServerCore.SERVER_DIR);
        File[] subs = dir.listFiles();
        if (subs == null) return;

        for (File f : subs) {
            if (!f.isDirectory()) continue;
            if (!new File(f, "user.txt").exists()) continue;

            String created = "";
            try {
                File dateFile = new File(f, "date.txt");
                if (dateFile.exists()) {
                    Scanner sc = new Scanner(dateFile, "UTF-8");
                    if (sc.hasNextLine()) created = sc.nextLine();
                    sc.close();
                }
            } catch (Exception ignored) {}

            File inbox = new File(f, "inbox");
            File[] mails = inbox.listFiles((d, n) -> n.startsWith("email_"));
            int mailCount = mails != null ? mails.length : 0;

            accountsData.add(new AccountRow(f.getName(), created, mailCount + " mail"));
        }
    }

    private VBox buildMailboxPage() {
        Label title = new Label("Mailbox");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        Label sub = new Label("Tất cả email đã được gửi giữa các tài khoản");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");
        VBox titleBox = new VBox(2, title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color:" + PRIMARY + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 18; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> refreshMails());

        HBox sectionTitle = new HBox(titleBox, sp, refreshBtn);
        sectionTitle.setAlignment(Pos.CENTER_LEFT);

        mailsTable = new TableView<>();
        mailsTable.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14; -fx-font-size:13px;");
        mailsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mailsTable.setPlaceholder(new Label("Chưa có email nào"));

        TableColumn<MailRow, String> cFrom = new TableColumn<>("Từ");
        cFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
        cFrom.setPrefWidth(160);

        TableColumn<MailRow, String> cTo = new TableColumn<>("Đến");
        cTo.setCellValueFactory(new PropertyValueFactory<>("to"));
        cTo.setPrefWidth(160);

        TableColumn<MailRow, String> cSub = new TableColumn<>("Tiêu đề");
        cSub.setCellValueFactory(new PropertyValueFactory<>("subject"));
        cSub.setPrefWidth(300);

        TableColumn<MailRow, String> cTime = new TableColumn<>("Thời gian");
        cTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        cTime.setPrefWidth(200);

        mailsTable.getColumns().addAll(cFrom, cTo, cSub, cTime);
        mailsTable.setItems(mailsData);
        VBox.setVgrow(mailsTable, Priority.ALWAYS);

        VBox card = new VBox(mailsTable);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        VBox.setVgrow(card, Priority.ALWAYS);

        VBox page = new VBox(18, sectionTitle, card);
        VBox.setVgrow(page, Priority.ALWAYS);
        page.setStyle("-fx-background-color:" + BG + ";");
        return page;
    }

    private void refreshMails() {
        mailsData.clear();
        File dir = new File(ServerCore.SERVER_DIR);
        File[] subs = dir.listFiles();
        if (subs == null) return;

        for (File account : subs) {
            if (!account.isDirectory()) continue;
            File inbox = new File(account, "inbox");
            if (!inbox.exists()) continue;
            File[] mails = inbox.listFiles((d, n) -> n.startsWith("email_"));
            if (mails == null) continue;
            for (File mail : mails) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(mail.toPath()), "UTF-8");
                    String from = extract(content, "From");
                    String to   = extract(content, "To");
                    String sub  = extract(content, "Tieu de");
                    String time = extract(content, "Thoi gian gui");
                    mailsData.add(new MailRow(from, to, sub, time));
                } catch (Exception ignored) {}
            }
        }
    }

    private String extract(String s, String key) {
        for (String line : s.split("\n")) {
            if (line.startsWith(key)) {
                int idx = line.indexOf(':');
                if (idx > 0) return line.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private VBox buildLogPage() {
        Label title = new Label("Live Log");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + FG + ";");
        Label sub = new Label("Toàn bộ hoạt động của server theo thời gian thực");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + FG_DIM + ";");
        VBox titleBox = new VBox(2, title, sub);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background:" + BG + "; -fx-background-color:" + BG + "; -fx-text-fill:" + GREEN + "; -fx-font-family:'Consolas'; -fx-font-size:13px; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox card = new VBox(logArea);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:" + BG_PANEL + "; -fx-background-radius:14; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:14;");
        VBox.setVgrow(card, Priority.ALWAYS);

        VBox page = new VBox(18, titleBox, card);
        VBox.setVgrow(page, Priority.ALWAYS);
        page.setStyle("-fx-background-color:" + BG + ";");
        return page;
    }

    private void updateUptime() {
        if (startTime == 0) return;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long h = elapsed / 3600, m = (elapsed % 3600) / 60, s = elapsed % 60;
        Platform.runLater(() -> lbUptime.setText(String.format("%02d:%02d:%02d", h, m, s)));
    }

    private void updateChart() {
        Platform.runLater(() -> {
            if (series == null) return;
            series.getData().add(new XYChart.Data<>(series.getData().size(), Math.random() * 5));
            if (series.getData().size() > 30) series.getData().remove(0);
        });
    }

    private void log(String s) {
        Platform.runLater(() -> {
            String t = java.time.LocalTime.now().withNano(0).toString();
            if (logArea != null) logArea.appendText("[" + t + "]  " + s + "\n");

            if (s.contains("[RECV]")) {
                totalRequests++;
                lbTotalReq.setText(String.valueOf(totalRequests));
                lbReqPerSec.setText(String.valueOf((int)(Math.random()*3+1)));

                if (s.contains("REGISTER")) {
                    totalRegisters++; lbReg.setText(String.valueOf(totalRegisters));
                    showToast("Account mới đăng ký", GREEN);
                    refreshAccounts();
                } else if (s.contains("LOGIN")) {
                    totalLogins++; lbLogin.setText(String.valueOf(totalLogins));
                } else if (s.contains("SEND")) {
                    totalSends++; lbSend.setText(String.valueOf(totalSends));
                    refreshMails();
                }

                pieChart.getData().clear();
                pieChart.getData().addAll(
                        new PieChart.Data("REGISTER", Math.max(totalRegisters,1)),
                        new PieChart.Data("LOGIN",    Math.max(totalLogins,1)),
                        new PieChart.Data("SEND",     Math.max(totalSends,1)));
            }
        });
    }

    private void showToast(String msg, String color) {
        Platform.runLater(() -> {
            FontIcon ic = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
            ic.setIconColor(Color.WHITE);
            ic.setIconSize(14);
            Label lbl = new Label("  " + msg);
            lbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;");
            HBox h = new HBox(ic, lbl);
            h.setAlignment(Pos.CENTER_LEFT);
            h.setPadding(new Insets(12, 18, 12, 14));
            h.setStyle("-fx-background-color:" + color + "; -fx-background-radius:10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 4);");
            h.setOpacity(0);

            toastBox.getChildren().add(h);

            FadeTransition fi = new FadeTransition(Duration.millis(300), h);
            fi.setFromValue(0);
            fi.setToValue(1);
            fi.play();

            PauseTransition p = new PauseTransition(Duration.seconds(3));
            p.setOnFinished(ev -> {
                FadeTransition fo = new FadeTransition(Duration.millis(300), h);
                fo.setFromValue(1);
                fo.setToValue(0);
                fo.setOnFinished(e2 -> toastBox.getChildren().remove(h));
                fo.play();
            });
            p.play();
        });
    }

    public static void main(String[] args) { launch(args); }
}