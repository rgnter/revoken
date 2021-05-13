package eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CodecKey {
    @NotNull String value();
}
