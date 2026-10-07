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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ClientApp extends Application {

    private static final String BG_MAIN     = "#F7F9FC";
    private static final String BG_WHITE    = "#FFFFFF";
    private static final String BG_HEADER   = "#0D47A1";
    private static final String BG_HEADER_2 = "#1565C0";
    private static final String BG_HOVER    = "#EFF6FF";
    private static final String BORDER      = "#E2E8F0";
    private static final String BORDER_SOFT = "#CBD5E1";
    private static final String TEXT_DARK   = "#0F172A";
    private static final String TEXT_GRAY   = "#475569";
    private static final String TEXT_DIM    = "#94A3B8";
    private static final String BLUE        = "#1976D2";
    private static final String BLUE_BG     = "#DBEAFE";
    private static final String GREEN       = "#10B981";
    private static final String YELLOW      = "#F59E0B";
    private static final String RED         = "#EF4444";

    private ClientCore core;
    private String currentUser = null;
    private String currentEmail = null;
    private String currentPass = null;
    private String myIP = "unknown";

    private TextField     loginUserField, regUserField, regEmailField;
    private PasswordField loginPassField, regPassField, regPassConfirmField;

    private ListView<MailItem> mailList;
    private Label              readerFrom, readerTo, readerSubj, readerTime, readerFromIp;
    private TextArea           readerBody;
    private MailItem           selectedMail;

    private Label      avatarLabel, userEmailLabel, serverIpLabel, myIpLabel;
    private TextArea   activityLog;
    private StackPane  rootStack, centerContent;
    private VBox       authPage;
    private BorderPane mainPage;
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

        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
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

                String name    = ni.getName()        != null ? ni.getName().toLowerCase()        : "";
                String display = ni.getDisplayName() != null ? ni.getDisplayName().toLowerCase() : "";

                if (name.contains("vbox") || name.contains("vmnet")
                    || name.contains("bluetooth") || name.contains("virtual")
                    || name.contains("docker") || name.contains("hyper-v")
                    || display.contains("virtualbox") || display.contains("vmware")
                    || display.contains("bluetooth") || display.contains("hyper-v")
                    || display.contains("virtual")) continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip.contains(".") && !ip.startsWith("169.254") && !ip.startsWith("192.168.56.") && !ip.startsWith("127.")) {
                        return ip;
                    }
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
    //              AUTH PAGE
    // ==================================================
    private VBox buildAuthPage() {
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

        Label serverBadge = new Label("● Server: " + core.getServerIP() + ":" + core.getPort());
        serverBadge.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(16,185,129,0.6); -fx-padding:8 16; -fx-background-radius:20;");

        Label myIpBadge = new Label("● IP máy bạn: " + myIP);
        myIpBadge.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(6,182,212,0.55); -fx-padding:8 16; -fx-background-radius:20;");

        HBox badges = new HBox(10, serverBadge, myIpBadge);
        badges.setAlignment(Pos.CENTER_RIGHT);

        HBox authHeader = new HBox(14, titleSection, sp0, badges);
        authHeader.setAlignment(Pos.CENTER_LEFT);
        authHeader.setPadding(new Insets(20, 28, 20, 28));
        authHeader.setStyle("-fx-background-color: linear-gradient(to right, " + BG_HEADER + ", " + BG_HEADER_2 + ");");

        Button tabLogin    = authTab("Đăng nhập", true);
        Button tabRegister = authTab("Tạo tài khoản", false);
        HBox tabs = new HBox(4, tabLogin, tabRegister);
        tabs.setAlignment(Pos.CENTER);

        loginUserField = lightField(FontAwesomeSolid.USER, "Tên đăng nhập");
        loginPassField = new PasswordField();
        VBox loginPassWrapper = passwordFieldWrapper(loginPassField, "Mật khẩu");

        Button loginBtn = primaryBtn("ĐĂNG NHẬP", BLUE, "#1565C0");
        loginBtn.setPrefWidth(380);
        loginBtn.setOnAction(e -> doLogin());

        Label hintLogin = new Label("Chưa có tài khoản? Chuyển sang tab \"Tạo tài khoản\"");
        hintLogin.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");

        VBox loginForm = new VBox(14,
                labeledInput("Tên đăng nhập", loginUserField),
                labeledInput("Mật khẩu", loginPassWrapper),
                new Region(),
                loginBtn, hintLogin);
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setPadding(new Insets(20, 0, 0, 0));

        regUserField  = lightField(FontAwesomeSolid.USER_PLUS, "Tên đăng nhập mới");
        regEmailField = lightField(FontAwesomeSolid.AT, "Email tự động");
        regEmailField.setDisable(true);

        regUserField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) regEmailField.setText("");
            else regEmailField.setText(val.trim() + "@gmail.com");
        });

        regPassField        = new PasswordField();
        regPassConfirmField = new PasswordField();
        VBox regPassWrapper        = passwordFieldWrapper(regPassField,        "Mật khẩu");
        VBox regPassConfirmWrapper = passwordFieldWrapper(regPassConfirmField, "Xác nhận mật khẩu");

        Button registerBtn = primaryBtn("TẠO TÀI KHOẢN", GREEN, "#059669");
        registerBtn.setPrefWidth(380);
        registerBtn.setOnAction(e -> doRegister());

        Label hintReg = new Label("Mỗi tài khoản có email dạng username@gmail.com");
        hintReg.setStyle("-fx-text-fill:" + TEXT_DIM + "; -fx-font-size:11px;");

        VBox registerForm = new VBox(14,
                labeledInput("Tên đăng nhập", regUserField),
                labeledInput("Email", regEmailField),
                labeledInput("Mật khẩu", regPassWrapper),
                labeledInput("Xác nhận mật khẩu", regPassConfirmWrapper),
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

        VBox card = new VBox(12, tabs, loginForm, registerForm);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(28, 42, 32, 42));
        card.setMaxWidth(500);
        card.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:16; -fx-border-color:" + BORDER + "; -fx-border-radius:16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 4);");

        Label serverInfo = new Label("🌐 Server IP: " + core.getServerIP() + " • Port: " + core.getPort());
        serverInfo.setStyle("-fx-text-fill:" + BLUE + "; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:" + BLUE_BG + "; -fx-padding:6 14; -fx-background-radius:20;");

        VBox cardWrap = new VBox(16, card, serverInfo);
        cardWrap.setAlignment(Pos.CENTER);
        cardWrap.setPadding(new Insets(40));
        VBox.setVgrow(cardWrap, Priority.ALWAYS);

        VBox page = new VBox(authHeader, cardWrap);
        page.setStyle("-fx-background-color:" + BG_MAIN + ";");
        VBox.setVgrow(cardWrap, Priority.ALWAYS);
        return page;
    }

    private VBox passwordFieldWrapper(PasswordField pf, String prompt) {
        FontIcon lockIc = new FontIcon(FontAwesomeSolid.LOCK);
        lockIc.setIconSize(13);
        lockIc.setIconColor(Color.web(TEXT_DIM));

        TextField tf = new TextField();
        tf.setVisible(false);
        tf.setManaged(false);

        pf.setPromptText(prompt);
        pf.setPrefHeight(44);
        pf.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px; -fx-padding:0 4;");

        tf.setPromptText(prompt);
        tf.setPrefHeight(44);
        tf.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_DARK + "; -fx-prompt-text-fill:" + TEXT_DIM + "; -fx-font-size:13px; -fx-padding:0 4;");

        pf.textProperty().addListener((obs, old, val) -> { if (!tf.getText().equals(val)) tf.setText(val); });
        tf.textProperty().addListener((obs, old, val) -> { if (!pf.getText().equals(val)) pf.setText(val); });

        FontIcon eyeIc = new FontIcon(FontAwesomeSolid.EYE);
        eyeIc.setIconSize(14);
        eyeIc.setIconColor(Color.web(TEXT_GRAY));

        Button eyeBtn = new Button();
        eyeBtn.setGraphic(eyeIc);
        eyeBtn.setStyle("-fx-background-color:transparent; -fx-cursor:hand; -fx-padding:4;");
        eyeBtn.setFocusTraversable(false);

        eyeBtn.setOnAction(e -> {
            boolean show = !tf.isVisible();
            if (show) {
                tf.setVisible(true);  tf.setManaged(true);
                pf.setVisible(false); pf.setManaged(false);
                FontIcon eyeSlash = new FontIcon(FontAwesomeSolid.EYE_SLASH);
                eyeSlash.setIconSize(14);
                eyeSlash.setIconColor(Color.web(BLUE));
                eyeBtn.setGraphic(eyeSlash);
            } else {
                tf.setVisible(false); tf.setManaged(false);
                pf.setVisible(true);  pf.setManaged(true);
                FontIcon eye = new FontIcon(FontAwesomeSolid.EYE);
                eye.setIconSize(14);
                eye.setIconColor(Color.web(TEXT_GRAY));
                eyeBtn.setGraphic(eye);
            }
        });

        StackPane inputStack = new StackPane(pf, tf);
        HBox.setHgrow(inputStack, Priority.ALWAYS);

        HBox wrapper = new HBox(10, lockIc, inputStack, eyeBtn);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(0, 8, 0, 14));
        wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");

        pf.focusedProperty().addListener((o, a, b) -> {
            if (b) wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BLUE + "; -fx-border-radius:10; -fx-border-width:2;");
            else wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        });
        tf.focusedProperty().addListener((o, a, b) -> {
            if (b) wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BLUE + "; -fx-border-radius:10; -fx-border-width:2;");
            else wrapper.setStyle("-fx-background-color:" + BG_WHITE + "; -fx-background-radius:10; -fx-border-color:" + BORDER_SOFT + "; -fx-border-radius:10;");
        });

        return new VBox(wrapper);
    }

    private Button authTab(String text, boolean active) {
        Button b = new Button(text);
        b.setPrefWidth(200);
        b.setPrefHeight(42);
        b.setStyle(authTabStyle(active));
        return b;
    }

    private String authTabStyle(boolean active) {
        if (active) return "-fx-background-color:" + BLUE + "; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px; -fx-background-radius:10; -fx-cursor:hand;";
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

    private VBox labeledInput(String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_DARK + ";");
        VBox v = new VBox(6, l, field);
        v.setMaxWidth(380);
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

    private BorderPane buildMainPage() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_MAIN + ";");
        root.setTop(buildTopBar());
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

        serverIpLabel = new Label("● Server: " + core.getServerIP());
        serverIpLabel.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(16,185,129,0.65); -fx-padding:6 14; -fx-background-radius:20;");

        myIpLabel = new Label("● IP: " + myIP);
        myIpLabel.setStyle("-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-color:rgba(6,182,212,0.55); -fx-padding:6 14; -fx-background-radius:20;");

        avatarLabel = new Label("?");
        avatarLabel.setStyle("-fx-background-color:white; -fx-text-fill:" + BLUE + "; -fx-font-size:13px; -fx-font-weight:bold; -fx-background-radius:50; -fx-padding:8 12;");

        userEmailLabel = new Label("guest");
        userEmailLabel.setStyle("-fx-text-fill:white; -fx-font-size:12px; -fx-font-weight:bold;");

        HBox userHBox = new HBox(8, avatarLabel, userEmailLabel);
        userHBox.setAlignment(Pos.CENTER);
        userHBox.setPadding(new Insets(4, 12, 4, 4));
        userHBox.setStyle("-fx-background-color:rgba(255,255,255,0.18); -fx-background-radius:24;");

        HBox top = new HBox(14, logoSection, sp1, serverIpLabel, myIpLabel, userHBox);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(14, 24, 14, 24));
        top.setStyle("-fx-background-color: linear-gradient(to right, " + BG_HEADER + ", " + BG_HEADER_2 + ");");
        return top;
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

    private void showMailView() {
        HBox view = new HBox();

        VBox leftPanel = new VBox(0);

        FontIcon inboxIc = new FontIcon(FontAwesomeSolid.INBOX);
        inboxIc.setIconSize(16);
        inboxIc.setIconColor(Color.web(BLUE));
        Label listTitle = new Label("  Hộp thư — " + currentUser);
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
        refreshBtn.setOnAction(e -> { silentRefresh(); log("🔄 Đã làm mới"); });

        Button composeBtn = new Button();
        FontIcon penIc = new FontIcon(FontAwesomeSolid.PEN);
        penIc.setIconSize(12);
        penIc.setIconColor(Color.WHITE);
        Label composeLbl = new Label("  Soạn thư");
        composeLbl.setGraphic(penIc);
        composeLbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;");
        composeBtn.setGraphic(composeLbl);
        composeBtn.setStyle("-fx-background-color:" + BLUE + "; -fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
        composeBtn.setOnAction(e -> openCompose("", "", ""));

        HBox header = new HBox(8, listTitle, sp, composeBtn, refreshBtn);
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
                    FontIcon starIc = new FontIcon(FontAwesomeSolid.ENVELOPE);
                    starIc.setIconSize(14);
                    starIc.setIconColor(Color.web(BLUE));

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

        replyBtn.setOnAction(e -> {
            if (selectedMail != null) openCompose(selectedMail.from, "Re: " + selectedMail.subject, "\n\n--- Mail gốc ---\n" + selectedMail.preview);
        });

        HBox toolbar = new HBox(8, replyBtn);
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
            String resp = core.send("GET|" + currentUser + "|" + item.fileName);
            if (resp.startsWith("OK|")) {
                String content = resp.substring(3);
                String fromIp = extractLine(content, "FromIP");
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
        return content.trim();
    }

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
                    showToast("Đã gửi thư tới " + toEmail);
                    closeModal();
                    silentRefresh();

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
            fi.setFromValue(0); fi.setToValue(1); fi.play();

            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(Duration.seconds(3));
            p.setOnFinished(ev -> {
                FadeTransition fo = new FadeTransition(Duration.millis(300), h);
                fo.setFromValue(1); fo.setToValue(0);
                fo.setOnFinished(e2 -> toastContainer.getChildren().remove(h));
                fo.play();
            });
            p.play();
        });
    }

    private void doLogin() {
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
                showMailView();
            } else {
                showAlert("Lỗi: " + resp.substring(6));
            }
        } catch (java.net.SocketTimeoutException te) {
            showAlert("Không kết nối được server.\nKiểm tra:\n1. Server đã START chưa?\n2. IP server: " + core.getServerIP() + "\n3. Cùng WiFi chưa?\n4. Firewall đã mở port 2023?");
        } catch (Exception e) {
            showAlert("Lỗi: " + e.getMessage());
        }
    }

    private void doRegister() {
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
            String resp = core.send("LOGIN|" + currentUser + "|" + currentPass);
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