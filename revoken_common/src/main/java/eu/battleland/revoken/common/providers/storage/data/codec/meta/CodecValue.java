package eu.battleland.revoken.common.providers.storage.data.codec.meta;

import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a codec value for any {@link ICodec} class
 */
@Builder
public class CodecValue {

    @Getter
    private Class<?> type;
    @Getter @Setter
    private Object value;

}
