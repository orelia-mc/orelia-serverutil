package rpg.serverutil.paper.module;

import rpg.serverutil.paper.OreliaServerUtilPlugin;

/**
 * Lifecycle contract for orelia-serverutil's top-level modules, mirroring orelia-core's
 * {@code RpgModule} / orelia-world's {@code WorldModule} / orelia-extra's {@code ExtraModule}.
 */
public interface ServerUtilModule {

    String getName();

    void onEnable(OreliaServerUtilPlugin plugin);

    void onDisable();

    default void onReload() {
    }
}
