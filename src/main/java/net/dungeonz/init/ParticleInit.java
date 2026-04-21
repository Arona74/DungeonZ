package net.dungeonz.init;

import net.dungeonz.particle.DungeonPortalParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ParticleInit {

    public static final ParticleType<DungeonPortalParticleEffect> DUNGEON_PORTAL_PARTICLE =
            DungeonPortalParticleEffect.TYPE;

    public static final ParticleType<DungeonPortalParticleEffect> DUNGEON_REVERSE_PORTAL_PARTICLE =
            DungeonPortalParticleEffect.REVERSE_TYPE;

    public static void init() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("dungeonz", "dungeon_portal_particle"), DUNGEON_PORTAL_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("dungeonz", "dungeon_reverse_portal_particle"), DUNGEON_REVERSE_PORTAL_PARTICLE);
    }
}
