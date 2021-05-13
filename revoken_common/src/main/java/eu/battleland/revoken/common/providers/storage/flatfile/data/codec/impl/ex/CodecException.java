package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex;

import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class CodecException extends Exception {

    private @NotNull CodecKey codecKey;
    private @NotNull CodecValue codecObject;

    public CodecException(String message, Throwable cause, @NotNull CodecKey codecKey, @NotNull CodecValue codecObject) {
        super(message, cause);
        this.codecKey = codecKey;
        this.codecObject = codecObject;
    }

    public CodecException(String message, @NotNull CodecKey codecKey, @NotNull CodecValue codecObject) {
        super(message);
        this.codecKey = codecKey;
        this.codecObject = codecObject;
    }

    @Override
    public String getMessage() {
        return String.format("Codec failed on key '%s' of type '%s': %s%s", codecKey.value(), codecObject.getType().getName(), super.getMessage(), getCause() != null ? " | Cause: " + getCause().getMessage() : "");
    }
}
