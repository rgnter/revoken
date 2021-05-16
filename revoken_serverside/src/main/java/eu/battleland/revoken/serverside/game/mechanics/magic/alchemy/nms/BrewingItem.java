package eu.battleland.revoken.serverside.game.mechanics.magic.alchemy.nms;

import net.minecraft.server.v1_16_R3.EntityItem;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.World;

public class BrewingItem extends EntityItem {

    public BrewingItem(EntityTypes<? extends EntityItem> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    public void tick() {

    }
}
