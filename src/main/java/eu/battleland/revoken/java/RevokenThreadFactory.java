package eu.battleland.revoken.java;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class RevokenThreadFactory implements ThreadFactory {

    public Thread newTickableThread(int id, @NotNull Runnable r) {
        return new Thread(r, "RevokenTickable #" + id);
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        return null;
    }

}
