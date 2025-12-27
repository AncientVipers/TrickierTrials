package de.t14d3.trickiertrials;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extra rewards for mobs spawned by Trial Spawners (tagged via PDC).
 * Supports:
 * - simple rewards list (chance based)
 * - reward pools (multiple rolls, weighted picks, optional uniqueness)
 * - tier/difficulty reward tables (DEFAULT/NORMAL/HARD/EXTREME) + optional per-entity overrides
 * - max items per kill cap
 * - command rewards (run as console or player)
 */
public class TrialDeathListener implements Listener {

    private final JavaPlugin plugin;

    private final NamespacedKey trialSpawnedKey = new NamespacedKey("trickiertrials", "trialspawned");
    private final NamespacedKey trialTierKey = new NamespacedKey("trickiertrials", "trialtier");

    private enum PickMode { WEIGHTED, ALL }

    private static class RewardOut {
        final ItemStack item;
        final List<String> commands;
        final boolean asConsole;

        private RewardOut(ItemStack item) {
            this.item = item;
            this.commands = null;
            this.asConsole = true;
        }

        private RewardOut(List<String> commands, boolean asConsole) {
            this.item = null;
            this.commands = commands;
            this.asConsole = asConsole;
        }

        static RewardOut item(ItemStack item) { return new RewardOut(item); }
        static RewardOut command(List<String> commands, boolean asConsole) { return new RewardOut(commands, asConsole); }
    }

