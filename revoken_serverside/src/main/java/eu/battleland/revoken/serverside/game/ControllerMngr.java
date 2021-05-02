package eu.battleland.revoken.serverside.game;

import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.abstracted.AMngr;
import eu.battleland.revoken.common.diagnostics.timings.Timer;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.controllers.VoteController;
import eu.battleland.revoken.serverside.game.controllers.security.AdminController;
import eu.battleland.revoken.serverside.game.controllers.security.BattleRepController;
import eu.battleland.revoken.serverside.game.controllers.uxui.ChatController;
import eu.battleland.revoken.serverside.game.controllers.uxui.InterfaceController;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2(topic = "Revoken - ControllerMngr")
public class ControllerMngr extends AMngr<RevokenPlugin, AController<RevokenPlugin>> {
    /**
     * Default constructor
     *
     * @param plugin Plugin instance
     */
    public ControllerMngr(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    /**
     * Constructs and Initializes bound Controllers
     */
    public void initialize() {
        Timer timer = Timer.timings().start();

        registerComponents(
                new InterfaceController(getPlugin()),
                new ChatController(getPlugin()),
                new VoteController(getPlugin()),
                new BattleRepController(getPlugin()),
                new AdminController(getPlugin())
        );

        log.info("Constructing and Initializing Controllers");
        callForComponents((clazz, instance) -> {
            try {
                instance.initialize();
                log.debug("Initialized controller '§e{}§r'", clazz.getSimpleName());
            } catch (Exception x) {
                log.error("Failed to initialize controller '§e{}§r'", clazz.getSimpleName(), x);
            }
        });
        log.info("Constructed and Initialized Controllers in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));

    }

    /**
     * Terminates bound Controllers
     */
    public void terminate() {
        Timer timer = Timer.timings().start();
        log.info("Terminating Controllers");
        callForComponents((clazz, instance) -> {
            try {
                instance.terminate();
                log.debug("Terminated controller '§e{}§r'", clazz.getSimpleName());
            } catch (Exception x) {
                log.error("Failed to terminate controller '§e{}§r'", clazz.getSimpleName(), x);
            }
        });
        log.info("Terminated Controllers in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }

    public void reload() {
        Timer timer = Timer.timings().start();

        log.info("Reloading Controllers");
        callForComponents((clazz, instance) -> {
            try {
                instance.reload();
            } catch (Exception x) {
                log.error("Failed to reload controller '§e{}§r'", clazz.getSimpleName(), x);
            }
        });
        log.info("Reloaded Controllers in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }
}
