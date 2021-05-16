package eu.battleland.revoken.common.providers.storage.data.codec.impl.ex;

import eu.battleland.revoken.common.providers.storage.data.codec.meta.CodecField;
import org.jetbrains.annotations.NotNull;

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
