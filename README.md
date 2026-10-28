# QoL Plus

Quality-of-life features for Minecraft **26.3** on **Fabric**, with a clean menu (Right Shift) where every feature can be switched on or off, configured and given a keybind.

> **Made with AI.** Most of this mod's code was written with the help of an AI assistant (Claude). It's shared as-is for anyone who finds it useful.

## Features

**World** (need QoL Plus on the server too; single-player always works)
- **Tree Felling**: break a log with an axe and the whole tree comes down (sneak for a single log). Logs can go straight into your inventory.
- **Leaf Decay**: leaves left behind after cutting a tree vanish in a few seconds.
- **Grass Clearing**: sneak + hit grass, flowers, leaf litter or petals with a hoe to clear the area.
- **Crop Harvest**: right-click a grown crop to harvest and replant it.
- **Double Doors**: opening one half opens both.
- **Infinite Trading** and **Fixed Prices** for villagers.
- **String Duper**: brings back the old tripwire string duplication.

**Mining**
- **Vein Miner**: sneak + mine an ore to mine the whole vein.
- **Excavator**: toggle with B to mine 3x3 (up to 7x7) in stone, deepslate, ores, dirt, sand and gravel with a pickaxe or shovel.

**Inventory**
- Search bar and **Sort** button on chests, barrels, shulker boxes, hoppers, dispensers and droppers.
- **Hotbar Refill**: the stack in your hand refills from your inventory when it runs out.
- **Shulker Preview**: see a shulker box's contents in its tooltip.
- **Tooltips**: food hunger/saturation and durability numbers.
- **Durability Warning**: sound and message when a tool or armor piece is almost broken.

**Render**
- **Zoom** (hold Z): smooth, scroll to zoom further, optional cinematic camera.
- **Freelook** (hold Left Alt): look around in third person without turning.
- **Fullbright**, **Light Overlay** (F7, shows where mobs can spawn), **Death Marker**, custom **Crosshair** and **Hit Color**.

**Performance**
- **Auto Render Distance**, **No Particles**, **No Shadows**, **Hide Far Signs**, **FPS Caps** (background and menus).

**HUD**
- Watermark, module list, FPS, CPS, ping, keystrokes, clock, direction, coordinates, day counter, session stats, armor and firework counters. Theme color and opacity are adjustable.

**Also:** an in-game **Screenshot Gallery** (Right Shift menu → Screenshots).

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu)

## Install

Download the `.jar` from [Releases](../../releases) and put it in your `mods` folder.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

## License

CC0 1.0: do whatever you want with it.
