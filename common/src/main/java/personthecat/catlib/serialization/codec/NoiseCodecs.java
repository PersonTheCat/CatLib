package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import personthecat.fastnoise.data.*;

import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

public class NoiseCodecs {
    public static final Codec<NoiseType> TYPE = ofEnum(NoiseType.class);
    public static final Codec<FractalType> FRACTAL = ofEnum(FractalType.class);
    public static final Codec<DomainWarpType> WARP = ofEnum(DomainWarpType.class);
    public static final Codec<CellularDistanceType> DISTANCE = ofEnum(CellularDistanceType.class);
    public static final Codec<CellularReturnType> RETURN = ofEnum(CellularReturnType.class);
    public static final Codec<MultiType> MULTI = ofEnum(MultiType.class);
}
