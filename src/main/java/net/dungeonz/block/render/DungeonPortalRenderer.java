package net.dungeonz.block.render;

import net.dungeonz.block.DungeonPortalBlock;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.ConfigInit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/**
 * Renders dungeon portal blocks with the vanilla end-portal visual.
 *
 * Two render paths:
 *  - No shaders: RenderLayer.getEndPortal() — position-only vertices, full end-portal shader.
 *  - Shaders active (Iris/OptiFine): PortalLayerHelper fallback layer using
 *    rendertype_entity_translucent, which shader packs handle correctly.
 *    Emits position + color + texture + overlay + light + normal vertices.
 */
@Environment(EnvType.CLIENT)
public class DungeonPortalRenderer extends EndPortalBlockEntityRenderer<DungeonPortalEntity> {

    private static final Identifier PORTAL_TEXTURE =
            Identifier.of("minecraft", "textures/entity/end_portal.png");

    private static final RenderLayer FALLBACK_LAYER =
            PortalLayerHelper.createPortalLayer(PORTAL_TEXTURE);

    public DungeonPortalRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    private static boolean areShadersActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void render(DungeonPortalEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!ConfigInit.CONFIG.customPortalRendering) return;

        World world = entity.getWorld();
        if (world == null) return;

        BlockState state = world.getBlockState(entity.getPos());
        if (!state.contains(DungeonPortalBlock.AXIS)) return;

        boolean axisX   = state.get(DungeonPortalBlock.AXIS) == Direction.Axis.X;
        boolean solo    = state.get(DungeonPortalBlock.SOLO);
        boolean shaders = areShadersActive();

        float near = solo ? 0.0f : 0.375f;
        float far  = solo ? 1.0f : 0.625f;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (shaders) {
            VertexConsumer consumer = vertexConsumers.getBuffer(FALLBACK_LAYER);
            if (solo) {
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f,  0f,  0f,  1f);
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f,  0f,  0f, -1f);
                renderFaceFull(consumer, matrix, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f,  1f,  0f,  0f);
                renderFaceFull(consumer, matrix, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, -1f,  0f,  0f);
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f,  0f, -1f,  0f);
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 0f,  0f,  1f,  0f);
            } else if (axisX) {
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 1f, far,  far,  far,  far,  0f, 0f,  1f);
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 0f, near, near, near, near, 0f, 0f, -1f);
            } else {
                renderFaceFull(consumer, matrix, near, near, 0f, 1f, 0f, 1f, 1f, 0f, -1f, 0f, 0f);
                renderFaceFull(consumer, matrix, far,  far,  1f, 0f, 0f, 1f, 1f, 0f,  1f, 0f, 0f);
            }
        } else {
            VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEndPortal());
            if (solo) {
                renderFace(consumer, matrix, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f);
                renderFace(consumer, matrix, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f);
                renderFace(consumer, matrix, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f);
                renderFace(consumer, matrix, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f);
                renderFace(consumer, matrix, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f);
                renderFace(consumer, matrix, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 0f);
            } else if (axisX) {
                renderFace(consumer, matrix, 0f, 1f, 0f, 1f, far,  far,  far,  far);
                renderFace(consumer, matrix, 0f, 1f, 1f, 0f, near, near, near, near);
            } else {
                renderFace(consumer, matrix, near, near, 0f, 1f, 0f, 1f, 1f, 0f);
                renderFace(consumer, matrix, far,  far,  1f, 0f, 0f, 1f, 1f, 0f);
            }
        }

        matrices.pop();
    }

    private static void renderFace(VertexConsumer consumer, Matrix4f matrix,
                                   float x0, float x1, float y0, float y1,
                                   float z0, float z1, float z2, float z3) {
        consumer.vertex(matrix, x0, y0, z0);
        consumer.vertex(matrix, x1, y0, z1);
        consumer.vertex(matrix, x1, y1, z2);
        consumer.vertex(matrix, x0, y1, z3);
    }

    private static void renderFaceFull(VertexConsumer consumer, Matrix4f matrix,
                                       float x0, float x1, float y0, float y1,
                                       float z0, float z1, float z2, float z3,
                                       float nx, float ny, float nz) {
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int noOverlay = OverlayTexture.DEFAULT_UV;
        consumer.vertex(matrix, x0, y0, z0).color(255,255,255,255).texture(0f,0f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz);
        consumer.vertex(matrix, x1, y0, z1).color(255,255,255,255).texture(1f,0f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz);
        consumer.vertex(matrix, x1, y1, z2).color(255,255,255,255).texture(1f,1f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz);
        consumer.vertex(matrix, x0, y1, z3).color(255,255,255,255).texture(0f,1f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz);
    }

    @Override
    protected float getTopYOffset() {
        return 1.0f;
    }

    @Override
    protected float getBottomYOffset() {
        return 0.0f;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}
