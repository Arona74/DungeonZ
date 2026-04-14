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

    @SuppressWarnings("deprecation")
    public static final ParticleEffect.Factory<DungeonPortalParticleEffect> FACTORY = new ParticleEffect.Factory<>() {
        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            return new DungeonPortalParticleEffect(r, g, b);
        }

        @Override
        public DungeonPortalParticleEffect read(ParticleType<DungeonPortalParticleEffect> type, PacketByteBuf buf) {
            return new DungeonPortalParticleEffect(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
    };

    public static final Codec<DungeonPortalParticleEffect> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(e -> e.r),
            Codec.FLOAT.fieldOf("g").forGetter(e -> e.g),
            Codec.FLOAT.fieldOf("b").forGetter(e -> e.b)
        ).apply(instance, DungeonPortalParticleEffect::new)
    );

    public static final ParticleType<DungeonPortalParticleEffect> TYPE = new ParticleType<>(false, FACTORY) {
        @Override
        public Codec<DungeonPortalParticleEffect> getCodec() {
            return CODEC;
        }
    };

    private final float r;
    private final float g;
    private final float b;

    public DungeonPortalParticleEffect(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public DungeonPortalParticleEffect(Vector3f color) {
        this(color.x, color.y, color.z);
    }

    @Override
    public ParticleType<?> getType() {
        return TYPE;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    @Override
    public String asString() {
        return Registries.PARTICLE_TYPE.getId(TYPE) + " " + r + " " + g + " " + b;
    }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
}
