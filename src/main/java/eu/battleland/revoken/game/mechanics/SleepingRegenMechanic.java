package eu.battleland.revoken.game.mechanics;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AMechanic;
import eu.battleland.revoken.statics.PktStatics;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.jetbrains.annotations.NotNull;


public class SleepingRegenMechanic extends AMechanic implements Listener {

    public SleepingRegenMechanic(@NotNull RevokenPlugin plugin) {
        super(plugin);
        setTickable(true);
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, getInstance());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }

    @Override
    public void tick() {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!event.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) {
        }
    }
}
