# destru

A lightweight client-side mod that allows you to **save** and **load** regions of the Minecraft world.

## ✨ Features

- Save any selected region(s) to a file
- Load saved regions into the world at any location
- Optional visual-only placement in multiplayer
- Extremely compact `.destru` format for storage
- Simple commands for ease of use
- Support on server (only save)

## ❓ Q&A

### 🧩 How does region placement work?

When placing a saved region into the world:

- **In singleplayer**, the region content is directly written into the world — blocks, entities, and tile data are fully restored.
- **On multiplayer servers**, the mod uses a ghost placement technique — the region will appear visually to the client, but the actual server world remains untouched.
  > ⚠️ **Do not use this feature to mislead or exploit multiplayer gameplay.** Always respect server rules.

---

### 🔧 Will server-side world editing be supported?

This mod is designed as a **basic client-side mod**.  
Server-side editing support (such as actual block modifications on multiplayer servers) will be introduced **via optional companion mods** in the future.

---

### 📦 What is the `.destru` file format?

`.destru` is a custom file format designed to **efficiently store** block data, block entities, and regular entities across multiple regions with **minimal file size**.

- Internally, it's a structured `.nbt` file.
- It prioritizes compactness and performance.

## 📝 License

This project is licensed under the [MIT License](LICENSE).