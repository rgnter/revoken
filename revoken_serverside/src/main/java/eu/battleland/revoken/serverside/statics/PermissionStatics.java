package eu.battleland.revoken.serverside.statics;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PermissionStatics {

    public static @Nullable Permission permissionFromString(@Nullable String permissionStr, @NotNull PermissionDefault permissionDefault) {
        if(permissionStr == null)
            return null;
        if(permissionStr.trim().isEmpty())
            return null;

        Permission permission = Bukkit.getPluginManager().getPermission(permissionStr);
        if(permission == null) {
            permission = new Permission(permissionStr, permissionDefault);
            Bukkit.getPluginManager().addPermission(permission);
        }
        return permission;
    }

    public static @Nullable Permission permissionFromString(@Nullable String permissionStr) {
        return permissionFromString(permissionStr, PermissionDefault.OP);
    }
}
