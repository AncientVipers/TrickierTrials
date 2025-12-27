package de.t14d3.trickiertrials;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TrickierTrials extends JavaPlugin implements CommandExecutor {

    private List<Material> trialChamberMaterials;

    private boolean decayPlacedBlocks;
    private boolean regenerateBrokenBlocks;
    private boolean strengthenTrialMobs;
    private boolean glowingEffect;
    private boolean secret;
    private String secretName;

    private long trialVaultResetTime;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig(); // Creates the config file with default values if it doesn't exist
        loadTrialChamberMaterials(); // Load trial chamber materials from config
        loadConfigurationOptions(); // Load decay and regeneration settings

        // Register event listeners
        this.getServer().getPluginManager().registerEvents(new TrialSpawnerListener(this, strengthenTrialMobs, glowingEffect, secret, secretName), this);
        this.getServer().getPluginManager().registerEvents(new TrialChamberProtector(this, getTrialChamberMaterials(), decayPlacedBlocks, regenerateBrokenBlocks), this);
        this.getServer().getPluginManager().registerEvents(new TrialDeathListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TrialVaultRefresher(this, trialVaultResetTime), this);

        // Register command executor
        this.getCommand("trickiertrials").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadTrialChamberMaterials() {
        FileConfiguration config = getConfig();
        List<String> blockNames = config.getStringList("blocks-to-protect");
        trialChamberMaterials = new ArrayList<>();

        for (String name : blockNames) {
            try {
                Material material = Material.valueOf(name);
                trialChamberMaterials.add(material);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config: " + name);
            }
        }
    }

    private void loadConfigurationOptions() {
        FileConfiguration config = getConfig();
        decayPlacedBlocks = config.getBoolean("decay-placed-blocks", true);
        regenerateBrokenBlocks = config.getBoolean("regenerate-broken-blocks", true);
        strengthenTrialMobs = config.getBoolean("strengthen-trial-mobs", true);
        trialVaultResetTime = config.getLong("trial-vault-reset-time", 86400000L);
        // Config path was previously under "modules.glowing-effect" but is now top-level
        // (as in the shipped config.yml). Keep both for compatibility.
        glowingEffect = config.getBoolean("glowing-effect", config.getBoolean("modules.glowing-effect", true));
        secret = config.getBoolean("easter-egg", false);
        secretName = config.getString("easter-egg-name", "Klein Tiade");

        // Config migrator
        if (config.get("decay-delay") == null) {
            config.set("decay-delay", "10 #Decay delay in seconds");
        }
        if (config.get("regenerate-delay") == null) {
            config.set("regenerate-delay", "10 #Regeneration delay in seconds, plus a random delay of up to 100 ticks");
        }
        if (config.get("mining-fatigue-level") == null) {
            config.set("mining-fatigue-level", 2);
        }

        // Extra rewards (mob drops) defaults
        if (config.get("extra-rewards") == null) {
            config.set("extra-rewards.enabled", false);
            config.set("extra-rewards.replace-default-drops", false);
            config.set("extra-rewards.rewards", List.of(
                    // Example:
                    // Map.of("material", "DIAMOND", "amount", 1, "chance", 0.02)
            ));
        }

        saveConfig();
    }

    public List<Material> getTrialChamberMaterials() {
        return trialChamberMaterials;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("trickiertrials")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                // Reload the configuration
                reloadConfig();
                loadTrialChamberMaterials(); // Reload trial chamber materials
                loadConfigurationOptions(); // Reload other options
                sender.sendMessage("Trickier Trials configuration reloaded successfully.");
                return true;
            } else {
                // Handle other command logic here (if applicable)
                sender.sendMessage("Usage: /trickiertrials reload");
                return true;
            }
        }
        return false;
    }
}
