package eu.battleland.revoken.common.providers.storage.flatfile.data.codec;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;

public interface ICodec {

    /**
     * @return Default type of this class
     */
    default Class<?> type() {
        return this.getClass();
    }

    /**
     * @return Default transformer for this codec
     */
    default AuxCodec.Transformer defaultTransformer() {
        return AuxCodec.COMMON_TRANSFORMER;
    }

    /**
     * @return Default class mapper for this codec
     */
    default AuxCodec.ClassMapper defaultClassMapper() {
        return AuxCodec.COMMON_CLASS_MAPPER;
    }

    /**
     * @return Default codec data format
     */
    default AuxData.Type dataType() {
        return AuxData.Type.YAML;
    }
}
