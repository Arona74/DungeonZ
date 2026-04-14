package net.dungeonz.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class DungeonPortalParticle extends SpriteBillboardParticle {

    // Destination point the particle converges toward
    private final double startX;
    private final double startY;
    private final double startZ;

    protected DungeonPortalParticle(ClientWorld world, double x, double y, double z,
                                    double vx, double vy, double vz,
                                    float r, float g, float b) {
        super(world, x, y, z);

        // velocityX/Y/Z repurposed as displacement from destination, matching vanilla PortalParticle
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;

        // Reset position to exact destination (super() may have added jitter)
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        this.startX = x;
        this.startY = y;
        this.startZ = z;

        this.red = r;
        this.green = g;
        this.blue = b;

        this.scale = 0.1f * (this.random.nextFloat() * 0.2f + 0.5f);
        // Vanilla lifetime: (int)(Math.random() * 10) + 40  → 40-49 ticks
        this.maxAge = 40 + this.random.nextInt(10);
        this.collidesWithWorld = false;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    /**
     * Size grows from 0 to scale as the particle approaches its destination.
     * Formula: scale * (1 - (1-t)²) = scale * t*(2-t)
     * Exactly matches vanilla PortalParticle.getSize().
     */
    @Override
    public float getSize(float tickDelta) {
        float t = ((float) this.age + tickDelta) / (float) this.maxAge;
        float invT = 1.0f - t;
        return this.scale * (1.0f - invT * invT);
    }

    /**
     * Particles glow brighter as they near the portal, matching vanilla PortalParticle.getBrightness().
     * Sky-light is boosted by t⁴ * 240, capped at 240.
     */
    @Override
    public int getBrightness(float tickDelta) {
        int packed = super.getBrightness(tickDelta);
        float t = (float) this.age / (float) this.maxAge;
        t = t * t; // t²
        t = t * t; // t⁴
        int blockLight = packed & 0xFF;
        int skyLight = (packed >> 16) & 0xFF;
        skyLight += (int) (t * 15.0f * 16.0f);
        if (skyLight > 240) skyLight = 240;
        return blockLight | (skyLight << 16);
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
        float f2 = t; // raw linear progress

        // Portal easing: 1 + t - 2t² — peaks ~1.125 at t≈0.25, falls to 0 at t=1
        // Exactly matches vanilla PortalParticle.tick()
        float f1 = 1.0f + t - 2.0f * t * t;

        this.x = this.startX + this.velocityX * f1;
        this.y = this.startY + this.velocityY * f1 + (1.0 - f2);
        this.z = this.startZ + this.velocityZ * f1;
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
            DungeonPortalParticle p = new DungeonPortalParticle(world, x, y, z, vx, vy, vz,
                    effect.getR(), effect.getG(), effect.getB());
            // setSprite picks one random sprite for the whole lifetime — matches vanilla portal factory
            p.setSprite(spriteProvider);
            return p;
        }
    }
}
