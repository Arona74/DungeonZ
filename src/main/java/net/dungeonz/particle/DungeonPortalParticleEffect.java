package net.dungeonz.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import org.joml.Vector3f;

public class DungeonPortalParticleEffect implements ParticleEffect {

    // Forward (inward-converging) type
    public static final MapCodec<DungeonPortalParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(e -> e.r),
            Codec.FLOAT.fieldOf("g").forGetter(e -> e.g),
            Codec.FLOAT.fieldOf("b").forGetter(e -> e.b)
        ).apply(instance, (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, false))
    );

    public static final PacketCodec<RegistryByteBuf, DungeonPortalParticleEffect> PACKET_CODEC =
        PacketCodec.tuple(PacketCodecs.FLOAT, e -> e.r,
                          PacketCodecs.FLOAT, e -> e.g,
                          PacketCodecs.FLOAT, e -> e.b,
                          (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, false));

    public static final ParticleType<DungeonPortalParticleEffect> TYPE = new ParticleType<>(false) {
        @Override public MapCodec<DungeonPortalParticleEffect> getCodec() { return CODEC; }
        @Override public PacketCodec<? super RegistryByteBuf, DungeonPortalParticleEffect> getPacketCodec() { return PACKET_CODEC; }
    };

    // Reverse (outward-bursting) type
    public static final MapCodec<DungeonPortalParticleEffect> REVERSE_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(e -> e.r),
            Codec.FLOAT.fieldOf("g").forGetter(e -> e.g),
            Codec.FLOAT.fieldOf("b").forGetter(e -> e.b)
        ).apply(instance, (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, true))
    );

    public static final PacketCodec<RegistryByteBuf, DungeonPortalParticleEffect> REVERSE_PACKET_CODEC =
        PacketCodec.tuple(PacketCodecs.FLOAT, e -> e.r,
                          PacketCodecs.FLOAT, e -> e.g,
                          PacketCodecs.FLOAT, e -> e.b,
                          (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, true));

    public static final ParticleType<DungeonPortalParticleEffect> REVERSE_TYPE = new ParticleType<>(false) {
        @Override public MapCodec<DungeonPortalParticleEffect> getCodec() { return REVERSE_CODEC; }
        @Override public PacketCodec<? super RegistryByteBuf, DungeonPortalParticleEffect> getPacketCodec() { return REVERSE_PACKET_CODEC; }
    };

    private final float r;
    private final float g;
    private final float b;
    private final boolean reverse;

    public DungeonPortalParticleEffect(float r, float g, float b) {
        this(r, g, b, false);
    }

    private DungeonPortalParticleEffect(float r, float g, float b, boolean reverse) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.reverse = reverse;
    }

    public DungeonPortalParticleEffect(Vector3f color) {
        this(color.x, color.y, color.z, false);
    }

    public static DungeonPortalParticleEffect reverse(Vector3f color) {
        return new DungeonPortalParticleEffect(color.x, color.y, color.z, true);
    }

    @Override
    public ParticleType<?> getType() {
        return reverse ? REVERSE_TYPE : TYPE;
    }

    public String asString() {
        ParticleType<?> type = reverse ? REVERSE_TYPE : TYPE;
        return Registries.PARTICLE_TYPE.getId(type) + " " + r + " " + g + " " + b;
    }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
}
