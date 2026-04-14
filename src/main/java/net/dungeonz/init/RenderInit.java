package net.dungeonz.init;

import net.dungeonz.block.render.DungeonGateRenderer;
import net.dungeonz.block.render.DungeonPortalRenderer;
import net.dungeonz.block.render.DungeonSpawnerRenderer;
import net.dungeonz.block.screen.DungeonPortalScreen;
import net.dungeonz.block.screen.DungeonSuperPortalScreen;
import net.dungeonz.item.DungeonCompassItem;
import net.dungeonz.util.RenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static void init() {
        BlockRenderLayerMap.INSTANCE.putBlock(BlockInit.DUNGEON_SPAWNER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockInit.DUNGEON_GATE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockInit.DUNGEON_PORTAL, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockInit.DUNGEON_SUPER_PORTAL, RenderLayer.getTranslucent());

        BlockEntityRendererFactories.register(BlockInit.DUNGEON_PORTAL_ENTITY, ctx -> new DungeonPortalRenderer(ctx, DungeonPortalRenderer.COLORS_DUNGEON_PORTAL));
        BlockEntityRendererFactories.register(BlockInit.DUNGEON_SPAWNER_ENTITY, DungeonSpawnerRenderer::new);
        BlockEntityRendererFactories.register(BlockInit.DUNGEON_GATE_ENTITY, DungeonGateRenderer::new);
        BlockEntityRendererFactories.register(BlockInit.DUNGEON_SUPER_PORTAL_ENTITY, ctx -> new DungeonPortalRenderer(ctx, DungeonPortalRenderer.COLORS_SUPER_PORTAL));

        HandledScreens.register(BlockInit.PORTAL, DungeonPortalScreen::new);
        HandledScreens.register(BlockInit.SUPER_PORTAL, DungeonSuperPortalScreen::new);

        ModelPredicateProviderRegistry.register(ItemInit.DUNGEON_COMPASS, new Identifier("angle"), new CompassAnglePredicateProvider((world, stack, entity) -> {
            return DungeonCompassItem.createGlobalDungeonStructurePos(world, stack);
        }));
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            RenderHelper.renderDungeonCountdown(drawContext, tickDelta);
        });
    }

}
