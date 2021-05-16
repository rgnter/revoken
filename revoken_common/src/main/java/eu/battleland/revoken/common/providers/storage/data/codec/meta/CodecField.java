package eu.battleland.revoken.common.providers.storage.data.codec.meta;

import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a member field from any {@link ICodec} class
 */
@Builder @Getter
public class CodecField {

    private final String fieldName;
    private final CodecKey key;
    private final CodecValue value;

}
