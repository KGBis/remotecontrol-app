# 📡 Remote PC Control

**Remote PC Control** is an Android application that lets you **discover, wake up and shut down computers** on your local network in a simple and reliable way.

The goal of the project is to provide a **lightweight, no-friction remote control tool** without background services, cloud dependencies or complex setup.

---

## ✨ Features

* 🔍 **Automatic device discovery (mDNS)**
* 🖥️ **Remote device information**
* ⚡ **Wake-on-LAN support**
* ⏻ **Remote shutdown**
* ⛔ **Cancel scheduled shutdown**
* 🧩 **Minimal desktop companion with system tray integration**

---

## 🧠 How it works

The app communicates directly with a small service running on the target computer.

### 📱 Android App

* Discovers devices via **mDNS**
* Connects to port `6800`
* Sends simple commands over the local network

### 💻 Desktop Companion

A lightweight application running on the target machine that:

* Listens on port **6800**
* Responds to commands
* Exposes basic system information
* Provides a **system tray icon** for quick access

**Download it from [https://github.com/KGBis/remotecontrol-tray/releases/latest](https://github.com/KGBis/remotecontrol-tray/releases/latest)**

---

## 📡 Supported Commands

| Command           | Description                                 |
|-------------------|---------------------------------------------|
| `INFO`            | Retrieve hostname, OS, IP and MAC addresses |
| `SHUTDOWN`        | Shut down the remote machine                |
| `CANCEL_SHUTDOWN` | Cancel a pending shutdown                   |

---

## 🚀 Getting Started

### Requirements

* Android device on the same local network
* Desktop companion running on the target machine

### Basic flow

1. Start the desktop companion on your computer
2. Open the Android app
3. Let it discover available devices automatically
4. Select a device and interact with it

---

## ⚙️ Design Principles

This project follows a few simple ideas:

* 🪶 **Lightweight** — no unnecessary background work
* 🔒 **Local-first** — no cloud, no external dependencies
* 🧩 **Simple protocol** — easy to understand and extend
* 🔁 **Predictable behavior** — explicit actions, no hidden magic

---

## 🛠️ Development

### Tech stack

* Android (Kotlin + Jetpack Compose)
* mDNS / NSD for discovery
* Simple TCP-based protocol

### Versioning

The project uses **semantic versioning** (`major.minor.patch`) and is automatically updated via CI based on pull request labels.

---

## 🤝 Contributing

Contributions are welcome!

If you plan to contribute:

* Use descriptive commits
* Follow the existing code style
* Apply a version label (`major`, `minor`, `patch`) to your PR

---

## 📄 License

This project is licensed under the GNU General Public License v3.0.

You are free to use, modify and distribute this software under the terms of the GPLv3.  
Any distributed modifications must also be released under the same license.

See the [LICENSE](LICENSE) file for details.

---

## 📌 Notes

This application is intended for **local network usage only**.
No authentication or encryption is currently implemented.

Use it in trusted environments.
