package rpg.serverutil.paper.spawn;

import org.bukkit.entity.Player;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

/** Backs {@code /suadmin setspawn}: sets the sender's current location as their world's spawn. */
public final class SpawnModule implements ServerUtilModule {

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
    }

    @Override
    public void onDisable() {
    }

    public void setSpawn(Player player) {
        player.getWorld().setSpawnLocation(player.getLocation());
    }
}
