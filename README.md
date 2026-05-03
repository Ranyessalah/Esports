# ⚡ ClutchX — Esports Management Platform

<p align="center">
  <img src="https://img.shields.io/badge/JavaFX-17+-blue?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/MySQL-Database-orange?style=for-the-badge&logo=mysql" />
  <img src="https://img.shields.io/badge/iTextPDF-Export-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" />
</p>

> **ClutchX** is a full-featured desktop esports management platform built with JavaFX. It provides a complete backoffice for administrators and a rich client interface for coaches and players — covering everything from team management and live match tracking to AI-powered predictions and tournament fixtures.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Modules](#modules)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Team](#team)

---

## 🎯 Overview

ClutchX is designed for esports organizations that need a centralized platform to manage their operations. It supports three user roles — **Admin**, **Coach**, and **Player** — each with a dedicated interface and access level.

The platform features a dark gaming-themed UI with a purple/violet accent system, real-time match tracking with live chronometers, AI prediction engine, chatbot-powered statistics assistant, and QR code team sharing.

---

## 📦 Modules

### 1. 👥 User Management
Handles the full user lifecycle across all roles.

- Role-based access control: **Admin**, **Coach**, **Player**
- User registration and login
- **Google OAuth Sign-Up** — one-click registration via Google account
- **Face ID Authentication** — biometric login for enhanced security
- **Forgot Password** — email-based password reset flow
- Admin backoffice: search, filter, sort users by role/status
- Ban / unblock users with confirmation dialog
- Profile editing with avatar support
- Admin profile self-management

---

### 2. 🛡 Team (Équipe) Management
Full CRUD for esports teams with rich detail views.

- Create, edit, delete teams with logo upload
- Assign coach and players to a team
- Team detail page showing full roster (coach card + player cards)
- Circular avatar rendering for all members
- **QR Code generation** per team — scan to view team page on mobile
- Local web server (`TeamWebServer`) serving team data at `http://[local-ip]:8081/team?id=X`
- QR code download as PNG
- Copy team URL to clipboard
- Client-side team browsing with search

---

### 3. 🎯 Match Management
Live match tracking with real-time updates and notifications.

- Full CRUD for matches (create, edit, delete)
- Match statuses: `À jouer`, `En cours`, `Terminé`, `Annulé`
- **Live chronometer** — counts up from match start time, auto-caps at 90 minutes
- **Auto-refresh** every 5 seconds to detect score/status changes
- **Goal alerts** — animated "⚽ BUT MARQUÉ !" badge for 2 minutes after a goal
- **Push notifications** — toast popups + sound alerts for matches starting or within 15 minutes
- Notification center dialog with live match cards
- Score stepper in the admin form (increment/decrement per team)
- Match detail page with full info and formatted dates
- **Google Calendar export** — FullCalendar web view with all matches, click any event to add to Google Calendar
- **PDF export** of match statistics with iTextPDF (zebra-striped table, metric cards, year range)

---

### 4. 📊 Statistics & Classement
Advanced standings dashboard with AI chatbot assistant.

- Live league table with rank, team, game, W/D/L, BP, BC, diff, points
- Filter by game, search by team name, sort by multiple criteria
- **Charts dashboard**:
  - Bar chart — Points per team (Top 10)
  - Pie chart — Global W/D/L distribution
  - Pie chart — Teams per game
  - Bar chart — Attack (BP) vs Defense (BC) comparison
  - Bar chart — Matches per year (configurable year range)
- **AI Chatbot** — natural language stats assistant (ask "top 5", "meilleure attaque", "compare ESS vs Espérance", etc.)
- Suggestion chips for quick queries
- Typing indicator with animated dots
- PDF export of the full standings table
- **AI Match Predictor** — select any two teams to get win probability percentages with animated progress bar

---

### 5. 🏆 Tournament Management
Organize and track esports tournaments.

- Tournament creation and management
- Bracket and group stage support
- Link matches to tournament rounds
- Tournament standings and progression tracking

---

### 6. 📅 Fixture Management
Schedule and display upcoming match fixtures.

- Fixture calendar view
- Filter fixtures by team, game, or date range
- Integration with match statuses for live fixture updates
- **Google Calendar sync** — export fixtures directly to Google Calendar

---

### 7. 🎓 Coaching Management
Dedicated module for coach-player relationships and session tracking.

- Coach profile management (speciality, experience, team assignment)
- Session scheduling between coach and players
- Coaching history and notes
- Coach dashboard with assigned team overview

---

### 8. 📚 Course Management
Learning and training content for players and coaches.

- Course creation with title, description, and content
- Assign courses to players or teams
- Track completion status
- Coach-created curriculum management

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🔐 Face ID Login | Biometric authentication using facial recognition |
| 🔑 Google Sign-Up | OAuth-based registration with Google account |
| 🔁 Forgot Password | Secure email-based password reset |
| 🤖 AI Prediction | Win probability engine for any two teams |
| 💬 Stats Chatbot | Natural language assistant for standings queries |
| 📱 QR Code Sharing | Generate and scan QR codes to view team pages on mobile |
| 📊 PDF Export | Download standings and stats as formatted PDF |
| 📅 Google Calendar | Export matches/fixtures to Google Calendar |
| ⏱ Live Chronometer | Real-time match timer visible to all users |
| 🔔 Live Notifications | Toast alerts + sound for match start events |
| 🎯 Goal Alerts | Animated alert badges when a team scores |
| 🌐 Local Web Server | Embedded HTTP server for QR code team pages |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| UI Framework | JavaFX 17+ with FXML |
| Styling | Custom CSS (dark gaming theme, violet accent) |
| Database | MySQL via JDBC |
| PDF Generation | iTextPDF |
| QR Code | ZXing (Zebra Crossing) |
| Web Server | Embedded Java HTTP server |
| Charts | JavaFX built-in Chart API |
| Calendar | FullCalendar.js (via JavaFX WebView) |
| AI / ML | Custom `PredictionService` + `ChatbotStatsService` |
| Auth | Google OAuth, Face Recognition API |
| Build | Maven |

---

## 🏗 Architecture

ClutchX follows a layered MVC architecture:

```
src/
├── controllers/
│   ├── AdminDashboardController.java     ← Admin layout controller
│   ├── MainLayoutController.java         ← Client layout controller
│   └── matchManagement/
│       ├── MatchController.java
│       ├── MatchClientController.java
│       ├── EquipeDetailsController.java
│       ├── ClientEquipeController.java
│       ├── StatsController.java
│       └── Statsclientcontroller.java
│   └── userManagement/
│       ├── AdminUsersController.java
│       ├── CoachProfileController.java
│       └── PlayerProfileController.java
│
├── entities/
│   ├── matchManagement/   (Equipe, Matchs, StatsRow, ...)
│   └── userManagement/    (User, Coach, Player, Roles)
│
├── services/
│   ├── matchManagement/   (EquipeService, MatchService, StatsService, PredictionService, ChatbotStatsService)
│   └── userManagement/    (UserService, PlayerService)
│
├── utils/
│   ├── matchManagement/   (QRCodeUtil, TeamWebServer, MatchNotificationService)
│   └── PreferencesRepository.java
│
└── resources/
    ├── matchManagement/   (FXML views)
    ├── userManagement/    (FXML views)
    └── *.css              (Stylesheets)
```

### Navigation Pattern

Both the **client** (`MainLayoutController`) and **admin** (`AdminDashboardController`) use a single-window content-swapping pattern:

- A persistent sidebar + topbar wraps a central `StackPane contentArea`
- Navigation calls `loadContent(fxmlPath)` or `loadNode(node)` to swap views in-place
- No new `Stage` or `Scene` is created on navigation — the window never flashes
- Back navigation uses a `Runnable onBack` passed from the caller, so each detail view knows exactly where to return (equipe list vs stats table)

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- JavaFX SDK 17+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/clutchx.git
   cd clutchx
   ```

2. **Configure the database**
   ```bash
   # Import the schema
   mysql -u root -p < schema.sql
   ```

3. **Update DB credentials** in `src/main/resources/config.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/clutchx
   db.user=root
   db.password=yourpassword
   ```

4. **Build and run**
   ```bash
   mvn clean javafx:run
   ```

### Default Admin Account

```
Email:    admin@clutchx.com
Password: admin123
```

---

## 👨‍💻 Team

Built as part of a Java desktop application development project.

| Module | Developer |
|---|---|
| Match & Statistics Management | — |
| User Management & Auth | — |
| Team (Équipe) Management | — |
| Tournament & Fixture | — |
| Coaching & Courses | — |

---

## 📄 License

This project is developed for educational purposes.

---

<p align="center">
  Made with ⚡ by the ClutchX Team · 2026
</p>
