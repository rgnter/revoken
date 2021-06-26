package eu.battleland.revoken.serverside.game.mechanics.common.items;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import org.jetbrains.annotations.NotNull;

public class ItemsMechanic extends AMechanic<RevokenPlugin> {

    public ItemsMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
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
}
