package net.dungeonz.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import org.joml.Vector3f;

public class DungeonPortalParticleEffect implements ParticleEffect {

    // -----------------------------------------------------------------------
    // Forward (inward-converging) effect
    // -----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    public static final ParticleEffect.Factory<DungeonPortalParticleEffect> FACTORY = new ParticleEffect.Factory<>() {
        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' '); float r = reader.readFloat();
            reader.expect(' '); float g = reader.readFloat();
            reader.expect(' '); float b = reader.readFloat();
            return new DungeonPortalParticleEffect(r, g, b, false);
        }
        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, PacketByteBuf buf) {
            return new DungeonPortalParticleEffect(buf.readFloat(), buf.readFloat(), buf.readFloat(), false);
        }
    };

    public static final Codec<DungeonPortalParticleEffect> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(e -> e.r),
            Codec.FLOAT.fieldOf("g").forGetter(e -> e.g),
            Codec.FLOAT.fieldOf("b").forGetter(e -> e.b)
        ).apply(instance, (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, false))
    );

    public static final ParticleType<DungeonPortalParticleEffect> TYPE = new ParticleType<>(false, FACTORY) {
        @Override public Codec<DungeonPortalParticleEffect> getCodec() { return CODEC; }
    };

    // -----------------------------------------------------------------------
    // Reverse (outward-bursting) effect — same data, different type/factory
    // -----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    public static final ParticleEffect.Factory<DungeonPortalParticleEffect> REVERSE_FACTORY = new ParticleEffect.Factory<>() {
        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' '); float r = reader.readFloat();
            reader.expect(' '); float g = reader.readFloat();
            reader.expect(' '); float b = reader.readFloat();
            return new DungeonPortalParticleEffect(r, g, b, true);
        }
        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, PacketByteBuf buf) {
            return new DungeonPortalParticleEffect(buf.readFloat(), buf.readFloat(), buf.readFloat(), true);
        }
    };

    public static final Codec<DungeonPortalParticleEffect> REVERSE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(e -> e.r),
            Codec.FLOAT.fieldOf("g").forGetter(e -> e.g),
            Codec.FLOAT.fieldOf("b").forGetter(e -> e.b)
        ).apply(instance, (r, g, b) -> new DungeonPortalParticleEffect(r, g, b, true))
    );

    public static final ParticleType<DungeonPortalParticleEffect> REVERSE_TYPE = new ParticleType<>(false, REVERSE_FACTORY) {
        @Override public Codec<DungeonPortalParticleEffect> getCodec() { return REVERSE_CODEC; }
    };

    // -----------------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------------

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

    /** Creates a reverse-burst effect with the given color. */
    public static DungeonPortalParticleEffect reverse(Vector3f color) {
        return new DungeonPortalParticleEffect(color.x, color.y, color.z, true);
    }

    @Override
    public ParticleType<?> getType() {
        return reverse ? REVERSE_TYPE : TYPE;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    @Override
    public String asString() {
        ParticleType<?> type = reverse ? REVERSE_TYPE : TYPE;
        return Registries.PARTICLE_TYPE.getId(type) + " " + r + " " + g + " " + b;
    }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
}
