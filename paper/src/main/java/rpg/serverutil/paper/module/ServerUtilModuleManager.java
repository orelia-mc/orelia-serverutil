package rpg.serverutil.paper.module;

import rpg.serverutil.paper.OreliaServerUtilPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Registration-order lifecycle registry for {@link ServerUtilModule}s, mirroring
 * orelia-core's {@code ModuleManager}.
 */
public final class ServerUtilModuleManager {

    private final OreliaServerUtilPlugin plugin;
    private final List<ServerUtilModule> registrationOrder = new ArrayList<>();
    private final Map<Class<? extends ServerUtilModule>, ServerUtilModule> byType = new LinkedHashMap<>();

    public ServerUtilModuleManager(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(ServerUtilModule module) {
        registrationOrder.add(module);
        byType.put(module.getClass(), module);
    }

    public void enableAll() {
        for (ServerUtilModule module : registrationOrder) {
            try {
                module.onEnable(plugin);
                plugin.getLogger().info("Module enabled: " + module.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + module.getName(), e);
            }
        }
    }

    public void disableAll() {
        List<ServerUtilModule> reversed = new ArrayList<>(registrationOrder);
        Collections.reverse(reversed);
        for (ServerUtilModule module : reversed) {
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + module.getName(), e);
            }
        }
    }

    public void reloadAll() {
        for (ServerUtilModule module : registrationOrder) {
            try {
                module.onReload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reload module: " + module.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ServerUtilModule> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) byType.get(type));
    }
}
