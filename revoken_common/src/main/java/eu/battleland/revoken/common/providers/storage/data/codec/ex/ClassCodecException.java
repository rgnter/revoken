package eu.battleland.revoken.common.providers.storage.data.codec.ex;

import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Member;

public class ClassCodecException extends Exception {

    private ICodec codec;


    public ClassCodecException(ICodec codec, @NotNull Throwable x) {
        this("", codec, x);
    }

    public ClassCodecException(@NotNull String message, ICodec codec, @NotNull Throwable x) {
        super(message, x);
        this.codec = codec;
    }
    public ClassCodecException(@NotNull String message, ICodec codec) {
        super(message);
        this.codec = codec;
    }


    @Override
    public String getMessage() {
        return "Failed to perform codec operation on class '" + codec.getClass().getName() + "'" + (getCause() != null ? ": " + getCause().getMessage() : "");
    }
}
