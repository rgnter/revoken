package eu.battleland.revoken.common;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;

public interface Revoken<T> {

    public T instance();

    @NotNull InputStream getResource(@NotNull String resourcePath);

    @NotNull File getDataFolder();
}
