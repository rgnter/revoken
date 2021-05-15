package eu.battleland.revoken.common.providers.storage.flatfile.data.codec;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.CommonClassMapper;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.CommonTransformer;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex.CodecException;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecField;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.spec.EncodedKeySpec;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2(topic = "AuxCodec")
public class AuxCodec {

    public static final ClassMapper COMMON_CLASS_MAPPER = new CommonClassMapper();
    public static final Transformer COMMON_TRANSFORMER = new CommonTransformer();

    /***
     *
     * @param toEncode Object to encode
     * @param data Data
     */
    public static void encode(@NotNull ICodec toEncode, @NotNull AuxData data) throws Exception {
        for (final Field field :
                toEncode.defaultClassMapper().getClassCodecFields(toEncode.type(), toEncode)
                        .filter(field -> !Modifier.isTransient(field.getModifiers()))
                        .filter(field -> field.isAnnotationPresent(CodecKey.class)).collect(Collectors.toList())) {
            field.setAccessible(true);

            CodecKey cKey = field.getDeclaredAnnotation(CodecKey.class);
            if (cKey == null)
                return;

            try {
                CodecValue cVal = CodecValue.builder()
                        .type(field.getType())
                        .value(field.get(toEncode))
                        .build();
                CodecField cField = CodecField.builder()
                        .fieldName(field.getName())
                        .key(cKey)
                        .value(cVal)
                        .build();

                toEncode.defaultTransformer().encode(cField, data);
            } catch (Exception e) {
                throw new Exception("Couldn't decode class " + toEncode.type().getName() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Decodes data from {@code data} object, to this object
     *
     * @param data Data
     */
    public static void decode(@NotNull ICodec toDecode, @NotNull AuxData data) throws Exception {
        final var transformer = toDecode.defaultTransformer();
        if(transformer == null)
            throw new Exception("Transformer not present");
        final var mapper = toDecode.defaultClassMapper();
        if(mapper == null)
            throw new Exception("Mapper not present");
        if(data == null)
            throw new Exception("Data is null");

        for (final Field field :
                mapper.getClassCodecFields(toDecode.type(), toDecode)
                        .filter(field -> !Modifier.isTransient(field.getModifiers()))
                        .filter(field -> field.isAnnotationPresent(CodecKey.class)).collect(Collectors.toList())) {
            field.setAccessible(true);

            CodecKey cKey = field.getDeclaredAnnotation(CodecKey.class);
            if (cKey == null)
                return;

            try {
                CodecValue cVal = CodecValue.builder()
                        .type(field.getType())
                        .value(field.get(toDecode))
                        .build();
                CodecField cField = CodecField.builder()
                        .fieldName(field.getName())
                        .key(cKey)
                        .value(cVal)
                        .build();
                transformer.decode(cField, data);

                field.set(toDecode, cField.getValue().getValue());
            } catch (Exception e) {
                throw new Exception("Couldn't decode class " + toDecode.type().getName() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * ClassMapper handles field and key mapping
     */
    public static abstract class ClassMapper {
        public abstract @NotNull Stream<@NotNull Field> getClassCodecFields(@NotNull Class<?> clazz, @NotNull Object object);
    }

    /**
     * Transformer handles transformation between data and fields
     */
    public static abstract class Transformer {
        public abstract void encode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException;

        public abstract void decode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException;
    }

}
