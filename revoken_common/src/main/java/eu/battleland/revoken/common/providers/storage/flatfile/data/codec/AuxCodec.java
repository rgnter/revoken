package eu.battleland.revoken.common.providers.storage.flatfile.data.codec;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.CommonClassMapper;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.CommonTransformer;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex.CodecException;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

            CodecKey key = field.getDeclaredAnnotation(CodecKey.class);
            if (key == null)
                return;

            try {
                CodecValue val = CodecValue.builder().type(field.getType()).value(field.get(toEncode)).build();

                toEncode.defaultTransformer().encode(key, val, data);
            } catch (Exception e) {
                throw new Exception("Couldn't decode: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Decodes data from {@code data} object, to this object
     *
     * @param data Data
     */
    public static void decode(@NotNull ICodec toDecode, @NotNull AuxData data) throws Exception {
        for (final Field field :
                toDecode.defaultClassMapper().getClassCodecFields(toDecode.type(), toDecode)
                        .filter(field -> !Modifier.isTransient(field.getModifiers()))
                        .filter(field -> field.isAnnotationPresent(CodecKey.class)).collect(Collectors.toList())) {
            field.setAccessible(true);

            CodecKey key = field.getDeclaredAnnotation(CodecKey.class);
            if (key == null)
                return;

            try {
                CodecValue val = CodecValue.builder().type(field.getType()).value(field.get(toDecode)).build();

                toDecode.defaultTransformer().decode(key, val, data);
                field.set(toDecode, val.getValue());
            } catch (Exception e) {
                throw new Exception("Couldn't decode: " + e.getMessage(), e);
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
        public abstract void encode(@NotNull CodecKey codecKey, @NotNull CodecValue codecValue, @NotNull AuxData data) throws CodecException;

        public abstract void decode(@NotNull CodecKey codecKey, @NotNull CodecValue codecValue, @NotNull AuxData data) throws CodecException;
    }

}
