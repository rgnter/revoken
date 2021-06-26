package eu.battleland.revoken.serverside.game.mechanics.magical.alchemy.nms;


import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.World;

public class BrewingItem extends EntityItem {

    public BrewingItem(EntityTypes<? extends EntityItem> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    public void tick() {

    }
}
