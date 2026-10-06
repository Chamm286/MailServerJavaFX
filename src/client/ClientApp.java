package client;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ClientApp extends Application {

    // ═══════════ PROFESSIONAL LIGHT THEME ═══════════
    private static final String BG_MAIN     = "#F7F9FC";
    private static final String BG_WHITE    = "#FFFFFF";
    private static final String BG_HEADER   = "#0D47A1";
    private static final String BG_HEADER_2 = "#1565C0";
    private static final String BG_HOVER    = "#EFF6FF";
    private static final String BG_ACTIVE   = "#DBEAFE";
    private static final String BORDER      = "#E2E8F0";
    private static final String BORDER_SOFT = "#CBD5E1";
    private static final String TEXT_DARK   = "#0F172A";
    private static final String TEXT_GRAY   = "#475569";
    private static final String TEXT_DIM    = "#94A3B8";
    private static final String BLUE        = "#1976D2";
    private static final String BLUE_BG     = "#DBEAFE";
    private static final String GREEN       = "#10B981";
    private static final String GREEN_BG    = "#D1FAE5";
    private static final String YELLOW      = "#F59E0B";
    private static final String RED         = "#EF4444";
    private static final String PURPLE      = "#8B5CF6";
    private static final String CYAN        = "#06B6D4";

    private ClientCore core;
    private String currentUser = null;
    private String currentEmail = null;
    private String currentPass = null;
    private String myIP = "unknown";

    private TextField     serverIpField, loginUserField, regUserField, regEmailField;
    private PasswordField loginPassField, regPassField, regPassConfirmField;

    private ListView<MailItem> mailList;
    private Label              readerFrom, readerTo, readerSubj, readerTime, readerFromIp;
    private TextArea           readerBody;
    private MailItem           selectedMail;

    private Label      avatarLabel, userEmailLabel, myIpLabel, statusLabel;
    private TextArea   activityLog;
    private StackPane  rootStack, centerContent;
    private VBox       authPage;
    private BorderPane mainPage;
    private Button     currentSideBtn = null;
    private String     currentFolder = "INBOX";
    private StackPane  modalLayer;
    private ListView<String> contactsList;
    private java.util.List<String> allContacts = new java.util.ArrayList<>();
    private Timeline autoRefreshTimer;
    private VBox     toastContainer;
    private boolean  isSending = false;

    @Override
    public void start(Stage stage) {
        try { core = new ClientCore(); }
        catch (Exception e) { e.printStackTrace(); }

        myIP = detectMyIP();

        rootStack = new StackPane();
        authPage  = buildAuthPage();
        mainPage  = buildMainPage();

        modalLayer = new StackPane();
        modalLayer.setVisible(false);
        modalLayer.setMouseTransparent(true);
        modalLayer.setStyle("-fx-background-color:rgba(0,0,0,0.5);");

        toastContainer = new VBox(8);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setMouseTransparent(true);
        toastContainer.setPadding(new Insets(70, 20, 20, 20));
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);

        StackPane master = new StackPane(rootStack, modalLayer, toastContainer);
        rootStack.getChildren().add(authPage);

        Scene scene = new Scene(master, 1350, 820);
        stage.setTitle("UDP Mail — Client");
        stage.setScene(scene);
        stage.show();

        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
            if (currentUser != null && currentPass != null && mailList != null && !isSending) {
                silentRefresh();
            }
        }));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    private String detectMyIP() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip.contains(".") && !ip.startsWith("169.254")) return ip;
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) { return "unknown"; }
    }

    public static class MailItem {
        public String folder, fileName, from, to, subject, time, preview;
        public boolean starred;
        public MailItem(String fo, String fn, String fr, String t, String s, String tm, String p, boolean st) {
            folder = fo; fileName = fn; from = fr; to = t; subject = s; time = tm; preview = p; starred = st;
        }
    }

    // ==================================================
    //         AUTH PAGE
    // ==================================================
    private VBox buildAuthPage() {

        // ═══ HEADER XANH ═══
        FontIcon logoIc = new FontIcon(FontAwesomeSolid.ENVELOPE);
        logoIc.setIconSize(26);
        logoIc.setIconColor(Color.WHITE);
        StackPane logoBox = new StackPane(logoIc);
        logoBox.setStyle("-fx-background-color:rgba(255,255,255,0.18); -fx-background-radius:12; -fx-padding:12;");

        Label appName = new Label("UDP MAIL CLIENT");
        appName.setStyle("-fx-text-fill:white; -fx-font-size:20px; -fx-font-weight:bold;");
        Label appSub = new Label("Ứng dụng khách — Kết nối đến máy chủ mail");
        appSub.setStyle("-fx-text-fill:rgba(255,255,255,0.85); -fx-font-size:11px;");
        VBox titleBox = new VBox(2, appName, appSub);

        HBox titleSection = new HBox(14, logoBox, titleBox);
        titleSection.setAlignment(Pos.CENTER_LEFT);

        Region sp0 = new Region();
        HBox.setHgrow(sp0, Priority.ALWAYS);

        Label myIpBadge = new Label("● IP máy bạn: " + myIP);
        myIpBadge.setStyle(
            "-fx-text-fill:white;" +
            "-fx-font-size:12px;" +
            "-fx-font-weight:bold;" +
            "-fx-background-color:rgba(16,185,129,0.55);" +
            "-fx-padding:8 16;" +
            "-fx-background-radius:20;");

        HBox authHeader = new HBox(14, titleSection, sp0, myIpBadge);
        authHeader.setAlignment(Pos.CENTER_LEFT);
        authHeader.setPadding(new Insets(20, 28, 20, 28));
        authHeader.setStyle("-fx-background-color: linear-gradient(to right, " + BG_HEADER + ", " + BG_HEADER_2 + ");");

        // ═══ FORM CARD ═══
        Label ipTitle = new Label("🌐  SERVER IP — KẾT NỐI ĐẾN SERVER");
        ipTitle.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:12px; -fx-font-weight:bold;");

        Label ipHint = new Label("Nhập IP của máy chạy Server (VD: 192.168.1.10 hoặc localhost)");
        ipHint.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:10px;");

        serverIpField = lightField(FontAwesomeSolid.GLOBE, "VD: 192.168.1.10 hoặc localhost");
        serverIpField.setText("localhost");
        serverIpField.setPrefHeight(46);
        HBox.setHgrow(serverIpField, Priority.ALWAYS);

        Button testBtn = new Button();
        FontIcon testIc = new FontIcon(FontAwesomeSolid.PLUG);
        testIc.setIconSize(13);
        testIc.setIconColor(Color.WHITE);
        Label testLbl = new Label("  KẾT NỐI");
        testLbl.setGraphic(testIc);
        testLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;");
        testBtn.setGraphic(testLbl);
        testBtn.setPrefHeight(46);
        testBtn.setPrefWidth(140);
        testBtn.setStyle("-fx-background-color:" + GREEN + "; -fx-background-radius:10; -fx-cursor:hand;");
        testBtn.setOnMouseEntered(e -> testBtn.setStyle("-fx-background-color:#059669; -fx-background-radius:10; -fx-cursor:hand;"));
        testBtn.setOnMouseExited(e -> testBtn.setStyle("-fx-background-color:" + GREEN + "; -fx-background-radius:10; -fx-cursor:hand;"));
        testBtn.setOnAction(e -> testConnection());

        HBox ipRow = new HBox(10, serverIpField, testBtn);
        ipRow.setAlignment(Pos.CENTER);

        VBox ipBox = new VBox(6, ipTitle, ipHint, ipRow);
        ipBox.setAlignment(Pos.CENTER_LEFT);
        ipBox.setPadding(new Insets(14, 18, 14, 18));
        ipBox.setStyle("-fx-background-color:" + BG_HOVER + "; -fx-background-radius:12; -fx-border-color:" + BLUE + "40; -fx-border-radius:12;");

        // Tabs
        Button tabLogin    = authTab("Đăng nhập", true);
        Button tabRegister = authTab("Tạo tài khoản", false);
        HBox tabs = new HBox(4, tabLogin, tabRegister);
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(8, 0, 4, 0));

        // Login
        loginUserField = lightField(FontAwesomeSolid.USER, "Tên đăng nhập");
        loginPassField = lightPassword(FontAwesomeSolid.LOCK, "Mật khẩu");

        Button loginBtn = primaryBtn("ĐĂNG NHẬP", BLUE, "#1565C0");
        loginBtn.setPrefWidth(360);
        loginBtn.setOnAction(e -> doLogin());

        Label hintLogin = new Label("Chưa có tài khoản? Chuyển sang tab \"Tạo tài khoản\"");
        hintLogin.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");

        VBox loginForm = new VBox(14,
                labeledInput("Tên đăng nhập", loginUserField),
                labeledInput("Mật khẩu", loginPassField),
                new Region(),
                loginBtn, hintLogin);
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setPadding(new Insets(20, 0, 0, 0));

        // Register
        regUserField  = lightField(FontAwesomeSolid.USER_PLUS, "Tên đăng nhập mới");
        regEmailField = lightField(FontAwesomeSolid.AT, "Email tự động");
        regEmailField.setDisable(true);

        regUserField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) regEmailField.setText("");
            else regEmailField.setText(val.trim() + "@gmail.com");
        });

        regPassField        = lightPassword(FontAwesomeSolid.LOCK, "Mật khẩu");
        regPassConfirmField = lightPassword(FontAwesomeSolid.LOCK, "Xác nhận mật khẩu");

        Button registerBtn = primaryBtn("TẠO TÀI KHOẢN", GREEN, "#059669");
        registerBtn.setPrefWidth(360);
        registerBtn.setOnAction(e -> doRegister());

        Label hintReg = new Label("Mỗi tài khoản có email dạng username@gmail.com");
        hintReg.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");

        VBox registerForm = new VBox(14,
                labeledInput("Tên đăng nhập", regUserField),
                labeledInput("Email", regEmailField),
                labeledInput("Mật khẩu", regPassField),
                labeledInput("Xác nhận mật khẩu", regPassConfirmField),
                new Region(),
                registerBtn, hintReg);
        registerForm.setAlignment(Pos.CENTER);
        registerForm.setPadding(new Insets(20, 0, 0, 0));
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        tabLogin.setOnAction(e -> {
            tabLogin.setStyle(authTabStyle(true));
            tabRegister.setStyle(authTabStyle(false));
            loginForm.setVisible(true); loginForm.setManaged(true);
            registerForm.setVisible(false); registerForm.setManaged(false);
        });
        tabRegister.setOnAction(e -> {
            tabLogin.setStyle(authTabStyle(false));
            tabRegister.setStyle(authTabStyle(true));
            loginForm.setVisible(false); loginForm.setManaged(false);
            registerForm.setVisible(true); registerForm.setManaged(true);
        });

        VBox card = new VBox(12, ipBox, tabs, loginForm, registerForm);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(24, 38, 30, 38));
        card.setMaxWidth(480);
        card.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:16; -fx-border-color:" + BORDER + "; -fx-border-radius:16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 4);");

        VBox cardWrap = new VBox(card);
        cardWrap.setAlignment(Pos.CENTER);
        cardWrap.setPadding(new Insets(40));
        VBox.setVgrow(cardWrap, Priority.ALWAYS);

        VBox page = new VBox(authHeader, cardWrap);
        page.setStyle("-fx-background-color:" + BG_MAIN + ";");
        VBox.setVgrow(cardWrap, Priority.ALWAYS);
        return page;
    }

    private Button authTab(String text, boolean active) {
        Button b = new Button(text);
        b.setPrefWidth(200);
        b.setPrefHeight(42);
        b.setStyle(authTabStyle(active));
        return b;
    }

    private String authTabStyle(boolean active) {
        if (active) {
            return "-fx-background-color:" + BLUE + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px; -fx-background-radius:10; -fx-cursor:hand;";
        }
        return "-fx-background-color:#F1F5F9; -fx-text-fill:" + TEXT_GRAY + "; -fx-font-size:13px; -fx-background-radius:10; -fx-cursor:hand;";
    }

    private TextField lightField(FontAwesomeSolid icon, String prompt) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(13);
        fi.setIconColor(Color.web(TEXT_DIM));

        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefHeight(44);
        f.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px; -fx-padding:0 4;");

        HBox wrapper = new HBox(10, fi, f);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(0, 14, 0, 14));
        wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        HBox.setHgrow(f, Priority.ALWAYS);

        f.focusedProperty().addListener((o, a, b) -> {
            if (b) wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BLUE + "; -fx-border-radius:10; -fx-border-width:2;");
            else wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        });

        return f;
    }

    private PasswordField lightPassword(FontAwesomeSolid icon, String prompt) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(13);
        fi.setIconColor(Color.web(TEXT_DIM));

        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setPrefHeight(44);
        f.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px; -fx-padding:0 4;");

        HBox wrapper = new HBox(10, fi, f);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(0, 14, 0, 14));
        wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        HBox.setHgrow(f, Priority.ALWAYS);

        f.focusedProperty().addListener((o, a, b) -> {
            if (b) wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BLUE + "; -fx-border-radius:10; -fx-border-width:2;");
            else wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        });

        return f;
    }

    private VBox labeledInput(String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_DARK + ";");
        VBox v = new VBox(6, l, field);
        v.setMaxWidth(360);
        return v;
    }

    private Button primaryBtn(String text, String c1, String c2) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px;");

        Button b = new Button();
        b.setGraphic(l);
        b.setPrefHeight(46);
        b.setStyle("-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + "); -fx-background-radius:10; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: linear-gradient(to right, " + c2 + ", " + c1 + "); -fx-background-radius:10; -fx-cursor:hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + "); -fx-background-radius:10; -fx-cursor:hand;"));
        return b;
    }

    private void testConnection() {
        String ip = serverIpField.getText().trim();
        if (ip.isEmpty()) { showAlert("Vui lòng nhập Server IP!"); return; }
        core.setHost(ip);
        try {
            String resp = core.send("PING");
            if (resp.startsWith("OK")) showAlert("KẾT NỐI THÀNH CÔNG!\n\nServer: " + ip + ":2023\nIP máy bạn: " + myIP);
            else showAlert("Server phản hồi: " + resp);
        } catch (java.net.SocketTimeoutException te) {
            showAlert("KHÔNG KẾT NỐI ĐƯỢC!\n\nIP: " + ip + ":2023\n\nKiểm tra:\n1. Server đã START chưa?\n2. IP đúng chưa?\n3. Cùng WiFi chưa?\n4. Firewall đã mở port 2023?");
        } catch (Exception e) {
            showAlert("Lỗi kết nối: " + e.getMessage());
        }
    }

    // ==================================================
    //              MAIN PAGE
    // ==================================================
    private BorderPane buildMainPage() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_MAIN + ";");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenterArea());
        root.setBottom(buildActivityBar());
        return root;
    }

    private HBox buildTopBar() {
        FontIcon logoIc = new FontIcon(FontAwesomeSolid.ENVELOPE);
        logoIc.setIconSize(20);
        logoIc.setIconColor(Color.WHITE);
        StackPane logoBox = new StackPane(logoIc);
        logoBox.setStyle("-fx-background-color:rgba(255,255,255,0.18); -fx-background-radius:10; -fx-padding:10;");

        Label title = new Label("UDP MAIL CLIENT");
        title.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");
        Label sub = new Label("Port 2023");
        sub.setStyle("-fx-text-fill:rgba(255,255,255,0.75); -fx-font-size:10px;");
        VBox titleBox = new VBox(0, title, sub);

        HBox logoSection = new HBox(10, logoBox, titleBox);
        logoSection.setAlignment(Pos.CENTER_LEFT);

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        // ⭐ IP badge
        myIpLabel = new Label("● IP: " + myIP);
        myIpLabel.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(6,182,212,0.55); -fx-padding:6 14; -fx-background-radius:20;");

        statusLabel = new Label("● Online");
        statusLabel.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(16,185,129,0.65); -fx-padding:6 14; -fx-background-radius:20;");

        avatarLabel = new Label("?");
        avatarLabel.setStyle("-fx-background-color:white; -fx-text-fill:" + BLUE + "; -fx-font-size:13px; -fx-font-weight:bold; -fx-background-radius:50; -fx-padding:8 12;");

        userEmailLabel = new Label("guest");
        userEmailLabel.setStyle("-fx-text-fill:white; -fx-font-size:12px; -fx-font-weight:bold;");

        HBox userHBox = new HBox(8, avatarLabel, userEmailLabel);
        userHBox.setAlignment(Pos.CENTER);
        userHBox.setPadding(new Insets(4, 12, 4, 4));
        userHBox.setStyle("-fx-background-color:rgba(255,255,255,0.18); -fx-background-radius:24;");

        HBox top = new HBox(14, logoSection, sp1, myIpLabel, statusLabel, userHBox);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(14, 24, 14, 24));
        top.setStyle("-fx-background-color: linear-gradient(to right, " + BG_HEADER + ", " + BG_HEADER_2 + ");");
        return top;
    }

    private VBox buildSidebar() {
        VBox box = new VBox(4);
        box.setPrefWidth(240);
        box.setPadding(new Insets(16, 12, 16, 12));
        box.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-border-color:" + BORDER + "; -fx-border-width:0 1 0 0;");

        Label sectionLbl = new Label("MENU");
        sectionLbl.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:0 0 6 8;");

        Button inboxBtn   = sideBtn(FontAwesomeSolid.INBOX,        "Hộp thư đến",  true);
        Button sentBtn    = sideBtn(FontAwesomeSolid.PAPER_PLANE,  "Đã gửi",       false);
        Button starredBtn = sideBtn(FontAwesomeSolid.STAR,         "Đã đánh dấu",  false);
        Button trashBtn   = sideBtn(FontAwesomeSolid.TRASH_ALT,    "Thùng rác",    false);
        Button contactBtn = sideBtn(FontAwesomeSolid.ADDRESS_BOOK, "Danh bạ",      false);
        Button logoutBtn  = sideBtn(FontAwesomeSolid.SIGN_OUT_ALT, "Đăng xuất",    false);

        currentSideBtn = inboxBtn;

        inboxBtn.setOnAction(e   -> switchFolder(inboxBtn, "INBOX"));
        sentBtn.setOnAction(e    -> switchFolder(sentBtn, "SENT"));
        starredBtn.setOnAction(e -> switchFolder(starredBtn, "STARRED"));
        trashBtn.setOnAction(e   -> switchFolder(trashBtn, "TRASH"));
        contactBtn.setOnAction(e -> { loadContacts(); switchFolder(contactBtn, "CONTACTS"); });
        logoutBtn.setOnAction(e  -> {
            currentUser = null; currentEmail = null; currentPass = null;
            avatarLabel.setText("?");
            userEmailLabel.setText("guest");
            log("Đã đăng xuất");
            rootStack.getChildren().setAll(authPage);
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        FontIcon penIc = new FontIcon(FontAwesomeSolid.PEN);
        penIc.setIconSize(14);
        penIc.setIconColor(Color.WHITE);
        Label composeLbl = new Label("  Soạn thư");
        composeLbl.setGraphic(penIc);
        composeLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px;");
        Button composeBtn = new Button();
        composeBtn.setGraphic(composeLbl);
        composeBtn.setPrefWidth(216);
        composeBtn.setPrefHeight(46);
        composeBtn.setAlignment(Pos.CENTER_LEFT);
        composeBtn.setPadding(new Insets(0, 0, 0, 20));
        composeBtn.setStyle("-fx-background-color: linear-gradient(to right, " + BLUE + ", #1565C0); -fx-background-radius:10; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(25,118,210,0.35), 10, 0, 0, 0);");
        composeBtn.setOnMouseEntered(e -> composeBtn.setStyle("-fx-background-color: linear-gradient(to right, #1565C0, " + BLUE + "); -fx-background-radius:10; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(25,118,210,0.55), 14, 0, 0, 0);"));
        composeBtn.setOnMouseExited (e -> composeBtn.setStyle("-fx-background-color: linear-gradient(to right, " + BLUE + ", #1565C0); -fx-background-radius:10; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(25,118,210,0.35), 10, 0, 0, 0);"));
        composeBtn.setOnAction(e -> openCompose("", "", ""));

        box.getChildren().addAll(sectionLbl, inboxBtn, sentBtn, starredBtn, trashBtn, contactBtn, spacer, composeBtn, logoutBtn);
        return box;
    }

    private Button sideBtn(FontAwesomeSolid ic, String text, boolean active) {
        FontIcon fi = new FontIcon(ic);
        fi.setIconSize(14);
        fi.setIconColor(Color.web(active ? BLUE : TEXT_GRAY));
        Label l = new Label("  " + text);
        l.setStyle("-fx-text-fill:" + (active ? BLUE : TEXT_DARK) + "; -fx-font-size:13px; -fx-font-weight:" + (active ? "bold" : "normal") + ";");
        HBox h = new HBox(fi, l);
        h.setAlignment(Pos.CENTER_LEFT);

        Button b = new Button();
        b.setGraphic(h);
        b.setPrefWidth(216);
        b.setPrefHeight(42);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(0, 0, 0, 16));
        b.setStyle(sideBtnStyle(active));
        b.setOnMouseEntered(e -> { if (b != currentSideBtn) b.setStyle("-fx-background-color:" + BG_HOVER + "; -fx-background-radius:10; -fx-cursor:hand;"); });
        b.setOnMouseExited (e -> { if (b != currentSideBtn) b.setStyle(sideBtnStyle(false)); });
        return b;
    }

    private String sideBtnStyle(boolean active) {
        if (active) return "-fx-background-color:" + BG_ACTIVE + "; -fx-background-radius:10; -fx-cursor:hand;";
        return "-fx-background-color:transparent; -fx-background-radius:10; -fx-cursor:hand;";
    }

    private void switchFolder(Button btn, String folder) {
        if (currentSideBtn != null) {
            ((HBox) currentSideBtn.getGraphic()).getChildren().forEach(n -> {
                if (n instanceof FontIcon) ((FontIcon) n).setIconColor(Color.web(TEXT_GRAY));
                if (n instanceof Label) ((Label) n).setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:13px;");
            });
            currentSideBtn.setStyle(sideBtnStyle(false));
        }
        currentSideBtn = btn;
        if (btn != null) {
            ((HBox) btn.getGraphic()).getChildren().forEach(n -> {
                if (n instanceof FontIcon) ((FontIcon) n).setIconColor(Color.web(BLUE));
                if (n instanceof Label) ((Label) n).setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:13px; -fx-font-weight:bold;");
            });
            btn.setStyle(sideBtnStyle(true));
        }

        currentFolder = folder;
        if (folder.equals("CONTACTS")) showContactsView();
        else { loadFolder(folder); showInboxView(); }
    }

    private StackPane buildCenterArea() {
        centerContent = new StackPane();
        centerContent.setStyle("-fx-background-color:" + BG_MAIN + ";");
        HBox.setHgrow(centerContent, Priority.ALWAYS);
        return centerContent;
    }

    private VBox buildActivityBar() {
        FontIcon ic = new FontIcon(FontAwesomeSolid.TERMINAL);
        ic.setIconColor(Color.web(BLUE));
        ic.setIconSize(12);
        Label title = new Label("  Nhật ký hoạt động");
        title.setGraphic(ic);
        title.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:12px; -fx-font-weight:bold;");

        Label hint = new Label("IP máy bạn: " + myIP);
        hint.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:10px; -fx-font-weight:bold; -fx-background-color:" + BLUE_BG + "; -fx-padding:4 10; -fx-background-radius:10;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox header = new HBox(8, title, sp, hint);
        header.setAlignment(Pos.CENTER_LEFT);

        activityLog = new TextArea();
        activityLog.setEditable(false);
        activityLog.setPrefRowCount(3);
        activityLog.setStyle("-fx-control-inner-background:" + BG_WHITE + "; -fx-background-color:" + BG_WHITE + "; -fx-text-fill:" + GREEN + "; -fx-font-family:'Consolas'; -fx-font-size:11px; -fx-background-radius:8; -fx-border-color:" + BORDER + "; -fx-border-radius:8;");

        VBox v = new VBox(6, header, activityLog);
        v.setPadding(new Insets(10, 20, 12, 20));
        v.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-border-color:" + BORDER + "; -fx-border-width:1 0 0 0;");
        return v;
    }

    private void showInboxView() {
        HBox view = new HBox();

        VBox leftPanel = new VBox(0);

        FontIcon inboxIc = new FontIcon(FontAwesomeSolid.INBOX);
        inboxIc.setIconSize(16);
        inboxIc.setIconColor(Color.web(BLUE));
        Label listTitle = new Label("  " + folderTitle(currentFolder));
        listTitle.setGraphic(inboxIc);
        listTitle.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:15px; -fx-font-weight:bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button();
        FontIcon refIc = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        refIc.setIconSize(12);
        refIc.setIconColor(Color.web(TEXT_GRAY));
        refreshBtn.setGraphic(refIc);
        refreshBtn.setStyle("-fx-background-color:transparent; -fx-cursor:hand; -fx-padding:6;");
        refreshBtn.setOnAction(e -> { loadFolder(currentFolder); log("🔄 Đã làm mới"); });

        HBox header = new HBox(8, listTitle, sp, refreshBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent; -fx-border-width:0 0 1 0; -fx-background-color:" + BG_WHITE + ";");

        mailList = new ListView<>();
        mailList.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-border-color:transparent; -fx-control-inner-background:" + BG_WHITE + ";");
        mailList.setPlaceholder(emptyState());
        mailList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(MailItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color:" + BG_WHITE + ";");
                } else {
                    FontIcon starIc = new FontIcon(item.starred ? FontAwesomeSolid.STAR : FontAwesomeSolid.ENVELOPE);
                    starIc.setIconSize(14);
                    starIc.setIconColor(Color.web(item.starred ? YELLOW : BLUE));

                    Label from = new Label(item.from);
                    from.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:13px; -fx-font-weight:bold;");
                    Label subj = new Label(item.subject);
                    subj.setStyle("-fx-text-fill:" + TEXT_GRAY + "; -fx-font-size:12px;");
                    Label prev = new Label(item.preview);
                    prev.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");
                    prev.setMaxWidth(280);
                    Label time = new Label(item.time);
                    time.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:10px;");

                    VBox textBox = new VBox(2, from, subj, prev);
                    HBox.setHgrow(textBox, Priority.ALWAYS);

                    HBox h = new HBox(10, starIc, textBox, time);
                    h.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(h);
                    setStyle("-fx-padding:12 16; -fx-background-color:" + BG_WHITE + "; -fx-border-color:transparent transparent " + BORDER + " transparent; -fx-border-width:0 0 1 0; -fx-cursor:hand;");
                }
            }
        });
        mailList.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) { selectedMail = item; showReader(item); }
        });
        VBox.setVgrow(mailList, Priority.ALWAYS);

        leftPanel.getChildren().addAll(header, mailList);
        leftPanel.setPrefWidth(420);
        leftPanel.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-border-color:transparent " + BORDER + " transparent transparent; -fx-border-width:0 1 0 0;");

        VBox readerPane = buildReaderPane();
        HBox.setHgrow(readerPane, Priority.ALWAYS);

        view.getChildren().addAll(leftPanel, readerPane);
        view.setStyle("-fx-background-color:" + BG_MAIN + ";");

        centerContent.getChildren().setAll(view);
    }

    private VBox emptyState() {
        FontIcon fi = new FontIcon(FontAwesomeSolid.INBOX);
        fi.setIconSize(48);
        fi.setIconColor(Color.web(TEXT_DIM));
        Label l = new Label("Không có mail nào");
        l.setStyle("-fx-text-fill:" + TEXT_GRAY + "; -fx-font-size:14px;");
        VBox v = new VBox(10, fi, l);
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private String folderTitle(String folder) {
        switch (folder) {
            case "INBOX":   return "Hộp thư đến";
            case "SENT":    return "Đã gửi";
            case "STARRED": return "Đã đánh dấu sao";
            case "TRASH":   return "Thùng rác";
            default:        return "Mail";
        }
    }

    // ═══════════════════════════════════════════════════════
    //   READER PANE
    // ═══════════════════════════════════════════════════════
    private VBox buildReaderPane() {
        Label subjLabel = new Label("Chọn một mail để đọc");
        subjLabel.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:20px; -fx-font-weight:bold;");
        subjLabel.setWrapText(true);

        Label senderAvatar = new Label("?");
        senderAvatar.setStyle("-fx-background-color: linear-gradient(to bottom right, " + BLUE + ", #1565C0); -fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold; -fx-background-radius:50; -fx-padding:10 14;");

        readerFrom = new Label("—");
        readerFrom.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:13px; -fx-font-weight:bold;");
        readerTo = new Label("—");
        readerTo.setStyle("-fx-text-fill:" + TEXT_GRAY + "; -fx-font-size:12px;");
        readerFromIp = new Label("—");
        readerFromIp.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:" + BLUE_BG + "; -fx-padding:3 8; -fx-background-radius:6;");
        readerTime = new Label("—");
        readerTime.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");

        VBox senderInfo = new VBox(2, readerFrom, readerTo, readerFromIp);
        HBox senderRow = new HBox(10, senderAvatar, senderInfo);
        senderRow.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox metaRow = new HBox(senderRow, sp, readerTime);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon replyIc = new FontIcon(FontAwesomeSolid.REPLY);
        replyIc.setIconSize(12);
        replyIc.setIconColor(Color.web(BLUE));
        Button replyBtn = styledToolBtn("Trả lời", replyIc, BLUE);

        FontIcon starIc = new FontIcon(FontAwesomeSolid.STAR);
        starIc.setIconSize(12);
        starIc.setIconColor(Color.web(YELLOW));
        Button starBtn = styledToolBtn("Đánh dấu", starIc, YELLOW);

        FontIcon delIc = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        delIc.setIconSize(12);
        delIc.setIconColor(Color.web(RED));
        Button delBtn = styledToolBtn("Xóa", delIc, RED);

        replyBtn.setOnAction(e -> {
            if (selectedMail != null) openCompose(selectedMail.from, "Re: " + selectedMail.subject, "\n\n--- Mail gốc ---\n" + selectedMail.preview);
        });
        starBtn.setOnAction(e -> {
            if (selectedMail != null) {
                try {
                    String resp = core.send("STAR|" + currentUser + "|" + selectedMail.folder + "|" + selectedMail.fileName);
                    log("⭐ " + resp.substring(3));
                    loadFolder(currentFolder);
                } catch (Exception ignored) {}
            }
        });
        delBtn.setOnAction(e -> {
            if (selectedMail != null) {
                try {
                    String resp = core.send("DELETE|" + currentUser + "|" + selectedMail.folder + "|" + selectedMail.fileName);
                    log("🗑 " + resp.substring(3));
                    selectedMail = null;
                    subjLabel.setText("Chọn một mail để đọc");
                    readerFrom.setText("—");
                    readerTo.setText("—");
                    readerFromIp.setText("—");
                    readerTime.setText("—");
                    readerBody.clear();
                    loadFolder(currentFolder);
                } catch (Exception ignored) {}
            }
        });

        HBox toolbar = new HBox(8, replyBtn, starBtn, delBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        readerBody = new TextArea();
        readerBody.setEditable(false);
        readerBody.setWrapText(true);
        readerBody.setStyle("-fx-control-inner-background:" + BG_WHITE + "; -fx-background-color:" + BG_WHITE + "; -fx-text-fill:" + TEXT_DARK + "; -fx-font-size:13px; -fx-font-family:'Consolas'; -fx-border-color:transparent; -fx-background-radius:12; -fx-border-radius:12; -fx-padding:16;");
        VBox.setVgrow(readerBody, Priority.ALWAYS);

        VBox headerCard = new VBox(14, subjLabel, metaRow, toolbar);
        headerCard.setPadding(new Insets(20, 20, 16, 20));
        headerCard.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:12; -fx-border-color:" + BORDER + "; -fx-border-radius:12;");

        VBox bodyCard = new VBox(readerBody);
        bodyCard.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:12; -fx-border-color:" + BORDER + "; -fx-border-radius:12;");
        VBox.setVgrow(bodyCard, Priority.ALWAYS);
        VBox.setVgrow(readerBody, Priority.ALWAYS);

        VBox pane = new VBox(14, headerCard, bodyCard);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color:" + BG_MAIN + ";");

        readerSubj = subjLabel;
        return pane;
    }

    private Button styledToolBtn(String text, FontIcon ic, String color) {
        Label l = new Label("  " + text);
        l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:bold;");
        HBox h = new HBox(ic, l);
        h.setAlignment(Pos.CENTER);
        Button b = new Button();
        b.setGraphic(h);
        b.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:8; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:8; -fx-padding:8 16; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:" + BG_HOVER + "; -fx-background-radius:8; -fx-border-color:" + color + "80; -fx-border-radius:8; -fx-padding:8 16; -fx-cursor:hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:8; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:8; -fx-padding:8 16; -fx-cursor:hand;"));
        return b;
    }

    private void showReader(MailItem item) {
        readerSubj.setText(item.subject);
        readerFrom.setText(item.from);
        readerTo.setText("đến " + item.to);
        readerTime.setText(item.time);

        try {
            String resp = core.send("GET|" + currentUser + "|" + item.folder + "|" + item.fileName);
            if (resp.startsWith("OK|")) {
                String content = resp.substring(3);

                String fromIp = extractLine(content, "FromIP");
                if (fromIp.isEmpty()) fromIp = extractLine(content, "IP nguoi gui");
                readerFromIp.setText("IP người gửi: " + (fromIp.isEmpty() ? "—" : fromIp));

                String body = extractBody(content);
                if (body.isEmpty()) body = content;
                readerBody.setText(body);
            } else {
                readerBody.setText(item.preview);
                readerFromIp.setText("IP người gửi: —");
            }
        } catch (Exception e) {
            readerBody.setText(item.preview);
            readerFromIp.setText("IP người gửi: —");
        }
    }

    private String extractLine(String content, String key) {
        for (String line : content.split("\n")) {
            if (line.startsWith(key + ":")) return line.substring(key.length() + 1).trim();
            if (line.startsWith(key)) {
                int idx = line.indexOf(':');
                if (idx > 0) return line.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private String extractBody(String content) {
        int idx = content.indexOf("\n\n");
        if (idx > 0) return content.substring(idx + 2).trim();
        for (String line : content.split("\n")) {
            if (line.startsWith("Noi dung")) {
                int cIdx = line.indexOf(':');
                if (cIdx > 0) return line.substring(cIdx + 1).trim();
            }
        }
        return content.trim();
    }

    private void showContactsView() {
        Label title = new Label("📇 Danh bạ");
        title.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:18px; -fx-font-weight:bold;");

        Label hint = new Label("Bấm đúp vào một liên hệ để soạn thư");
        hint.setStyle("-fx-text-fill:" + TEXT_GRAY + "; -fx-font-size:12px;");

        contactsList = new ListView<>();
        contactsList.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-border-color:transparent; -fx-control-inner-background:" + BG_WHITE + ";");
        contactsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    FontIcon fi = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
                    fi.setIconSize(20);
                    fi.setIconColor(Color.web(BLUE));
                    Label t = new Label(item);
                    t.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:14px;");
                    HBox h = new HBox(12, fi, t);
                    h.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(h);
                    setStyle("-fx-padding:12 16; -fx-background-color:" + BG_WHITE + "; -fx-border-color:transparent transparent " + BORDER + " transparent; -fx-border-width:0 0 1 0; -fx-cursor:hand;");
                }
            }
        });
        contactsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = contactsList.getSelectionModel().getSelectedItem();
                if (sel != null) openCompose(sel, "", "");
            }
        });
        VBox.setVgrow(contactsList, Priority.ALWAYS);

        VBox card = new VBox(12, title, hint, contactsList);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:12; -fx-border-color:" + BORDER + "; -fx-border-radius:12;");
        VBox.setVgrow(card, Priority.ALWAYS);

        centerContent.getChildren().setAll(card);

        if (allContacts.isEmpty()) loadContacts();
        else contactsList.getItems().setAll(allContacts);
    }

    // ==================================================
    //              COMPOSE MODAL
    // ==================================================
    private void openCompose(String to, String subject, String content) {
        FontIcon ic = new FontIcon(FontAwesomeSolid.PAPER_PLANE);
        ic.setIconSize(18);
        ic.setIconColor(Color.WHITE);
        StackPane icBox = new StackPane(ic);
        icBox.setStyle("-fx-background-color: linear-gradient(to bottom right, " + BLUE + ", #1565C0); -fx-background-radius:10; -fx-padding:10;");

        Label title = new Label("THƯ MỚI");
        title.setStyle("-fx-text-fill:" + TEXT_DARK + "; -fx-font-size:15px; -fx-font-weight:bold;");
        Label subTitle = new Label("IP máy bạn (FromIP): " + myIP);
        subTitle.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:" + BLUE_BG + "; -fx-padding:2 8; -fx-background-radius:6;");
        VBox titleBox = new VBox(4, title, subTitle);

        HBox titleSection = new HBox(12, icBox, titleBox);
        titleSection.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        FontIcon xIc = new FontIcon(FontAwesomeSolid.TIMES);
        xIc.setIconSize(14);
        xIc.setIconColor(Color.web(TEXT_GRAY));
        Button closeBtn = new Button();
        closeBtn.setGraphic(xIc);
        closeBtn.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:8 12; -fx-border-color:" + BORDER + "; -fx-border-radius:8;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color:" + RED + "30; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:8 12; -fx-border-color:" + RED + "; -fx-border-radius:8;"));
        closeBtn.setOnMouseExited (e -> closeBtn.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:8 12; -fx-border-color:" + BORDER + "; -fx-border-radius:8;"));
        closeBtn.setOnAction(e -> closeModal());

        HBox header = new HBox(10, titleSection, sp, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent; -fx-border-width:0 0 1 0;");

        FontIcon toIc = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        toIc.setIconSize(14);
        toIc.setIconColor(Color.web(BLUE));

        TextField toF = new TextField();
        toF.setPromptText("username hoặc username@gmail.com");
        toF.setText(to);
        toF.setPrefHeight(44);
        toF.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px;");
        HBox.setHgrow(toF, Priority.ALWAYS);

        Label toLbl = new Label("Đến:");
        toLbl.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:13px; -fx-font-weight:bold;");

        HBox toRow = new HBox(10, toIc, toLbl, toF);
        toRow.setAlignment(Pos.CENTER_LEFT);
        toRow.setPadding(new Insets(0, 16, 0, 14));
        toRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");
        toRow.setPrefHeight(48);

        toF.focusedProperty().addListener((o, a, b) -> {
            if (b) toRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BLUE + "; -fx-border-radius:12; -fx-border-width:2;");
            else   toRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");
        });

        FontIcon subIc = new FontIcon(FontAwesomeSolid.TAG);
        subIc.setIconSize(14);
        subIc.setIconColor(Color.web(YELLOW));

        TextField subF = new TextField();
        subF.setPromptText("Tiêu đề thư...");
        subF.setText(subject);
        subF.setPrefHeight(44);
        subF.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px;");
        HBox.setHgrow(subF, Priority.ALWAYS);

        Label subLbl = new Label("Tiêu đề:");
        subLbl.setStyle("-fx-text-fill:" + YELLOW + "; -fx-font-size:13px; -fx-font-weight:bold;");

        HBox subRow = new HBox(10, subIc, subLbl, subF);
        subRow.setAlignment(Pos.CENTER_LEFT);
        subRow.setPadding(new Insets(0, 16, 0, 14));
        subRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");
        subRow.setPrefHeight(48);

        subF.focusedProperty().addListener((o, a, b) -> {
            if (b) subRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BLUE + "; -fx-border-radius:12; -fx-border-width:2;");
            else   subRow.setStyle("-fx-background-color:" + BG_MAIN + "; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12;");
        });

        TextArea bodyA = new TextArea();
        bodyA.setPromptText("Viết nội dung thư ở đây...");
        bodyA.setText(content);
        bodyA.setWrapText(true);
        bodyA.setPrefRowCount(10);
        bodyA.setStyle("-fx-control-inner-background:" + BG_MAIN + "; -fx-background-color:" + BG_MAIN + "; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px; -fx-font-family:'Segoe UI'; -fx-background-radius:12; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:12; -fx-padding:16;");
        VBox.setVgrow(bodyA, Priority.ALWAYS);

        FontIcon sendIc = new FontIcon(FontAwesomeSolid.PAPER_PLANE);
        sendIc.setIconSize(14);
        sendIc.setIconColor(Color.WHITE);
        Label sendLbl = new Label("  GỬI");
        sendLbl.setGraphic(sendIc);
        sendLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px;");

        Button sendBtn = new Button();
        sendBtn.setGraphic(sendLbl);
        sendBtn.setPrefHeight(42);
        sendBtn.setPrefWidth(140);
        sendBtn.setStyle("-fx-background-color: linear-gradient(to right, " + BLUE + ", #1565C0); -fx-background-radius:21; -fx-cursor:hand;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle("-fx-background-color: linear-gradient(to right, #1565C0, " + BLUE + "); -fx-background-radius:21; -fx-cursor:hand;"));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle("-fx-background-color: linear-gradient(to right, " + BLUE + ", #1565C0); -fx-background-radius:21; -fx-cursor:hand;"));

        sendBtn.setOnAction(e -> {
            String t = toF.getText().trim();
            String s = subF.getText().trim().replace("|", "/");
            String b = bodyA.getText().trim().replace("|", "/");
            if (t.isEmpty()) { showAlert("Vui lòng nhập người nhận!"); return; }
            if (b.isEmpty()) { showAlert("Vui lòng nhập nội dung thư!"); return; }

            String toEmail = t.contains("@") ? t : t + "@gmail.com";

            isSending = true;
            try {
                String resp = core.send("SEND|" + currentUser + "|" + currentEmail + "|" + toEmail + "|" + s + "|" + b);
                if (resp.startsWith("OK")) {
                    log("✅ Đã gửi mail tới " + toEmail);
                    log("   FromIP ghi vào file: " + myIP);
                    showToast("Đã gửi thư tới " + toEmail);
                    closeModal();
                    loadFolder(currentFolder);

                    new Timeline(new KeyFrame(Duration.millis(300), ev -> {
                        silentRefresh();
                        isSending = false;
                    })).play();
                } else {
                    showAlert("Lỗi: " + resp.substring(6));
                    isSending = false;
                }
            } catch (Exception ex) {
                showAlert("Lỗi: " + ex.getMessage());
                isSending = false;
            }
        });

        HBox bottomBar = new HBox(sendBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        VBox bodyBox = new VBox(14, toRow, subRow, bodyA, bottomBar);
        bodyBox.setPadding(new Insets(18, 22, 20, 22));

        VBox fullCard = new VBox(0, header, bodyBox);
        fullCard.setMaxWidth(720);
        fullCard.setMaxHeight(700);
        fullCard.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:16; -fx-border-color:" + BORDER + "; -fx-border-radius:16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0, 0, 8);");

        modalLayer.getChildren().setAll(fullCard);
        modalLayer.setVisible(true);
        modalLayer.setMouseTransparent(false);

        Platform.runLater(() -> toF.requestFocus());
    }

    private void closeModal() {
        modalLayer.setVisible(false);
        modalLayer.setMouseTransparent(true);
        modalLayer.getChildren().clear();
    }

    private void showToast(String msg) {
        Platform.runLater(() -> {
            FontIcon ic = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
            ic.setIconColor(Color.WHITE);
            ic.setIconSize(14);
            Label lbl = new Label("  " + msg);
            lbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;");
            HBox h = new HBox(ic, lbl);
            h.setAlignment(Pos.CENTER_LEFT);
            h.setPadding(new Insets(12, 18, 12, 14));
            h.setStyle("-fx-background-color:" + GREEN + "; -fx-background-radius:10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 4);");
            h.setOpacity(0);

            toastContainer.getChildren().add(h);

            FadeTransition fi = new FadeTransition(Duration.millis(300), h);
            fi.setFromValue(0);
            fi.setToValue(1);
            fi.play();

            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(Duration.seconds(3));
            p.setOnFinished(ev -> {
                FadeTransition fo = new FadeTransition(Duration.millis(300), h);
                fo.setFromValue(1);
                fo.setToValue(0);
                fo.setOnFinished(e2 -> toastContainer.getChildren().remove(h));
                fo.play();
            });
            p.play();
        });
    }

    private void applyServerHost() {
        String ip = serverIpField.getText().trim();
        if (!ip.isEmpty()) core.setHost(ip);
    }

    private void doLogin() {
        applyServerHost();
        String u = loginUserField.getText().trim();
        String p = loginPassField.getText().trim();
        if (u.isEmpty() || p.isEmpty()) { showAlert("Nhập đầy đủ!"); return; }

        try {
            String resp = core.send("LOGIN|" + u + "|" + p);
            if (resp.startsWith("OK")) {
                currentUser = u;
                currentPass = p;

                for (String line : resp.split("\n")) {
                    if (line.startsWith("EMAIL|")) {
                        currentEmail = line.substring(6).trim();
                        break;
                    }
                }

                if (currentEmail != null) {
                    avatarLabel.setText(u.substring(0, 1).toUpperCase());
                    userEmailLabel.setText(currentEmail);
                }

                rootStack.getChildren().setAll(mainPage);
                parseMailbox(resp);
                log("✅ Đăng nhập: " + currentEmail);
                log("   IP máy bạn: " + myIP);
                showToast("Đăng nhập thành công!");
                showInboxView();
            } else {
                showAlert("Lỗi: " + resp.substring(6));
            }
        } catch (java.net.SocketTimeoutException te) {
            showAlert("Không kết nối được server.\nKiểm tra IP: " + core.getHost());
        } catch (Exception e) {
            showAlert("Lỗi: " + e.getMessage());
        }
    }

    private void doRegister() {
        applyServerHost();
        String u = regUserField.getText().trim();
        String p = regPassField.getText().trim();
        String c = regPassConfirmField.getText().trim();

        if (u.isEmpty() || p.isEmpty()) { showAlert("Nhập đầy đủ!"); return; }
        if (!p.equals(c)) { showAlert("Mật khẩu xác nhận không khớp!"); return; }

        try {
            String resp = core.send("REGISTER|" + u + "|" + p);
            if (resp.startsWith("OK")) {
                showAlert("Đăng ký thành công!\nEmail: " + u + "@gmail.com\nIP máy bạn: " + myIP);
                loginUserField.setText(u);
                loginPassField.setText(p);
                regUserField.clear(); regPassField.clear(); regPassConfirmField.clear();
                regEmailField.clear();
            } else {
                showAlert("Lỗi: " + resp.substring(6));
            }
        } catch (Exception e) {
            showAlert("Lỗi: " + e.getMessage());
        }
    }

    private void silentRefresh() {
        try {
            String cmd;
            switch (currentFolder) {
                case "SENT":    cmd = "SENT|" + currentUser;    break;
                case "TRASH":   cmd = "TRASH|" + currentUser;   break;
                case "STARRED": cmd = "STARRED|" + currentUser; break;
                case "CONTACTS": return;
                default:        cmd = "LOGIN|" + currentUser + "|" + currentPass;
            }
            String resp = core.send(cmd);
            if (resp.startsWith("OK")) {
                MailItem sel = selectedMail;
                int oldCount = mailList != null ? mailList.getItems().size() : 0;
                parseMailbox(resp);

                if (sel != null && mailList != null) {
                    for (MailItem m : mailList.getItems()) {
                        if (m.fileName.equals(sel.fileName)) {
                            mailList.getSelectionModel().select(m);
                            break;
                        }
                    }
                }
                if (mailList != null && mailList.getItems().size() > oldCount) {
                    log("📬 Có mail mới!");
                    showToast("Bạn có mail mới!");
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadFolder(String folder) {
        if (currentUser == null) return;
        try {
            String cmd;
            switch (folder) {
                case "SENT":    cmd = "SENT|" + currentUser;    break;
                case "TRASH":   cmd = "TRASH|" + currentUser;   break;
                case "STARRED": cmd = "STARRED|" + currentUser; break;
                default:        cmd = "LOGIN|" + currentUser + "|" + currentPass;
            }
            String resp = core.send(cmd);
            if (resp.startsWith("OK")) parseMailbox(resp);
        } catch (Exception ignored) {}
    }

    private void parseMailbox(String resp) {
        if (mailList == null) return;
        mailList.getItems().clear();
        for (String line : resp.split("\n")) {
            String s = line.trim();
            if (s.startsWith("MAIL|")) {
                String[] p = s.split("\\|", 9);
                if (p.length >= 9) {
                    boolean star = "1".equals(p[8]);
                    mailList.getItems().add(new MailItem(p[1], p[2], p[3], p[4], p[5], p[6], p[7], star));
                }
            }
        }
    }

    private void loadContacts() {
        try {
            applyServerHost();
            String resp = core.send("LIST");
            allContacts.clear();
            if (resp.startsWith("OK")) {
                for (String line : resp.split("\n")) {
                    if (line.startsWith("USER|")) {
                        String[] p = line.split("\\|");
                        if (p.length >= 3) allContacts.add(p[2]);
                    }
                }
            }
            if (contactsList != null) contactsList.getItems().setAll(allContacts);
        } catch (Exception ignored) {}
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            if (activityLog != null) activityLog.appendText("[" + java.time.LocalTime.now().withNano(0) + "]  " + msg + "\n");
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @Override public void stop() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        if (core != null) core.close();
    }
    public static void main(String[] args) { launch(args); }
}