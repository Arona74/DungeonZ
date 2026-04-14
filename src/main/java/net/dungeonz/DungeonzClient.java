package net.dungeonz;

import net.dungeonz.init.ParticleInit;
import net.dungeonz.init.RenderInit;
import net.dungeonz.network.DungeonClientPacket;
import net.dungeonz.particle.DungeonPortalParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

@Environment(EnvType.CLIENT)
public class DungeonzClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DungeonClientPacket.init();
        RenderInit.init();
        ParticleFactoryRegistry.getInstance().register(ParticleInit.DUNGEON_PORTAL_PARTICLE, DungeonPortalParticle.Factory::new);
    }

}
