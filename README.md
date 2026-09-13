# MiBand DevBox Android
Android companion app for DevBox (duh)

This app serves as the central control hub for the [MiBand DevBox Lua Service](https://github.com/Sucharek233/MiBand-DevBox-Watchface) and [QuickApp](https://github.com/Sucharek233/MiBand-DevBox-QuickApp).

<img src="images/qjs.png" alt="QJS workspaces" width="220">
<img src="images/lua.png" alt="Lua workspaces" width="220">

## User Interface
A modern Jetpack Compose UI with:
- **Device Selection**: Scan and connect to Xiaomi wearable devices via Mi Interconnect.
- **Main Dashboard**: Quick access to all available activities and connection status.
- **Activity Screens**: Specialized interfaces for each service (Terminal, File Manager, Sensors, etc.).
- **System Logs**: Real-time view of interconnect traffic and application logs.
- **WebSocket Control**: Remote control interface.

## How It Works
1. The app uses the **Xiaomi Wearable SDK** (Mi Interconnect) to establish a communication channel with the watch.
2. It sends and receives structured **JSON messages** over the `MessageApi`.
3. Requests are dispatched to the watch's router, which triggers the corresponding Lua or QuickJS activity.
4. Responses are parsed and routed to the active screen's `ViewModel` via `StateFlow` and `SharedFlow`.
5. The app maintains an **Operation Tracker** to handle timeouts and mailbox states on the watch side.

## Mailbox States
The app tracks the following states reported by the watch:

| Value | State | Description |
| ---: | --- | --- |
| `0` | `DONE` | Operation completed successfully |
| `1` | `IDLE` | Service is ready |
| `2` | `PENDING` | Request is waiting in mailbox |
| `3` | `RUNNING` | Activity is currently executing |
| `4` | `ERROR` | Operation failed |
| `5` | `TIMEOUT` | Mailbox operation timed out |
| `6` | `STREAM` | Continuous data stream (e.g., sensors) |

## Available Activities

The Android app provides UIs for the following watch-side activities:

| Name              | Route                    | Purpose                                 |
|-------------------|--------------------------|-----------------------------------------|
| **Ping**          | `ping`                   | Latency testing and connection check    |
| **Terminal**      | `cmd`                    | Remote shell command execution          |
| **File Manager**  | `io`                     | Explore, read, and stream files         |
| **Lua Shell**     | `luashell`               | Execute Lua code snippets               |
| **QuickJS Shell** | `qjs`                    | Execute QuickJS code snippets           |
| **Sensors**       | `sensors` / `sensorsLua` | Real-time sensor data and charting      |
| **Apps**          | `apps`                   | Manage watch applications and manifests |
| **System Info**   | `sysInfoLua` / `sysinfo` | View device hardware and software info  |
| **Module Compat** | `modules`                | Check available JS modules on the watch |
| **System Logs**   | `systemlogs`             | View interconnect and mailbox logs      |

## WebSocket Relay
The app includes a built-in **Ktor WebSocket server** that allows remote control from a PC.
1. Enable the WebSocket server in the app.
2. Connect a client (e.g., a simple JS script or `websocat`) to the app's IP.
3. Send JSON commands directly to the relay; the app will forward them to the connected watch and broadcast the response back.

## Adding an Activity Screen
1. Create a new package under `ui/screens/activities/[name]`.
2. Implement a `ViewModel` that interacts with `WatchViewModel` flows.
3. Create a Compose `Screen` for the UI.
4. Define the route in `ui/navigation/Screen.kt`.
5. Register the screen in `MainActivity.kt`'s `NavHost`.
6. Add the activity tile to `MainDashboardScreen.kt`.

## Build & Installation
- Built with Android Studio `Quail 3 | 2026.1.3`

## License
This project is licensed under the GPL v3.0 license.