    public TrialDeathListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(trialSpawnedKey, PersistentDataType.INTEGER)) {
            return;
        }

        // Keep original behavior: prevent farms by limiting what vanilla drops can stay.
        // (Users can still add extra rewards below.)
        event.getDrops().removeIf(item -> item.getType() != Material.BREEZE_ROD
                && item.getType() != Material.ROTTEN_FLESH
                && item.getType() != Material.BONE
                && item.getType() != Material.ARROW);

        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("extra-rewards.enabled", false)) {
            return;
        }

        if (cfg.getBoolean("extra-rewards.replace-default-drops", false)) {
            event.getDrops().clear();
        }

        int cap = cfg.getInt("extra-rewards.max-items-per-kill", -1);
        if (cap == 0) cap = -1; // treat 0 as "no cap" for convenience

        Player killer = event.getEntity().getKiller();
        String tier = event.getEntity().getPersistentDataContainer().get(trialTierKey, PersistentDataType.STRING);
        if (tier == null || tier.isBlank()) tier = "DEFAULT";
        tier = tier.toUpperCase(Locale.ROOT);

        ConfigurationSection extra = cfg.getConfigurationSection("extra-rewards");
        if (extra == null) return;

        // Build reward sources (global + tier + per-entity overrides)
        List<RewardOut> outputs = new ArrayList<>();

        // global
        outputs.addAll(gatherRewards(extra, event, killer, tier));

        // tier (optionally replace global)
        ConfigurationSection tierSec = extra.getConfigurationSection("tiers." + tier);
        if (tierSec != null) {
            boolean replaceGlobal = tierSec.getBoolean("replace-global", false);
            if (replaceGlobal) outputs.clear();
            outputs.addAll(gatherRewards(tierSec, event, killer, tier));
        }

        // Apply rewards with cap: items first (cap), then commands (not capped)
        applyWithCap(event, outputs, cap);
    }

    private List<RewardOut> gatherRewards(ConfigurationSection root, EntityDeathEvent event, Player killer, String tier) {
        List<RewardOut> out = new ArrayList<>();

        // 1) root rewards/pools
        out.addAll(rewardsFromList(root.getList("rewards"), killer, event, tier));
        out.addAll(rewardsFromPools(root.getList("pools"), killer, event, tier));

        // 2) per-entity override inside this root
        ConfigurationSection perEntity = root.getConfigurationSection("per-entity");
        if (perEntity == null) return out;

        String entityKey = event.getEntityType().name();

        // Legacy format: per-entity.<ENTITY>: [ {material, amount, chance}, ... ]
        if (perEntity.isList(entityKey)) {
            out.addAll(rewardsFromList(perEntity.getList(entityKey), killer, event, tier));
            return out;
        }

        // New format: per-entity.<ENTITY>:
        //   rewards: [ ... ]
        //   pools: [ ... ]
        if (perEntity.isConfigurationSection(entityKey)) {
            ConfigurationSection sec = perEntity.getConfigurationSection(entityKey);
            if (sec != null) {
                out.addAll(rewardsFromList(sec.getList("rewards"), killer, event, tier));
                out.addAll(rewardsFromPools(sec.getList("pools"), killer, event, tier));
            }
        }

        return out;
    }

    private void applyWithCap(EntityDeathEvent event, List<RewardOut> outputs, int cap) {
        int addedUnits = 0;

        // 1) items (cap applies)
        for (RewardOut r : outputs) {
            if (r.item == null) continue;
            if (cap > 0 && addedUnits >= cap) break;

            ItemStack toAdd = r.item.clone();
            if (cap > 0) {
                int remaining = cap - addedUnits;
                if (toAdd.getAmount() > remaining) {
                    toAdd.setAmount(remaining);
                }
            }

            if (toAdd.getAmount() > 0) {
                event.getDrops().add(toAdd);
                addedUnits += toAdd.getAmount();
            }
        }

        // 2) commands (not capped)
        for (RewardOut r : outputs) {
            if (r.commands == null || r.commands.isEmpty()) continue;
            CommandSender sender = r.asConsole ? Bukkit.getConsoleSender() : event.getEntity().getKiller();
            if (sender == null) sender = Bukkit.getConsoleSender();

            for (String cmd : r.commands) {
                if (cmd == null) continue;
                String finalCmd = cmd.trim();
                if (finalCmd.isEmpty()) continue;
                Bukkit.dispatchCommand(sender, finalCmd);
            }
        }
    }

    // -------------------- rewards list --------------------

    @SuppressWarnings("unchecked")
    private List<RewardOut> rewardsFromList(List<?> rawList, Player killer, EntityDeathEvent event, String tier) {
        if (rawList == null || rawList.isEmpty()) return Collections.emptyList();
        List<RewardOut> out = new ArrayList<>();

        for (Object entryObj : rawList) {
            if (!(entryObj instanceof Map)) continue;
            Map<String, Object> entry = (Map<String, Object>) entryObj;

            double chance = asDouble(entry.get("chance"), 1.0);
            if (chance <= 0) continue;
            if (ThreadLocalRandom.current().nextDouble() > Math.min(chance, 1.0)) continue;

            RewardOut reward = buildReward(entry, killer, event, tier);
            if (reward != null) out.add(reward);
        }

        return out;
    }

    // -------------------- pools --------------------

    @SuppressWarnings("unchecked")
    private List<RewardOut> rewardsFromPools(List<?> rawPools, Player killer, EntityDeathEvent event, String tier) {
        if (rawPools == null || rawPools.isEmpty()) return Collections.emptyList();
        List<RewardOut> out = new ArrayList<>();

        for (Object poolObj : rawPools) {
            if (!(poolObj instanceof Map)) continue;
            Map<String, Object> pool = (Map<String, Object>) poolObj;

            int rolls = asInt(pool.get("rolls"), 1);
            if (rolls <= 0) continue;

            boolean unique = Boolean.TRUE.equals(pool.get("unique"));

            Object entriesObj = pool.get("entries");
            if (!(entriesObj instanceof List)) continue;
            List<?> entries = (List<?>) entriesObj;
            if (entries.isEmpty()) continue;

            String modeRaw = asString(pool.get("pick"));
            PickMode mode = PickMode.WEIGHTED;
            if (modeRaw != null) {
                try {
                    mode = PickMode.valueOf(modeRaw.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    mode = PickMode.WEIGHTED;
                }
            }

            if (mode == PickMode.ALL) {
                out.addAll(rewardsFromList(entries, killer, event, tier));
                continue;
            }

            // WEIGHTED
            List<Map<String, Object>> entryMaps = new ArrayList<>();
            for (Object e : entries) {
                if (e instanceof Map) entryMaps.add((Map<String, Object>) e);
            }
            if (entryMaps.isEmpty()) continue;

            int effectiveRolls = unique ? Math.min(rolls, entryMaps.size()) : rolls;
            for (int i = 0; i < effectiveRolls; i++) {
                Map<String, Object> chosen = pickWeighted(entryMaps);
                if (chosen == null) break;

                double chance = asDouble(chosen.get("chance"), 1.0);
                if (chance > 0 && ThreadLocalRandom.current().nextDouble() <= Math.min(chance, 1.0)) {
                    RewardOut reward = buildReward(chosen, killer, event, tier);
                    if (reward != null) out.add(reward);
                }

                if (unique) entryMaps.remove(chosen);
            }
        }

        return out;
    }

    private Map<String, Object> pickWeighted(List<Map<String, Object>> entries) {
        double total = 0.0;
        for (Map<String, Object> e : entries) {
            double w = asDouble(e.get("weight"), 1.0);
            if (w > 0) total += w;
        }
        if (total <= 0) return null;

        double r = ThreadLocalRandom.current().nextDouble() * total;
        double upto = 0.0;
        for (Map<String, Object> e : entries) {
            double w = asDouble(e.get("weight"), 1.0);
            if (w <= 0) continue;
            upto += w;
            if (upto >= r) return e;
        }
        return entries.get(entries.size() - 1);
    }

    // -------------------- reward builders --------------------

    @SuppressWarnings("unchecked")
    private RewardOut buildReward(Map<String, Object> entry, Player killer, EntityDeathEvent event, String tier) {
        // COMMAND reward: either type: COMMAND, or has "command" / "commands"
        String type = asString(entry.get("type"));
        boolean looksCommand = (type != null && type.equalsIgnoreCase("COMMAND"))
                || entry.containsKey("command")
                || entry.containsKey("commands");

        if (looksCommand) {
            List<String> cmds = new ArrayList<>();
            Object single = entry.get("command");
            if (single instanceof String) cmds.add((String) single);

            Object multi = entry.get("commands");
            if (multi instanceof List) {
                for (Object o : (List<?>) multi) {
                    if (o != null) cmds.add(String.valueOf(o));
                }
            }

            if (cmds.isEmpty()) return null;

            boolean asConsole = !Boolean.FALSE.equals(entry.get("as-console"));
            List<String> rendered = new ArrayList<>(cmds.size());
            for (String c : cmds) {
                rendered.add(applyPlaceholders(c, killer, event, tier));
            }

            return RewardOut.command(rendered, asConsole);
        }

        // ITEM reward
        String matName = asString(entry.get("material"));
        if (matName == null) return null;

        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid extra-reward material: " + matName);
            return null;
        }

        int amount = resolveAmount(entry.get("amount"), entry.get("min-amount"), entry.get("max-amount"));
        if (amount <= 0) return null;

        return RewardOut.item(new ItemStack(mat, amount));
    }

    private String applyPlaceholders(String cmd, Player killer, EntityDeathEvent event, String tier) {
        if (cmd == null) return null;
        String s = cmd;
        if (killer != null) {
            s = s.replace("%player%", killer.getName());
            s = s.replace("%uuid%", killer.getUniqueId().toString());
        }
        s = s.replace("%entity%", event.getEntityType().name());
        s = s.replace("%tier%", tier);
        if (event.getEntity().getLocation() != null) {
            s = s.replace("%world%", event.getEntity().getWorld().getName());
            s = s.replace("%x%", String.valueOf(event.getEntity().getLocation().getBlockX()));
            s = s.replace("%y%", String.valueOf(event.getEntity().getLocation().getBlockY()));
            s = s.replace("%z%", String.valueOf(event.getEntity().getLocation().getBlockZ()));
        }
        // allow configs that include leading slash
        if (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    // -------------------- utils --------------------

    @SuppressWarnings("unchecked")
    private int resolveAmount(Object amountObj, Object minObj, Object maxObj) {
        // 1) amount: 3
        if (amountObj instanceof Number) {
            return ((Number) amountObj).intValue();
        }

        // 2) amount: {min: 1, max: 3}
        if (amountObj instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) amountObj;
            int min = asInt(m.get("min"), 1);
            int max = asInt(m.get("max"), min);
            return randomIntClamped(min, max);
        }

        // 3) min-amount / max-amount
        int min = asInt(minObj, 1);
        int max = asInt(maxObj, min);
        return randomIntClamped(min, max);
    }

    private int randomIntClamped(int min, int max) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (max < min) max = min;
        if (min == max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private String asString(Object o) {
        if (o == null) return null;
        if (o instanceof String) return (String) o;
        return String.valueOf(o);
    }

    private int asInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private double asDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
