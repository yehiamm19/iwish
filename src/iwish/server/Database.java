package iwish.server;

import iwish.common.Models.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MySQL-backed repository for I-Wish.
 * All persistence uses JDBC connections with prepared queries and transaction safety.
 */
public class Database {
    public final List<User> users = new ArrayList<>();
    public final List<WishItem> items = new ArrayList<>();
    public final List<Friendship> friendships = new ArrayList<>();
    public final List<Contribution> contributions = new ArrayList<>();
    public final List<Notification> notifications = new ArrayList<>();

    private final String url, user, password;
    private AtomicInteger ids = new AtomicInteger(1);

    /**
     * Hashes password using SHA-256 for secure database storage.
     * Built using standard Java MessageDigest without external dependencies.
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return rawPassword;
        }
    }

    /**
     * Checks if a string is already a 64-character SHA-256 hexadecimal hash.
     */
    public static boolean isSha256(String s) {
        return s != null && s.length() == 64 && s.matches("^[0-9a-fA-F]{64}$");
    }

    public Database() {
        this(System.getenv().getOrDefault("IWISH_DB_URL", "jdbc:mysql://localhost:3306/iwish?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"),
             System.getenv().getOrDefault("IWISH_DB_USER", "root"),
             System.getenv().getOrDefault("IWISH_DB_PASSWORD", ""));
    }

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL Connector/J is missing from the classpath", e);
        }
        try (Connection c = connection()) {
            createSchema(c);
            load(c);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect to MySQL at " + url + ". Please start XAMPP MySQL and verify database settings.", e);
        }
        if (users.isEmpty()) {
            seed();
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void createSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "id INT PRIMARY KEY, "
                    + "name VARCHAR(120) NOT NULL, "
                    + "email VARCHAR(190) UNIQUE NOT NULL, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS friendships ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "requester_id INT NOT NULL, "
                    + "receiver_id INT NOT NULL, "
                    + "status VARCHAR(20) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY uq_friend(requester_id, receiver_id), "
                    + "FOREIGN KEY(requester_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS wish_items ("
                    + "id INT PRIMARY KEY, "
                    + "owner_id INT NOT NULL, "
                    + "title VARCHAR(180) NOT NULL, "
                    + "category VARCHAR(80), "
                    + "description TEXT, "
                    + "price DECIMAL(10,2) NOT NULL, "
                    + "contributed DECIMAL(10,2) NOT NULL DEFAULT 0, "
                    + "purchased BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS contributions ("
                    + "id INT PRIMARY KEY, "
                    + "item_id INT NOT NULL, "
                    + "buyer_id INT NOT NULL, "
                    + "amount DECIMAL(10,2) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY(item_id) REFERENCES wish_items(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY(buyer_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS notifications ("
                    + "id INT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "message VARCHAR(500) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    public synchronized void reload() {
        try (Connection c = connection()) {
            load(c);
        } catch (SQLException e) {
            System.err.println("Database reload error: " + e.getMessage());
        }
    }

    private void load(Connection c) throws SQLException {
        users.clear();
        items.clear();
        friendships.clear();
        contributions.clear();
        notifications.clear();

        try (Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT id, name, email, password_hash FROM users")) {
                while (r.next()) {
                    users.add(new User(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)));
                }
            }

            // Automatically upgrade any legacy plaintext passwords to SHA-256 hash
            boolean hasPlaintext = false;
            for (User u : users) {
                if (u.password != null && !isSha256(u.password)) {
                    u.password = hashPassword(u.password);
                    hasPlaintext = true;
                }
            }
            if (hasPlaintext) {
                try (PreparedStatement pu = c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                    for (User u : users) {
                        pu.setString(1, u.password);
                        pu.setInt(2, u.id);
                        pu.addBatch();
                    }
                    pu.executeBatch();
                }
            }
            try (ResultSet r = s.executeQuery("SELECT id, owner_id, title, category, description, price, contributed, purchased FROM wish_items")) {
                while (r.next()) {
                    WishItem i = new WishItem(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5), r.getDouble(6));
                    i.contributed = r.getDouble(7);
                    i.purchased = r.getBoolean(8);
                    items.add(i);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT requester_id, receiver_id, status FROM friendships")) {
                while (r.next()) {
                    friendships.add(new Friendship(r.getInt(1), r.getInt(2), r.getString(3)));
                }
            }
            try (ResultSet r = s.executeQuery("SELECT id, item_id, buyer_id, amount FROM contributions")) {
                while (r.next()) {
                    contributions.add(new Contribution(r.getInt(1), r.getInt(2), r.getInt(3), r.getDouble(4)));
                }
            }
            try (ResultSet r = s.executeQuery("SELECT id, user_id, message, created_at, is_read FROM notifications")) {
                while (r.next()) {
                    Notification n = new Notification();
                    n.id = r.getInt(1);
                    n.userId = r.getInt(2);
                    n.text = r.getString(3);
                    n.createdAt = r.getTimestamp(4);
                    n.read = r.getBoolean(5);
                    notifications.add(n);
                }
            }
        }

        // Calculate max ID across ALL tables to prevent primary key collision
        int maxUser = 0;
        for (User u : users) if (u.id > maxUser) maxUser = u.id;

        int maxItem = 0;
        for (WishItem i : items) if (i.id > maxItem) maxItem = i.id;

        int maxContrib = 0;
        for (Contribution ct : contributions) if (ct.id > maxContrib) maxContrib = ct.id;

        int maxNotif = 0;
        for (Notification n : notifications) if (n.id > maxNotif) maxNotif = n.id;

        int maxAll = Math.max(Math.max(maxUser, maxItem), Math.max(maxContrib, maxNotif));
        ids = new AtomicInteger(Math.max(1, maxAll + 1));

        // Reconcile any existing duplicates loaded from DB
        Set<Integer> seenNotifIds = new HashSet<>();
        for (Notification n : notifications) {
            if (!seenNotifIds.add(n.id)) {
                n.id = id();
                seenNotifIds.add(n.id);
            }
        }
        Set<Integer> seenContribIds = new HashSet<>();
        for (Contribution ct : contributions) {
            if (!seenContribIds.add(ct.id)) {
                ct.id = id();
                seenContribIds.add(ct.id);
            }
        }
    }

    private void seed() {
        users.add(new User(id(), "Demo User", "demo@iwish.local", hashPassword("demo")));
        users.add(new User(id(), "Mona Ali", "mona@iwish.local", hashPassword("demo")));
        users.add(new User(id(), "Omar Hassan", "omar@iwish.local", hashPassword("demo")));
        friendships.add(new Friendship(1, 2, "ACCEPTED"));
        friendships.add(new Friendship(1, 3, "ACCEPTED"));
        items.add(new WishItem(id(), 2, "Noise Cancelling Headphones", "Tech", "For my daily focus time", 129.99));
        items.add(new WishItem(id(), 2, "Weekend Backpack", "Travel", "A compact adventure bag", 79.50));
        items.add(new WishItem(id(), 3, "Classic Watch", "Style", "A timeless everyday watch", 150.0));
        save();
    }

    public synchronized void save() {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
                s.executeUpdate("DELETE FROM notifications");
                s.executeUpdate("DELETE FROM contributions");
                s.executeUpdate("DELETE FROM wish_items");
                s.executeUpdate("DELETE FROM friendships");
                s.executeUpdate("DELETE FROM users");
                s.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
            }

            // Ensure unique IDs on all notifications and contributions before inserting
            Set<Integer> seenNotifs = new HashSet<>();
            for (Notification n : notifications) {
                if (n.id <= 0 || !seenNotifs.add(n.id)) {
                    n.id = id();
                    seenNotifs.add(n.id);
                }
            }

            Set<Integer> seenContribs = new HashSet<>();
            for (Contribution ct : contributions) {
                if (ct.id <= 0 || !seenContribs.add(ct.id)) {
                    ct.id = id();
                    seenContribs.add(ct.id);
                }
            }

            try (PreparedStatement p = c.prepareStatement("INSERT INTO users(id, name, email, password_hash) VALUES(?,?,?,?)")) {
                for (User u : users) {
                    p.setInt(1, u.id);
                    p.setString(2, u.name);
                    p.setString(3, u.email);
                    p.setString(4, u.password);
                    p.addBatch();
                }
                p.executeBatch();
            }

            try (PreparedStatement p = c.prepareStatement("INSERT INTO friendships(requester_id, receiver_id, status) VALUES(?,?,?)")) {
                for (Friendship f : friendships) {
                    p.setInt(1, f.requesterId);
                    p.setInt(2, f.receiverId);
                    p.setString(3, f.status);
                    p.addBatch();
                }
                p.executeBatch();
            }

            try (PreparedStatement p = c.prepareStatement("INSERT INTO wish_items(id, owner_id, title, category, description, price, contributed, purchased) VALUES(?,?,?,?,?,?,?,?)")) {
                for (WishItem i : items) {
                    p.setInt(1, i.id);
                    p.setInt(2, i.ownerId);
                    p.setString(3, i.title);
                    p.setString(4, i.category);
                    p.setString(5, i.description);
                    p.setDouble(6, i.price);
                    p.setDouble(7, i.contributed);
                    p.setBoolean(8, i.purchased);
                    p.addBatch();
                }
                p.executeBatch();
            }

            try (PreparedStatement p = c.prepareStatement("INSERT INTO contributions(id, item_id, buyer_id, amount) VALUES(?,?,?,?)")) {
                for (Contribution ct : contributions) {
                    p.setInt(1, ct.id);
                    p.setInt(2, ct.itemId);
                    p.setInt(3, ct.buyerId);
                    p.setDouble(4, ct.amount);
                    p.addBatch();
                }
                p.executeBatch();
            }

            try (PreparedStatement p = c.prepareStatement("INSERT INTO notifications(id, user_id, message, created_at, is_read) VALUES(?,?,?,?,?)")) {
                for (Notification n : notifications) {
                    p.setInt(1, n.id);
                    p.setInt(2, n.userId);
                    p.setString(3, n.text);
                    p.setTimestamp(4, new Timestamp(n.createdAt != null ? n.createdAt.getTime() : System.currentTimeMillis()));
                    p.setBoolean(5, n.read);
                    p.addBatch();
                }
                p.executeBatch();
            }

            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("MySQL transaction failed: " + e.getMessage(), e);
        }
    }

    public synchronized int id() {
        return ids.getAndIncrement();
    }

    public User user(int id) {
        for (User u : users) {
            if (u.id == id) return u;
        }
        return null;
    }

    public User byEmail(String email) {
        for (User u : users) {
            if (u.email.equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    public WishItem item(int id) {
        for (WishItem i : items) {
            if (i.id == id) return i;
        }
        return null;
    }
}
