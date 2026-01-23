package net.dungeonz.block.screen;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;

import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.network.DungeonClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class DungeonPortalOpScreen extends Screen {

    private static final Text DUNGEON_TYPE_TEXT = Text.translatable("dungeon.op_screen.dungeon_type");
    private static final Text DEFAULT_DIFFICULTY_TEXT = Text.translatable("dungeon.op_screen.default_difficulty");
    private static final Text CHECKING_PORTAL_TEXT = Text.translatable("dungeon.op_screen.checking_portals");
    private static final int MIN_PORTAL_DISTANCE = 256;
    private final BlockPos dungeonPortalPos;

    private ButtonWidget doneButton;
    private TextFieldWidget dungeonTypeTextFieldWidget;
    private TextFieldWidget dungeonDefaultDifficultyTextFieldWidget;

    private String defaultDungeonType = "dark_dungeon";
    private String defaultDungeonDifficulty = "normal";
    private Text errorMessage = null;
    private int nearestPortalDistance = -1;
    private boolean isCheckingPortals = false;

    public DungeonPortalOpScreen(BlockPos dungeonPortalPos) {
        super(NarratorManager.EMPTY);
        this.dungeonPortalPos = dungeonPortalPos;
    }

    @Override
    protected void init() {
        if (client.world != null && client.world.getBlockEntity(this.dungeonPortalPos) != null && client.world.getBlockEntity(this.dungeonPortalPos) instanceof DungeonPortalEntity) {
            DungeonPortalEntity dungeonPortalEntity = (DungeonPortalEntity) client.world.getBlockEntity(this.dungeonPortalPos);
            if (!dungeonPortalEntity.getDungeonType().equals("")) {
                defaultDungeonType = dungeonPortalEntity.getDungeonType();
            }
            if (!dungeonPortalEntity.getDifficulty().equals("")) {
                defaultDungeonDifficulty = dungeonPortalEntity.getDifficulty();
            }
        }

        this.dungeonTypeTextFieldWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 152, 50, 300, 20, DUNGEON_TYPE_TEXT);
        this.dungeonTypeTextFieldWidget.setMaxLength(128);
        this.dungeonTypeTextFieldWidget.setText(defaultDungeonType);
        this.dungeonTypeTextFieldWidget.setChangedListener(pool -> this.updateDoneButtonState());
        this.addSelectableChild(this.dungeonTypeTextFieldWidget);
        this.dungeonDefaultDifficultyTextFieldWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 152, 85, 300, 20, DEFAULT_DIFFICULTY_TEXT);
        this.dungeonDefaultDifficultyTextFieldWidget.setMaxLength(128);
        this.dungeonDefaultDifficultyTextFieldWidget.setText(defaultDungeonDifficulty);
        this.dungeonDefaultDifficultyTextFieldWidget.setChangedListener(name -> this.updateDoneButtonState());
        this.addSelectableChild(this.dungeonDefaultDifficultyTextFieldWidget);

        this.doneButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
            this.onDone();
        }).dimensions(this.width / 2 - 75, 126, 150, 20).build());
        this.setInitialFocus(this.dungeonTypeTextFieldWidget);
        this.updateDoneButtonState();

        // Check for nearby portals (must be after widgets are initialized)
        checkForNearbyPortals();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String string = this.dungeonTypeTextFieldWidget.getText();
        String string2 = this.dungeonDefaultDifficultyTextFieldWidget.getText();

        this.init(client, width, height);
        this.dungeonTypeTextFieldWidget.setText(string);
        this.dungeonDefaultDifficultyTextFieldWidget.setText(string2);
    }

    @Override
    public void tick() {
        this.dungeonTypeTextFieldWidget.tick();
        this.dungeonDefaultDifficultyTextFieldWidget.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawTextWithShadow(this.textRenderer, DUNGEON_TYPE_TEXT, this.width / 2 - 153, 40, 0xA0A0A0);
        this.dungeonTypeTextFieldWidget.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, DEFAULT_DIFFICULTY_TEXT, this.width / 2 - 153, 75, 0xA0A0A0);
        this.dungeonDefaultDifficultyTextFieldWidget.render(context, mouseX, mouseY, delta);

        // Display status message
        if (this.isCheckingPortals) {
            context.drawCenteredTextWithShadow(this.textRenderer, CHECKING_PORTAL_TEXT, this.width / 2, 150, 0xFFFF55);
        } else if (this.errorMessage != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.errorMessage, this.width / 2, 150, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void updateDoneButtonState() {
        boolean hasValidInput = !StringUtils.isEmpty(this.dungeonTypeTextFieldWidget.getText()) && !StringUtils.isEmpty(this.dungeonDefaultDifficultyTextFieldWidget.getText());
        boolean noNearbyPortal = this.errorMessage == null;
        boolean notChecking = !this.isCheckingPortals;
        this.doneButton.active = hasValidInput && noNearbyPortal && notChecking;
    }

    private void checkForNearbyPortals() {
        if (client.world == null) {
            return;
        }

        this.errorMessage = null;
        this.nearestPortalDistance = -1;
        this.isCheckingPortals = true;
        this.updateDoneButtonState();

        // Capture references for the async task
        final MinecraftClient mc = this.client;
        final BlockPos portalPos = this.dungeonPortalPos;
        final int chunkRadius = (MIN_PORTAL_DISTANCE / 16) + 1;
        final int centerChunkX = portalPos.getX() >> 4;
        final int centerChunkZ = portalPos.getZ() >> 4;

        CompletableFuture.supplyAsync(() -> {
            int foundDistance = -1;

            // Iterate through chunks in range
            for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                    if (mc.world == null || !mc.world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }

                    // Search block entities within this chunk
                    int minX = chunkX << 4;
                    int minZ = chunkZ << 4;
                    int maxX = minX + 15;
                    int maxZ = minZ + 15;
                    int minY = mc.world.getBottomY();
                    int maxY = mc.world.getTopY();

                    BlockPos.Mutable mutable = new BlockPos.Mutable();
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                mutable.set(x, y, z);

                                // Skip the current portal position
                                if (mutable.equals(portalPos)) {
                                    continue;
                                }

                                try {
                                    if (mc.world != null && mc.world.getBlockEntity(mutable) instanceof DungeonPortalEntity otherPortal) {
                                        // Only count portals that are configured (have a dungeon type set)
                                        if (!otherPortal.getDungeonType().isEmpty()) {
                                            int distance = (int) Math.sqrt(portalPos.getSquaredDistance(mutable));
                                            if (distance < MIN_PORTAL_DISTANCE) {
                                                if (foundDistance == -1 || distance < foundDistance) {
                                                    foundDistance = distance;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // Ignore any concurrent access issues
                                }
                            }
                        }
                    }
                }
            }

            return foundDistance;
        }).thenAccept(foundDistance -> {
            // Update UI on the main thread
            mc.execute(() -> {
                this.isCheckingPortals = false;
                this.nearestPortalDistance = foundDistance;

                if (foundDistance != -1) {
                    this.errorMessage = Text.translatable("dungeon.op_screen.portal_too_close", foundDistance, MIN_PORTAL_DISTANCE);
                } else {
                    this.errorMessage = null;
                }

                this.updateDoneButtonState();
            });
        });
    }

    private void onDone() {
        this.client.setScreen(null);
        DungeonClientPacket.writeC2SSetDungeonTypePacket(client, this.dungeonTypeTextFieldWidget.getText(), this.dungeonDefaultDifficultyTextFieldWidget.getText(), dungeonPortalPos);
    }

}
