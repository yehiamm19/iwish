package iwish.client;

import iwish.common.*;
import iwish.common.Models.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class JavaFxApp extends Application {
    private final String css = resolveCss();

    private static String resolveCss() {
        String[] candidates = {
            "/iwish.css",
            "/main/resources/iwish.css",
            "/src/main/resources/iwish.css",
            "/iwish/client/iwish.css",
            "iwish.css"
        };
        for (String c : candidates) {
            java.net.URL u = JavaFxApp.class.getResource(c);
            if (u != null) {
                return u.toExternalForm();
            }
        }
        String[] filePaths = {
            "src/main/resources/iwish.css",
            "src/iwish.css",
            "src/iwish/client/iwish.css",
            "build/classes/main/resources/iwish.css",
            "build/classes/iwish.css",
            "target/classes/iwish.css"
        };
        for (String fp : filePaths) {
            java.io.File f = new java.io.File(fp);
            if (f.exists()) {
                return f.toURI().toString();
            }
        }
        return null;
    }

    /**
     * Resolves the official project logo image from resources or filesystem.
     */
    public static Image getLogoImage() {
        String[] candidates = {
            "/img/logo.png",
            "/resources/img/logo.png",
            "img/logo.png",
            "/logo.png"
        };
        for (String path : candidates) {
            java.io.InputStream is = JavaFxApp.class.getResourceAsStream(path);
            if (is != null) {
                try {
                    return new Image(is);
                } catch (Exception e) {}
            }
        }
        String[] filePaths = {
            "img/logo.png",
            "src/main/resources/img/logo.png",
            "src/img/logo.png",
            "build/classes/img/logo.png",
            "target/classes/img/logo.png"
        };
        for (String fp : filePaths) {
            java.io.File f = new java.io.File(fp);
            if (f.exists()) {
                try {
                    return new Image(f.toURI().toString());
                } catch (Exception e) {}
            }
        }
        return null;
    }

    /**
     * Creates a responsive, proportional logo node for auth panel or dashboard sidebar.
     */
    private static Node createLogoNode(double width, boolean isAuth) {
        Image img = getLogoImage();
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(width);
            iv.setSmooth(true);
            return iv;
        }
        if (isAuth) {
            VBox box = new VBox(4);
            Label mark = new Label("✦");
            mark.getStyleClass().add("brand-mark");
            Label logo = new Label("I-Wish");
            logo.getStyleClass().add("brand-logo");
            box.getChildren().addAll(mark, logo);
            return box;
        } else {
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER_LEFT);
            Label mark = new Label("✦");
            mark.getStyleClass().add("sidebar-logo-mark");
            Label logo = new Label("I-Wish");
            logo.getStyleClass().add("logo");
            box.getChildren().addAll(mark, logo);
            return box;
        }
    }

    private Stage stage;
    private ApiClient api;
    private User user;
    private Snapshot snapshot;

    // Background auto-refresh timeline for real-time MySQL sync
    private Timeline autoRefreshTimeline;

    // Navigation and state preservation
    private String currentTab = "Overview";
    private StackPane contentArea;
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private Label notifCountBadge;
    private Label sidebarAvatar;
    private Label sidebarUserName;
    private Label sidebarUserEmail;

    // Wishlist filter state
    private String wishSearchQuery = "";
    private String wishSelectedCategory = "All Categories";
    private String wishSelectedSort = "Default";

    // Real-time optimistic contribution tracker
    private final Map<Integer, Double> localContributions = new HashMap<>();

    @Override
    public void start(Stage s) {
        stage = s;
        stage.setTitle("I-Wish • Make wishes happen");
        Image icon = getLogoImage();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
        stage.setOnCloseRequest(e -> stopAutoRefresh());
        showLogin();
    }

    private void showLogin() {
        showAuth(false);
    }

    private void showSignup() {
        showAuth(true);
    }

    private void showAuth(boolean signup) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("auth-root");

        VBox brand = new VBox(18);
        brand.getStyleClass().add("auth-brand");
        brand.setPrefWidth(430);

        Node brandLogo = createLogoNode(220, true);
        Label tagline = new Label("Small wishes.\nBig smiles.");
        tagline.getStyleClass().add("brand-tagline");
        Label detail = new Label("A thoughtful place to share what you love\nand help friends make it happen.");
        detail.getStyleClass().add("brand-detail");
        Region line = new Region();
        line.getStyleClass().add("brand-line");
        brand.getChildren().addAll(brandLogo, tagline, detail, line, new Label("MAKE A WISH  •  SHARE THE JOY"));
        root.setLeft(brand);

        VBox form = new VBox(14);
        form.getStyleClass().add("auth-card");
        form.setMaxWidth(480);

        Label eyebrow = new Label(signup ? "WELCOME TO THE CIRCLE" : "YOUR WISH LIST AWAITS");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label(signup ? "Create your account" : "Welcome back");
        title.getStyleClass().add("auth-title");
        Label sub = new Label(signup ? "Start turning little wishes into happy moments."
                : "Sign in to keep the good moments moving.");
        sub.getStyleClass().add("auth-sub");

        TextField name = new TextField();
        name.setPromptText("Full name");
        name.getStyleClass().add("auth-input");
        name.setManaged(signup);
        name.setVisible(signup);

        TextField email = new TextField();
        email.setPromptText("Email address");
        email.getStyleClass().add("auth-input");

        PasswordBox pass = new PasswordBox("Password (4+ characters)", "auth-input");

        Label message = new Label();
        message.getStyleClass().add("error");

        Button submit = new Button(signup ? "Create account" : "Sign in");
        submit.getStyleClass().add("primary");
        submit.setMaxWidth(Double.MAX_VALUE);

        Hyperlink switchLink = new Hyperlink(
                signup ? "Already have an account? Sign in" : "New here? Create an account");
        switchLink.getStyleClass().add("auth-link");

        form.getChildren().addAll(eyebrow, title, sub, name, email, pass, submit, message, switchLink);
        submit.setOnAction(e -> authenticate(name, email, pass.getText(), message, signup));
        switchLink.setOnAction(e -> {
            if (signup)
                showLogin();
            else
                showSignup();
        });

        StackPane panel = new StackPane(form);
        panel.getStyleClass().add("auth-panel");
        panel.setPadding(new Insets(42, 70, 42, 70));
        root.setCenter(panel);

        showScene(root, 1000, 650);
    }

    private void authenticate(TextField name, TextField email, String password, Label msg, boolean register) {
        String emailText = email.getText().trim();
        if (register && name.getText().trim().isEmpty()) {
            msg.setText("Please enter your full name.");
            return;
        }
        if (emailText.isEmpty() || !emailText.contains("@") || !emailText.contains(".") || emailText.indexOf("@") >= emailText.lastIndexOf(".")) {
            msg.setText("Please enter a valid email address containing '@' (e.g. name@domain.com).");
            return;
        }
        if (password == null || password.length() < 4) {
            msg.setText("Password must be at least 4 characters.");
            return;
        }
        try {
            if (api == null)
                api = new ApiClient();
            Request r = new Request(register ? "register" : "login").put("email", emailText)
                    .put("password", password);
            if (register)
                r.put("name", name.getText().trim());
            Response res = api.call(r);
            if (!res.ok) {
                msg.setText(res.message);
                return;
            }
            user = (User) res.payload;
            currentTab = "Overview";
            showDashboard();
        } catch (Exception ex) {
            msg.setText("Start I-Wish Server first, then try again.");
        }
    }

    private void showDashboard() {
        refreshData();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // SIDEBAR
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("sidebar");

        Node brandHeader = createLogoNode(160, false);

        Label motto = new Label("Make it memorable");
        motto.getStyleClass().add("sidebar-motto");

        // User profile badge (Clickable to edit profile)
        HBox userBadge = new HBox(10);
        userBadge.getStyleClass().add("sidebar-user-card");
        userBadge.setAlignment(Pos.CENTER_LEFT);
        String initial = (user != null && user.name != null && !user.name.isEmpty())
                ? user.name.substring(0, 1).toUpperCase()
                : "U";
        sidebarAvatar = new Label(initial);
        sidebarAvatar.getStyleClass().add("user-avatar");
        VBox userInfo = new VBox(2);
        sidebarUserName = new Label(user != null ? user.name : "");
        sidebarUserName.getStyleClass().add("sidebar-user-name");
        sidebarUserEmail = new Label(user != null ? user.email : "");
        sidebarUserEmail.getStyleClass().add("sidebar-user-email");
        userInfo.getChildren().addAll(sidebarUserName, sidebarUserEmail);

        Region editSp = new Region();
        HBox.setHgrow(editSp, Priority.ALWAYS);
        Label editHint = new Label("✏️");
        editHint.setStyle("-fx-font-size: 11px; -fx-opacity: 0.6;");
        userBadge.getChildren().addAll(sidebarAvatar, userInfo, editSp, editHint);

        Tooltip.install(userBadge, new Tooltip("Click to edit your name, email, or password"));
        userBadge.setOnMouseClicked(e -> showEditProfileDialog());

        sidebar.getChildren().addAll(brandHeader, motto, userBadge, new Separator());

        // Nav tabs
        navButtons.clear();
        String[] tabs = { "Overview", "My Wish List", "Friends", "Notifications" };
        for (String tabName : tabs) {
            Button btn = createNavButton(tabName);
            navButtons.put(tabName, btn);
            sidebar.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button syncBtn = new Button("🔄 Sync with DB");
        syncBtn.getStyleClass().add("nav-button");
        syncBtn.setMaxWidth(Double.MAX_VALUE);
        syncBtn.setOnAction(e -> renderCurrentTab());

        Button logout = new Button("Sign out");
        logout.getStyleClass().add("nav-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            stopAutoRefresh();
            try {
                if (api != null)
                    api.close();
            } catch (Exception ignored) {
            }
            api = null;
            showLogin();
        });

        sidebar.getChildren().addAll(spacer, syncBtn, logout);
        root.setLeft(sidebar);

        // Content host
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content");
        root.setCenter(contentArea);

        // Switch to initial tab
        switchTab(currentTab);
        startAutoRefresh();
        showScene(root, 1200, 780);
    }

    private Button createNavButton(String tabName) {
        Button b = new Button();
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);

        if (tabName.equals("Notifications")) {
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER_LEFT);
            Label textLbl = new Label(tabName);
            textLbl.setTextFill(javafx.scene.paint.Color.WHITE);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            notifCountBadge = new Label("0");
            notifCountBadge.getStyleClass().add("nav-badge");
            notifCountBadge.setVisible(false);
            notifCountBadge.setManaged(false);
            box.getChildren().addAll(textLbl, sp, notifCountBadge);
            b.setGraphic(box);
        } else {
            b.setText(tabName);
        }

        b.setOnAction(e -> switchTab(tabName));
        return b;
    }

    private void switchTab(String tabName) {
        currentTab = tabName;
        // Update active classes on navigation buttons
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(tabName)) {
                if (!entry.getValue().getStyleClass().contains("active")) {
                    entry.getValue().getStyleClass().add("active");
                }
            } else {
                entry.getValue().getStyleClass().remove("active");
            }
        }
        renderCurrentTab();
    }

    private void renderCurrentTab() {
        refreshData();
        updateNotificationBadge();

        VBox page = new VBox(20);
        page.getStyleClass().add("page");

        // Page header
        HBox pageHeader = new HBox(15);
        pageHeader.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label heading = new Label(currentTab);
        heading.getStyleClass().add("page-title");
        Label subtitle = new Label(getPageSubtitle(currentTab));
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(heading, subtitle);
        pageHeader.getChildren().add(titleBox);

        page.getChildren().add(pageHeader);

        // Render page specific content
        switch (currentTab) {
            case "Overview" -> overview(page);
            case "My Wish List" -> myWishes(page);
            case "Friends" -> friends(page);
            case "Notifications" -> notices(page);
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("clean-scroll");
        contentArea.getChildren().setAll(scroll);
    }

    private String getPageSubtitle(String tab) {
        return switch (tab) {
            case "Overview" -> "Here is what is happening across your circle today.";
            case "My Wish List" -> "Manage your wishes, track contributions, and celebrate milestones.";
            case "Friends" -> "Connect with friends and discover their wishlist dreams.";
            case "Notifications" -> "Stay updated on recent contributions and friend activities.";
            default -> "";
        };
    }

    private void updateNotificationBadge() {
        if (notifCountBadge != null && snapshot != null) {
            int unread = 0;
            for (Notification n : snapshot.notifications) {
                if (!n.read) {
                    unread++;
                }
            }
            if (unread > 0) {
                notifCountBadge.setText(String.valueOf(unread));
                notifCountBadge.setVisible(true);
                notifCountBadge.setManaged(true);
            } else {
                notifCountBadge.setVisible(false);
                notifCountBadge.setManaged(false);
            }
        }
    }

    // OVERVIEW TAB
    private void overview(VBox p) {
        int unreadCount = 0;
        if (snapshot.notifications != null) {
            for (Notification n : snapshot.notifications) {
                if (!n.read) {
                    unreadCount++;
                }
            }
        }

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                stat("MY WISHES", String.valueOf(snapshot.myItems.size()), "✦ Total dreams on your list"),
                stat("FRIENDS", String.valueOf(snapshot.friends.size()), "♥ People in your circle"),
                stat("NEW UPDATES", String.valueOf(unreadCount), "🔔 Recent moments to check"));
        p.getChildren().add(stats);

        // Visual Data Analytics Section (PieChart & BarChart)
        p.getChildren().add(section("Analytics & Insights"));
        p.getChildren().add(buildAnalyticsSection());

        p.getChildren().add(section("Friends' Wishes"));
        if (snapshot.friendItems.isEmpty()) {
            p.getChildren().add(empty(
                    "No friend wishes to display yet. Add friends or encourage them to create their first wish!"));
        } else {
            for (WishItem i : snapshot.friendItems) {
                p.getChildren().add(friendWishCard(i));
            }
        }
    }

    private Node buildAnalyticsSection() {
        HBox chartsRow = new HBox(18);
        chartsRow.setAlignment(Pos.CENTER);

        List<WishItem> itemsForCharts = !snapshot.myItems.isEmpty() ? snapshot.myItems : snapshot.friendItems;

        // Empty state if neither current user nor friends have wishes
        if (itemsForCharts.isEmpty()) {
            VBox emptyCard = new VBox(10);
            emptyCard.getStyleClass().add("chart-card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPrefHeight(240);
            HBox.setHgrow(emptyCard, Priority.ALWAYS);
            Label icon = new Label("📊");
            icon.setStyle("-fx-font-size: 34px;");
            Label title = new Label("Analytics will appear here");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
            Label sub = new Label(
                    "Add wishes or connect with friends to see interactive charts and spending insights.");
            sub.getStyleClass().add("muted");
            emptyCard.getChildren().addAll(icon, title, sub);
            chartsRow.getChildren().add(emptyCard);
            return chartsRow;
        }

        boolean isMyData = !snapshot.myItems.isEmpty();

        // Collect chart statistics per category (count, goal, raised)
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<String, Double> categoryRaised = new LinkedHashMap<>();

        for (WishItem item : itemsForCharts) {
            String cat = (item.category != null && !item.category.trim().isEmpty()) ? item.category.trim() : "General";
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + item.price);
            categoryRaised.put(cat, categoryRaised.getOrDefault(cat, 0.0) + item.contributed);
        }

        // 1. PIECHART CARD (Wishes by Category)
        VBox pieCard = new VBox(12);
        pieCard.getStyleClass().add("chart-card");
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        VBox pieHeader = new VBox(3);
        Label pieTitle = new Label("✦ Wishes by Category");
        pieTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label pieSub = new Label(isMyData ? "Breakdown of your wishlist items across categories"
                : "Category distribution from friends' wishes");
        pieSub.getStyleClass().add("muted");
        pieHeader.getChildren().addAll(pieTitle, pieSub);

        javafx.collections.ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        int totalItemsCount = itemsForCharts.size();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            double pct = totalItemsCount > 0 ? (entry.getValue() * 100.0) / totalItemsCount : 0.0;
            PieChart.Data data = new PieChart.Data(String.format("%s (%.0f%%)", entry.getKey(), pct), entry.getValue());
            pieData.add(data);

            // Hover tooltip
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    double totalValue = categoryTotals.getOrDefault(entry.getKey(), 0.0);
                    Tooltip.install(newNode, new Tooltip(String.format("%s: %d wish(es)\nTotal Target: $%.2f",
                            entry.getKey(), entry.getValue(), totalValue)));
                }
            });
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setPrefHeight(290);
        pieChart.setMinHeight(290);
        pieChart.setLegendSide(Side.RIGHT);
        pieChart.setLabelsVisible(true);
        pieChart.setAnimated(true);

        pieCard.getChildren().addAll(pieHeader, pieChart);

        // 2. BARCHART CARD (Goal vs Raised by Category)
        VBox barCard = new VBox(12);
        barCard.getStyleClass().add("chart-card");
        HBox.setHgrow(barCard, Priority.ALWAYS);

        VBox barHeader = new VBox(3);
        Label barTitle = new Label("📊 Funding vs Target Goal");
        barTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label barSub = new Label("Total target price vs funds raised per category ($)");
        barSub.getStyleClass().add("muted");
        barHeader.getChildren().addAll(barTitle, barSub);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount ($)");
        yAxis.setAnimated(true);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setPrefHeight(290);
        barChart.setMinHeight(290);
        barChart.setAnimated(true);
        barChart.setLegendSide(Side.TOP);
        barChart.setCategoryGap(22);
        barChart.setBarGap(6);

        XYChart.Series<String, Number> seriesGoal = new XYChart.Series<>();
        seriesGoal.setName("Target Goal ($)");

        XYChart.Series<String, Number> seriesRaised = new XYChart.Series<>();
        seriesRaised.setName("Raised Funds ($)");

        for (String cat : categoryCounts.keySet()) {
            double goalVal = categoryTotals.getOrDefault(cat, 0.0);
            double raisedVal = categoryRaised.getOrDefault(cat, 0.0);

            XYChart.Data<String, Number> goalData = new XYChart.Data<>(cat, goalVal);
            goalData.nodeProperty().addListener((obs, oldN, newN) -> {
                if (newN != null) {
                    Tooltip.install(newN, new Tooltip(String.format("%s - Goal: $%.2f", cat, goalVal)));
                }
            });
            seriesGoal.getData().add(goalData);

            XYChart.Data<String, Number> raisedData = new XYChart.Data<>(cat, raisedVal);
            raisedData.nodeProperty().addListener((obs, oldN, newN) -> {
                if (newN != null) {
                    Tooltip.install(newN, new Tooltip(String.format("%s - Raised: $%.2f", cat, raisedVal)));
                }
            });
            seriesRaised.getData().add(raisedData);
        }

        barChart.getData().addAll(seriesGoal, seriesRaised);
        barCard.getChildren().addAll(barHeader, barChart);

        chartsRow.getChildren().addAll(pieCard, barCard);
        return chartsRow;
    }

    private VBox stat(String label, String value, String hint) {
        VBox b = new VBox(6);
        b.getStyleClass().add("stat");
        HBox.setHgrow(b, Priority.ALWAYS);

        Label l = new Label(label);
        l.getStyleClass().add("accent");
        Label v = new Label(value);
        v.getStyleClass().add("stat-number");
        Label h = new Label(hint);
        h.getStyleClass().add("muted");

        b.getChildren().addAll(l, v, h);
        return b;
    }

    private Label section(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("section-title");
        return l;
    }

    // MY WISH LIST TAB
    private void myWishes(VBox p) {
        // Calculate wishlist summary metrics
        double totalGoal = 0;
        double totalRaised = 0;
        int fundedCount = 0;
        for (WishItem item : snapshot.myItems) {
            totalGoal += item.price;
            totalRaised += item.contributed;
            if (item.purchased || item.contributed >= item.price) {
                fundedCount++;
            }
        }
        double overallPct = totalGoal > 0 ? (totalRaised / totalGoal) * 100.0 : 0.0;

        HBox metricsBanner = new HBox(20);
        metricsBanner.getStyleClass().add("metrics-banner");
        metricsBanner.setAlignment(Pos.CENTER_LEFT);

        metricsBanner.getChildren().addAll(
                metricItem("TOTAL WISHES", String.valueOf(snapshot.myItems.size())),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                metricItem("TOTAL GOAL", String.format("$%.2f", totalGoal)),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                metricItem("FUNDS RAISED", String.format("$%.2f", totalRaised)),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                metricItem("OVERALL PROGRESS", String.format("%.0f%% (%d funded)", overallPct, fundedCount)));
        p.getChildren().add(metricsBanner);

        // 2. Wishes list container
        VBox wishesContainer = new VBox(12);

        // 3. Toolbar & Filter Row
        HBox toolBar = new HBox(14);
        toolBar.getStyleClass().add("filter-bar");
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("＋  Add a Wish");
        addBtn.getStyleClass().add("primary");
        addBtn.setOnAction(e -> showAddWishDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search input
        TextField searchInput = new TextField(wishSearchQuery);
        searchInput.setPromptText("🔍 Search wishes...");
        searchInput.getStyleClass().add("search-field");

        // Category filter
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        categories.add("All Categories");
        for (WishItem item : snapshot.myItems) {
            if (item.category != null && !item.category.trim().isEmpty()) {
                categories.add(item.category.trim());
            }
        }
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(categories));
        categoryBox.getStyleClass().add("filter-combo");
        categoryBox.setValue(categories.contains(wishSelectedCategory) ? wishSelectedCategory : "All Categories");

        // Sort options
        ComboBox<String> sortBox = new ComboBox<>(FXCollections.observableArrayList(
                "Default",
                "Price: Low to High",
                "Price: High to Low",
                "Most Progress (%)"));
        sortBox.getStyleClass().add("filter-combo");
        sortBox.setValue(wishSelectedSort);

        Runnable updateFilteredList = () -> {
            wishesContainer.getChildren().clear();
            String rawText = searchInput.getText() == null ? "" : searchInput.getText();
            String query = rawText.trim().toLowerCase();
            String selCat = categoryBox.getValue() != null ? categoryBox.getValue() : "All Categories";
            String selSort = sortBox.getValue() != null ? sortBox.getValue() : "Default";

            wishSearchQuery = rawText;
            wishSelectedCategory = selCat;
            wishSelectedSort = selSort;

            // Filter items matching search text and category
            List<WishItem> filtered = new ArrayList<>();
            for (WishItem i : snapshot.myItems) {
                boolean matchSearch = true;
                if (!query.isEmpty()) {
                    boolean matchTitle = i.title != null && i.title.toLowerCase().contains(query);
                    boolean matchDesc = i.description != null && i.description.toLowerCase().contains(query);
                    matchSearch = matchTitle || matchDesc;
                }
                boolean matchCat = "All Categories".equals(selCat) || selCat.equalsIgnoreCase(i.category);
                if (matchSearch && matchCat) {
                    filtered.add(i);
                }
            }

            // Apply selected sorting order
            if ("Price: Low to High".equals(selSort)) {
                filtered.sort((a, b) -> Double.compare(a.price, b.price));
            } else if ("Price: High to Low".equals(selSort)) {
                filtered.sort((a, b) -> Double.compare(b.price, a.price));
            } else if ("Most Progress (%)".equals(selSort)) {
                filtered.sort((a, b) -> {
                    double pctA = a.price > 0 ? (a.contributed / a.price) : 0;
                    double pctB = b.price > 0 ? (b.contributed / b.price) : 0;
                    return Double.compare(pctB, pctA);
                });
            }

            if (filtered.isEmpty()) {
                if (snapshot.myItems.isEmpty()) {
                    wishesContainer.getChildren().add(empty(
                            "Your wish list is currently empty. Click 'Add a Wish' above to create your first dream!"));
                } else {
                    wishesContainer.getChildren().add(empty("No wishes match your current search or category filter."));
                }
            } else {
                for (WishItem item : filtered) {
                    wishesContainer.getChildren().add(myWishCard(item));
                }
            }
        };

        searchInput.textProperty().addListener((obs, oldV, newV) -> updateFilteredList.run());
        categoryBox.valueProperty().addListener((obs, oldV, newV) -> updateFilteredList.run());
        sortBox.valueProperty().addListener((obs, oldV, newV) -> updateFilteredList.run());

        toolBar.getChildren().addAll(addBtn, spacer, searchInput, categoryBox, sortBox);
        p.getChildren().addAll(toolBar, wishesContainer);

        // Initial populate of cards
        updateFilteredList.run();
    }

    private VBox metricItem(String label, String value) {
        VBox b = new VBox(3);
        b.setAlignment(Pos.CENTER_LEFT);
        Label v = new Label(value);
        v.getStyleClass().add("metrics-banner-val");
        Label l = new Label(label);
        l.getStyleClass().add("metrics-banner-lbl");
        b.getChildren().addAll(v, l);
        return b;
    }

    // WISH CARD COMPONENTS
    private HBox myWishCard(WishItem i) {
        HBox card = new HBox(18);
        card.getStyleClass().add("wish-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox contentBox = new VBox(8);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Header: Category Pill + Status Badge
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(i.category != null && !i.category.isEmpty() ? i.category : "General");
        catBadge.getStyleClass().addAll("badge", "badge-category");

        boolean isFunded = i.purchased || i.contributed >= i.price;
        Label statusBadge = new Label();
        if (isFunded) {
            statusBadge.setText("✓ Fully Funded");
            statusBadge.getStyleClass().addAll("badge", "badge-funded");
        } else if (i.contributed > 0) {
            statusBadge.setText(String.format("In Progress ($%.2f left)", i.remaining()));
            statusBadge.getStyleClass().addAll("badge", "badge-progress");
        } else {
            statusBadge.setText("Awaiting Gifts");
            statusBadge.getStyleClass().addAll("badge", "badge-open");
        }
        metaRow.getChildren().addAll(catBadge, statusBadge);

        // Title and description
        Label title = new Label(i.title);
        title.getStyleClass().add("card-title");

        Label desc = new Label(
                i.description == null || i.description.trim().isEmpty() ? "No description provided." : i.description);
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        // Visual Progress Bar
        double ratio = i.price > 0 ? Math.min(1.0, i.contributed / i.price) : 0.0;
        ProgressBar pbar = new ProgressBar(ratio);
        pbar.setMaxWidth(Double.MAX_VALUE);
        if (isFunded) {
            pbar.getStyleClass().add("funded");
        }

        HBox progressInfo = new HBox(12);
        progressInfo.setAlignment(Pos.CENTER_LEFT);
        Label raisedLbl = new Label(
                String.format("$%.2f raised of $%.2f (%.0f%%)", i.contributed, i.price, ratio * 100));
        raisedLbl.getStyleClass().add(isFunded ? "success" : "accent");

        Label remainLbl = new Label(isFunded ? "Goal reached!" : String.format("$%.2f remaining", i.remaining()));
        remainLbl.getStyleClass().add("muted");

        progressInfo.getChildren().addAll(raisedLbl, new Label("•"), remainLbl);

        contentBox.getChildren().addAll(metaRow, title, desc, pbar, progressInfo);
        card.getChildren().add(contentBox);

        // Action Buttons
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);
        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("secondary");
        editBtn.setMinWidth(85);
        editBtn.setOnAction(e -> showEditWishDialog(i));

        Button delBtn = new Button("🗑 Delete");
        delBtn.getStyleClass().add("danger");
        delBtn.setMinWidth(85);
        delBtn.setOnAction(e -> showDeleteWishDialog(i));

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().add(actions);

        return card;
    }

    private HBox friendWishCard(WishItem i) {
        HBox card = new HBox(18);
        card.getStyleClass().add("wish-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox contentBox = new VBox(8);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Resolve owner name from friends list
        String ownerName = null;
        if (snapshot != null && snapshot.friends != null) {
            for (User friend : snapshot.friends) {
                if (friend.id == i.ownerId) {
                    ownerName = friend.name;
                    break;
                }
            }
        }
        if (ownerName == null && snapshot != null && snapshot.user != null && snapshot.user.id == i.ownerId) {
            ownerName = snapshot.user.name;
        }

        Label ownerBadge = new Label("👤 " + (ownerName != null ? ownerName : "Friend"));
        ownerBadge.getStyleClass().addAll("badge", "badge-owner");

        Label catBadge = new Label(i.category != null && !i.category.isEmpty() ? i.category : "General");
        catBadge.getStyleClass().addAll("badge", "badge-category");

        boolean isFunded = i.purchased || i.contributed >= i.price;
        Label statusBadge = new Label(isFunded ? "✓ Fully Funded" : String.format("$%.2f remaining", i.remaining()));
        statusBadge.getStyleClass().addAll("badge", isFunded ? "badge-funded" : "badge-progress");
        metaRow.getChildren().addAll(ownerBadge, catBadge, statusBadge);

        Label title = new Label(i.title);
        title.getStyleClass().add("card-title");

        Label desc = new Label(
                i.description == null || i.description.trim().isEmpty() ? "No description." : i.description);
        desc.getStyleClass().add("card-desc");

        double ratio = i.price > 0 ? Math.min(1.0, i.contributed / i.price) : 0.0;
        ProgressBar pbar = new ProgressBar(ratio);
        pbar.setMaxWidth(Double.MAX_VALUE);
        if (isFunded)
            pbar.getStyleClass().add("funded");

        HBox progressInfo = new HBox(12);
        progressInfo.setAlignment(Pos.CENTER_LEFT);
        Label raisedLbl = new Label(
                String.format("$%.2f raised of $%.2f (%.0f%%)", i.contributed, i.price, ratio * 100));
        raisedLbl.getStyleClass().add(isFunded ? "success" : "accent");
        progressInfo.getChildren().addAll(raisedLbl);

        contentBox.getChildren().addAll(metaRow, title, desc, pbar, progressInfo);
        card.getChildren().add(contentBox);

        // Contribute Action
        if (!isFunded) {
            Button contributeBtn = new Button("🎁 Contribute");
            contributeBtn.getStyleClass().add("primary");
            contributeBtn.setOnAction(e -> showContributeDialog(i));
            card.getChildren().add(contributeBtn);
        } else {
            Label completedLbl = new Label("🎉 Fully Funded");
            completedLbl.getStyleClass().add("success");
            card.getChildren().add(completedLbl);
        }

        return card;
    }

    // FRIENDS TAB
    private void friends(VBox p) {
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button findBtn = new Button("＋  Find & Add Friends");
        findBtn.getStyleClass().add("primary");
        findBtn.setOnAction(e -> showFindFriendsDialog());
        topBar.getChildren().add(findBtn);
        p.getChildren().add(topBar);

        p.getChildren().add(section("Your Circle (" + snapshot.friends.size() + ")"));
        if (snapshot.friends.isEmpty()) {
            p.getChildren().add(empty("You have not added any friends yet. Connect with people to share wishes!"));
        } else {
            for (User u : snapshot.friends) {
                p.getChildren().add(personCard(u, "Remove"));
            }
        }

        p.getChildren().add(section("Incoming Friend Requests (" + snapshot.incoming.size() + ")"));
        if (snapshot.incoming.isEmpty()) {
            p.getChildren().add(empty("No pending friend requests at this time."));
        } else {
            for (User u : snapshot.incoming) {
                p.getChildren().add(personCard(u, "Accept"));
            }
        }
    }

    private HBox personCard(User u, String action) {
        HBox box = new HBox(14);
        box.getStyleClass().add("person");
        box.setAlignment(Pos.CENTER_LEFT);

        String initial = (u.name != null && !u.name.isEmpty()) ? u.name.substring(0, 1).toUpperCase() : "U";
        Label avatar = new Label(initial);
        avatar.getStyleClass().add("user-avatar");

        VBox info = new VBox(3);
        Label name = new Label(u.name);
        name.getStyleClass().add("card-title");
        Label email = new Label(u.email);
        email.getStyleClass().add("muted");
        info.getChildren().addAll(name, email);
        box.getChildren().addAll(avatar, info);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        box.getChildren().add(sp);

        if (action.equals("Accept")) {
            HBox actions = new HBox(8);
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("primary");
            acceptBtn.setOnAction(e -> respondFriend(u, true));

            Button declineBtn = new Button("Decline");
            declineBtn.getStyleClass().add("secondary");
            declineBtn.setOnAction(e -> respondFriend(u, false));

            actions.getChildren().addAll(acceptBtn, declineBtn);
            box.getChildren().add(actions);
        } else {
            HBox actions = new HBox(8);
            Button viewBtn = new Button("🎁 View Wishes");
            viewBtn.getStyleClass().add("secondary");
            viewBtn.setOnAction(e -> showFriendWishesDialog(u));

            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("danger");
            removeBtn.setOnAction(e -> confirmRemoveFriend(u));

            actions.getChildren().addAll(viewBtn, removeBtn);
            box.getChildren().add(actions);
        }

        return box;
    }

    private void showFriendWishesDialog(User friend) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(friend.name + "'s Wish List");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(560);

        Label titleHeader = new Label("🌟 " + friend.name + "'s Wish List");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Browse items and help make " + friend.name + "'s wishes come true:");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        VBox list = new VBox(12);
        List<WishItem> friendWishes = new ArrayList<>();
        if (snapshot != null && snapshot.friendItems != null) {
            for (WishItem item : snapshot.friendItems) {
                if (item.ownerId == friend.id) {
                    friendWishes.add(item);
                }
            }
        }

        if (friendWishes.isEmpty()) {
            list.getChildren().add(empty(friend.name + " has not added any wishes yet."));
        } else {
            for (WishItem item : friendWishes) {
                list.getChildren().add(friendWishCard(item));
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(360);
        sp.getStyleClass().add("clean-scroll");

        content.getChildren().addAll(titleHeader, subtitle, sp);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
        renderCurrentTab();
    }

    // NOTIFICATIONS TAB
    private void notices(VBox p) {
        if (snapshot.notifications.isEmpty()) {
            p.getChildren().add(empty(
                    "No notifications yet. You will see updates when friends join or contribute to your wishes."));
        } else {
            for (Notification n : snapshot.notifications) {
                HBox card = new HBox(12);
                card.getStyleClass().add("person");
                card.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label("💌");
                icon.setStyle("-fx-font-size: 18px;");
                Label text = new Label(n.text);
                text.setStyle("-fx-font-size: 14px; -fx-text-fill: #15233f;");
                card.getChildren().addAll(icon, text);
                p.getChildren().add(card);
            }
        }
        try {
            api.call(new Request("readNotifications").put("userId", user.id));
        } catch (Exception ignored) {
        }
    }

    private VBox empty(String t) {
        VBox box = new VBox(8);
        box.getStyleClass().add("empty");
        box.setAlignment(Pos.CENTER);
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        box.getChildren().add(l);
        return box;
    }

    // CUSTOM THEMED DIALOGS & MODAL BOXES

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (css != null) {
            pane.getStylesheets().add(css);
        }
        pane.getStyleClass().add("dialog-pane");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
    }

    private void showEditProfileDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(440);

        Label titleHeader = new Label("👤 Edit Your Profile");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Update your name, email, or set a new password below:");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Name
        VBox nameGrp = new VBox(4);
        Label nameLbl = new Label("Full Name *");
        nameLbl.getStyleClass().add("form-label");
        TextField nameField = new TextField(user != null ? user.name : "");
        nameField.setPromptText("Enter your full name");
        nameGrp.getChildren().addAll(nameLbl, nameField);

        // Email
        VBox emailGrp = new VBox(4);
        Label emailLbl = new Label("Email Address *");
        emailLbl.getStyleClass().add("form-label");
        TextField emailField = new TextField(user != null ? user.email : "");
        emailField.setPromptText("Enter your email");
        emailGrp.getChildren().addAll(emailLbl, emailField);

        // Password
        VBox passGrp = new VBox(4);
        Label passLbl = new Label("New Password (optional)");
        passLbl.getStyleClass().add("form-label");
        PasswordBox passBox = new PasswordBox("Leave empty to keep current password", "text-field");
        Label passHint = new Label("Only fill this if you want to change your password (4+ chars).");
        passHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        passGrp.getChildren().addAll(passLbl, passBox, passHint);

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("modal-error-label");

        content.getChildren().addAll(titleHeader, subtitle, nameGrp, emailGrp, passGrp, errorLbl);
        dialog.getDialogPane().setContent(content);

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        boolean[] success = new boolean[1];
        String[] successMsg = new String[1];

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String nameVal = nameField.getText().trim();
            String emailVal = emailField.getText().trim();
            String passVal = passBox.getText();

            if (nameVal.isEmpty()) {
                errorLbl.setText("Full name cannot be empty.");
                nameField.requestFocus();
                event.consume();
                return;
            }
            if (emailVal.isEmpty() || !emailVal.contains("@")) {
                errorLbl.setText("Please enter a valid email address.");
                emailField.requestFocus();
                event.consume();
                return;
            }
            if (!passVal.isEmpty() && passVal.length() < 4) {
                errorLbl.setText("New password must be at least 4 characters.");
                passBox.requestFocus();
                event.consume();
                return;
            }

            try {
                Request req = new Request("updateProfile")
                        .put("userId", user.id)
                        .put("name", nameVal)
                        .put("email", emailVal);
                if (!passVal.isEmpty()) {
                    req.put("password", passVal);
                }
                Response r = api.call(req);
                if (!r.ok) {
                    errorLbl.setText(r.message != null ? r.message : "Failed to update profile.");
                    event.consume();
                    return;
                }
                user = (User) r.payload;
                success[0] = true;
                successMsg[0] = r.message != null ? r.message : "Profile updated successfully!";
            } catch (Exception ex) {
                errorLbl.setText("Error updating profile: " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();

        if (success[0]) {
            if (sidebarAvatar != null && user.name != null && !user.name.isEmpty()) {
                sidebarAvatar.setText(user.name.substring(0, 1).toUpperCase());
            }
            if (sidebarUserName != null) {
                sidebarUserName.setText(user.name);
            }
            if (sidebarUserEmail != null) {
                sidebarUserEmail.setText(user.email);
            }
            renderCurrentTab();
            showAlert("Profile Updated", successMsg[0], false);
        }
    }

    private void showAddWishDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add a New Wish");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(460);

        Label titleHeader = new Label("✨ Create a New Wish");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Enter the details of your dream item to let your friends discover it.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Title
        VBox titleGrp = new VBox(4);
        Label titleLbl = new Label("Wish Title *");
        titleLbl.getStyleClass().add("form-label");
        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Sony Wireless Headphones");
        titleGrp.getChildren().addAll(titleLbl, titleField);

        // Category
        VBox catGrp = new VBox(4);
        Label catLbl = new Label("Category");
        catLbl.getStyleClass().add("form-label");
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "Tech & Gadgets",
                "Books & Learning",
                "Fashion & Apparel",
                "Home & Living",
                "Gaming & Toys",
                "Experiences & Travel",
                "Other"));
        catBox.setEditable(true);
        catBox.setValue("Tech & Gadgets");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catGrp.getChildren().addAll(catLbl, catBox);

        // Price
        VBox priceGrp = new VBox(4);
        Label priceLbl = new Label("Target Price ($) *");
        priceLbl.getStyleClass().add("form-label");
        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 150.00");
        priceGrp.getChildren().addAll(priceLbl, priceField);

        // Description
        VBox descGrp = new VBox(4);
        Label descLbl = new Label("Description / Note");
        descLbl.getStyleClass().add("form-label");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Add details, color, model, or where to find it...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descGrp.getChildren().addAll(descLbl, descArea);

        // Error message
        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("modal-error-label");

        content.getChildren().addAll(titleHeader, subtitle, titleGrp, catGrp, priceGrp, descGrp, errorLbl);
        dialog.getDialogPane().setContent(content);

        ButtonType addBtnType = new ButtonType("Add Wish", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(addBtnType);
        boolean[] success = new boolean[1];
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String titleVal = titleField.getText().trim();
            if (titleVal.isEmpty()) {
                errorLbl.setText("Please provide a title for your wish.");
                titleField.requestFocus();
                event.consume();
                return;
            }
            double priceVal;
            try {
                priceVal = Double.parseDouble(priceField.getText().trim());
                if (priceVal <= 0) {
                    errorLbl.setText("Price must be greater than zero.");
                    priceField.requestFocus();
                    event.consume();
                    return;
                }
            } catch (Exception e) {
                errorLbl.setText("Please enter a valid numeric price (e.g. 49.99).");
                priceField.requestFocus();
                event.consume();
                return;
            }

            String catVal = catBox.getValue() != null && !catBox.getValue().trim().isEmpty() ? catBox.getValue().trim()
                    : "General";
            try {
                api.call(new Request("addItem")
                        .put("userId", user.id)
                        .put("title", titleVal)
                        .put("category", catVal)
                        .put("description", descArea.getText().trim())
                        .put("price", priceVal));
                success[0] = true;
            } catch (Exception ex) {
                errorLbl.setText("Error saving wish: " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
        if (success[0]) {
            renderCurrentTab();
        }
    }

    private void showEditWishDialog(WishItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Wish");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(460);

        Label titleHeader = new Label("✏️ Edit Wish");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Update your wish item details.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Already contributed info box
        if (item.contributed > 0) {
            VBox infoBox = new VBox(4);
            infoBox.getStyleClass().add("modal-info-box");
            Label infoTitle = new Label(String.format("Already Raised: $%.2f", item.contributed));
            infoTitle.getStyleClass().add("accent");
            Label infoNotice = new Label("Target price cannot be set below the already collected amount.");
            infoNotice.getStyleClass().add("muted");
            infoBox.getChildren().addAll(infoTitle, infoNotice);
            content.getChildren().add(infoBox);
        }

        // Title
        VBox titleGrp = new VBox(4);
        Label titleLbl = new Label("Wish Title *");
        titleLbl.getStyleClass().add("form-label");
        TextField titleField = new TextField(item.title);
        titleGrp.getChildren().addAll(titleLbl, titleField);

        // Category
        VBox catGrp = new VBox(4);
        Label catLbl = new Label("Category");
        catLbl.getStyleClass().add("form-label");
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "Tech & Gadgets",
                "Books & Learning",
                "Fashion & Apparel",
                "Home & Living",
                "Gaming & Toys",
                "Experiences & Travel",
                "Other"));
        catBox.setEditable(true);
        catBox.setValue(item.category != null ? item.category : "General");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catGrp.getChildren().addAll(catLbl, catBox);

        // Price
        VBox priceGrp = new VBox(4);
        Label priceLbl = new Label("Target Price ($) *");
        priceLbl.getStyleClass().add("form-label");
        TextField priceField = new TextField(String.format(Locale.US, "%.2f", item.price));
        priceGrp.getChildren().addAll(priceLbl, priceField);

        // Description
        VBox descGrp = new VBox(4);
        Label descLbl = new Label("Description / Note");
        descLbl.getStyleClass().add("form-label");
        TextArea descArea = new TextArea(item.description != null ? item.description : "");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descGrp.getChildren().addAll(descLbl, descArea);

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("modal-error-label");

        content.getChildren().addAll(titleHeader, subtitle, titleGrp, catGrp, priceGrp, descGrp, errorLbl);
        dialog.getDialogPane().setContent(content);

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        boolean[] success = new boolean[1];
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String titleVal = titleField.getText().trim();
            if (titleVal.isEmpty()) {
                errorLbl.setText("Title cannot be empty.");
                titleField.requestFocus();
                event.consume();
                return;
            }
            double priceVal;
            try {
                priceVal = Double.parseDouble(priceField.getText().trim());
                if (priceVal <= 0) {
                    errorLbl.setText("Price must be greater than zero.");
                    event.consume();
                    return;
                }
                if (priceVal < item.contributed) {
                    errorLbl.setText(String.format("Price cannot be less than already collected amount ($%.2f).",
                            item.contributed));
                    event.consume();
                    return;
                }
            } catch (Exception e) {
                errorLbl.setText("Please enter a valid numeric price.");
                event.consume();
                return;
            }

            String catVal = catBox.getValue() != null && !catBox.getValue().trim().isEmpty() ? catBox.getValue().trim()
                    : "General";
            try {
                api.call(new Request("updateItem")
                        .put("itemId", item.id)
                        .put("title", titleVal)
                        .put("category", catVal)
                        .put("description", descArea.getText().trim())
                        .put("price", priceVal));
                success[0] = true;
            } catch (Exception ex) {
                errorLbl.setText("Error updating wish: " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
        if (success[0]) {
            renderCurrentTab();
        }
    }

    private void showDeleteWishDialog(WishItem item) {
        if (showConfirmDialog("Delete Wish", "Are you sure you want to remove '" + item.title + "' from your wishlist?",
                "Yes, Delete", true)) {
            try {
                api.call(new Request("deleteItem").put("itemId", item.id));
                renderCurrentTab();
            } catch (Exception ignored) {
            }
        }
    }

    private void showContributeDialog(WishItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Contribute to Wish");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(440);

        Label titleHeader = new Label("🎁 Make a Contribution");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Help your friend reach their goal for: " + item.title);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Summary Card
        VBox summaryBox = new VBox(6);
        summaryBox.getStyleClass().add("modal-info-box");

        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label totalGoalLbl = new Label(String.format("Total Goal: $%.2f", item.price));
        totalGoalLbl.getStyleClass().add("muted");
        Label raisedLbl = new Label(String.format("Raised: $%.2f", item.contributed));
        raisedLbl.getStyleClass().add("accent");
        row1.getChildren().addAll(totalGoalLbl, new Label("•"), raisedLbl);

        Label remainLbl = new Label(String.format("Remaining Needed: $%.2f", item.remaining()));
        remainLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #15233f;");

        summaryBox.getChildren().addAll(row1, remainLbl);

        // Quick amount chips
        Label quickLbl = new Label("Quick Select Amount:");
        quickLbl.getStyleClass().add("form-label");
        HBox chipRow = new HBox(8);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        TextField amountField = new TextField();
        amountField.setPromptText("Enter custom amount ($)");

        double remaining = item.remaining();
        double[] quickAmounts = { 10.0, 25.0, 50.0 };
        for (double amt : quickAmounts) {
            if (remaining >= amt) {
                Button chip = new Button(String.format("$%.0f", amt));
                chip.getStyleClass().add("chip-button");
                chip.setOnAction(e -> amountField.setText(String.format(Locale.US, "%.2f", amt)));
                chipRow.getChildren().add(chip);
            }
        }
        Button fullChip = new Button(String.format("Full ($%.2f)", remaining));
        fullChip.getStyleClass().add("chip-button");
        fullChip.setOnAction(e -> amountField.setText(String.format(Locale.US, "%.2f", remaining)));
        chipRow.getChildren().add(fullChip);

        VBox inputGrp = new VBox(4);
        Label amtLbl = new Label("Contribution Amount ($) *");
        amtLbl.getStyleClass().add("form-label");
        inputGrp.getChildren().addAll(amtLbl, amountField);

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("modal-error-label");

        content.getChildren().addAll(titleHeader, subtitle, summaryBox, quickLbl, chipRow, inputGrp, errorLbl);
        dialog.getDialogPane().setContent(content);

        ButtonType contBtnType = new ButtonType("Contribute", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(contBtnType, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(contBtnType);
        boolean[] success = new boolean[1];
        String[] successMsg = new String[1];
        double[] addedAmount = new double[1];
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            double amt;
            try {
                amt = Double.parseDouble(amountField.getText().trim());
                if (amt <= 0) {
                    errorLbl.setText("Please enter an amount greater than 0.");
                    amountField.requestFocus();
                    event.consume();
                    return;
                }
                if (amt > item.remaining() + 0.001) {
                    errorLbl.setText(String.format("Amount exceeds the remaining needed ($%.2f).", item.remaining()));
                    amountField.requestFocus();
                    event.consume();
                    return;
                }
            } catch (Exception e) {
                errorLbl.setText("Please enter a valid dollar amount.");
                amountField.requestFocus();
                event.consume();
                return;
            }

            try {
                Response r = api.call(new Request("contribute")
                        .put("userId", user.id)
                        .put("itemId", item.id)
                        .put("amount", amt));
                if (!r.ok) {
                    errorLbl.setText(r.message != null ? r.message : "Failed to process contribution.");
                    event.consume();
                    return;
                }
                success[0] = true;
                addedAmount[0] = amt;
                successMsg[0] = r.message != null ? r.message : "Thank you for making this wish come true!";
            } catch (Exception ex) {
                errorLbl.setText("Error processing contribution: " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();

        if (success[0]) {
            double amtNow = addedAmount[0];
            double newContributed = item.contributed + amtNow;
            localContributions.put(item.id, newContributed);
            item.contributed = newContributed;
            if (item.remaining() <= 0.001) {
                item.purchased = true;
            }
            if (snapshot != null) {
                for (WishItem fi : snapshot.friendItems) {
                    if (fi.id == item.id) {
                        fi.contributed = newContributed;
                        if (fi.remaining() <= 0.001)
                            fi.purchased = true;
                    }
                }
                for (WishItem mi : snapshot.myItems) {
                    if (mi.id == item.id) {
                        mi.contributed = newContributed;
                        if (mi.remaining() <= 0.001)
                            mi.purchased = true;
                    }
                }
            }
            renderCurrentTab();
            showAlert("Contribution Successful!", successMsg[0], false);
        }
    }

    private void showFindFriendsDialog() {
        if (snapshot.discoverable.isEmpty()) {
            showAlert("Find Friends", "No new people to discover right now. Check back later!", false);
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Find Friends");
        styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(440);

        Label titleHeader = new Label("👥 Connect with People");
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15233f;");
        Label subtitle = new Label("Select a user to send them a friend request:");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        ListView<User> listView = new ListView<>(FXCollections.observableArrayList(snapshot.discoverable));
        listView.setPrefHeight(220);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    String init = u.name != null && !u.name.isEmpty() ? u.name.substring(0, 1).toUpperCase() : "U";
                    Label av = new Label(init);
                    av.getStyleClass().add("user-avatar");
                    VBox textGrp = new VBox(2);
                    Label n = new Label(u.name);
                    n.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #15233f;");
                    Label em = new Label(u.email);
                    em.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                    textGrp.getChildren().addAll(n, em);
                    row.getChildren().addAll(av, textGrp);
                    setGraphic(row);
                }
            }
        });
        listView.getSelectionModel().selectFirst();

        content.getChildren().addAll(titleHeader, subtitle, listView);
        dialog.getDialogPane().setContent(content);

        ButtonType sendBtnType = new ButtonType("Send Request", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == sendBtnType ? listView.getSelectionModel().getSelectedItem() : null);

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(targetUser -> {
            try {
                api.call(new Request("addFriend").put("userId", user.id).put("otherId", targetUser.id));
                showAlert("Friend Request Sent", "Your friend request to " + targetUser.name + " has been sent!",
                        false);
                renderCurrentTab();
            } catch (Exception ignored) {
            }
        });
    }

    private void respondFriend(User u, boolean accept) {
        try {
            api.call(
                    new Request(accept ? "acceptFriend" : "declineFriend").put("userId", user.id).put("otherId", u.id));
            renderCurrentTab();
        } catch (Exception ignored) {
        }
    }

    private void confirmRemoveFriend(User u) {
        if (showConfirmDialog("Remove Friend", "Are you sure you want to remove " + u.name + " from your circle?",
                "Yes, Remove", true)) {
            try {
                api.call(new Request("removeFriend").put("userId", user.id).put("otherId", u.id));
                renderCurrentTab();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean showConfirmDialog(String title, String message, String confirmBtnText, boolean isDanger) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(400);

        Label titleHeader = new Label((isDanger ? "⚠️ " : "❓ ") + title);
        titleHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #15233f;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
        msgLbl.setWrapText(true);

        content.getChildren().addAll(titleHeader, msgLbl);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType(confirmBtnText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        if (isDanger) {
            Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
            okButton.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;");
        }

        return dialog.showAndWait().orElse(ButtonType.CANCEL) == okType;
    }

    private void showAlert(String title, String message, boolean isError) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(380);

        Label titleHeader = new Label((isError ? "❌ " : "✨ ") + title);
        titleHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #15233f;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
        msgLbl.setWrapText(true);

        content.getChildren().addAll(titleHeader, msgLbl);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private boolean hasDataChanged(Snapshot oldS, Snapshot newS) {
        if (oldS == null || newS == null) return true;
        if (oldS.myItems.size() != newS.myItems.size()) return true;
        if (oldS.friendItems.size() != newS.friendItems.size()) return true;
        if (oldS.friends.size() != newS.friends.size()) return true;
        if (oldS.incoming.size() != newS.incoming.size()) return true;
        if (oldS.notifications.size() != newS.notifications.size()) return true;

        for (int i = 0; i < oldS.myItems.size(); i++) {
            WishItem a = oldS.myItems.get(i);
            WishItem b = newS.myItems.get(i);
            if (a.id != b.id || Math.abs(a.contributed - b.contributed) > 0.001 || Math.abs(a.price - b.price) > 0.001
                    || a.purchased != b.purchased || !Objects.equals(a.title, b.title) || !Objects.equals(a.category, b.category)) {
                return true;
            }
        }
        for (int i = 0; i < oldS.friendItems.size(); i++) {
            WishItem a = oldS.friendItems.get(i);
            WishItem b = newS.friendItems.get(i);
            if (a.id != b.id || Math.abs(a.contributed - b.contributed) > 0.001 || Math.abs(a.price - b.price) > 0.001
                    || a.purchased != b.purchased || !Objects.equals(a.title, b.title) || !Objects.equals(a.category, b.category)) {
                return true;
            }
        }
        return false;
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> {
            if (user != null && api != null && stage != null && stage.isShowing()) {
                refreshDataSilently();
            }
        }));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    private void refreshDataSilently() {
        try {
            if (api != null && user != null) {
                Snapshot s = (Snapshot) api.call(new Request("snapshot").put("userId", user.id)).payload;
                if (s != null) {
                    for (WishItem fi : s.friendItems) {
                        if (localContributions.containsKey(fi.id)) {
                            fi.contributed = Math.max(fi.contributed, localContributions.get(fi.id));
                            if (fi.remaining() <= 0.001)
                                fi.purchased = true;
                        }
                    }
                    for (WishItem mi : s.myItems) {
                        if (localContributions.containsKey(mi.id)) {
                            mi.contributed = Math.max(mi.contributed, localContributions.get(mi.id));
                            if (mi.remaining() <= 0.001)
                                mi.purchased = true;
                        }
                    }

                    boolean changed = hasDataChanged(snapshot, s);
                    snapshot = s;
                    updateNotificationBadge();
                    if (changed) {
                        renderCurrentTab();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void refreshData() {
        try {
            if (api != null && user != null) {
                Snapshot s = (Snapshot) api.call(new Request("snapshot").put("userId", user.id)).payload;
                if (s != null) {
                    for (WishItem fi : s.friendItems) {
                        if (localContributions.containsKey(fi.id)) {
                            fi.contributed = Math.max(fi.contributed, localContributions.get(fi.id));
                            if (fi.remaining() <= 0.001)
                                fi.purchased = true;
                        }
                    }
                    for (WishItem mi : s.myItems) {
                        if (localContributions.containsKey(mi.id)) {
                            mi.contributed = Math.max(mi.contributed, localContributions.get(mi.id));
                            if (mi.remaining() <= 0.001)
                                mi.purchased = true;
                        }
                    }
                    snapshot = s;
                }
            }
        } catch (Exception ignored) {
            if (snapshot == null)
                snapshot = new Snapshot();
        }
    }

    private void showScene(Region root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        if (css != null)
            scene.getStylesheets().add(css);
        stage.setScene(scene);
        stage.show();
    }

    // REUSABLE PASSWORD BOX WITH EYE TOGGLE
    private static class PasswordBox extends StackPane {
        final PasswordField passwordField = new PasswordField();
        final TextField textField = new TextField();
        final Button eyeBtn = new Button("👁");

        PasswordBox(String prompt, String styleClass) {
            passwordField.setPromptText(prompt);
            passwordField.getStyleClass().add(styleClass);
            passwordField.setStyle("-fx-padding: 13px 44px 13px 14px;");

            textField.setPromptText(prompt);
            textField.getStyleClass().add(styleClass);
            textField.setStyle("-fx-padding: 13px 44px 13px 14px;");
            textField.setVisible(false);
            textField.setManaged(false);

            textField.textProperty().bindBidirectional(passwordField.textProperty());

            eyeBtn.getStyleClass().add("eye-toggle-btn");
            eyeBtn.setCursor(javafx.scene.Cursor.HAND);
            StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);
            StackPane.setMargin(eyeBtn, new Insets(0, 8, 0, 0));

            Tooltip tooltip = new Tooltip("Show password");
            Tooltip.install(eyeBtn, tooltip);

            eyeBtn.setOnAction(e -> {
                if (passwordField.isVisible()) {
                    passwordField.setVisible(false);
                    passwordField.setManaged(false);
                    textField.setVisible(true);
                    textField.setManaged(true);
                    eyeBtn.setText("🙈");
                    tooltip.setText("Hide password");
                    textField.requestFocus();
                    textField.positionCaret(textField.getText() != null ? textField.getText().length() : 0);
                } else {
                    textField.setVisible(false);
                    textField.setManaged(false);
                    passwordField.setVisible(true);
                    passwordField.setManaged(true);
                    eyeBtn.setText("👁");
                    tooltip.setText("Show password");
                    passwordField.requestFocus();
                    passwordField.positionCaret(passwordField.getText() != null ? passwordField.getText().length() : 0);
                }
            });

            getChildren().addAll(passwordField, textField, eyeBtn);
        }

        String getText() {
            return passwordField.getText() != null ? passwordField.getText() : "";
        }
    }
}
