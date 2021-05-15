package eu.battleland.revoken.serverside.providers.data.codecs;

import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;

public class BlockStateCodec implements ICodec {
    @CodecKey("BlockEntityTag.Delay")
    public int delay = 20;
    @CodecKey("BlockEntityTag.MaxNearbyEntities")
    public int maxNearbyEntities = 6;
    @CodecKey("BlockEntityTag.MaxSpawnDelay")
    public int maxSpawnDelay = 800;
    @CodecKey("BlockEntityTag.MinSpawnDelay")
    public int minSpawnDelay = 200;
    @CodecKey("BlockEntityTag.RequiredPlayerRange")
    public int requiredPlayerRange = 16;
    @CodecKey("BlockEntityTag.SpawnCount")
    public int spawnCount = 4;
    @CodecKey("BlockEntityTag.SpawnRange")
    public int spawnRange = 4;

    @Override
    public AuxData.Type dataType() {
        return AuxData.Type.PARSABLE_JSON;
    }
}
