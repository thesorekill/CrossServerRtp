/*
 * Copyright © 2026 Sorekill
 *
 * Licensed under the Apache License, Version 2.0.
 * See https://www.apache.org/licenses/LICENSE-2.0
 */

package net.chumbucket.sorekillrtp.msg;

import net.chumbucket.sorekillrtp.SorekillRTPPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Messaging implemented only with the Spigot API so it also runs on Paper. */
public final class MessageService {

    private final SorekillRTPPlugin plugin;
    private final FileConfiguration messages;
    private final boolean chatEnabled;
    private final boolean actionbarEnabled;
    private final boolean bossbarEnabled;
    private final boolean titleEnabled;
    private final boolean toastEnabled;
    private final int titleFadeInTicks;
    private final int titleStayTicks;
    private final int titleFadeOutTicks;
    private final int bossbarSeconds;
    private final double bossbarProgress;
    private final BarColor bossbarColor;
    private final BarStyle bossbarStyle;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> bossbarHideTasks = new ConcurrentHashMap<>();

    private MessageService(SorekillRTPPlugin plugin, FileConfiguration messages,
                           boolean chatEnabled, boolean actionbarEnabled,
                           boolean bossbarEnabled, boolean titleEnabled,
                           boolean toastEnabled, int titleFadeInTicks,
                           int titleStayTicks, int titleFadeOutTicks,
                           int bossbarSeconds, double bossbarProgress,
                           BarColor bossbarColor, BarStyle bossbarStyle) {
        this.plugin = plugin;
        this.messages = messages;
        this.chatEnabled = chatEnabled;
        this.actionbarEnabled = actionbarEnabled;
        this.bossbarEnabled = bossbarEnabled;
        this.titleEnabled = titleEnabled;
        this.toastEnabled = toastEnabled;
        this.titleFadeInTicks = titleFadeInTicks;
        this.titleStayTicks = titleStayTicks;
        this.titleFadeOutTicks = titleFadeOutTicks;
        this.bossbarSeconds = bossbarSeconds;
        this.bossbarProgress = bossbarProgress;
        this.bossbarColor = bossbarColor;
        this.bossbarStyle = bossbarStyle;
    }

    public static MessageService load(SorekillRTPPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration messages = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "messages.yml"));

        int seconds = Math.max(0, cfg.getInt("messages.bossbar_seconds", 5));
        double progress = Math.max(0.0, Math.min(1.0,
                cfg.getDouble("messages.bossbar_progress", 1.0)));

        return new MessageService(
                plugin, messages,
                cfg.getBoolean("messages.chat", true),
                cfg.getBoolean("messages.actionbar", false),
                cfg.getBoolean("messages.bossbar", false),
                cfg.getBoolean("messages.title", false),
                cfg.getBoolean("messages.toast", false),
                Math.max(0, cfg.getInt("messages.title_fade_in_ticks", 10)),
                Math.max(0, cfg.getInt("messages.title_stay_ticks", 40)),
                Math.max(0, cfg.getInt("messages.title_fade_out_ticks", 10)),
                seconds, progress,
                parseEnum(BarColor.class, cfg.getString("messages.bossbar_color"), BarColor.BLUE),
                parseEnum(BarStyle.class, cfg.getString("messages.bossbar_overlay"), BarStyle.SOLID)
        );
    }

    public void send(CommandSender to, String path) {
        send(to, path, null);
    }

    public void send(CommandSender to, String path, Map<String, String> placeholders) {
        if (to == null || path == null || path.isBlank()) return;
        List<String> lines = resolveLines(path);
        if (lines.isEmpty()) return;
        Map<String, String> ph = placeholders == null ? Collections.emptyMap() : placeholders;

        String first = null;
        for (String raw : lines) {
            String line = color(applyPlaceholders(raw, ph).trim());
            if (line.isEmpty()) continue;
            if (first == null) first = line;
            if (chatEnabled) to.sendMessage(line);
        }

        if (first == null || !(to instanceof Player player)) return;
        final String display = first;

        if (actionbarEnabled) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(display));
        }
        if (bossbarEnabled) showBossBar(player, display);
        if (titleEnabled) {
            player.sendTitle(display, "", titleFadeInTicks, titleStayTicks, titleFadeOutTicks);
        }
        // Stock Spigot has no toast API. Preserve the setting without making it
        // a runtime requirement; chat/title/actionbar/bossbar remain portable.
        if (toastEnabled) {
            plugin.getLogger().fine("Toast messages are unavailable on stock Spigot.");
        }
    }

    private List<String> resolveLines(String path) {
        Object value = messages.get(path);
        if (value instanceof String text) {
            return text.isBlank() ? List.of() : List.of(text);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> v == null ? "" : String.valueOf(v)).toList();
        }
        return List.of();
    }

    private static String applyPlaceholders(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void showBossBar(Player player, String title) {
        if (!player.isOnline() || bossbarSeconds <= 0) return;
        Runnable action = () -> {
            UUID uuid = player.getUniqueId();
            BukkitTask oldTask = bossbarHideTasks.remove(uuid);
            if (oldTask != null) oldTask.cancel();

            BossBar oldBar = activeBossBars.remove(uuid);
            if (oldBar != null) oldBar.removeAll();

            BossBar bar = Bukkit.createBossBar(title, bossbarColor, bossbarStyle);
            bar.setProgress(bossbarProgress);
            bar.addPlayer(player);
            activeBossBars.put(uuid, bar);
            bossbarHideTasks.put(uuid, Bukkit.getScheduler().runTaskLater(
                    plugin, () -> hideBossBar(uuid), bossbarSeconds * 20L));
        };
        runSync(action);
    }

    private void hideBossBar(UUID uuid) {
        BukkitTask task = bossbarHideTasks.remove(uuid);
        if (task != null) task.cancel();
        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }

    public void shutdown() {
        runSync(() -> {
            bossbarHideTasks.values().forEach(BukkitTask::cancel);
            bossbarHideTasks.clear();
            activeBossBars.values().forEach(BossBar::removeAll);
            activeBossBars.clear();
        });
    }

    private void runSync(Runnable action) {
        if (Bukkit.isPrimaryThread()) action.run();
        else Bukkit.getScheduler().runTask(plugin, action);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (type == BarStyle.class && normalized.equals("PROGRESS")) normalized = "SOLID";
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
