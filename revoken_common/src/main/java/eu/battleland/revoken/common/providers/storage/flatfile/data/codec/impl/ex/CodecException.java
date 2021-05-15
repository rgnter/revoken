package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex;

import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecField;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class CodecException extends Exception {

    private @NotNull CodecField codecField;

    public CodecException(String message, Throwable cause, @NotNull CodecField codecField) {
        super(message, cause);
        this.codecField = codecField;
    }

    public CodecException(String message, @NotNull CodecField codecField) {
        super(message);
        this.codecField = codecField;
    }

    public String getCodecKey() {
        return codecField.getKey().value();
    }

    public Class<?> getCodecValueType() {
        return codecField.getValue().getType();
    }

    public Object getCodecValue() {
        return codecField.getValue().getValue();
    }

    public String getCodecFieldName() {
        return codecField.getFieldName();
    }


    @Override
    public String getMessage() {
        return String.format("Codec failed on field %s with key '%s' of type '%s': %s%s", getCodecFieldName(), getCodecKey(), getCodecValueType().getName(), super.getMessage(), getCause() != null ? " | Cause: " + getCause().getMessage() : "");
    }
}
