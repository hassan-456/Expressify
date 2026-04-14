# Expressify Spring Boot – Folder Structure

```
expressify-spring/
├── pom.xml
├── README.md
├── FOLDER_STRUCTURE.md
│
└── src/
    ├── main/
    │   ├── java/com/expressify/
    │   │   ├── ExpressifyApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java      # Form login, logout, CSRF
    │   │   │   └── WebMvcConfig.java         # Static uploads mapping
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── AuthController.java      # GET /auth, POST /auth/register
    │   │   │   ├── HomeController.java      # /, /home
    │   │   │   ├── PostController.java      # /posts (create post page + POST)
    │   │   │   ├── ProfileController.java  # /profile, /profile?id=
    │   │   │   ├── ExploreController.java  # /explore
    │   │   │   ├── FriendsController.java  # /friends
    │   │   │   ├── NotificationsController.java  # /notifications
    │   │   │   ├── SettingsController.java      # /settings + POSTs
    │   │   │   ├── ApiController.java       # REST: likes, comments, delete_post
    │   │   │   └── FriendApiController.java # REST: friend_request
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── PostDto.java
    │   │   │   └── CommentDto.java
    │   │   │
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   ├── Admin.java
    │   │   │   ├── Media.java
    │   │   │   ├── Post.java
    │   │   │   ├── Comment.java
    │   │   │   ├── Like.java
    │   │   │   ├── CommentLike.java
    │   │   │   ├── FriendRequest.java
    │   │   │   ├── Notification.java
    │   │   │   ├── Settings.java
    │   │   │   ├── PostHashtag.java
    │   │   │   ├── Report.java
    │   │   │   └── (optional: AdminLog, AdminSetting, ReportedContent, etc.)
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── PostRepository.java
    │   │   │   ├── CommentRepository.java
    │   │   │   ├── LikeRepository.java
    │   │   │   ├── FriendRequestRepository.java
    │   │   │   ├── NotificationRepository.java
    │   │   │   ├── MediaRepository.java
    │   │   │   ├── SettingsRepository.java
    │   │   │   ├── AdminRepository.java
    │   │   │   └── ReportRepository.java
    │   │   │
    │   │   └── service/
    │   │       ├── CustomUserDetailsService.java  # Spring Security UserDetailsService
    │   │       ├── UserService.java
    │   │       ├── PostService.java
    │   │       ├── LikeService.java
    │   │       ├── CommentService.java
    │   │       ├── NotificationService.java
    │   │       └── FriendService.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       │
    │       ├── static/
    │       │   ├── css/
    │       │   │   └── style.css
    │       │   ├── js/
    │       │   │   └── app.js
    │       │   └── assets/
    │       │       └── logo.png
    │       │
    │       └── templates/
    │           ├── index.html
    │           ├── auth.html
    │           ├── home.html
    │           ├── posts.html
    │           ├── profile.html
    │           ├── explore.html
    │           ├── friends.html
    │           ├── notifications.html
    │           ├── settings.html
    │           └── fragments/
    │               └── layout.html
    │
    └── test/
        └── java/com/expressify/
            └── (optional tests)
```

## Configuration summary

| File | Purpose |
|------|--------|
| `application.properties` | Server port 8080, MySQL `expressify_db`, JPA `ddl-auto=validate`, Thymeleaf, file upload 10MB |
| `SecurityConfig` | Permit /, /auth, /css, /js, /assets, /uploads; form login `/auth/login`, logout `/logout`; `CustomUserDetailsService` + BCrypt |
| `WebMvcConfig` | `/uploads/**` → `file:uploads/` for profile/post media |

## URL mapping

| URL | Controller | Description |
|-----|------------|-------------|
| `/` | HomeController | Landing |
| `/auth` | AuthController | Login/register page |
| `/auth/login` | Spring Security | Login POST |
| `/auth/register` | AuthController | Register POST |
| `/home` | HomeController | Feed (posts, like, comment, delete) |
| `/posts` | PostController | Create post page + POST |
| `/profile` | ProfileController | Own or other profile |
| `/explore` | ExploreController | Random posts |
| `/friends` | FriendsController | Users + friend actions |
| `/notifications` | NotificationsController | List + mark read |
| `/settings` | SettingsController | Account, password, privacy, theme, profile picture |
| `/api/likes` | ApiController | GET like/unlike |
| `/api/get_comments` | ApiController | GET comments for post |
| `/api/comments` | ApiController | POST add/delete comment |
| `/api/delete_post` | ApiController | POST delete post |
| `/api/friend_request` | FriendApiController | POST send/accept/cancel |

All existing Expressify features and the same UI (style.css, Thymeleaf structure) are preserved.
