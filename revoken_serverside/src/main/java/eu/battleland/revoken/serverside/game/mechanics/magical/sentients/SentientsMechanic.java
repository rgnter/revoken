package eu.battleland.revoken.serverside.game.mechanics.magical.sentients;

import com.destroystokyo.paper.entity.ai.Goal;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.magical.sentients.model.FollowPlayerGoal;
import net.minecraft.world.entity.monster.EntityZombie;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

public class SentientsMechanic extends AMechanic<RevokenPlugin>  {

    public SentientsMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getServer().getCommandMap().register("revoken", new Command("sentient") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String string, @NotNull String[] args) {
                Player player = (Player) sender;

                Zombie block = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
                Bukkit.getMobGoals().removeAllGoals(block);
                Bukkit.getMobGoals().addGoal(block, 1, new FollowPlayerGoal(block, player));

                return true;
            }
        });
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }
}
