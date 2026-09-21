package iwish.server;

import iwish.common.*;
import iwish.common.Models.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class IWishServer {
    private final Database db;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public IWishServer() {
        db = new Database();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Protocol.PORT);
        running = true;
        System.out.println("I-Wish Server started on port " + Protocol.PORT);
        try {
            while (running) {
                Socket s = serverSocket.accept();
                new Thread(() -> handle(s), "client-handler").start();
            }
        } catch (SocketException ignored) {
        } finally {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void handle(Socket socket) {
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Request r = (Request) in.readObject();
                Response res = dispatch(r);
                out.reset();
                out.writeObject(res);
                out.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized Response dispatch(Request r) {
        try {
            db.reload();
            switch (r.action) {
                case "register": return register(r);
                case "login": return login(r);
                case "snapshot": return snapshot((int) r.data.get("userId"));
                case "addFriend": return friend(r, true);
                case "removeFriend": return friend(r, false);
                case "acceptFriend": return accept(r, true);
                case "declineFriend": return accept(r, false);
                case "addItem": return addItem(r);
                case "updateItem": return updateItem(r);
                case "deleteItem": return deleteItem(r);
                case "contribute": return contribute(r);
                case "readNotifications": return readNotifications(r);
                case "updateProfile": return updateProfile(r);
                default: return Response.fail("Unknown request");
            }
        } catch (Exception e) {
            return Response.fail(e.getMessage() == null ? "Operation failed" : e.getMessage());
        }
    }

    private Response register(Request r) {
        String name = (String) r.data.get("name"), email = (String) r.data.get("email"), pass = (String) r.data.get("password");
        if (name == null || email == null || pass == null || name.isBlank() || email.isBlank() || !email.contains("@") || pass.length() < 4) {
            return Response.fail("Please enter valid details with an email containing '@' (password: 4+ chars).");
        }
        if (db.byEmail(email.trim()) != null) {
            return Response.fail("This email is already registered.");
        }
        // Hash password before saving to MySQL
        String hashed = Database.hashPassword(pass);
        User u = new User(db.id(), name.trim(), email.trim(), hashed);
        db.users.add(u);
        db.save();
        return Response.ok("Welcome to I-Wish!", u);
    }

    private Response login(Request r) {
        String email = (String) r.data.get("email");
        String pass = (String) r.data.get("password");
        if (email == null || pass == null || !email.contains("@")) {
            return Response.fail("Please enter a valid email containing '@'.");
        }
        User u = db.byEmail(email.trim());
        if (u == null) {
            return Response.fail("Email or password is incorrect.");
        }
        String hashed = Database.hashPassword(pass);
        boolean matchesHash = hashed.equalsIgnoreCase(u.password);
        boolean matchesPlain = pass.equals(u.password);
        if (!matchesHash && !matchesPlain) {
            return Response.fail("Email or password is incorrect.");
        }
        // If stored password was plain text, upgrade it to hashed in DB
        if (!Database.isSha256(u.password)) {
            u.password = hashed;
            db.save();
        }
        return Response.ok("Welcome back, " + u.name + "!", u);
    }

    private Response snapshot(int uid) {
        Snapshot s = new Snapshot();
        s.user = db.user(uid);
        Set<Integer> friendIds = db.friendships.stream()
                .filter(f -> "ACCEPTED".equals(f.status) && (f.requesterId == uid || f.receiverId == uid))
                .map(f -> f.requesterId == uid ? f.receiverId : f.requesterId)
                .collect(Collectors.toSet());
        s.friends = friendIds.stream().map(db::user).filter(Objects::nonNull).collect(Collectors.toList());
        s.incoming = db.friendships.stream()
                .filter(f -> f.receiverId == uid && "PENDING".equals(f.status))
                .map(f -> db.user(f.requesterId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        s.discoverable = db.users.stream()
                .filter(u -> u.id != uid && !friendIds.contains(u.id) && db.friendships.stream().noneMatch(f -> f.status.equals("PENDING") && ((f.requesterId == uid && f.receiverId == u.id) || (f.requesterId == u.id && f.receiverId == uid))))
                .collect(Collectors.toList());
        s.myItems = db.items.stream().filter(i -> i.ownerId == uid).collect(Collectors.toList());
        s.friendItems = db.items.stream().filter(i -> friendIds.contains(i.ownerId)).collect(Collectors.toList());
        s.notifications = db.notifications.stream()
                .filter(n -> n.userId == uid)
                .sorted((a, b) -> b.createdAt.compareTo(a.createdAt))
                .collect(Collectors.toList());
        return Response.ok("", s);
    }

    private Response friend(Request r, boolean add) {
        int uid = (int) r.data.get("userId"), other = (int) r.data.get("otherId");
        if (add) {
            if (uid == other) return Response.fail("You cannot add yourself.");
            db.friendships.add(new Friendship(uid, other, "PENDING"));
            db.notifications.add(new Notification(db.id(), other, db.user(uid).name + " sent you a friend request."));
        } else {
            db.friendships.removeIf(f -> (f.requesterId == uid && f.receiverId == other) || (f.requesterId == other && f.receiverId == uid));
        }
        db.save();
        return Response.ok(add ? "Friend request sent." : "Friend removed.", null);
    }

    private Response accept(Request r, boolean yes) {
        int uid = (int) r.data.get("userId"), other = (int) r.data.get("otherId");
        Friendship f = db.friendships.stream()
                .filter(x -> x.receiverId == uid && x.requesterId == other && x.status.equals("PENDING"))
                .findFirst()
                .orElse(null);
        if (f == null) return Response.fail("Request not found.");
        if (yes) {
            f.status = "ACCEPTED";
            db.notifications.add(new Notification(db.id(), other, db.user(uid).name + " accepted your friend request."));
        } else {
            db.friendships.remove(f);
        }
        db.save();
        return Response.ok(yes ? "Friend request accepted." : "Request declined.", null);
    }

    private Response addItem(Request r) {
        WishItem i = new WishItem(db.id(), (int) r.data.get("userId"), (String) r.data.get("title"), (String) r.data.get("category"), (String) r.data.get("description"), (double) r.data.get("price"));
        if (i.title.isBlank() || i.price <= 0) return Response.fail("Enter a title and a positive price.");
        db.items.add(i);
        db.save();
        return Response.ok("Wish added to your list.", null);
    }

    private Response updateItem(Request r) {
        WishItem i = db.item((int) r.data.get("itemId"));
        if (i == null) return Response.fail("Item not found.");
        i.title = (String) r.data.get("title");
        i.category = (String) r.data.get("category");
        i.description = (String) r.data.get("description");
        i.price = (double) r.data.get("price");
        db.save();
        return Response.ok("Wish updated.", null);
    }

    private Response deleteItem(Request r) {
        db.items.removeIf(i -> i.id == (int) r.data.get("itemId"));
        db.save();
        return Response.ok("Wish removed.", null);
    }

    private Response contribute(Request r) {
        int uid = (int) r.data.get("userId"), itemId = (int) r.data.get("itemId");
        double amount = (double) r.data.get("amount");
        WishItem i = db.item(itemId);
        if (i == null || amount <= 0 || amount > i.remaining() + .001) return Response.fail("Contribution must be within the remaining amount.");
        i.contributed += amount;
        db.contributions.add(new Contribution(db.id(), itemId, uid, amount));
        User owner = db.user(i.ownerId);
        db.notifications.add(new Notification(db.id(), owner.id, db.user(uid).name + " contributed $" + String.format("%.2f", amount) + " toward your " + i.title + "."));
        if (i.remaining() <= .001) {
            i.purchased = true;
            // Requirement 8: [As Buyer] Receive notification on gift completion for all buyers
            Set<Integer> buyerIds = db.contributions.stream()
                    .filter(c -> c.itemId == itemId)
                    .map(c -> c.buyerId)
                    .collect(Collectors.toSet());
            for (int bId : buyerIds) {
                db.notifications.add(new Notification(db.id(), bId, "The gift item \"" + i.title + "\" you contributed to is now fully funded — what a lovely surprise!"));
            }

            // Requirement 9: [As Receiver] Receive notification naming specific friend(s) who contributed
            Set<String> contributorNames = db.contributions.stream()
                    .filter(c -> c.itemId == itemId)
                    .map(c -> db.user(c.buyerId))
                    .filter(Objects::nonNull)
                    .map(u -> u.name)
                    .collect(Collectors.toSet());
            String friendsText = String.join(", ", contributorNames);
            db.notifications.add(new Notification(db.id(), owner.id, "Your wish \"" + i.title + "\" has been fully funded by " + (friendsText.isEmpty() ? "your friends" : friendsText) + "!"));
        }
        db.save();
        return Response.ok("Contribution added. You made someone happy!", null);
    }

    private Response readNotifications(Request r) {
        int uid = (int) r.data.get("userId");
        db.notifications.stream().filter(n -> n.userId == uid).forEach(n -> n.read = true);
        db.save();
        return Response.ok("", null);
    }

    private Response updateProfile(Request r) {
        int uid = (int) r.data.get("userId");
        String name = (String) r.data.get("name"), email = (String) r.data.get("email"), pass = (String) r.data.get("password");
        if (name == null || name.isBlank() || email == null || email.isBlank() || !email.contains("@")) {
            return Response.fail("Enter a valid name and email address.");
        }
        User u = db.user(uid);
        if (u == null) return Response.fail("User not found.");
        User other = db.byEmail(email);
        if (other != null && other.id != uid) return Response.fail("This email address is already in use.");
        u.name = name.trim();
        u.email = email.trim();
        if (pass != null && !pass.isBlank()) {
            if (pass.length() < 4) return Response.fail("Password must be at least 4 characters.");
            u.password = Database.hashPassword(pass);
        }
        db.save();
        return Response.ok("Profile updated successfully!", u);
    }

    public static void main(String[] args) throws Exception {
        new IWishServer().start();
    }
}
