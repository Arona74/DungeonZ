package net.dungeonz.block.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;

import io.netty.buffer.Unpooled;
import net.dungeonz.DungeonzMain;
import net.dungeonz.init.DimensionInit;
import net.dungeonz.network.DungeonClientPacket;
import net.dungeonz.network.DungeonServerPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.dungeonz.util.InventoryHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.levelz.access.PlayerStatsManagerAccess;
import net.levelz.stats.PlayerStatsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;

@Environment(EnvType.CLIENT)
public class DungeonSuperPortalScreen extends HandledScreen<DungeonSuperPortalScreenHandler>
        implements ScreenHandlerListener {

    private static final Identifier ICONS = new Identifier("dungeonz", "textures/gui/dungeon_icons.png");
    private static final Text JOIN = Text.translatable("dungeon.task.join");
    private static final Text LEAVE = Text.translatable("dungeon.task.leave");
    private static final ItemStack INFO_ITEMSTACK = new ItemStack(Items.CREEPER_BANNER_PATTERN);

    private final Identifier texture;
    public DungeonDifficultyButton difficultyButton;
    private DungeonButton dungeonButton;
    private DungeonSliderButton privateButton;
    private ButtonWidget changeDungeonButton;
    private final PlayerEntity playerEntity;
    private boolean hasNotifiedServerOfOpen = false;

    public DungeonSuperPortalScreen(DungeonSuperPortalScreenHandler handler,
                                    PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.playerEntity = inventory.player;
        texture = handler.getBackgroundId() != null
                ? handler.getBackgroundId()
                : new Identifier("dungeonz", "textures/gui/dungeon_portal.png");
        this.backgroundWidth = 256;
        this.backgroundHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width / 2 - this.backgroundWidth / 2);
        this.y = (this.height / 2 - this.backgroundHeight / 2);

        this.handler.addListener(this);

        if (!hasNotifiedServerOfOpen) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_OPENED_PACKET, buf);
            hasNotifiedServerOfOpen = true;
        }

        final boolean playerIsInDungeonWorld =
                playerEntity.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD;
        Text buttonText = playerIsInDungeonWorld ? LEAVE : JOIN;

        this.dungeonButton = this.addDrawableChild(new DungeonButton(
                this.x + this.backgroundWidth - 130, this.y + this.backgroundHeight - 28,
                buttonText, (button) -> {
                    if (button.active) {
                        DungeonClientPacket.writeC2SDungeonTeleportPacket(
                                this.client, this.handler.getPos(), this.playerEntity.getUuid());
                        this.handler.setWaitingGroupSize(this.handler.getWaitingGroupSize() + 1);
                        button.active = false;
                    }
                }));

        this.difficultyButton = this.addDrawableChild(new DungeonDifficultyButton(
                this.x + 144, this.y + 36, Text.of(""), (button) -> {
                    if (button.active) {
                        DungeonClientPacket.writeC2SChangeDifficultyPacket(
                                this.client, this.handler.getPos());
                    }
                }));

        this.privateButton = this.addDrawableChild(new DungeonSliderButton(
                this.x + 144, this.y + 63, (button) -> {
                    if (button.active) {
                        ((DungeonSliderButton) button).cycleEnabled();
                        DungeonClientPacket.writeC2SChangePrivateGroupPacket(
                                client, this.handler.getPos(),
                                ((DungeonSliderButton) button).isEnabled());
                    }
                }));

        boolean canChangeDungeon =
                this.handler.getDungeonPortalEntity().getDungeonPlayerCount() == 0
                && !this.handler.getDungeonPortalEntity()
                        .isOnCooldown((int) this.client.world.getTime());

        this.changeDungeonButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.translatable("text.dungeonz.super_portal.change_dungeon"),
                        button -> this.client.setScreen(new DungeonSuperPortalSelectionScreen(
                                this.handler.getPos(), this.handler.getDungeonIdList())))
                        .dimensions(this.x + 8, this.y + this.backgroundHeight - 28, 90, 20)
                        .build());
        this.changeDungeonButton.active = canChangeDungeon;

        if (this.handler.getDungeonPortalEntity().getPrivateGroup() != this.privateButton.isEnabled()) {
            this.privateButton.cycleEnabled();
        }

        if (playerIsInDungeonWorld) {
            this.dungeonButton.active = true;
            this.difficultyButton.active = false;
            this.privateButton.active = false;
        } else {
            if (!this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty()) {
                this.difficultyButton.active = false;
                this.privateButton.active = false;
            } else {
                this.difficultyButton.active = true;
                this.privateButton.active = true;
            }
            List<ItemStack> tickRequiredItems = this.handler.getRequiredItemStacks()
                    .get(this.handler.getDungeonPortalEntity().getDifficulty());
            if ((this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                    + this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size())
                    < this.handler.getDungeonPortalEntity().getMaxGroupSize()
                    && (tickRequiredItems == null || InventoryHelper.hasRequiredItemStacks(
                            this.playerEntity.getInventory(), tickRequiredItems))
                    && !this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids()
                            .contains(this.playerEntity.getUuid())) {
                this.dungeonButton.active = true;
            } else {
                this.dungeonButton.active = false;
            }

            if (this.dungeonButton.active && DungeonzMain.isLevelZLoaded) {
                PlayerStatsManager statsManager =
                        ((PlayerStatsManagerAccess) this.playerEntity).getPlayerStatsManager();
                if (statsManager.getOverallLevel() < this.handler.getRequiredLevel()) {
                    this.dungeonButton.active = false;
                }
            }

            if (this.dungeonButton.active && this.privateButton.isEnabled()
                    && !this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty()) {
                if (DungeonzMain.isPartyAddonLoaded) {
                    GroupManager groupManager =
                            ((GroupManagerAccess) this.playerEntity).getGroupManager();
                    if (groupManager.getGroupPlayerIdList().isEmpty()
                            || !groupManager.getGroupPlayerIdList().contains(
                                    this.handler.getDungeonPortalEntity()
                                            .getDungeonPlayerUuids().get(0))) {
                        this.dungeonButton.active = false;
                    }
                } else {
                    this.dungeonButton.active = false;
                }
            }
        }

        if (this.handler.getDifficulties().contains(
                this.handler.getDungeonPortalEntity().getDifficulty())) {
            this.difficultyButton.setText(Text.translatable(
                    "dungeonz.difficulty." + this.handler.getDungeonPortalEntity().getDifficulty()));
        } else if (!this.handler.getDifficulties().isEmpty()) {
            this.difficultyButton.setText(Text.translatable(
                    "dungeonz.difficulty." + this.handler.getDifficulties().get(0)));
        }

        if (this.handler.getDungeonPortalEntity().isOnCooldown((int) this.client.world.getTime())) {
            this.dungeonButton.active = false;
        }
    }

    private Text getPlayerName(UUID playerId, int length, int substringLength) {
        if (this.client.getNetworkHandler().getPlayerListEntry(playerId) != null) {
            String playerName = this.client.getNetworkHandler()
                    .getPlayerListEntry(playerId).getProfile().getName();
            if (this.client.textRenderer.getWidth(playerName) > length && substringLength != 0) {
                playerName = playerName.substring(0, substringLength) + "..";
            }
            return Text.of(playerName);
        }
        return Text.translatable("text.dungeonz.empty_name");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawText(this.textRenderer, this.title,
                this.x + this.backgroundWidth / 2 - this.textRenderer.getWidth(this.title) / 2,
                this.y + 8, 0x404040, false);

        // Dungeon player list
        int k = this.y + 37;
        context.drawText(this.textRenderer,
                Text.translatable("text.dungeonz.player_list",
                        this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                                + this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size(),
                        this.handler.getDungeonPortalEntity().getMaxGroupSize()),
                this.x + 8, this.y + 24, 0x3F3F3F, false);
        for (int i = 0; i < this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                && i < 13; i++) {
            String playerName = getPlayerName(
                    this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(i),
                    102, 15).getString();
            if (i == 12) {
                playerName = "...";
                if (this.isPointWithinBounds(13, k, 16, 7, mouseX, mouseY)) {
                    List<Text> otherPlayerNames = new ArrayList<>();
                    for (int u = 12; u < this.handler.getDungeonPortalEntity()
                            .getDungeonPlayerUuids().size(); u++) {
                        otherPlayerNames.add(getPlayerName(
                                this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(u),
                                102, 15));
                    }
                    context.drawTooltip(this.textRenderer, otherPlayerNames, mouseX, mouseY);
                }
            }
            context.drawText(this.textRenderer, playerName, this.x + 13, k, 0xFFFFFF, false);
            k += 13;
        }

        // Required items
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.required"),
                this.x + 139, this.y + 81, 0x3F3F3F, false);
        List<ItemStack> requiredItems = this.handler.getRequiredItemStacks()
                .get(this.handler.getDungeonPortalEntity().getDifficulty());
        context.drawTexture(ICONS,
                this.x + 142 + this.textRenderer.getWidth(Text.translatable("text.dungeonz.required")),
                this.y + 78,
                52 + (requiredItems != null && InventoryHelper.hasRequiredItemStacks(
                        this.playerEntity.getInventory(), requiredItems) ? 0 : 14),
                0, 14, 14);

        if (requiredItems != null && !requiredItems.isEmpty()) {
            int l = 0;
            for (ItemStack stack : requiredItems) {
                context.drawItem(stack, this.x + 144 + l, this.y + 93);
                context.drawItemInSlot(this.textRenderer, stack, this.x + 144 + l, this.y + 93);
                if (this.isPointWithinBounds(144 + l, 93, 16, 16, mouseX, mouseY)) {
                    context.drawTooltip(this.textRenderer, stack.getName(), mouseX, mouseY);
                }
                l += 18;
            }
        } else {
            context.drawText(this.textRenderer,
                    Text.translatable("text.dungeonz.nothing_required"),
                    this.x + 144, this.y + 93, 0x3F3F3F, false);
        }

        // Possible loot
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.possible"),
                this.x + 139, this.y + 115, 0x3F3F3F, false);
        String currentDifficulty = this.handler.getDungeonPortalEntity().getDifficulty();
        if (this.handler.getPossibleLootDifficultyItemStackMap().size() > 0
                && this.handler.getPossibleLootDifficultyItemStackMap().containsKey(currentDifficulty)
                && this.handler.getPossibleLootDifficultyItemStackMap().get(currentDifficulty).size() > 0) {
            int l = 0;
            int o = 0;
            List<ItemStack> lootList = this.handler.getPossibleLootDifficultyItemStackMap()
                    .get(currentDifficulty);
            for (int i = 0; i < lootList.size() && i < 10; i++) {
                context.drawItem(lootList.get(i), this.x + 144 + l, this.y + o + 127);
                context.drawItemInSlot(this.textRenderer, lootList.get(i),
                        this.x + 144 + l, this.y + o + 127);
                if (this.isPointWithinBounds(144 + l, o + 127, 16, 16, mouseX, mouseY)) {
                    context.drawTooltip(this.textRenderer, lootList.get(i).getName(),
                            mouseX, mouseY);
                }
                l += 18;
                if (i == 4) {
                    l = 0;
                    o = 18;
                }
            }
        }

        context.drawText(this.textRenderer, Text.translatable("dungeonz.difficulty"),
                this.x + 139, this.y + 24, 0x3F3F3F, false);
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.private"),
                this.x + 169, this.y + 65, 0x3F3F3F, false);

        // Min group size
        if (this.handler.getDungeonPortalEntity().getDungeonPlayerCount() <= 0
                && this.handler.getDungeonPortalEntity().getMinGroupSize() > 1) {
            context.drawText(this.textRenderer,
                    Text.translatable("text.dungeonz.waiting_player_list",
                            this.handler.getWaitingGroupSize(),
                            this.handler.getDungeonPortalEntity().getMinGroupSize()),
                    this.x + 139, this.y + 167, 0x3F3F3F, false);
        }

        // LevelZ
        if (DungeonzMain.isLevelZLoaded) {
            context.drawText(this.textRenderer,
                    Text.translatable("text.dungeonz.required_level",
                            this.handler.getRequiredLevel()),
                    this.x + 139, this.y + 180, 0x3F3F3F, false);
        }

        // Information button
        if (this.isPointWithinBounds(230, 6, 20, 18, mouseX, mouseY)) {
            context.drawTexture(ICONS, this.x + 230, this.y + 6, 20, 84, 20, 18);

            List<Text> dungeonInfo = new ArrayList<>();
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info"));
            for (int i = 1; i < 10; i++) {
                String key = "dungeon." + this.handler.getDungeonPortalEntity().getDungeonType()
                        + ".description." + i;
                Text infoText = Text.translatable(key);
                if (infoText.getString().equals(key)) {
                    break;
                }
                dungeonInfo.add(infoText);
            }
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.respawn"
                    + (this.handler.isAllowRespawn() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.keep_inventory"
                    + (this.handler.isKeepInventory() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.positive_effects"
                    + (this.handler.isAllowPositiveEffects() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.ender_pearl"
                    + (this.handler.isAllowEnderPearl() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.elytra"
                    + (this.handler.isAllowElytra() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.mobs_loot"
                    + (this.handler.isAllowMobsLoot() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.boss_loot"
                    + (this.handler.isAllowBossLoot() ? "" : ".disabled")));
            context.drawTooltip(this.textRenderer, dungeonInfo, mouseX, mouseY);
        } else {
            context.drawTexture(ICONS, this.x + 230, this.y + 6, 0, 84, 20, 18);
        }
        context.drawItem(INFO_ITEMSTACK, this.x + 232, this.y + 7);

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(texture, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(this.handler.getPos());
        ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        super.close();
    }

    @Override
    public void removed() {
        if (hasNotifiedServerOfOpen) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        }
        super.removed();
    }

    public void refresh() {
        if (this.handler.getDifficulties().contains(
                this.handler.getDungeonPortalEntity().getDifficulty())) {
            this.difficultyButton.setText(Text.translatable(
                    "dungeonz.difficulty." + this.handler.getDungeonPortalEntity().getDifficulty()));
        } else if (!this.handler.getDifficulties().isEmpty()) {
            this.difficultyButton.setText(Text.translatable(
                    "dungeonz.difficulty." + this.handler.getDifficulties().get(0)));
        }
    }

    @Override
    public void onSlotUpdate(ScreenHandler var1, int var2, ItemStack var3) {
    }

    @Override
    public void onPropertyUpdate(ScreenHandler var1, int var2, int var3) {
    }

    // -------------------------------------------------------------------------
    // Inner button classes — mirrors DungeonPortalScreen, adapted for super portal
    // -------------------------------------------------------------------------

    public class DungeonButton extends ButtonWidget {

        public DungeonButton(int x, int y, Text text, ButtonWidget.PressAction onPress) {
            super(x, y, 52, 20, text, onPress, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            int j = 20;
            if (!this.active) {
                j = 0;
            } else if (this.isHovered()) {
                j = 40;
            }
            context.drawTexture(ICONS, this.getX(), this.getY(), 0, j, this.width, this.height);

            int o = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredTextWithShadow(textRenderer, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                    o | MathHelper.ceil(this.alpha * 255.0f) << 24);

            if (!this.active && this.isHovered()) {
                Text text = null;
                if (DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                        .isOnCooldown((int) DungeonSuperPortalScreen.this.client.world.getTime())) {
                    int cooldown = (DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                            .getCooldownTime()
                            - (int) DungeonSuperPortalScreen.this.client.world.getTime()) / 20;
                    int seconds = cooldown % 60;
                    int minutes = cooldown / 60 % 60;
                    int hours = cooldown / 60 / 60;
                    text = Text.translatable("text.dungeonz.dungeon_cooldown_time",
                            hours, minutes, seconds);
                } else if ((DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                        .getDungeonPlayerUuids().size()
                        + DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                                .getDeadDungeonPlayerUuids().size())
                        >= DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                                .getMaxGroupSize()) {
                    text = Text.translatable("text.dungeonz.dungeon_full");
                } else if (client.player != null
                        && !DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                                .getDeadDungeonPlayerUuids().isEmpty()
                        && DungeonSuperPortalScreen.this.handler.getDungeonPortalEntity()
                                .getDeadDungeonPlayerUuids().contains(client.player.getUuid())) {
                    text = Text.translatable("text.dungeonz.dead_player");
                } else if (!InventoryHelper.hasRequiredItemStacks(client.player.getInventory(),
                        DungeonSuperPortalScreen.this.handler.getRequiredItemStacks()
                                .get(DungeonSuperPortalScreen.this.handler
                                        .getDungeonPortalEntity().getDifficulty()))) {
                    text = Text.translatable("text.dungeonz.missing");
                } else if (DungeonzMain.isLevelZLoaded) {
                    PlayerStatsManager statsManager = ((PlayerStatsManagerAccess)
                            DungeonSuperPortalScreen.this.playerEntity).getPlayerStatsManager();
                    if (statsManager.getOverallLevel()
                            < DungeonSuperPortalScreen.this.handler.getRequiredLevel()) {
                        text = Text.translatable("text.dungeonz.required_level",
                                DungeonSuperPortalScreen.this.handler.getRequiredLevel());
                    }
                }
                if (text != null) {
                    context.drawTooltip(textRenderer, text, mouseX, mouseY);
                }
            }
        }
    }

    public class DungeonDifficultyButton extends ButtonWidget {
        private Text text;

        public DungeonDifficultyButton(int x, int y, Text text,
                                       ButtonWidget.PressAction onPress) {
            super(x, y, 52, 20, text, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.text = text;
        }

        public void setText(Text text) {
            this.text = text;
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            TextRenderer textRenderer = minecraftClient.textRenderer;

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
            int j = getTextureY();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            context.drawTexture(ICONS, this.getX(), this.getY(), 0, j, this.width, this.height);
            int o = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredTextWithShadow(textRenderer, this.text,
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                    o | MathHelper.ceil(this.alpha * 255.0f) << 24);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        private int getTextureY() {
            int j = 20;
            if (!this.active) {
                j = 0;
            } else if (this.isSelected()) {
                j = 40;
            }
            return j;
        }
    }

    public class DungeonSliderButton extends ButtonWidget {
        private boolean enabled = false;

        public DungeonSliderButton(int x, int y, ButtonWidget.PressAction onPress) {
            super(x, y, 20, 12, Text.of(""), onPress, DEFAULT_NARRATION_SUPPLIER);
        }

        public void cycleEnabled() {
            this.enabled = !this.enabled;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            int i = 60;
            if (this.enabled) {
                i = 72;
            }
            int j = 0;
            if (!this.active) {
                j = 40;
            } else if (this.isHovered()) {
                j = 20;
            }
            context.drawTexture(ICONS, this.getX(), this.getY(), j, i, this.width, this.height);
        }
    }
}
