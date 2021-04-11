package eu.battleland.revoken.serverside.game;

import eu.battleland.common.Revoken;
import eu.battleland.common.abstracted.AMngr;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.common.diagnostics.timings.Timer;
import eu.battleland.revoken.serverside.game.controllers.VoteController;
import eu.battleland.revoken.serverside.game.controllers.report.ReportController;
import eu.battleland.revoken.serverside.game.controllers.uxui.ChatController;
import eu.battleland.revoken.serverside.game.controllers.uxui.InterfaceController;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2(topic = "Revoken - ControllerMngr")
public class ControllerMngr extends AMngr<RevokenPlugin> {

    private @NotNull InterfaceController interfaceController;
    private @NotNull ChatController      chatController;

    private @NotNull VoteController voteController;


    private @NotNull ReportController reportController;

    /**
     * Default constructor
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
        log.info("Constructing and Initializing Controllers");
        {
            this.interfaceController = new InterfaceController(this.getPlugin().instance());
            this.chatController      = new ChatController(this.getPlugin().instance());

            this.voteController      = new VoteController(this.getPlugin().instance());
            this.reportController    = new ReportController(this.getPlugin().instance());
        }
        {
            try {
                this.interfaceController.initialize();
            } catch (Exception e) {
                log.error("Failed to initialize InterfaceController", e);
            }
            try {
                this.chatController.initialize();
            } catch (Exception e) {
                log.error("Failed to initialize ChatController", e);
            }

            try {
                this.voteController.initialize();
            } catch (Exception e) {
                log.error("Failed to initialize VoteController", e);
            }
            try {
                this.reportController.initialize();
            } catch (Exception e) {
                log.error("Failed to initialize ReportController", e);
            }
        }
        log.info("Constructed and Initialized Controllers in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));


    }

    /**
     * Terminates bound Controllers
     */
    public void terminate() {
        Timer timer = Timer.timings().start();
        log.info("Terminating  Controllers");
        {
            try {
                this.reportController.terminate();
            } catch (Exception e) {
                log.error("Failed to terminate ReportController", e);
            }
            try {
                this.voteController.terminate();
            } catch (Exception e) {
                log.error("Failed to terminate VoteController", e);
            }

            try {
                this.interfaceController.terminate();
            } catch (Exception e) {
                log.error("Failed to terminate InterfaceController", e);
            }
            try {
                this.chatController.terminate();
            } catch (Exception e) {
                log.error("Failed to terminate ChatController", e);
            }
        }
        log.info("Terminated Controllers in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }

    public void reload() {
        this.interfaceController.reload();
        this.chatController.reload();
        this.voteController.reload();

        this.reportController.reload();
    }

}
