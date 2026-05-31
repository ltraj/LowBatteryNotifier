# 🔋 Low Battery Notifier

A lightweight, ad-free Android battery alarm and analytics app designed to prevent unexpected phone shutdowns.

Built out of frustration with missing important family calls when my phone died overnight, this app combines a reliable emergency alarm with local usage insights.

---

## ✨ Features

### 🚨 Smart Battery Alarm

* **Volume Override:** Forces full volume for the alarm even if your phone is set to low, silent, or mute modes.
* **Custom Thresholds:** Set alerts anywhere between 1%–50%.
* **Intelligent Behavior:** Loop/repeat logic until noticed, with automatic cutoff as soon as a charger is connected.
* **Customization:** Choose your own custom ringtone.
* **Persistent Monitoring:** Low-footprint ongoing notification ensures the system doesn't kill the background task.

### 📊 Battery Analytics

* **Shutdown Predictions:** Calculates estimated time until shutdown with a confidence indicator based on a weighted drain rate.
* **Drain Tracking:** Monitors hourly drain, overnight drain patterns, and active charging sessions.
* **Local History:** View recent charging logs and trends without a single byte leaving your device.

### 🛡️ Performance & Privacy Focus

* **Zero Polling Loops:** Event-driven architecture uses Android battery broadcasts to maximize efficiency.
* **100% Offline:** No internet permission, no cloud services, no trackers, and no user accounts required.
* **Local Storage:** Data is stored strictly in a local SQLite database with automatic cleanup.

---

## 🏗 Architecture

```text
Battery Broadcast Event ➔ Store Sample ➔ Calculate Insights (Only When App Opened)

```

By calculating insights on-demand rather than in the background, the app ensures it never becomes the very thing it fights against: a battery drainer.

---

## 📱 Screenshots
### Main Screen

<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/ac9c5b8c-6905-4859-8067-22fab3ae8284" />


### Analytics Screen

<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/0495a4d3-3412-4dce-97e6-14e28999cc2e" />


### Settings Screen

<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/2ff89787-ceee-4cfa-bd31-d7a2fbe634c6" />


### Alarm Notification
<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/43e98ba6-e1d1-4741-b47a-ce0d13eefdc1" />
---

## 🚀 Installation & Build

### Download APK

Grab the latest release directly from the **Releases** section.

### Build From Source

```bash
git clone https://github.com/ltraj/LowBatteryNotifier.git

```

Open the project in **Android Studio** and navigate to `Build` -> `Build APK(s)`.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Storage:** SQLite
* **Core Android:** Broadcast Receivers

---

## 🤝 Contributing & Support

Suggestions, bug reports, and pull requests are highly welcome. If you find this project saved your morning, consider giving it a **⭐ Star**!

---

## 📄 License

MIT License







