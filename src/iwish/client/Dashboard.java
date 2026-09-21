package iwish.client;

import iwish.common.*;
import iwish.common.Models.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class Dashboard extends JFrame {
    final Color NAVY = new Color(19, 30, 58);
    final Color PINK = new Color(239, 92, 125);
    final Color BG = new Color(247, 249, 252);
    final User user;
    final ApiClient api;
    Snapshot snap;
    JPanel content;
    JLabel welcome, notif;
    String currentTab = "Overview";

    Dashboard(User u, ApiClient a) {
        user = u;
        api = a;
        setTitle("I-Wish • " + u.name);
        setSize(1160, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        load();
        build();
    }

    void load() {
        try {
            snap = (Snapshot) api.call(new Request("snapshot").put("userId", user.id)).payload;
        } catch (Exception e) {
            snap = new Snapshot();
        }
    }

    void build() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel side = new JPanel();
        side.setBackground(NAVY);
        side.setBorder(BorderFactory.createEmptyBorder(28, 20, 20, 20));
        side.setPreferredSize(new Dimension(240, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("✦ I-Wish");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 28));
        side.add(logo);

        JLabel motto = new JLabel("make it memorable");
        motto.setForeground(new Color(175, 190, 219));
        side.add(motto);
        side.add(Box.createVerticalStrut(30));

        // User badge
        JPanel uCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        uCard.setBackground(new Color(30, 44, 76));
        uCard.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JLabel uName = new JLabel("👤 " + user.name);
        uName.setForeground(Color.WHITE);
        uName.setFont(new Font("SansSerif", Font.BOLD, 13));
        uCard.add(uName);
        side.add(uCard);
        side.add(Box.createVerticalStrut(20));

        String[] tabs = {"Overview", "My Wish List", "Friends", "Notifications"};
        for (String t : tabs) {
            JButton b = new JButton(t);
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(200, 44));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setForeground(Color.WHITE);
            b.setBackground(NAVY);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.BOLD, 14));
            b.addActionListener(e -> show(t));
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }

        side.add(Box.createVerticalGlue());
        JButton sync = new JButton("🔄 Sync Data");
        sync.setAlignmentX(LEFT_ALIGNMENT);
        sync.addActionListener(e -> refresh(currentTab));
        side.add(sync);
        side.add(Box.createVerticalStrut(8));

        JButton out = new JButton("Sign out");
        out.setAlignmentX(LEFT_ALIGNMENT);
        out.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        side.add(out);

        root.add(side, BorderLayout.WEST);
        content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        root.add(content);
        add(root);
        show("Overview");

        javax.swing.Timer timer = new javax.swing.Timer(2500, e -> {
            if (isShowing() && api != null && user != null) {
                try {
                    Snapshot s = (Snapshot) api.call(new Request("snapshot").put("userId", user.id)).payload;
                    if (s != null && hasChanged(snap, s)) {
                        snap = s;
                        show(currentTab);
                    }
                } catch (Exception ignored) {}
            }
        });
        timer.start();
    }

    boolean hasChanged(Snapshot a, Snapshot b) {
        if (a == null || b == null) return true;
        if (a.myItems.size() != b.myItems.size() || a.friendItems.size() != b.friendItems.size()
                || a.friends.size() != b.friends.size() || a.notifications.size() != b.notifications.size()) return true;
        for (int i = 0; i < a.myItems.size(); i++) {
            if (Math.abs(a.myItems.get(i).contributed - b.myItems.get(i).contributed) > 0.001
                    || Math.abs(a.myItems.get(i).price - b.myItems.get(i).price) > 0.001) return true;
        }
        for (int i = 0; i < a.friendItems.size(); i++) {
            if (Math.abs(a.friendItems.get(i).contributed - b.friendItems.get(i).contributed) > 0.001
                    || Math.abs(a.friendItems.get(i).price - b.friendItems.get(i).price) > 0.001) return true;
        }
        return false;
    }

    void show(String tab) {
        currentTab = tab;
        content.removeAll();
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        welcome = new JLabel(tab);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcome.setForeground(NAVY);
        top.add(welcome, BorderLayout.WEST);

        long unreadCount = snap.notifications.stream().filter(n -> !n.read).count();
        notif = new JLabel("💌 " + unreadCount + " new notifications");
        notif.setForeground(PINK);
        notif.setFont(new Font("SansSerif", Font.BOLD, 13));
        top.add(notif, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        if (tab.equals("Overview")) overview(body);
        else if (tab.equals("My Wish List")) myList(body);
        else if (tab.equals("Friends")) friends(body);
        else notifications(body);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        p.add(scroll, BorderLayout.CENTER);

        content.add(p);
        content.revalidate();
        content.repaint();
    }

    void overview(JPanel b) {
        JLabel hi = new JLabel("Good to see you, " + user.name + ".");
        hi.setFont(new Font("SansSerif", Font.PLAIN, 20));
        hi.setForeground(NAVY);
        b.add(hi);
        b.add(Box.createVerticalStrut(18));

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(card("MY WISHES", String.valueOf(snap.myItems.size()), "dreams on your list"));
        cards.add(card("FRIENDS", String.valueOf(snap.friends.size()), "people in your circle"));
        cards.add(card("UPDATES", String.valueOf(snap.notifications.stream().filter(n -> !n.read).count()), "moments to discover"));
        b.add(cards);
        b.add(Box.createVerticalStrut(25));

        JLabel title = new JLabel("Friends' wishes");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(NAVY);
        b.add(title);
        b.add(Box.createVerticalStrut(10));

        for (WishItem i : snap.friendItems) wishRow(b, i, true);
        if (snap.friendItems.isEmpty()) b.add(empty("Add friends to discover their wishes."));
    }

    JPanel card(String a, String n, String d) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel x = new JLabel(a);
        x.setForeground(PINK);
        x.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel y = new JLabel(n);
        y.setForeground(NAVY);
        y.setFont(new Font("SansSerif", Font.BOLD, 30));
        JLabel z = new JLabel(d);
        z.setForeground(Color.GRAY);
        p.add(x);
        p.add(y);
        p.add(z);
        return p;
    }

    void myList(JPanel b) {
        // Summary banner
        double totalGoal = snap.myItems.stream().mapToDouble(i -> i.price).sum();
        double totalRaised = snap.myItems.stream().mapToDouble(i -> i.contributed).sum();
        double overallPct = totalGoal > 0 ? (totalRaised / totalGoal) * 100.0 : 0.0;

        JPanel banner = new JPanel(new GridLayout(1, 4, 12, 0));
        banner.setBackground(NAVY);
        banner.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        banner.add(bannerItem("TOTAL WISHES", String.valueOf(snap.myItems.size())));
        banner.add(bannerItem("TOTAL GOAL", String.format("$%.2f", totalGoal)));
        banner.add(bannerItem("FUNDS RAISED", String.format("$%.2f", totalRaised)));
        banner.add(bannerItem("PROGRESS", String.format("%.0f%%", overallPct)));
        b.add(banner);
        b.add(Box.createVerticalStrut(18));

        JButton add = new JButton("＋ Add a wish");
        add.setAlignmentX(LEFT_ALIGNMENT);
        add.setBackground(PINK);
        add.setForeground(Color.WHITE);
        add.setFocusPainted(false);
        add.setFont(new Font("SansSerif", Font.BOLD, 14));
        add.addActionListener(e -> addWish());
        b.add(add);
        b.add(Box.createVerticalStrut(16));

        for (WishItem i : snap.myItems) wishRow(b, i, false);
        if (snap.myItems.isEmpty()) b.add(empty("Your list is empty. Add your first wish to get started!"));
    }

    JPanel bannerItem(String label, String value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel v = new JLabel(value);
        v.setForeground(Color.WHITE);
        v.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel l = new JLabel(label);
        l.setForeground(new Color(175, 190, 219));
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        p.add(v);
        p.add(l);
        return p;
    }

    void wishRow(JPanel b, WishItem i, boolean contribute) {
        JPanel r = new JPanel(new BorderLayout(14, 8));
        r.setBackground(Color.WHITE);
        r.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));

        String ownerName = null;
        if (contribute && snap != null && snap.friends != null) {
            ownerName = snap.friends.stream()
                    .filter(u -> u.id == i.ownerId)
                    .map(u -> u.name)
                    .findFirst()
                    .orElse(null);
        }
        JLabel n = new JLabel((ownerName != null ? "👤 " + ownerName + " — " : "") + i.title);
        n.setFont(new Font("SansSerif", Font.BOLD, 16));
        n.setForeground(NAVY);
        t.add(n);

        JLabel cat = new JLabel((i.category != null ? i.category : "General") + "  •  " + (i.description == null ? "" : i.description));
        cat.setForeground(Color.GRAY);
        t.add(cat);

        double pct = i.price > 0 ? Math.min(100.0, (i.contributed / i.price) * 100.0) : 0;
        JLabel money = new JLabel(String.format("$%.2f raised of $%.2f (%.0f%%)  •  %s",
                i.contributed, i.price, pct,
                i.purchased ? "Fully funded" : "$" + String.format("%.2f remaining", i.remaining())));
        money.setForeground(i.purchased ? new Color(33, 155, 105) : PINK);
        money.setFont(new Font("SansSerif", Font.BOLD, 13));
        t.add(money);

        r.add(t, BorderLayout.CENTER);

        if (contribute && !i.purchased) {
            JButton c = new JButton("Contribute");
            c.setBackground(PINK);
            c.setForeground(Color.WHITE);
            c.setFocusPainted(false);
            c.setFont(new Font("SansSerif", Font.BOLD, 13));
            c.addActionListener(e -> contribute(i));
            r.add(c, BorderLayout.EAST);
        } else if (!contribute) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);
            JButton edit = new JButton("Edit");
            edit.addActionListener(e -> edit(i));
            JButton d = new JButton("Delete");
            d.addActionListener(e -> delete(i));
            actions.add(edit);
            actions.add(d);
            r.add(actions, BorderLayout.EAST);
        }
        b.add(r);
    }

    void friends(JPanel b) {
        JButton add = new JButton("＋ Find people");
        add.setBackground(PINK);
        add.setForeground(Color.WHITE);
        add.setFocusPainted(false);
        add.addActionListener(e -> findFriends());
        b.add(add);
        b.add(Box.createVerticalStrut(18));

        JLabel a = new JLabel("Your circle (" + snap.friends.size() + ")");
        a.setFont(new Font("SansSerif", Font.BOLD, 20));
        a.setForeground(NAVY);
        b.add(a);
        b.add(Box.createVerticalStrut(8));
        for (User u : snap.friends) b.add(person(b, u, "Remove"));
        if (snap.friends.isEmpty()) b.add(empty("You haven't connected with any friends yet."));

        b.add(Box.createVerticalStrut(20));
        JLabel q = new JLabel("Friend requests (" + snap.incoming.size() + ")");
        q.setFont(new Font("SansSerif", Font.BOLD, 20));
        q.setForeground(NAVY);
        b.add(q);
        b.add(Box.createVerticalStrut(8));
        for (User u : snap.incoming) b.add(person(b, u, "Accept"));
        if (snap.incoming.isEmpty()) b.add(empty("No incoming friend requests."));
    }

    JPanel person(JPanel b, User u, String action) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel l = new JLabel(u.name + "  (" + u.email + ")");
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(NAVY);
        p.add(l, BorderLayout.WEST);

        if (action.equals("Accept")) {
            JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            acts.setOpaque(false);
            JButton acc = new JButton("Accept");
            acc.addActionListener(e -> respond(u, true));
            JButton dec = new JButton("Decline");
            dec.addActionListener(e -> respond(u, false));
            acts.add(acc);
            acts.add(dec);
            p.add(acts, BorderLayout.EAST);
        } else if (action.equals("Remove")) {
            JButton x = new JButton("Remove");
            x.addActionListener(e -> removeFriend(u));
            p.add(x, BorderLayout.EAST);
        }
        return p;
    }

    void notifications(JPanel b) {
        for (Notification n : snap.notifications) b.add(empty("💌 " + n.text));
        if (snap.notifications.isEmpty()) b.add(empty("No notifications at this time."));
        try {
            api.call(new Request("readNotifications").put("userId", user.id));
        } catch (Exception ignored) {}
    }

    JLabel empty(String s) {
        JLabel l = new JLabel(s);
        l.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        l.setForeground(Color.GRAY);
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return l;
    }

    void addWish() {
        JTextField t = new JTextField(), cat = new JTextField("Tech & Gadgets"), price = new JTextField();
        JTextArea desc = new JTextArea(3, 20);
        Object[] f = {"Title:", t, "Category:", cat, "Description:", desc, "Price ($):", price};
        if (JOptionPane.showConfirmDialog(this, f, "✨ Add a wish", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String title = t.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                double p = Double.parseDouble(price.getText().trim());
                if (p <= 0) {
                    JOptionPane.showMessageDialog(this, "Price must be greater than zero.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                api.call(new Request("addItem")
                        .put("userId", user.id)
                        .put("title", title)
                        .put("category", cat.getText().trim())
                        .put("description", desc.getText().trim())
                        .put("price", p));
                refresh(currentTab);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void contribute(WishItem i) {
        String a = JOptionPane.showInputDialog(this,
                String.format("Wish: %s\nRemaining needed: $%.2f\n\nHow much would you like to contribute ($)?", i.title, i.remaining()),
                "🎁 Make a Contribution", JOptionPane.QUESTION_MESSAGE);
        if (a != null && !a.trim().isEmpty()) {
            try {
                double amt = Double.parseDouble(a.trim());
                if (amt <= 0 || amt > i.remaining() + 0.001) {
                    JOptionPane.showMessageDialog(this, "Amount must be between $0.01 and $" + String.format("%.2f", i.remaining()), "Invalid Amount", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Response r = api.call(new Request("contribute")
                        .put("userId", user.id)
                        .put("itemId", i.id)
                        .put("amount", amt));
                if (!r.ok) {
                    JOptionPane.showMessageDialog(this, r.message != null ? r.message : "Contribution failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, r.message, "Contribution Received", JOptionPane.INFORMATION_MESSAGE);
                refresh(currentTab);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid contribution amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void edit(WishItem i) {
        JTextField t = new JTextField(i.title), cat = new JTextField(i.category), price = new JTextField(String.valueOf(i.price));
        JTextArea desc = new JTextArea(i.description, 3, 20);
        Object[] f = {"Title:", t, "Category:", cat, "Description:", desc, "Price ($):", price};
        if (JOptionPane.showConfirmDialog(this, f, "✏️ Edit wish", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String title = t.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                double p = Double.parseDouble(price.getText().trim());
                if (p <= 0 || p < i.contributed) {
                    JOptionPane.showMessageDialog(this, "Price cannot be less than the already contributed amount ($" + String.format("%.2f", i.contributed) + ").", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Response r = api.call(new Request("updateItem")
                        .put("itemId", i.id)
                        .put("title", title)
                        .put("category", cat.getText().trim())
                        .put("description", desc.getText().trim())
                        .put("price", p));
                JOptionPane.showMessageDialog(this, r.message, "Success", JOptionPane.INFORMATION_MESSAGE);
                refresh(currentTab);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void delete(WishItem i) {
        if (JOptionPane.showConfirmDialog(this, "Remove wish '" + i.title + "' from your list?", "⚠️ Confirm Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                api.call(new Request("deleteItem").put("itemId", i.id));
                refresh(currentTab);
            } catch (Exception ignored) {}
        }
    }

    void findFriends() {
        if (snap.discoverable.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No new people to discover right now.", "Find People", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] names = snap.discoverable.stream().map(u -> u.name + " (" + u.email + ")").toArray(String[]::new);
        int x = JOptionPane.showOptionDialog(this, "Choose someone to add to your circle:", "Find people",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (x >= 0) {
            try {
                api.call(new Request("addFriend").put("userId", user.id).put("otherId", snap.discoverable.get(x).id));
                JOptionPane.showMessageDialog(this, "Friend request sent!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refresh(currentTab);
            } catch (Exception ignored) {}
        }
    }

    void respond(User u, boolean yes) {
        try {
            api.call(new Request(yes ? "acceptFriend" : "declineFriend").put("userId", user.id).put("otherId", u.id));
            refresh(currentTab);
        } catch (Exception ignored) {}
    }

    void removeFriend(User u) {
        if (JOptionPane.showConfirmDialog(this, "Remove " + u.name + " from your friends?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                api.call(new Request("removeFriend").put("userId", user.id).put("otherId", u.id));
                refresh(currentTab);
            } catch (Exception ignored) {}
        }
    }

    void refresh(String tab) {
        load();
        show(tab);
    }
}
