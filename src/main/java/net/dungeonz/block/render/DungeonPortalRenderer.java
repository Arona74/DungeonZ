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
 *
 * Vertex winding for each face mirrors vanilla EndPortalBlockEntityRenderer.renderSide()
 * so the shader receives a consistent surface orientation.
 */
@Environment(EnvType.CLIENT)
public class DungeonPortalRenderer extends EndPortalBlockEntityRenderer<DungeonPortalEntity> {

    private static final Identifier PORTAL_TEXTURE =
            new Identifier("minecraft", "textures/entity/end_portal.png");

    // Fallback layer used when a shader pack is active.
    // Uses rendertype_entity_translucent, which all shader packs support.
    private static final RenderLayer FALLBACK_LAYER =
            PortalLayerHelper.createPortalLayer(PORTAL_TEXTURE);

    public DungeonPortalRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    // ---------------------------------------------------------------------------
    // Shader detection — uses reflection so Iris is not a hard dependency
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------------------

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

        // Solo (full-cube): faces span 0 → 1.
        // Multiblock (thin slab): faces sit at 0.375 → 0.625.
        float near = solo ? 0.0f : 0.375f;
        float far  = solo ? 1.0f : 0.625f;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (shaders) {
            VertexConsumer consumer = vertexConsumers.getBuffer(FALLBACK_LAYER);
            if (solo) {
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f,  0f,  0f,  1f);  // south
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f,  0f,  0f, -1f);  // north
                renderFaceFull(consumer, matrix, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f,  1f,  0f,  0f);  // east
                renderFaceFull(consumer, matrix, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, -1f,  0f,  0f);  // west
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f,  0f, -1f,  0f);  // down
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 0f,  0f,  1f,  0f);  // up
            } else if (axisX) {
                renderFaceFull(consumer, matrix, 0f, 1f, 0f, 1f, far,  far,  far,  far,  0f, 0f,  1f);  // south
                renderFaceFull(consumer, matrix, 0f, 1f, 1f, 0f, near, near, near, near, 0f, 0f, -1f);  // north
            } else {
                renderFaceFull(consumer, matrix, near, near, 0f, 1f, 0f, 1f, 1f, 0f, -1f, 0f, 0f);  // west
                renderFaceFull(consumer, matrix, far,  far,  1f, 0f, 0f, 1f, 1f, 0f,  1f, 0f, 0f);  // east
            }
        } else {
            VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEndPortal());
            if (solo) {
                renderFace(consumer, matrix, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f);  // south
                renderFace(consumer, matrix, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f);  // north
                renderFace(consumer, matrix, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f);  // east
                renderFace(consumer, matrix, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f);  // west
                renderFace(consumer, matrix, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f);  // down
                renderFace(consumer, matrix, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 0f);  // up
            } else if (axisX) {
                renderFace(consumer, matrix, 0f, 1f, 0f, 1f, far,  far,  far,  far);   // south
                renderFace(consumer, matrix, 0f, 1f, 1f, 0f, near, near, near, near);  // north
            } else {
                renderFace(consumer, matrix, near, near, 0f, 1f, 0f, 1f, 1f, 0f);  // west
                renderFace(consumer, matrix, far,  far,  1f, 0f, 0f, 1f, 1f, 0f);  // east
            }
        }

        matrices.pop();
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Position-only quad for RenderLayer.getEndPortal().
     * Signature matches vanilla EndPortalBlockEntityRenderer.renderSide:
     *   v0=(x0,y0,z0)  v1=(x1,y0,z1)  v2=(x1,y1,z2)  v3=(x0,y1,z3)
     */
    private static void renderFace(VertexConsumer consumer, Matrix4f matrix,
                                   float x0, float x1, float y0, float y1,
                                   float z0, float z1, float z2, float z3) {
        consumer.vertex(matrix, x0, y0, z0).next();
        consumer.vertex(matrix, x1, y0, z1).next();
        consumer.vertex(matrix, x1, y1, z2).next();
        consumer.vertex(matrix, x0, y1, z3).next();
    }

    /**
     * Full-data quad for the POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL fallback layer.
     * UV is mapped 0→1 across the quad surface; face is rendered fully bright and opaque.
     */
    private static void renderFaceFull(VertexConsumer consumer, Matrix4f matrix,
                                       float x0, float x1, float y0, float y1,
                                       float z0, float z1, float z2, float z3,
                                       float nx, float ny, float nz) {
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int noOverlay = OverlayTexture.DEFAULT_UV;
        consumer.vertex(matrix, x0, y0, z0).color(255,255,255,255).texture(0f,0f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz).next();
        consumer.vertex(matrix, x1, y0, z1).color(255,255,255,255).texture(1f,0f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz).next();
        consumer.vertex(matrix, x1, y1, z2).color(255,255,255,255).texture(1f,1f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz).next();
        consumer.vertex(matrix, x0, y1, z3).color(255,255,255,255).texture(0f,1f).overlay(noOverlay).light(fullLight).normal(nx,ny,nz).next();
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
