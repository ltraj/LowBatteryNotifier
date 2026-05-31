# 🔋 Low Battery Notifier

A lightweight Android battery alarm and analytics app designed to prevent unexpected phone shutdowns.

The idea for this app came from a real problem: my phone often switched off during the night because the hotspot was left on, causing me to miss important calls from family. Existing battery alarm apps were either too basic, filled with ads, or lacked useful battery insights.

Low Battery Notifier solves that problem by providing a loud customizable battery alarm along with battery analytics and shutdown predictions.

---

## ✨ Features

### Battery Alarm

- Custom low battery threshold (1%–50%)
- Loud alarm when battery reaches threshold
- Repeat alarm logic
- Automatic alarm stop when charger is connected
- Custom ringtone selection
- Persistent monitoring notification

### Battery Analytics

- Battery drain tracking
- Charging session tracking
- Average battery drain per hour
- Estimated shutdown time
- Confidence indicator for shutdown estimates
- Weighted drain-rate calculation
- Overnight battery drain analysis
- Recent charging history
- Battery usage insights

### Performance Focus

- Event-driven architecture
- No battery-draining polling loops
- No cloud services
- No user accounts
- No internet dependency
- All analytics stored locally

---

## 📊 Analytics Included

The app automatically records:

- Charging start percentage
- Charging end percentage
- Charging duration
- Battery drain samples
- Average drain rate
- Overnight battery drain patterns

Using this data, the app provides:

- Estimated time until shutdown
- Average battery drain/hour
- Charging statistics
- Battery usage insights

---

## 🏗 Architecture

### Battery Efficient Design

The app uses Android battery broadcasts instead of constantly checking battery status.

```text
Battery Event
      ↓
Store Sample
      ↓
Calculate Insights Only When Opened
```

This approach minimizes battery usage while still providing useful analytics.

### Storage

- SQLite database
- Local-only storage
- Automatic cleanup of old records

---

## 📱 Screenshots

### Main Screen

(Add screenshot here)

### Analytics Screen

(Add screenshot here)

### Settings Screen

(Add screenshot here)

### Alarm Notification

(Add screenshot here)

---

## 🚀 Installation

### Download APK

Download the latest APK from the Releases section.

### Build From Source

Clone the repository:

```bash
git clone https://github.com/ltraj/LowBatteryNotifier.git
```

Open the project in Android Studio and run:

```bash
Build → Build APK(s)
```

---

## 🛠 Tech Stack

- Kotlin
- Jetpack Compose
- Android SDK
- Material 3
- SQLite
- Android Broadcast Receivers

---

## 🔒 Privacy

This application:

✅ Stores data only on your device

✅ Does not send data to any server

✅ Does not require an account

✅ Does not use cloud storage

✅ Does not track users

---

## 🎯 Why I Built This

I built this app because my phone frequently switched off overnight due to battery drain while hotspot was enabled. This caused me to miss important calls from family members early in the morning.

Instead of relying on existing battery alarm apps, I decided to build my own solution and add useful battery analytics that help users better understand their battery usage patterns.


---

## 🤝 Contributing

Suggestions, bug reports, and pull requests are welcome.

If you find a bug or have an idea for improvement, open an issue.

---

## ⭐ Support

If you find this project useful, consider giving it a star.

---

## 📄 License

MIT License
