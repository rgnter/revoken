package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl;

import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.AuxCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

public class CommonClassMapper extends AuxCodec.ClassMapper {

    @Override
    public @NotNull Stream<@NotNull Field> getClassCodecFields(@NotNull Class<?> clazz, @NotNull Object object) {
        return Arrays.stream(clazz.getDeclaredFields());
    }
}
