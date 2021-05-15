package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.AuxCodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex.CodecException;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecField;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import eu.battleland.revoken.common.util.ThrowingBiFunction;
import eu.battleland.revoken.common.util.ThrowingFunction;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CommonTransformer extends AuxCodec.Transformer {


    public final Map<Class<?>, ThrowingFunction<Object, Object, Exception>> encodeTransformers = new HashMap<>() {{
        put(Byte.class, Object::toString);
        put(byte.class, get(Byte.class));
        put(Short.class, Object::toString);
        put(short.class, get(Short.class));
        put(Integer.class, Object::toString);
        put(int.class, get(Integer.class));
        put(Long.class, Object::toString);
        put(long.class, get(Long.class));

        put(Float.class, Object::toString);
        put(float.class, get(Float.class));
        put(Double.class, Object::toString);
        put(double.class, get(Double.class));

        put(String.class, Object::toString);
        put(Boolean.class, Object::toString);
        put(boolean.class, get(Boolean.class));

        put(ICodec.class, (source) -> {
            final var target = (ICodec) source;
            final var dataType = target.dataType();
            final AuxData data = dataType.get().apply(null);
            data.encode(target);

            return dataType.isParsable() ? data.toString() : data;
        });
    }};

    public final Map<Class<?>, ThrowingBiFunction<Object, Object, Object, Exception>> decodeTransformers = new HashMap<>() {{

        put(Byte.class, (origin, source) -> {
            try {
                return Byte.parseByte(source.toString());
            } catch (Exception x) {
                throw new Exception("Required byte value but got '" + source.getClass().getName() + "'");
            }
        });
        put(byte.class, get(Byte.class));

        put(Short.class, (origin, source) -> {
            try {
                return Short.parseShort(source.toString());
            } catch (Exception x) {
                throw new Exception("Required short value but got '" + source.getClass().getName() + "'");
            }
        });
        put(short.class, get(Short.class));


        put(Integer.class, (origin, source) -> {
            try {
                return Integer.parseInt(source.toString());
            } catch (Exception x) {
                throw new Exception("Required integer value but got '" + source.getClass().getName() + "'");
            }
        });
        put(int.class, get(Integer.class));

        put(Long.class, (origin, source) -> {
            try {
                return Long.parseLong(source.toString());
            } catch (Exception x) {
                throw new Exception("Required long integer value but got '" + source.getClass().getName() + "'");
            }
        });
        put(long.class, get(Long.class));

        put(Boolean.class, (origin, source) -> {
            try {
                return Boolean.parseBoolean(source.toString());
            } catch (Exception x) {
                throw new Exception("Required boolean value but got '" + source.getClass().getName() + "'");
            }
        });
        put(boolean.class, get(Boolean.class));

        put(String.class, (origin, source) -> {
            return source;
        });

        put(ICodec.class, (origin, source) -> {
            final var target = (ICodec) origin;
            final var dataType = target.dataType();

            AuxData data;
            if(dataType.isParsable()) {
                data = dataType.get().apply((String) source);
            } else
                data = dataType.get().apply((ConfigurationSection) source);

            data.decode(target);

            return target;
        });
    }};


    @Override
    public void encode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException {
        final String key = codecField.getKey().value();
        final Class<?> type = codecField.getValue().getType();
        final Object value = codecField.getValue().getValue();


        if (!this.encodeTransformers.containsKey(type))
            throw new CodecException("Couldn't find suitable encode transformer for type class " + type.getName(), codecField);

        final var transformer = this.encodeTransformers.get(type);
        try {
            data.set(key, transformer.apply(value));
        } catch (Exception e) {
            throw new CodecException("Failed to encode field", e, codecField);
        }
    }

    @Override
    public void decode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException {
        final String key = codecField.getKey().value();
        final Class<?> type;
        final Object value = codecField.getValue().getValue();

        if(ICodec.class.isAssignableFrom(codecField.getValue().getType()))
            type = ICodec.class;
        else
            type = codecField.getValue().getType();

        final var transformer = this.decodeTransformers.get(type);
        if (transformer == null)
            throw new CodecException("Couldn't find suitable decode transformer for class field", codecField);

        Object source;
        // if codec field is ICodec
        if(type.equals(ICodec.class)) {

            // require default value for codec field
            if(value == null)
                throw new CodecException("Specify default value (transformer can not deduce class fields of specified codec)", codecField);

            // is source parsable or data
            if(!((ICodec) value).dataType().isParsable())
                source = data.getSector(key);
            else
                source = data.getString(key);
        }
        else
            source = data.getString(key);

        if (source == null)
            throw new CodecException("Missing codec key in data", codecField);
        try {
            codecField.getValue().setValue(transformer.apply(value, source));
        } catch (Exception e) {
            throw new CodecException("Failed to decode field", e, codecField);
        }

    }


}
