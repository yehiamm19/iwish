# I-Wish

## A JavaFX Desktop Application for Shared Wishes and Meaningful Gifts

I-Wish is a Java desktop application that helps friends turn personal wishes into shared moments. A user can create a wish list, connect with friends, discover their friends' wishes, and contribute a specific amount toward an item. When an item reaches its full price, the system records the completion and notifies the people involved.

The project is implemented as a **client-server application**. The client provides a JavaFX desktop interface with a dedicated CSS theme. The server manages socket connections, validates requests, applies the business rules, and persists data in MySQL through JDBC. The repository also contains the NetBeans project files, IntelliJ/Maven configuration, database schema and backup scripts, packaged third-party libraries, and a runnable demonstration path.

> **Primary desktop client:** JavaFX 21 with an external CSS theme. The legacy Swing classes remain in the source tree for compatibility, but `iwish.client.Main` launches the JavaFX application.

---

## Table of Contents

- [Project Goals](#project-goals)
- [Core Features](#core-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [Requirements](#requirements)
- [Database Setup with XAMPP](#database-setup-with-xampp)
- [Run the Project in NetBeans](#run-the-project-in-netbeans)
- [Run the Project in IntelliJ IDEA](#run-the-project-in-intellij-idea)
- [Command-Line Execution](#command-line-execution)
- [Demo Accounts](#demo-accounts)
- [Suggested Demonstration Flow](#suggested-demonstration-flow)
- [Database Design](#database-design)
- [Client-Server Request Flow](#client-server-request-flow)
- [Course Requirements Traceability](#course-requirements-traceability)
- [Team Contributions and Study Map](#team-contributions-and-study-map)
- [Screenshots](#screenshots)
- [Demo Video](#demo-video)
- [Delivery Package](#delivery-package)
- [Troubleshooting](#troubleshooting)
- [Security and Scope Notes](#security-and-scope-notes)
- [References](#references)

---

## Project Goals

The application was designed around four goals:

1. Make it easy for users to maintain a personal wish list.
2. Make friendships and friend requests simple to manage.
3. Allow several friends to contribute toward the same gift item.
4. Give the receiver and contributors clear feedback through notifications and funding progress.

The result is a focused desktop experience rather than a generic CRUD screen. The interface presents wishes as visual cards, displays funding progress, separates personal wishes from friends' wishes, and provides confirmation dialogs for sensitive actions such as deleting a wish or removing a friend.

---

## Core Features

### Account and profile management

- Register a new account with validation.
- Sign in using email and password.
- Store passwords as SHA-256 hashes in MySQL.
- Edit the signed-in user's profile information.
- Automatically upgrade legacy plaintext demo passwords to SHA-256 when they are loaded.

### Friend management

- Discover users who are not already friends.
- Send friend requests.
- Accept or decline incoming requests.
- Remove an existing friend after confirmation.
- Receive notifications for relevant friendship activity.

### Wish-list management

- Create a wish with a title, category, description, and target price.
- Edit an existing wish.
- Delete a wish with confirmation.
- Protect the funded amount when editing an item so the financial state remains valid.
- View personal wishes with funding progress and funded status.

### Shared contributions

- Browse friends' wish items.
- Contribute a custom amount toward a friend's wish.
- Use quick contribution amounts where provided by the interface.
- Reject invalid contributions that exceed the remaining balance.
- Mark the item as fully funded when the target price is reached.
- Record each contribution with its buyer, item, amount, and timestamp.

### Notifications and live updates

- Notify the receiver when a friend contributes toward a wish.
- Notify contributors when an item they helped fund reaches completion.
- Notify the receiver when the item becomes fully funded.
- Display unread notification counts.
- Refresh the dashboard automatically so changes made by another client can appear without restarting the application.

### User experience

- JavaFX interface with a consistent navy, pink, and neutral color system.
- External CSS styling for buttons, cards, forms, dialogs, progress bars, and navigation.
- Search and category filtering where available in the dashboard.
- Empty states that guide the user instead of showing blank screens.
- Confirmation dialogs for destructive actions.
- Logo assets included in the repository.

---

## Architecture

I-Wish follows a simple three-layer client-server design:

```mermaid
flowchart LR
    U[User] --> UI[JavaFX Client]
    UI -->|Serialized Request| API[ApiClient]
    API -->|TCP Socket :5050| S[I-Wish Server]
    S --> D[Business Rules and Request Dispatch]
    D -->|JDBC Connection and Prepared Statements| DB[(MySQL / XAMPP)]
    DB --> D
    D -->|Serialized Response| API
    API --> UI
```

### Client layer

The client is responsible for presentation and user interaction. It creates `Request` objects, sends them through `ApiClient`, receives `Response` objects, and renders the result in JavaFX. It does not connect directly to MySQL.

Important client classes include:

- `iwish.client.Main`: JavaFX entry point.
- `iwish.client.JavaFxApp`: JavaFX screens, navigation, dialogs, refresh behavior, and user flows.
- `iwish.client.ApiClient`: socket communication with the server.
- `iwish.client.LoginFrame` and `iwish.client.Dashboard`: retained Swing compatibility classes; they are not the primary entry point.

### Server layer

The server owns the application rules and the database boundary. It listens on port `5050`, creates a dedicated handler thread for each client socket, dispatches supported actions, and persists changes through the repository.

Important server classes include:

- `iwish.server.ServerMain`: server entry point.
- `iwish.server.IWishServer`: socket lifecycle, client handling, request dispatch, validation, contributions, and notifications.
- `iwish.server.Database`: MySQL JDBC repository and transaction-backed persistence.

### Shared layer

The `iwish.common` package contains serializable models and the request/response protocol shared by both sides:

- `User`
- `WishItem`
- `Friendship`
- `Contribution`
- `Notification`
- `Snapshot`
- `Request`
- `Response`
- `Protocol`

---

## Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Desktop GUI | JavaFX 21.0.5 |
| Styling | External JavaFX CSS |
| Networking | Java TCP sockets and object serialization |
| Database | MySQL 8+ or XAMPP MySQL |
| Database access | JDBC with MySQL Connector/J 9.0.0 |
| Build tools | Maven and Apache Ant |
| Supported IDEs | Apache NetBeans and IntelliJ IDEA |
| Assets | PNG logo and JavaFX CSS resources |

---

## Repository Structure

```text
IWish/
├── database/
│   ├── iwish-backup.sql          # Full restore script for phpMyAdmin/XAMPP
│   ├── iwish_dump.sql            # mysqldump export from the demonstration database
│   ├── iwish-schema.sql          # Database DDL
│   ├── iwish-seed.sql            # Demo data and sample accounts
│   └── setup-mysql.sql           # Optional database creation helper
├── docs/
│   ├── database-schema.md
│   ├── delivery-checklist.md
│   └── requirements-traceability.md
├── img/
│   └── logo.png
├── lib/                          # NetBeans-ready Windows runtime libraries
├── nbproject/                    # NetBeans project metadata
├── src/
│   ├── iwish/client/             # JavaFX client and socket API
│   ├── iwish/common/             # Shared models and protocol
│   ├── iwish/server/             # Server and MySQL repository
│   ├── main/resources/           # Maven resources, CSS, and image assets
│   └── iwish.css                 # NetBeans-compatible CSS copy
├── third-party/                  # Dependency notes and additional platform JARs
├── build.xml                     # NetBeans/Ant build file
├── pom.xml                       # Maven build file
└── README.md
```

---

## Requirements

Install the following before running the application:

- **JDK 21**.
- **XAMPP with MySQL** or another compatible MySQL 8+ server.
- **Apache NetBeans** or **IntelliJ IDEA**.
- Maven if you want to use the Maven commands. NetBeans can also use the included Ant project.

The repository includes the required JavaFX and MySQL Connector/J JARs. Maven remains the preferred cross-platform dependency resolver, while the `lib/` directory is prepared for the Windows NetBeans setup included with this delivery.

---

## Database Setup with XAMPP

The application expects a database named `iwish`. The default connection values in `Database.java` are:

```text
JDBC URL: jdbc:mysql://localhost:3306/iwish
User: root
Password: empty
```

These defaults match the usual XAMPP MySQL installation. If your XAMPP installation uses another username, password, host, or port, set the following environment variables before launching the server:

```text
IWISH_DB_URL=jdbc:mysql://localhost:3306/iwish?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8
IWISH_DB_USER=root
IWISH_DB_PASSWORD=
```

### Recommended phpMyAdmin procedure

1. Open the XAMPP Control Panel.
2. Click **Start** next to MySQL.
3. Open `http://localhost/phpmyadmin`.
4. Select the **Import** tab.
5. Choose `database/iwish-backup.sql`.
6. Click **Go**.
7. Confirm that the `iwish` database and its tables were created.

The full backup recreates the application tables and inserts demonstration data. It contains `DROP TABLE` statements, so use it as a clean restore script rather than importing it into a production database that contains data you want to preserve.

The server also calls `CREATE TABLE IF NOT EXISTS` during startup. Importing the backup first is recommended because it gives the demonstration a complete, known starting state.

### Alternative SQL files

- Use `database/iwish-schema.sql` when you only need the table definitions.
- Use `database/iwish-seed.sql` after the schema when you need sample users and wishes.
- Use `database/iwish_dump.sql` when you need the exported demonstration state.
- Use `database/setup-mysql.sql` when you only need to create the database before importing the schema.

---

## Run the Project in NetBeans

The repository includes a complete NetBeans project with `nbproject/`, `build.xml`, source roots, JavaFX libraries, and MySQL Connector/J references.

### Open the project

1. Start Apache NetBeans.
2. Select **File > Open Project**.
3. Choose the root `IWish` folder.
4. Confirm that NetBeans recognizes the project and that `iwish.client.Main` is the main class.
5. Make sure the project is using JDK 21.

### Start the server

Run:

```text
iwish.server.ServerMain
```

In NetBeans, you can right-click `src/iwish/server/ServerMain.java` and select **Run File**, or run the Ant target named `server`.

Expected output:

```text
I-Wish Server started on port 5050
```

### Start the client

Keep the server running, then run:

```text
iwish.client.Main
```

In NetBeans, right-click `src/iwish/client/Main.java` and select **Run File**, or run the Ant target named `client`.

The JavaFX login window should open. The client must be started after the server because the client connects to `localhost:5050` during startup.

### Ant commands

From the project root:

```bash
ant compile
ant jar
ant server
ant client
```

`server` and `client` are long-running targets. Run them in separate terminals if you use the command line.

---

## Run the Project in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **Open** and choose the `IWish` directory.
3. Open `pom.xml` as a Maven project.
4. Select JDK 21 as the Project SDK.
5. Reload the Maven project.
6. Start the server using `iwish.server.ServerMain` or the saved **I-Wish Server** run configuration.
7. Start the client using `iwish.client.Main` or the saved **I-Wish Client** run configuration.

Maven commands:

```bash
mvn clean package
mvn exec:java -Dexec.mainClass=iwish.server.ServerMain
mvn javafx:run
```

The default JavaFX Maven configuration launches the client. The server should remain running in a separate process or IntelliJ run configuration.

---

## Command-Line Execution

### Maven

```bash
mvn clean compile
```

Start the server:

```bash
mvn exec:java -Dexec.mainClass=iwish.server.ServerMain
```

In a second terminal, start the JavaFX client:

```bash
mvn javafx:run
```

### Packaged JAR

After building with Ant:

```bash
ant jar
java -cp dist/IWish.jar:lib/mysql-connector-j-9.0.0.jar iwish.server.ServerMain
```

For the most reliable JavaFX launch, use `mvn javafx:run` or the IDE run configuration because JavaFX requires platform-specific runtime modules.

---

## Demo Accounts

The backup contains the following demo accounts:

| Name | Email | Password |
|---|---|---|
| Demo User | `demo@iwish.local` | `demo` |
| Mona Ali | `mona@iwish.local` | `demo` |
| Omar Hassan | `omar@iwish.local` | `demo` |

Passwords are stored as SHA-256 hashes. The application also upgrades legacy plaintext demo values when it loads them, so older database backups remain usable.

---

## Suggested Demonstration Flow

Use this sequence for a clear project demonstration:

1. Start XAMPP MySQL and import `database/iwish-backup.sql`.
2. Start `ServerMain` and show that the socket server is listening on port `5050`.
3. Start the JavaFX client and sign in as `demo@iwish.local`.
4. Open the Friends screen and show the existing connections and discovery list.
5. Open the wishes area and create a new wish with a title, category, description, and price.
6. Edit the wish to demonstrate update support, then leave it in the list.
7. Sign in with a second account in another client window.
8. Send and accept a friend request if the accounts are not already connected.
9. Open the friend's wishes and contribute a specific amount.
10. Complete the remaining balance from another client or contribution.
11. Open Notifications on the buyer and receiver accounts.
12. Show the updated `contributions`, `wish_items`, and `notifications` rows in phpMyAdmin to demonstrate persistence.

This flow covers authentication, friendship management, wish-list CRUD, friend wish discovery, contributions, notifications, client connections, and database persistence in one demonstration.

---

## Database Design

| Table | Responsibility |
|---|---|
| `users` | User identity, email, password hash, and creation timestamp. |
| `friendships` | Friend requests and accepted relationships between users. |
| `wish_items` | Personal wishes, target prices, contributed totals, and funded state. |
| `contributions` | Individual contribution records linked to an item and buyer. |
| `notifications` | User-specific messages, read state, and timestamps. |

The core relationships are:

- One user owns many wish items.
- A friendship connects two users and has a status.
- One wish item can have many contributions.
- One contribution belongs to one buyer and one wish item.
- One user can receive many notifications.

The database layer uses JDBC `Connection`, `Statement`, `PreparedStatement`, and `ResultSet` objects. Changes are written through transaction-safe persistence logic, and foreign keys use cascade rules where appropriate.

---

## Client-Server Request Flow

The client does not access MySQL directly. Every operation follows this pattern:

```text
User action
    -> JavaFX controller logic
    -> Request(action, data)
    -> ApiClient TCP connection
    -> IWishServer dispatch
    -> validation and business rule
    -> MySQL JDBC query or transaction
    -> Response(success, message, payload)
    -> JavaFX refresh
```

Supported server actions include:

```text
register
login
snapshot
addFriend
removeFriend
acceptFriend
declineFriend
addItem
updateItem
deleteItem
contribute
readNotifications
updateProfile
```

The server supports multiple clients by accepting sockets continuously and creating a handler thread for each connection.

---

## Course Requirements Traceability

| Requirement | Where it is implemented |
|---|---|
| Register and sign in | `JavaFxApp`, `Request`, `IWishServer.register`, `IWishServer.login` |
| Add and remove friends | Friends screen and `addFriend` / `removeFriend` actions |
| Accept and decline requests | Incoming request cards and `acceptFriend` / `declineFriend` actions |
| Create, update, and delete wishes | Wish dialogs and `addItem` / `updateItem` / `deleteItem` actions |
| View friends | Friends screen and the `Snapshot` model |
| View friends' wish lists | Friend wish dialog and overview wish cards |
| Contribute a specific amount | Contribution dialog, remaining-balance validation, and contribution persistence |
| Buyer completion notification | Server funding-completion logic and notifications list |
| Receiver notification | Server receiver notification with the contributing friend information |
| Friendly GUI | JavaFX scenes, CSS theme, cards, filters, progress visuals, dialogs, and empty states |
| Server start and stop | `IWishServer.start`, `IWishServer.stop`, and `ServerMain` |
| Database connection and queries | `Database` JDBC repository and MySQL Connector/J |
| Add items for wish lists | SQL seed/backup data and wish creation UI |
| Client connection handling | `ServerSocket` and per-client handler threads |
| Client request handling | Request dispatch switch in `IWishServer` |

---

## Team Contributions and Study Map

The project is divided among **six team members** according to real technical modules. This is not a general list of tasks: each member owns a clear part of the codebase and should be able to open the listed files, explain the design decisions, and demonstrate the related feature during the discussion. Replace the generic labels with the actual names before publishing the repository.

| ID | Team member | Technical ownership | Exact files to open | What the member should explain and demonstrate |
|---|---|---|---|---|
| 23100755 | Ahmed Sherif Mohamed | Server lifecycle and networking | `src/iwish/server/IWishServer.java`, `src/iwish/server/ServerMain.java`, `src/iwish/common/Protocol.java` | Explain `ServerSocket`, port `5050`, the accept loop, one handler thread per client, `start()` and `stop()`, socket closing, and why the client must start after the server. Demonstrate two clients connecting to the same server. |
| 23101609 | Yehia Mohamed Gaber | Request/response protocol and shared OOP models | `src/iwish/common/Request.java`, `Response.java`, `Models.java`, `src/iwish/client/ApiClient.java` | Explain serialization, how an action and its data move from the JavaFX client to the server, how a `Response` returns success or an error, and how `User`, `WishItem`, `Friendship`, `Contribution`, `Notification`, and `Snapshot` model the domain. Trace one complete `contribute` request. |
| 22100792 | Ali Khaled Mohamed | JDBC repository and in-memory persistence bridge | `src/iwish/server/Database.java` lines 15-340, `src/iwish/server/IWishServer.java` lines 61-83 and 254-340 | Explain how the repository opens JDBC connections, reloads database rows into domain lists, calculates IDs, uses prepared batches, disables foreign keys during the controlled rewrite, commits transactions, and exposes helper lookups such as `user`, `byEmail`, and `item`. Trace one save from a server operation to MySQL. |
| 23101547 | Ahmed Abd El-Aleem Ahmed Ahmed Ismail | Database schema, SQL delivery, ERD, and environment configuration | `database/iwish-schema.sql`, `database/iwish-backup.sql`, `database/iwish-seed.sql`, `database/iwish_dump.sql`, `database/setup-mysql.sql`, `docs/database-schema.md`, `pom.xml`, `nbproject/project.properties` | Explain the relational design, primary and foreign keys, cascade rules, indexes, SQL restore order, XAMPP/phpMyAdmin import, MySQL connection variables, JavaFX and Connector/J dependency configuration, and how the ERD maps to the Java classes. Demonstrate importing the backup and locating users, wishes, contributions, and notifications in phpMyAdmin. |
| 22100601 | Zeyad Ali Mohamed | JavaFX screens and visual system | `src/iwish/client/JavaFxApp.java`, `src/main/resources/iwish.css`, `src/iwish.css`, `src/main/resources/img/logo.png` | Explain `Application`, `Stage`, `Scene`, layouts, controls, dialogs, navigation, CSS classes, validation messages, progress visuals, search/filter behavior, and auto-refresh. Demonstrate sign-in, dashboard navigation, and the visual feedback after a contribution. |
| 23101598 | Mahmoud Khamies | Authentication, friendships, and profile workflows | `src/iwish/client/JavaFxApp.java` friendship/profile methods, `src/iwish/server/IWishServer.java` methods `register`, `login`, `friend`, `accept`, `updateProfile`, and `Database.hashPassword` | Explain registration validation, login verification, unique email handling, password hashing, friend discovery, pending versus accepted relationships, accept/decline/remove actions, and profile editing. Demonstrate the workflow using two accounts and identify the database rows that change. |
| 23101614 | Youssef Ramdan | Wish-list and contribution business rules | `src/iwish/server/IWishServer.java` methods `addItem`, `updateItem`, `deleteItem`, `contribute`, `src/iwish/client/JavaFxApp.java` wish and contribution methods, `src/iwish/common/Models.java` `WishItem.remaining()` | Explain wish creation, editing, deletion, positive-price validation, remaining-balance calculation, contribution limits, funded-state transitions, and the data shown in the wish cards. Demonstrate an item moving from partial funding to fully funded. |

### How to prepare as a team

Each member should first read the files in the **Exact files to open** column. Then the member should trace one request from the JavaFX button, through `ApiClient`, into `IWishServer`, through `Database`, and back to the screen. The strongest discussion is based on code ownership: each person explains the classes they own, the data they receive, the data they return, and the failure cases they handle.

Before uploading to GitHub, replace `Team Member 1` through `Team Member 6` with the real names. Keep the technical ownership and study map so the contribution record remains specific and verifiable.

### Recommended easiest discussion role

For a member who wants the smallest, most focused module to study, **Yehia Mohamed Gaber (ID: 23101609)** owns the request/response protocol and shared OOP models. This module is intentionally limited to four core areas: `Request.java`, `Response.java`, `Models.java`, and `ApiClient.java`. The preparation path is:

1. Explain that `Request` contains an action name and a map of input data.
2. Explain that `ApiClient` opens a TCP socket to `localhost:5050`, sends the request, and reads a serialized response.
3. Explain that `Response` returns `ok`, `message`, and an optional payload.
4. Explain the role of the model classes: a `User` owns `WishItem` objects, friendships connect users, contributions belong to buyers and wishes, notifications belong to users, and `Snapshot` gives the client one screen-ready data object.
5. Trace the simple `login` flow and the `contribute` flow from the button to `ApiClient`, then to the server, and back to the JavaFX screen.

The member should still understand the general architecture, but does not need to memorize the JavaFX layout implementation or the MySQL repository internals. This is the most compact technical ownership area in the six-person division.

---

## Screenshots

Below is a complete visual walkthrough of the I-Wish application, showcasing all key user journeys, real-time validations, dashboard analytics, social features, and database persistence.

### 1. User Sign In Screen
Clean authentication interface with official I-Wish branding, email input, password visibility toggle, and server-side validation.

![User Sign In](screenshots/1.png)

---

### 2. User Registration (Create Account)
Account creation screen allowing new members to register with full name, valid email address, and password.

![User Registration](screenshots/2.png)

---

### 3. Email Format Validation
Client-side validation preventing form submission if an invalid email format without `@` is provided.

![Email Validation](screenshots/3.png)

---

### 4. Overview Dashboard & Analytics
Main dashboard displaying summary statistics (My Wishes, Friends, New Updates) along with interactive Category Distribution (PieChart) and Funding vs Target Goal (BarChart).

![Overview Dashboard](screenshots/4.png)

---

### 5. Friends' Wishes Feed
Live feed on the dashboard displaying friends' active wishes, remaining funding amounts, progress bars, and quick contribution buttons.

![Friends Wishes Feed](screenshots/5.png)

---

### 6. Profile Management & Password Update
Profile dialog enabling users to update their name and email, or set a new password with visibility toggle.

![Profile Management](screenshots/6.png)

---

### 7. Personal Wish List Dashboard
Wish management center showing overall funding progress, total goals, search bar, category filters, and active wish cards.

![Personal Wish List](screenshots/7.png)

---

### 8. Add New Wish Modal
Modal dialog to create and publish a new wish with item title, category selection, target budget ($), and optional description.

![Add New Wish](screenshots/8.png)

---

### 9. Friends Circle Management
Friends tab showing your connected circle, incoming friend requests, and direct options to view friends' wishlists or remove connections.

![Friends Circle Management](screenshots/9.png)

---

### 10. Find Friends - Discovery State
Friend discovery dialog displaying the status when no new discoverable users are currently unlinked.

![Find Friends Discovery](screenshots/10.png)

---

### 11. Remove Friend Confirmation
Safety confirmation popup ensuring intentional action before disconnecting a friend from your circle.

![Remove Friend Confirmation](screenshots/11.png)

---

### 12. Connect with People (Send Friend Request)
Discovery modal listing platform users with a single-click action to send friend requests.

![Send Friend Request](screenshots/12.png)

---

### 13. Viewing a Friend's Wish List
Dedicated modal showcasing all active wishes belonging to a selected friend with goal progress and contribution actions.

![Viewing Friend Wish List](screenshots/13.png)

---

### 14. Make a Contribution Dialog
Contribution dialog featuring target progress breakdown, preset amount chips ($10, $25, $50, Full Amount), and custom amount entry.

![Make a Contribution](screenshots/14.png)

---

### 15. Edit Existing Wish
Modal dialog allowing users to update their wish title, category, target amount, or descriptive notes.

![Edit Existing Wish](screenshots/15.png)

---

### 16. Quick Contribution from Dashboard Feed
One-click contribution dialog launched directly from the main Overview feed for quick friend support.

![Quick Contribution from Dashboard](screenshots/16.png)

---

### 17. Contribution Success Notification
Success alert confirming that the contribution transaction was processed and recorded in the database.

![Contribution Success Notification](screenshots/17.png)

---

### 18. Contributor Notifications View
Notifications panel for the contributor displaying welcome messages and completion alerts when funded items reach 100%.

![Contributor Notifications View](screenshots/18.png)

---

### 19. Wish Owner Notifications & Activity Feed
Real-time notification inbox for the wish owner indicating received contributions, full item completions, and new friend requests.

![Wish Owner Notifications](screenshots/19.png)

---

### 20. Incoming Friend Requests Management
Incoming friend requests panel featuring direct "Accept" and "Decline" actions to manage pending connections.

![Incoming Friend Requests Management](screenshots/20.png)

---

### 21. MySQL Database Schema & Tables in phpMyAdmin
XAMPP phpMyAdmin view of the `iwish` relational database showing all 6 populated tables (`users`, `wish_items`, `contributions`, `friendships`, `notifications`, `available_items`).

![Database Schema in phpMyAdmin](screenshots/21.png)

---

## Demo Video

Watch the complete working demonstration of the I-Wish application:

<iframe frameBorder='0' width='640' height='360' webkitallowfullscreen mozallowfullscreen allowfullscreen src="https://www.awesomescreenshot.com/embed?id=56731161&shareKey=95c19397b890f85d7cecb8e692a05704&info=false"></iframe>

>  **Direct Link**: [Watch I-Wish Working Demo Video](https://www.awesomescreenshot.com/video/56731161?key=95c19397b890f85d7cecb8e692a05704)

The demo showcases the end-to-end user workflows:
1. XAMPP MySQL database service and the imported `iwish` database schema.
2. `ServerMain` startup and socket listener on port `5050`.
3. JavaFX client launch and secure user authentication.
4. Wish management (creation, editing, goal progress tracking).
5. Social features: sending friend requests and browsing friends' wish lists.
6. Multi-user contribution flow and milestone funding (100% completed).
7. Live notification delivery across both buyer and receiver accounts.
8. Database synchronization and verified record persistence in phpMyAdmin.

---

## Delivery Package

The repository includes all requested submission materials:

- A NetBeans project with `nbproject/`, `build.xml`, source roots, and local Windows libraries.
- A Maven project with JavaFX and MySQL dependencies in `pom.xml`.
- Database schema, seed data, full backup, and a `mysqldump` export.
- Third-party JavaFX and MySQL Connector/J libraries.
- JavaFX CSS and logo assets.
- Documentation for architecture, schema, requirements traceability, and delivery verification.
- A working demo path with sample accounts and a clear multi-client scenario.

---

## Troubleshooting

### The server says that it cannot connect to MySQL

Confirm that MySQL is running in XAMPP, that the `iwish` database exists, and that the username and password match the environment variables. The default is `root` with an empty password.

### The client cannot connect to port 5050

Start `iwish.server.ServerMain` first. The client opens a socket to `localhost:5050`; it cannot authenticate until the server is running.

### JavaFX classes cannot be found

Use JDK 21 and run the project through NetBeans with the included `lib/` directory or through Maven with `mvn javafx:run`. Do not mix JavaFX runtime JARs from different operating systems.

### The database contains old data

The full backup intentionally drops and recreates the application tables. Import `database/iwish-backup.sql` only when a clean demonstration state is required. Back up any data you want to keep first.

### Port 5050 is already in use

Stop the old server process before starting another one, or update `Protocol.PORT` in `src/iwish/common/Protocol.java` and use the same build for both client and server.

---

## Security and Scope Notes

This is an educational desktop application. Passwords are hashed with SHA-256 to satisfy the project's secure-storage requirement, but a production authentication system should use a salted password-hashing algorithm such as Argon2id, bcrypt, or scrypt. The socket protocol is intended for a local or trusted demonstration environment and does not provide production TLS, authorization tokens, payment processing, or real money transfer.

Contributions in I-Wish represent cooperative gift funding records inside the application. They are not financial transactions with a payment provider.
