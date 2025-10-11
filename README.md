# PickThrow

> "Walking over items won't pick them up anymore. If you want something, you have to actually try."

A lightweight, vanilla-friendly, simple mod that changes item interaction in Minecraft to be more deliberate. No more accidentally filling your inventory with junk while running past it. originally made for a 1.16.5 modpack i was working on, because no one made it how I like it, but now for 1.20.1!

### Features


#### Item Pick Up Pull
Aim at any item on the ground from and **right-click** to pull it directly to you. you can pick up items as far as you can place blocks


#### Area Pickup
Clean up your messes. **Crouch** to suck up all nearby items in a small radius. Ideal for quickly gathering all your ores after a mining session.


#### Charged Throw
**Hold the Drop key (default Q)** to charge up a throw. The longer you hold, the farther and faster the item flies. A quick tap will still perform a normal drop.

Thats it!


---
### Technicals
This mod is lightweight and designed to be server-friendly. Here's a quick look under the hood:

* **Pickup Prevention:** The mod listens for Forge's `EntityItemPickupEvent`. If an item isn't being intentionally pulled by a player, the event is canceled, preventing the pickup without any expensive checks.
* **Pull Physics:** Items targeted for pickup are added to a map. A single server-side tick handler iterates this map, applying a velocity vector to each item, smoothly pulling it towards its target player.
* **Charged Throw Integration:** To avoid conflicts, the mod basically replaces the vanilla 'Drop' key.
    * On the client, it measures the key-hold time. A short tap is ignored, allowing the vanilla drop to happen normally.
    * A long hold sends a packet on release. The server then finds the `ItemEntity` just created by the vanilla drop and applies a new, stronger velocity to it. This works *with* vanilla mechanics, not against them.

---


Make sure you have **Forge** for Minecraft 1.20.1 installed.
