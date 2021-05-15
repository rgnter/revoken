package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta;

import lombok.Builder;
import lombok.Getter;

@Builder @Getter
public class CodecField {

    private final String fieldName;
    private final CodecKey key;
    private final CodecValue value;

}
