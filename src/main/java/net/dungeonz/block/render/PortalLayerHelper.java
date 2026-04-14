package net.dungeonz.block.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Abstract subclass of RenderLayer (which itself extends RenderPhase) used solely to
 * access the protected static members of both classes when building a custom RenderLayer.
 * It is never instantiated; only {@link #createPortalLayer} is called.
 */
@Environment(EnvType.CLIENT)
abstract class PortalLayerHelper extends RenderLayer {

    private PortalLayerHelper() {
        super("", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 0, false, false, () -> {}, () -> {});
    }

    /**
     * Creates a render layer suitable for the layered portal effect:
     * - No depth writes (COLOR_MASK) so inner layers are never depth-culled by the opaque base.
     * - Backface culling disabled (DISABLE_CULLING) so each quad is visible from both sides.
     * - Translucent blending for correct alpha compositing.
     */
    static RenderLayer createPortalLayer(Identifier texture) {
        return of(
            "dungeon_portal",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            VertexFormat.DrawMode.QUADS,
            256, false, true,
            MultiPhaseParameters.builder()
                .program(new ShaderProgram(() -> GameRenderer.getRenderTypeEntityTranslucentProgram()))
                .texture(new Texture(texture, false, false))
                .transparency(TRANSLUCENT_TRANSPARENCY)
                .cull(DISABLE_CULLING)
                .writeMaskState(COLOR_MASK)
                .lightmap(ENABLE_LIGHTMAP)
                .overlay(ENABLE_OVERLAY_COLOR)
                .build(false)
        );
    }
}
