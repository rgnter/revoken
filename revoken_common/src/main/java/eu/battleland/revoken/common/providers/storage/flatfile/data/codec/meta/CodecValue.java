package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class CodecValue {

    @Getter
    private Class<?> type;
    @Getter @Setter
    private Object value;

}
