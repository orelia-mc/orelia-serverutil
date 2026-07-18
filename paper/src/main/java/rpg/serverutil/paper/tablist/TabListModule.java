package rpg.serverutil.paper.tablist;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;
import rpg.serverutil.api.TabListApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;
import rpg.serverutil.paper.placeholder.PlaceholderService;
import rpg.serverutil.paper.util.ColorUtil;

import java.util.List;

public final class TabListModule implements ServerUtilModule {

    private OreliaServerUtilPlugin plugin;
    private TabListManager manager;
    private BukkitTask tickTask;
    private BukkitTask headerFooterTask;

    @Override
    public String getName() {
        return "tablist";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        long intervalTicks = config.getLong("tablist.update-interval-ticks", 40L);

        this.manager = new TabListManager();
        plugin.getServer().getServicesManager().register(TabListApi.class, manager, plugin, ServicePriority.Normal);
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);

        if (config.getBoolean("tablist.header-footer.enabled", false)) {
            this.headerFooterTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                    () -> tickHeaderFooter(plugin), intervalTicks, intervalTicks);
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReload() {
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        long intervalTicks = config.getLong("tablist.update-interval-ticks", 40L);

        tickTask.cancel();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);

        if (headerFooterTask != null) {
            headerFooterTask.cancel();
            headerFooterTask = null;
        }
        if (config.getBoolean("tablist.header-footer.enabled", false)) {
            headerFooterTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                    () -> tickHeaderFooter(plugin), intervalTicks, intervalTicks);
        }
    }

    public TabListManager getManager() {
        return manager;
    }

    private void tickHeaderFooter(OreliaServerUtilPlugin plugin) {
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        PlaceholderService placeholders = plugin.getPlaceholderService();
        List<String> headerLines = config.getStringList("tablist.header-footer.header");
        List<String> footerLines = config.getStringList("tablist.header-footer.footer");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(
                    joinLines(headerLines, placeholders, player),
                    joinLines(footerLines, placeholders, player));
        }
    }

    private Component joinLines(List<String> lines, PlaceholderService placeholders, Player player) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(ColorUtil.component(placeholders.resolve(lines.get(i), player)));
        }
        return result;
    }
}
