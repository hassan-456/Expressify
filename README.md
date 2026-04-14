# Expressify - Spring Boot MVC

Spring Boot port of the Expressify PHP social platform. Same features and UI, built with:

- **Spring Boot 3.2**
- **Thymeleaf** (templates)
- **MySQL** + **JPA/Hibernate**
- **Spring Security** (session-based login)
- **MVC** architecture

## Requirements

- Java 17+
- Maven 3.6+
- MySQL 8 (existing `expressify_db` from PHP project)

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/expressify_db?...
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Use the same database as the PHP app to keep existing users and data.

## Run

```bash
mvn spring-boot:run
```

Open: http://localhost:8080

## Project structure

```
expressify-spring/
├── pom.xml
├── src/main/java/com/expressify/
│   ├── ExpressifyApplication.java
│   ├── config/          # Security, WebMvc
│   ├── controller/      # MVC + REST API
│   ├── dto/
│   ├── entity/         # JPA entities
│   ├── repository/      # JPA repositories
│   └── service/
├── src/main/resources/
│   ├── application.properties
│   ├── static/          # CSS, JS, assets
│   │   ├── css/style.css
│   │   ├── js/app.js
│   │   └── assets/
│   └── templates/       # Thymeleaf
│       ├── index.html
│       ├── auth.html
│       ├── home.html
│       ├── posts.html
│       ├── profile.html
│       ├── explore.html
│       ├── friends.html
│       ├── notifications.html
│       ├── settings.html
│       └── fragments/
└── README.md
```

## Features (same as PHP)

- Landing, login, register
- Home feed (posts, like, comment, delete own post)
- Create post (text + optional image/video)
- Profile (own and others), friend count
- Explore (random posts)
- Friends (add, accept, cancel, reject)
- Notifications (like, comment, friend request)
- Settings (account, password, privacy, theme, profile picture)
- Themes: light, dark, unite
- REST API for likes, comments, delete post, friend request (used by frontend JS)

## Default avatar

If you see a broken image for default avatar, add an image at `src/main/resources/static/assets/default-avatar.png` or keep the PHP `assets/default-avatar.png` and reference it (e.g. copy into Spring `static/assets/`).
