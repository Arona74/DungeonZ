package net.dungeonz.block.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Accesses protected RenderPhase members to build a custom portal render layer.
 * Used as the shader-compatible fallback when Iris/OptiFine is active, since
 * RenderLayer.getEndPortal() uses a core shader that most shader packs don't implement.
 */
@Environment(EnvType.CLIENT)
abstract class PortalLayerHelper extends RenderLayer {

    private PortalLayerHelper() {
        super("", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 0, false, false, () -> {}, () -> {});
    }

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
