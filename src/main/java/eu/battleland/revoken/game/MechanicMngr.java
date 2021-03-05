package eu.battleland.revoken.game;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AMngr;
import eu.battleland.revoken.diagnostics.timings.Timer;
import eu.battleland.revoken.game.mechanics.SleepingRegenMechanic;
import eu.battleland.revoken.game.special.SittingMechanic;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2(topic = "Revoken - MechanicMngr")
public class MechanicMngr extends AMngr {

    private final AtomicInteger lastTickableId = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, Runnable> syncTickables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Runnable> asyncTickables = new ConcurrentHashMap<>();

    private final ExecutorService cachedExecutor;
    {
        this.cachedExecutor = Executors.newCachedThreadPool();
    }

    private SittingMechanic sittingMechanic;
    private SleepingRegenMechanic sleepingRegenMechanic;

    /**
     * Default constructor
     * @param plugin Plugin instance
     */
    public MechanicMngr(@NotNull RevokenPlugin plugin) {
        super(plugin);

        this.sittingMechanic = new SittingMechanic(this.getPlugin());
        this.sleepingRegenMechanic = new SleepingRegenMechanic(this.getPlugin());
    }

    @Override
    public void initialize() {
        ((CraftServer) getPlugin().getServer()).getServer().b(() -> {
            this.cachedExecutor.submit(() -> {
                // async tickables
               asyncTickables.forEach((id, tickable) -> {
                   this.cachedExecutor.submit(() -> {
                       try {
                           tickable.run();
                       } catch (Exception x) {
                           if(!this.getPlugin().isDebug())
                               log.error("Caught exception while ticking async task #{}({})" , id, tickable.getClass().getName(), x);
                       }
                   });
               });
            });

            // sync tickables
            syncTickables.forEach((id, tickable) -> {
                var timer = Timer.timings().start();
                try {
                    tickable.run();
                    double tookMs = timer.stop().resultMilli();
                    if(tookMs > 500)
                        log.warn("Ticking sync task #{}({}) took {}ms", id, tickable.getClass().getName(), tookMs);

                } catch (Exception x) {
                    if(!this.getPlugin().isDebug())
                        log.error("Caught exception while ticking sync task #{}({})", id, tickable.getClass().getName(), x);
                }
            });
        });

        {
            this.sleepingRegenMechanic.initialize();

            Bukkit.getPluginManager().registerEvents(this.sittingMechanic, this.getPlugin());
            Bukkit.getCommandMap().register("revoken", new Command("sit") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args)
                {
                    if(!sender.hasPermission("revoken.sit"))
                        return true;

                    Player player = (Player) sender;
                    MechanicMngr.this.sittingMechanic.sitOnLocation(player.getLocation().add(new Vector(0, -1.2, 0)), player);
                    return true;
                }
            });
        }

    }

    @Override
    public void terminate() {
        this.sittingMechanic.getEntites().forEach((entityUuid, data) -> {
            var entity = Bukkit.getEntity(entityUuid);
            if(entity != null) {
                entity.remove();
            } else
                log.warn("SittingMechanic tried to remove non-existing entity");
        });
        this.sleepingRegenMechanic.terminate();
    }

    @Override
    public void reload() {

    }

    /**
     * Registers sync tickable
     * @param runnable Task
     * @return Tickable ID
     */
    public int registerSyncTickable(@NotNull Runnable runnable) {
        this.syncTickables.put(lastTickableId.addAndGet(1), runnable);
        return lastTickableId.get();
    }
    /**
     * Registers sync tickable
     * @param runnable Task
     * @return Tickable ID
     */
    public int registerAsyncTickable(@NotNull Runnable runnable) {
        this.asyncTickables.put(lastTickableId.addAndGet(1), runnable);
        return lastTickableId.get();
    }

    /**
     * Unregisters any tickable with this id
     * @param tickableId Tickable ID
     */
    public void unregisterAnyTickable(int tickableId) {
        this.syncTickables.remove(tickableId);
        this.asyncTickables.remove(tickableId);
    }

}
