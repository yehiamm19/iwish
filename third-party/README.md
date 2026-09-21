# Third-party libraries

This project uses **OpenJFX (JavaFX) 21.0.5** for the desktop GUI. The project is Maven-based, so IntelliJ IDEA and NetBeans can resolve the dependencies automatically from `pom.xml`.

The resolved Linux runtime JARs and MySQL JDBC driver are also included in `third-party/lib/` for delivery and offline inspection. Maven remains the recommended cross-platform way to resolve the correct JavaFX platform binaries.

| Library | Maven coordinate | Purpose |
|---|---|---|
| JavaFX Controls | `org.openjfx:javafx-controls:21.0.5` | Buttons, forms, dialogs, controls |
| JavaFX Graphics | `org.openjfx:javafx-graphics:21.0.5` | Stage, Scene, layouts, rendering |
| JavaFX Base | `org.openjfx:javafx-base:21.0.5` | JavaFX runtime and collections |
| MySQL Connector/J | `com.mysql:mysql-connector-j:9.0.0` | JDBC connection and SQL queries to MySQL |

The server protocol and client communication use the Java 21 standard library; JavaFX and MySQL Connector/J are the only third-party dependencies.

## Resolve dependencies

From the project root:

```bash
mvn clean package
```

Maven downloads the correct JavaFX artifacts for the current platform. The exact dependency declarations are in the root `pom.xml`.
