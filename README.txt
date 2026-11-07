# Attribute RPG & Leveling (Epic Magic Skill)
**Author & Owner:** Dzarrin  
**Minecraft Version:** 1.20.1  
**Loader:** Minecraft Forge (47.4.10+)

---

## 🌟 Overview
Attribute RPG & Leveling is a comprehensive, production-ready RPG attribute leveling mod. It seamlessly hooks into Vanilla Minecraft's Experience system and provides dynamic soft-dependency integrations with **Epic Fight** and **Iron's Spells 'n Spellbooks**.

---

## ⚔️ Attributes & Mechanics

1. **Strength (STR)**:
   - Increases Vanilla Melee Attack Damage (`generic.attack_damage`).
   - Increases Attack Speed (`generic.attack_speed`, +0.2% per point, max +20% at Lv. 100).
   - *(Optional: Epic Fight)* Adds Armor Negation (+0.2% per point).

2. **Ranged (RNG)**:
   - Increases projectile arrow damage.
   - Increases projectile flight speed (+2% per point).

3. **Constitution (CON)**:
   - Increases Vanilla Max Health (+2.0 Health = 1 Heart per point).
   - Increases Knockback Resistance (`generic.knockback_resistance`, +0.8% per point = 80% knockback resistance at Lv. 100).

4. **Agility (AGI)**:
   - Increases Vanilla Movement Speed (+0.5% per point).
   - *(Optional: Epic Fight)* Increases Stamina Regeneration Speed.

5. **Intelligence (INT)**:
   - *(Optional: Iron's Spells)* Increases Spell Power (+2.0% per point).
   - Automatically hidden in GUI if Iron's Spells is not installed.

6. **Wisdom (WIS)**:
   - *(Optional: Iron's Spells)* Increases Max Mana (+10 Max Mana per point) and Mana Regeneration (+2.0% per point).
   - Automatically hidden in GUI if Iron's Spells is not installed.

7. **Luck (LUK)**:
   - Increases Vanilla Luck (`generic.luck`).
   - Adds Critical Mob Loot Drop chance (+2.5% per point) to duplicate mob drops.

---

## 🎮 In-Game Character Screen (`O` Key)
- **3D Character Model**: Interactive live preview of the player character.
- **Leveling**: 5 Status Points granted upon reaching a new highest level (anti-exploit for enchanting tables/deaths).
- **Staged Allocation**:
  - `[+]` button to increase points.
  - `[-]` button to decrease points per skill before applying.
  - **Reset**: Reverts all unapplied pending points in the current session.
  - **Apply**: Permanently locks and synchronizes points with the server. Once applied, points cannot be reset.

---

## ⚙️ Configuration
All attribute scaling, leveling rewards, and caps are fully configurable in:
`config/attributerpg-common.toml`

---

## 📜 Credits & License
- **Lead Developer & Owner:** Dzarrin
- **License:** MIT License
