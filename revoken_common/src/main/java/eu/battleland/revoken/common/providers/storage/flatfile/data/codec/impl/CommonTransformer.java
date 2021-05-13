package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.AuxCodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex.CodecException;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecValue;
import eu.battleland.revoken.common.util.ThrowingFunction;
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
            AuxCodec.encode(target, target.defaultDataFormat());
            return source;
        });
    }};

    public final Map<Class<?>, ThrowingFunction<Object, Object, Exception>> decodeTransformers = new HashMap<>() {{

        put(Byte.class, (source) -> {
            try {
                return Byte.parseByte(source.toString());
            } catch (Exception x) {
                throw new Exception("Required byte value");
            }
        });
        put(byte.class, get(Byte.class));

        put(Short.class, (source) -> {
            try {
                return Short.parseShort(source.toString());
            } catch (Exception x) {
                throw new Exception("Required short value");
            }
        });
        put(short.class, get(Short.class));


        put(Integer.class, (source) -> {
            try {
                return Integer.parseInt(source.toString());
            } catch (Exception x) {
                throw new Exception("Required integer value");
            }
        });
        put(int.class, get(Integer.class));

        put(Long.class, (source) -> {
            try {
                return Long.parseLong(source.toString());
            } catch (Exception x) {
                throw new Exception("Required long integer value");
            }
        });
        put(long.class, get(Long.class));

        put(Boolean.class, (source) -> {
            try {
                return Boolean.parseBoolean(source.toString());
            } catch (Exception x) {
                throw new Exception("Required boolean value");
            }
        });
        put(boolean.class, get(Boolean.class));

        put(String.class, (source) -> {
            return source;
        });

        put(ICodec.class, (source) -> {
            final var target = ICodec.class.getConstructor().newInstance().instance();
            AuxCodec.decode(target, (AuxData) source);
            return target;
        });
    }};


    @Override
    public void encode(@NotNull CodecKey codecKey, @NotNull CodecValue codecValue, @NotNull AuxData data) throws CodecException {
        final Class<?> type = codecValue.getType();

        if (!this.encodeTransformers.containsKey(type))
            throw new CodecException("Couldn't find suitable encode transformer for type class " + type.getName(), codecKey, codecValue);

        final var transformer = this.encodeTransformers.get(type);
        try {
            data.set(codecKey.value(), transformer.apply(codecValue.getValue()));
        } catch (Exception e) {
            throw new CodecException("Failed to encode", e, codecKey, codecValue);
        }
    }

    @Override
    public void decode(@NotNull CodecKey codecKey, @NotNull CodecValue codecValue, @NotNull AuxData data) throws CodecException {
        Class<?> type = codecValue.getType();

        if (!this.decodeTransformers.containsKey(type))
            throw new CodecException("Couldn't find suitable decode transformer", codecKey, codecValue);

        if(type.isInstance(ICodec.class))
            type = ICodec.class;

        final var transformer = this.decodeTransformers.get(type);
        Object source;
        if(type.equals(ICodec.class))
            source = data.getSector(codecKey.value());
        else
            source = data.getString(codecKey.value());

        if (source == null)
            throw new CodecException("Missing key in data", codecKey, codecValue);
        try {
            codecValue.setValue(transformer.apply(source));
        } catch (Exception e) {
            throw new CodecException("Failed to encode", e, codecKey, codecValue);
        }

    }


}
