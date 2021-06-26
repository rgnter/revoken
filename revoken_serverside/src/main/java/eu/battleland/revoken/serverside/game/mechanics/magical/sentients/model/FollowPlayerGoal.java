package eu.battleland.revoken.serverside.game.mechanics.magical.sentients.model;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class FollowPlayerGoal implements Goal<Zombie> {

    private final Mob mob;
    private final Player owner;
    public static final GoalKey<Zombie> KEY = GoalKey.of(Zombie.class, NamespacedKey.fromString("revoken:follow_player"));

    public FollowPlayerGoal(Mob mob, Player owner) {
        this.mob = mob;
        this.owner = owner;
    }

    @Override
    public void tick() {
        mob.setTarget(owner);
        mob.getPathfinder().moveTo(owner, 1.0D);
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public @NotNull GoalKey<Zombie> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.TARGET);
    }
}
