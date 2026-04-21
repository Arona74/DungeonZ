package net.dungeonz.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class DungeonReversePortalParticle extends DungeonPortalParticle {

    protected DungeonReversePortalParticle(ClientWorld world,
                                           double x, double y, double z,
                                           double vx, double vy, double vz,
                                           float r, float g, float b) {
        super(world, x, y, z, vx, vy, vz, r, g, b);
        this.maxAge = 60 + this.random.nextInt(2);
    }

    @Override
    public float getSize(float tickDelta) {
        float t = ((float) this.age + tickDelta) / ((float) this.maxAge * 1.5f);
        return this.scale * (1.0f - t);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        float t = (float) this.age / (float) this.maxAge;
        this.x += this.velocityX * t;
        this.y += this.velocityY * t;
        this.z += this.velocityZ * t;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DungeonPortalParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DungeonPortalParticleEffect effect, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            DungeonReversePortalParticle p = new DungeonReversePortalParticle(
                    world, x, y, z, vx, vy, vz,
                    effect.getR(), effect.getG(), effect.getB());
            p.setSprite(spriteProvider);
            return p;
        }
    }
}
