package eu.battleland.revoken.common.providers.storage.flatfile.data.codec;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;

public interface ICodec {

    /**
     * @return Type of this class
     */
    Class<?> type();

    /**
     * @return Instance of this class
     */
    ICodec instance();

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
    default AuxData defaultDataFormat() {
        return AuxData.fromEmptyYaml();
    }
}
