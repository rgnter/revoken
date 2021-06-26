package eu.battleland.revoken.serverside.game.controllers.util;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.serverside.RevokenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class WorldExplore extends AController<RevokenPlugin> {

    public WorldExplore(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }

    public void exploreWorld(@NotNull World world) {
        final Location center = world.getWorldBorder().getCenter();
        final Chunk centerChunk = center.getChunk();

        Bukkit.getScheduler().runTaskAsynchronously(getPlugin().instance(), () -> {

        });
    }
}
