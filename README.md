# 📦 TrickierTrials — Changelog

## 🚀 Version: Custom Rewards & Fixes Update

---

## 🐛 Fixed
- Mining Fatigue can now be completely disabled by setting  
  `mining-fatigue-level` to `0` or lower.
- Fixed an incorrect configuration path that caused Mining Fatigue to remain
  active even when disabled in the config.
- Improved validation to prevent invalid reward entries from breaking reward rolls
  or causing errors during mob death events.

---

## ✨ Added

### 🎁 Advanced Reward System
- Support for **reward pools** with configurable `rolls` per mob kill.
- Weighted reward selection (`WEIGHTED`) or evaluation of all entries (`ALL`).
- Optional `unique` flag to prevent the same reward from being selected multiple
  times in a single pool.
- Support for **amount ranges** using `{ min, max }` instead of fixed amounts.

---

### 🧱 Tier / Difficulty-Based Rewards
- Separate reward tables per tier:
  - `DEFAULT`
  - `NORMAL`
  - `HARD`
  - `EXTREME`
- Option to **replace or merge** global reward pools per tier.
- Full support for **per-entity overrides** inside each tier.

---

### 🧑‍💻 Command-Based Rewards
- Rewards can now execute **commands** instead of only dropping items.
- Commands can be executed as:
  - Console
  - Player
- Supported placeholders:
  - `%player%`
  - `%uuid%`
  - `%entity%`
  - `%tier%`
  - `%world%`
  - `%x%`, `%y%`, `%z%`

---

### 📉 Drop Control & Balancing
- Configurable **maximum items per kill** (`max-items-per-kill`) to prevent
  excessive drops.
- Option to fully **replace default vanilla drops** or merge them with custom
  rewards.
- Command rewards are excluded from the item cap.

---

### 🧟 Entity-Specific Rewards
- Custom reward pools per entity type  
  (e.g. `ZOMBIE`, `SKELETON`, `BREEZE`).
- Entity-specific rewards supported:
  - Globally
  - Per tier / difficulty

---

## 🔄 Changed
- Refactored reward handling logic for better performance, scalability and clarity.
- Default drop handling now integrates cleanly with the custom reward system.
- Configuration structure updated to support large and expandable reward tables.

---

## ❌ Removed
- Legacy single-reward drop handling.
- Hardcoded drop limitations that could not be configured through the config.

---

## 📝 Notes
- Older configurations will require migration to the new reward pool format.
- The new system is fully backward-compatible with disabled extra rewards.
- Recommended to review reward weights and caps to maintain game balance.

---
