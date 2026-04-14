package net.dungeonz.block.render;

import net.dungeonz.block.DungeonPortalBlock;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.ConfigInit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class DungeonPortalRenderer extends EndPortalBlockEntityRenderer<DungeonPortalEntity> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/end_portal.png");

    // No depth write + backface culling disabled so every layer is visible from both sides.
    // Built via PortalLayerHelper (a RenderLayer subclass) to access protected RenderPhase members.
    private static final RenderLayer PORTAL_LAYER = PortalLayerHelper.createPortalLayer(TEXTURE);

    // UV scale per layer: inner layers are very zoomed-in, outer layers show the full texture.
    // This wide range is what creates the "infinite depth" parallax illusion.
    private static final float[] LAYER_UV_SCALES = {
        0.05f, 0.10f, 0.16f, 0.24f, 0.33f, 0.43f, 0.54f, 0.65f, 0.75f, 0.84f, 0.92f, 1.00f,
    };

    // Scroll speed per layer (UV units per tick). Reduced ~10x from the original values for
    // a slow, subtle drift rather than a fast spin.
    private static final float[] LAYER_SPEEDS = {
        0.00080f, 0.00065f, 0.00052f, 0.00041f,
        0.00032f, 0.00025f, 0.00020f, 0.00024f,
        0.00030f, 0.00037f, 0.00045f, 0.00054f,
    };

    // Dungeon portal: void black → deep crimson → bright orange glow [r, g, b, a]
    public static final float[][] COLORS_DUNGEON_PORTAL = {
        {0.00f, 0.00f, 0.00f, 1.00f},  // solid void (opaque base)
        {0.15f, 0.00f, 0.12f, 0.90f},  // dark purple
        {0.30f, 0.00f, 0.10f, 0.80f},  // purple-red
        {0.50f, 0.02f, 0.05f, 0.70f},  // dark crimson
        {0.70f, 0.05f, 0.02f, 0.60f},  // crimson
        {0.85f, 0.10f, 0.00f, 0.50f},  // bright red
        {0.95f, 0.20f, 0.00f, 0.42f},  // red-orange
        {1.00f, 0.35f, 0.00f, 0.35f},  // orange-red
        {1.00f, 0.55f, 0.05f, 0.25f},  // orange
        {1.00f, 0.70f, 0.15f, 0.18f},  // bright orange
        {1.00f, 0.85f, 0.40f, 0.12f},  // yellow-orange glow
        {1.00f, 0.95f, 0.75f, 0.08f},  // near-white outer glow
    };

    // Super portal: void black → deep blue → teal → bright gold glow [r, g, b, a]
    public static final float[][] COLORS_SUPER_PORTAL = {
        {0.00f, 0.00f, 0.00f, 1.00f},  // solid void (opaque base)
        {0.00f, 0.05f, 0.20f, 0.90f},  // deep blue
        {0.00f, 0.12f, 0.38f, 0.80f},  // dark blue
        {0.00f, 0.22f, 0.50f, 0.70f},  // blue
        {0.05f, 0.35f, 0.45f, 0.60f},  // teal-blue
        {0.15f, 0.50f, 0.32f, 0.50f},  // teal
        {0.30f, 0.58f, 0.15f, 0.42f},  // green-teal
        {0.52f, 0.62f, 0.05f, 0.35f},  // yellow-green
        {0.72f, 0.64f, 0.00f, 0.25f},  // gold
        {0.88f, 0.72f, 0.05f, 0.18f},  // bright gold
        {1.00f, 0.84f, 0.22f, 0.12f},  // yellow-gold
        {1.00f, 0.96f, 0.68f, 0.08f},  // near-white gold glow
    };

    private final float[][] layerColors;

    public DungeonPortalRenderer(BlockEntityRendererFactory.Context ctx, float[][] layerColors) {
        super(ctx);
        this.layerColors = layerColors;
    }

    @Override
    public void render(DungeonPortalEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!ConfigInit.CONFIG.customPortalRendering) return;

        World world = entity.getWorld();
        if (world == null) return;

        float animTime = (world.getTime() % 100000L) + tickDelta;

        // Guard against block being replaced by air during the render frame (e.g. when breaking)
        net.minecraft.block.BlockState state = world.getBlockState(entity.getPos());
        if (!state.contains(DungeonPortalBlock.AXIS)) return;
        boolean axisX = state.get(DungeonPortalBlock.AXIS) == Direction.Axis.X;

        // Solo block → layers span the full cube face (0.99 → 0.01).
        // Multi-block → thin slab range (0.625 → 0.375).
        boolean solo = state.get(DungeonPortalBlock.SOLO);
        float depthFar  = solo ? 0.99f : 0.625f;
        float depthSpan = solo ? 0.98f : 0.25f;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer consumer = vertexConsumers.getBuffer(PORTAL_LAYER);

        int n = layerColors.length;
        for (int i = 0; i < n; i++) {
            float[] c = layerColors[i];
            float speed = LAYER_SPEEDS[i % LAYER_SPEEDS.length];
            int scrollDir = (i % 2 == 0) ? 1 : -1;

            float uOff = (animTime * speed * scrollDir) % 1.0f;
            float vOff = (animTime * speed * 0.65f * -scrollDir) % 1.0f;
            float uvScale = LAYER_UV_SCALES[i % LAYER_UV_SCALES.length];

            // Layer 0 (void/base) sits at the far face, layer n-1 (outer glow) at the near face.
            // Submitted farthest-first so the void is the background and glow layers blend on top.
            float t = (float) i / (n - 1);
            float depth = depthFar - t * depthSpan;

            int r = (int)(c[0] * 255);
            int g = (int)(c[1] * 255);
            int b = (int)(c[2] * 255);
            int a = (int)(c[3] * 255);

            if (axisX) {
                // axis=x: thin in Z, portal faces north/south → quads in X-Y plane at fixed Z
                drawQuad(consumer, matrix,
                    0f, 0f, depth,   1f, 0f, depth,   1f, 1f, depth,   0f, 1f, depth,
                    uOff, vOff, uOff + uvScale, vOff + uvScale,
                    r, g, b, a, 0f, 0f, 1f);
            } else {
                // axis=z: thin in X, portal faces east/west → quads in Y-Z plane at fixed X
                drawQuad(consumer, matrix,
                    depth, 0f, 0f,   depth, 0f, 1f,   depth, 1f, 1f,   depth, 1f, 0f,
                    uOff, vOff, uOff + uvScale, vOff + uvScale,
                    r, g, b, a, 1f, 0f, 0f);
            }
        }

        matrices.pop();
    }

    private void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float u1, float v1, float u2, float v2,
                          int r, int g, int b, int a,
                          float nx, float ny, float nz) {
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(fullLight).normal(nx, ny, nz).next();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(fullLight).normal(nx, ny, nz).next();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(fullLight).normal(nx, ny, nz).next();
        consumer.vertex(matrix, x4, y4, z4).color(r, g, b, a).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(fullLight).normal(nx, ny, nz).next();
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
