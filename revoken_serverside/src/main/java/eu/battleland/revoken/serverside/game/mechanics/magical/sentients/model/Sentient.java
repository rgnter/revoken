package eu.battleland.revoken.serverside.game.mechanics.magical.sentients.model;

import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;


@Builder
public class Sentient {

    @Getter
    private final Component name;
    @Getter
    private final Entity underlyingEntity;




}
