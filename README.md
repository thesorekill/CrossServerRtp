# 🌍 SorekillRTP

**SorekillRTP** is a high-performance, cross-server Random Teleport (RTP) and respawn management plugin built for modern Minecraft networks.  
It supports local and cross-server RTP, **cross-server respawns**, multiple worlds (Overworld, Nether, End), Redis-backed coordination, and strict safety checks designed for large SMP environments.

Designed for **Paper backends** behind **Velocity** or **BungeeCord / Waterfall**.

---

## ✨ Features

- 🌐 **Cross-server RTP**
  - Teleport players between backend servers using Redis and proxy messaging

- ☠️ **Cross-server respawns**
  - Respawn players on a different server than where they died
  - Ideal for hub → SMP or lobby-based networks
  - Fully configurable per server

- 🌍 **Multi-world support**
  - Overworld, Nether, and End
  - Per-world radius and distribution settings

- 🧭 **Advanced RTP distribution**
  - Square ring (uniform or biased)
  - Circular annulus (uniform area or radius)
  - Gaussian-clamped distributions

- 🛡️ **Strict safety checks**
  - Avoid lava, fire, dangerous blocks, liquids, and void-adjacent areas
  - Prevent teleports near hostile mobs or other players

- ⏳ **Teleport countdown with movement detection**
  - Cancels on movement or jumping
  - Configurable countdown duration

- 🧠 **Smart chunk handling**
  - Async chunk loading on Paper
  - Safe sync fallback on Spigot

- 🔁 **Cross-server cooldowns**
  - Redis-backed cooldowns shared across servers

- 🔊 **Configurable sounds & messages**
  - Fully customizable via `config.yml` and `messages.yml`

- 🔄 **Safe reload support**
  - Reload config, messages, Redis, and listeners without restarting the server

---

## ☠️ Respawn Behavior

SorekillRTP can fully control where players respawn after death.

Supported behaviors include:

- Respawn on the **same server**
- Respawn on a **different server**
- Respawn at **spawn**, **bed**, or **random teleport**
- Cross-server respawns with optional RTP on respawn

This allows setups such as:
- Dying on a hub sends players to an SMP
- SMP respawns always perform RTP
- Private servers keep respawns local only

All respawn behavior is controlled via configuration and can differ per server.

---

## 🧩 Supported Platforms

**Backends**
- Paper 1.21.x
- Paper 26.1.2
- Paper 26.2

**Proxies**
- Velocity
- BungeeCord
- Waterfall

> Note: SorekillRTP is a backend plugin.  
> It does not run as a proxy plugin but communicates with the proxy using plugin messaging.

---

## 📦 Requirements

- Java 21 for Paper 1.21.x
- Java 25 for Paper 26.1.2 and 26.2
- A supported Paper server version listed above
- (Optional) Redis server for cross-server RTP and respawns

---

## ⚙️ Installation

1. Place `SorekillRTP.jar` into your backend server’s `plugins` folder
2. Start the server once to generate config files
3. (Optional) Configure Redis in `config.yml` for cross-server RTP and respawns
4. Restart or reload the server

---

## 🔧 Configuration

Main configuration file:
- `config.yml`

Message customization:
- `messages.yml`

All RTP and respawn behavior (radius, cooldowns, countdowns, distributions, safety rules, and respawn routing) is configurable per server and per world.

---

## 🧪 Commands

- `/rtp`
  - Randomly teleport yourself

- `/rtp <world>`
  - Randomly teleport within a specific world

- `/rtp <player> <server> <world>` (admin)
  - Teleport another player across servers

---

## 🔐 Permissions

- `sorekillrtp.use` – Use RTP
- `sorekillrtp.cooldown.bypass` – Bypass RTP cooldown
- `sorekillrtp.tptimer.bypass` – Bypass RTP countdown
- `sorekillrtp.admin` – Admin RTP commands

---

## 🧠 How Cross-Server RTP & Respawn Works

1. Player requests RTP or dies
2. Source server determines target server and world
3. Target server computes a safe location
4. Location is shared via Redis
5. Player is transferred through the proxy
6. Final teleport occurs on the destination server

---

## Planned Features

- Folia Support

## 🧾 License

This project is licensed under the Apache 2.0 License.

---

## ❤️ Credits

Developed by **Sorekill**  
Built for modern SMP networks that need fast, safe, and scalable RTP and respawn handling.
