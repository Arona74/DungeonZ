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
import net.dungeonz.util.InventoryHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
public class DungeonPortalScreen extends HandledScreen<DungeonPortalScreenHandler> implements ScreenHandlerListener {

    private static final Identifier DEFAULT_TEXTURE = new Identifier("dungeonz:textures/gui/dungeon_portal.png");
    private static final Identifier ICONS = new Identifier("dungeonz:textures/gui/dungeon_icons.png");
    private static final Text JOIN = Text.translatable("dungeon.task.join");
    private static final Text LEAVE = Text.translatable("dungeon.task.leave");
    private static final ItemStack INFO_ITEMSTACK = new ItemStack(Items.CREEPER_BANNER_PATTERN);

    public DungeonDifficultyButton difficultyButton;
    private DungeonButton dungeonButton;
    private DungeonButton leaveButton; // New leave button
    private DungeonSliderButton privateButton;
    private final PlayerEntity playerEntity;
    private boolean hasNotifiedServerOfOpen = false; // Track if we've already notified the server
    private final Identifier backgroundTexture;

    public DungeonPortalScreen(DungeonPortalScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.playerEntity = inventory.player;
        this.backgroundTexture = handler.getBackgroundId() != null ? handler.getBackgroundId() : DEFAULT_TEXTURE;
        this.backgroundWidth = 256;
        this.backgroundHeight = 222;
    }

    public DungeonPortalScreenHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void init() {
        super.init();

        // CRITICAL: Check for spectators and null entity before doing ANYTHING else
        if (this.playerEntity.isSpectator() || this.handler.getDungeonPortalEntity() == null) {
            // Close the screen immediately for spectators or if entity is null
            this.close();
            return;
        }
        
        this.x = (this.width / 2 - this.backgroundWidth / 2);
        this.y = (this.height / 2 - this.backgroundHeight / 2);

        this.handler.addListener(this);

        // Send GUI opened packet to server (only once per screen instance)
        if (!hasNotifiedServerOfOpen) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_OPENED_PACKET, buf);
            hasNotifiedServerOfOpen = true;
        }

        final boolean playerIsInDungeonWorld = playerEntity.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD;
        Text buttonText = playerIsInDungeonWorld ? LEAVE : JOIN;

        this.dungeonButton = this.addDrawableChild(new DungeonButton(this.x + this.backgroundWidth - 130, this.y + this.backgroundHeight - 28, buttonText, (button) -> {
            if (button.active) {
                // Send packet to server
                DungeonClientPacket.writeC2SDungeonTeleportPacket(this.client, this.handler.getPos(), this.playerEntity.getUuid());
                this.handler.setWaitingGroupSize(this.handler.getWaitingGroupSize() + 1);
                
                // Immediately update client-side entity state for responsive UI
                this.handler.getDungeonPortalEntity().addWaitingUuid(this.playerEntity.getUuid());
                
                // Update button states based on new state
                this.refresh();
            }
        }));

        // Add Leave button next to Join button - START AS DISABLED
        this.leaveButton = this.addDrawableChild(new DungeonButton(this.x + this.backgroundWidth - 74, this.y + this.backgroundHeight - 28, Text.translatable("dungeon.task.leave_waiting"), (button) -> {
            if (button.active) {
                // Send leave waiting packet to server
                DungeonClientPacket.writeC2SLeaveWaitingPacket(this.client, this.handler.getPos());
                
                // Immediately update client-side entity state for responsive UI
                this.handler.getDungeonPortalEntity().removeWaitingUuid(this.playerEntity.getUuid());
                this.handler.setWaitingGroupSize(this.handler.getWaitingGroupSize() - 1);
                
                // Update button states based on new state
                this.refresh();
            }
        }));
        
        // Initialize leave button as disabled by default
        this.leaveButton.active = false;
        this.difficultyButton = this.addDrawableChild(new DungeonDifficultyButton(this.x + 144, this.y + 36, Text.of(""), (button) -> {
            if (button.active) {
                DungeonClientPacket.writeC2SChangeDifficultyPacket(this.client, this.handler.getPos());
                this.refresh();
            }
        }));
        this.privateButton = this.addDrawableChild(new DungeonSliderButton(this.x + 144, this.y + 63, (button) -> {
            if (button.active) {
                ((DungeonSliderButton) button).cycleEnabled();
                this.handler.getDungeonPortalEntity().setPrivateGroup(((DungeonSliderButton) button).isEnabled());
                DungeonClientPacket.writeC2SChangePrivateGroupPacket(client, this.handler.getPos(), ((DungeonSliderButton) button).isEnabled());
                this.refresh();
            }
        }));

        this.privateButton.enabled = this.handler.getDungeonPortalEntity().getPrivateGroup();
        
        // Initialize button states properly
        if (playerIsInDungeonWorld) {
            this.dungeonButton.active = true;
            this.leaveButton.active = false;
            this.difficultyButton.active = false;
            this.privateButton.active = false;
        } else {
            if (!this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty()) {
                this.difficultyButton.active = false;
                this.privateButton.active = false;
                this.leaveButton.active = false; // Leave button disabled when dungeon is active
            } else if (this.handler.getDungeonPortalEntity().getWaitingUuids().contains(this.playerEntity.getUuid())){
                this.dungeonButton.active = false;
                this.difficultyButton.active = false;
                this.privateButton.active = true;
                this.leaveButton.active = true; // Leave button enabled for waiting players
            } else {
                this.difficultyButton.active = true;
                this.privateButton.active = true;
                this.leaveButton.active = false; // Leave button disabled for non-waiting players
            }

            boolean hasRequiredItems = InventoryHelper.hasRequiredItemStacks(
                this.playerEntity.getInventory(),
                this.handler.getRequiredItemStacks().get(this.handler.getDungeonPortalEntity().getDifficulty())
            );

            boolean isPlayerDead = this.handler.getDungeonPortalEntity()
                .getDeadDungeonPlayerUuids().contains(this.playerEntity.getUuid());

            boolean isSomeoneWaiting = this.handler.getDungeonPortalEntity().getWaitingUuids().size() >= 1;
            
            boolean isPlayerWaiting = this.handler.getDungeonPortalEntity()
                .getWaitingUuids().contains(this.playerEntity.getUuid());

            boolean isUnderMaxGroupSize = (this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                + this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size()) < this.handler.getDungeonPortalEntity().getMaxGroupSize();

            // Difficulty button should be disabled if anyone is waiting OR if dungeon is already active
            this.difficultyButton.active = !isSomeoneWaiting && this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty();
            
            this.dungeonButton.active = hasRequiredItems && !isPlayerDead && !isPlayerWaiting && isUnderMaxGroupSize;

            if (this.dungeonButton.active && DungeonzMain.isLevelZLoaded) {
                PlayerStatsManager PlayerStatsManager = ((PlayerStatsManagerAccess) this.playerEntity).getPlayerStatsManager();
                if (PlayerStatsManager.getOverallLevel() < this.handler.getRequiredLevel()) {
                    this.dungeonButton.active = false;
                }
            }

            if (this.dungeonButton.active && this.privateButton.enabled && !this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty()) {
                if (DungeonzMain.isPartyAddonLoaded) {
                    GroupManager groupManager = ((GroupManagerAccess) this.playerEntity).getGroupManager();
                    if (groupManager.getGroupPlayerIdList().isEmpty() || !groupManager.getGroupPlayerIdList().contains(this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(0))) {
                        this.dungeonButton.active = false;
                    }
                } else {
                    this.dungeonButton.active = false;
                }
            }
        }

        if (this.handler.getDifficulties().contains(this.handler.getDungeonPortalEntity().getDifficulty())) {
            this.difficultyButton.setText(Text.translatable("dungeonz.difficulty." + this.handler.getDungeonPortalEntity().getDifficulty()));
        } else {
            this.difficultyButton.setText(Text.translatable("dungeonz.difficulty." + this.handler.getDifficulties().get(0)));
        }

        if (this.handler.getDungeonPortalEntity().isOnCooldown((int) this.client.world.getTime())) {
            this.dungeonButton.active = false;
        }
    }

    @Override
    public void close() {
        // Send GUI closed packet to server before closing
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(this.handler.getPos());
        ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        
        super.close();
    }

    @Override
    public void removed() {
        // Also send close packet when screen is removed (alternative close method)
        if (hasNotifiedServerOfOpen) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        }
        
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle ESC key and other close keys
        if (keyCode == 256 || this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            // Send close packet before handling the key press
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBlockPos(this.handler.getPos());
            ClientPlayNetworking.send(DungeonServerPacket.GUI_CLOSED_PACKET, buf);
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Text getPlayerName(UUID playerId, int length, int substringLength) {
        if (this.client.getNetworkHandler().getPlayerListEntry(playerId) != null) {
            String playerName = this.client.getNetworkHandler().getPlayerListEntry(playerId).getProfile().getName();
            if (this.client.textRenderer.getWidth(playerName) > length && substringLength != 0) {
                playerName = playerName.substring(0, substringLength) + "..";
            }
            return Text.of(playerName);
        }
        return Text.translatable("text.dungeonz.empty_name");
    }

    public void refresh() {
        // Update button states without recreating the entire GUI
        this.updateButtonStates();
    }

    private void updateButtonStates() {
        final boolean playerIsInDungeonWorld = playerEntity.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD;
        
        if (playerIsInDungeonWorld) {
            this.dungeonButton.active = true;
            this.difficultyButton.active = false;
            this.privateButton.active = false;
        } else {
            List<ItemStack> requiredItems = this.handler.getRequiredItemStacks().get(this.handler.getDungeonPortalEntity().getDifficulty());
            boolean hasRequiredItems = requiredItems != null && InventoryHelper.hasRequiredItemStacks(
                this.playerEntity.getInventory(),
                requiredItems
            );

            boolean isPlayerDead = this.handler.getDungeonPortalEntity()
                .getDeadDungeonPlayerUuids().contains(this.playerEntity.getUuid());

            boolean isSomeoneWaiting = this.handler.getDungeonPortalEntity().getWaitingUuids().size() >= 1;
            
            boolean isPlayerWaiting = this.handler.getDungeonPortalEntity()
                .getWaitingUuids().contains(this.playerEntity.getUuid());

            boolean isUnderMaxGroupSize = (this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                + this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size()) < this.handler.getDungeonPortalEntity().getMaxGroupSize();

            boolean isDungeonActive = !this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty();

            // Update difficulty button state
            this.difficultyButton.active = !isSomeoneWaiting && !isDungeonActive;
            
            // Update effect and private buttons
            if (isDungeonActive) {
                this.privateButton.active = false;
            } else if (isSomeoneWaiting) {
                // Only the waiting player(s) can change effects and private settings
                this.privateButton.active = isPlayerWaiting;
            } else {
                this.privateButton.active = true;
            }
            
            // Update join and leave button states            
            this.dungeonButton.active = hasRequiredItems && !isPlayerDead && !isPlayerWaiting && isUnderMaxGroupSize;
            this.leaveButton.active = isPlayerWaiting; // Leave button only active if player is waiting

            if (this.dungeonButton.active && DungeonzMain.isLevelZLoaded) {
                PlayerStatsManager PlayerStatsManager = ((PlayerStatsManagerAccess) this.playerEntity).getPlayerStatsManager();
                if (PlayerStatsManager.getOverallLevel() < this.handler.getRequiredLevel()) {
                    this.dungeonButton.active = false;
                }
            }

            if (this.dungeonButton.active && this.privateButton.enabled && !this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().isEmpty()) {
                if (DungeonzMain.isPartyAddonLoaded) {
                    GroupManager groupManager = ((GroupManagerAccess) this.playerEntity).getGroupManager();
                    if (groupManager.getGroupPlayerIdList().isEmpty() || !groupManager.getGroupPlayerIdList().contains(this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(0))) {
                        this.dungeonButton.active = false;
                    }
                } else {
                    this.dungeonButton.active = false;
                }
            }

            if (this.handler.getDungeonPortalEntity().isOnCooldown((int) this.client.world.getTime())) {
                this.dungeonButton.active = false;
            }
        }

        // Update difficulty button text
        if (this.handler.getDifficulties().contains(this.handler.getDungeonPortalEntity().getDifficulty())) {
            this.difficultyButton.setText(Text.translatable("dungeonz.difficulty." + this.handler.getDungeonPortalEntity().getDifficulty()));
        } else {
            this.difficultyButton.setText(Text.translatable("dungeonz.difficulty." + this.handler.getDifficulties().get(0)));
        }

        // Update slider button states
        this.privateButton.enabled = this.handler.getDungeonPortalEntity().getPrivateGroup();
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawText(this.textRenderer, this.title, this.x + this.backgroundWidth / 2 - this.textRenderer.getWidth(this.title) / 2, this.y + 8, 0x000000, false);

        // Dungeon Timer (top left corner in dark blue)
        if (this.handler.isDungeonTimerActive() && this.handler.getDungeonTimeRemaining() > 0) {
            int timeRemaining = this.handler.getDungeonTimeRemaining();
            int seconds = timeRemaining % 60;
            int minutes = timeRemaining / 60 % 60;
            int hours = timeRemaining / 60 / 60;
            
            String timerText;
            if (hours > 0) {
                timerText = String.format("Run: %dh:%02dm:%02ds", hours, minutes, seconds);
            } else {
                timerText = String.format("Run: %dm:%02ds", minutes, seconds);
            }
            
            context.drawText(this.textRenderer, timerText, this.x + 8, this.y + 8, 0x000080, false);
        }

        // Cooldown Timer (top right corner, aligned with title)
        if (this.handler.getDungeonPortalEntity().isOnCooldown((int) this.client.world.getTime())) {
            int cooldown = (this.handler.getDungeonPortalEntity().getCooldownTime() - (int) this.client.world.getTime()) / 20;
            int seconds = cooldown % 60;
            int minutes = cooldown / 60 % 60;
            int hours = cooldown / 60 / 60;
            
            String cooldownText;
            if (hours > 0) {
                cooldownText = String.format("CD: %dh:%02dm:%02ds", hours, minutes, seconds);
            } else {
                cooldownText = String.format("CD: %dm:%02ds", minutes, seconds);
            }
            
            int cooldownTextWidth = this.textRenderer.getWidth(cooldownText);
            context.drawText(this.textRenderer, cooldownText, this.x + this.backgroundWidth - cooldownTextWidth - 8, this.y + 8, 0xFF0000, false);
        }

        // Dungeon player list
        int k = this.y + 37;
        context.drawText(this.textRenderer,
                Text.translatable("text.dungeonz.player_list",
                        this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size() + this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size(),
                        this.handler.getDungeonPortalEntity().getMaxGroupSize()),
                this.x + 9, this.y + 24, 0x3F3F3F, false);
        for (int i = 0; i < this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size() && i < 7; i++) {
            String playerName = getPlayerName(this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(i), 102, 15).getString();
            if (i == 6) {
                playerName = "...";
                if (this.isPointWithinBounds(13, k, 16, 7, mouseX, mouseY)) {
                    List<Text> otherPlayerNames = new ArrayList<Text>();
                    for (int u = 12; u < this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size(); u++) {
                        otherPlayerNames.add(getPlayerName(this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().get(u), 102, 15));
                    }
                    context.drawTooltip(this.textRenderer, otherPlayerNames, mouseX, mouseY);
                }
            }
            context.drawText(this.textRenderer, playerName, this.x + 14, k, 0x3F3F3F, false);
            k += 13;
        }
        // Dungeon player waiting list
        int j = this.y + 113;
        if (this.handler.getDungeonPortalEntity().getDungeonPlayerCount() <= 0 && this.handler.getDungeonPortalEntity().getMinGroupSize() > 1) {
            context.drawText(this.textRenderer, Text.translatable("text.dungeonz.waiting_player_list", this.handler.getWaitingGroupSize(), this.handler.getDungeonPortalEntity().getMinGroupSize()),
                this.x + 9, this.y + 100, 0x3F3F3F, false);
            for (int i = 0; i < this.handler.getDungeonPortalEntity().getWaitingUuids().size() && i < 7; i++) {
                String playerName = getPlayerName(this.handler.getDungeonPortalEntity().getWaitingUuids().get(i), 102, 15).getString();
                if (i == 6) {
                    playerName = "...";
                    if (this.isPointWithinBounds(13, j, 16, 7, mouseX, mouseY)) {
                        List<Text> otherWaitingPlayerNames = new ArrayList<Text>();
                        for (int u = 12; u < this.handler.getDungeonPortalEntity().getWaitingUuids().size(); u++) {
                            otherWaitingPlayerNames.add(getPlayerName(this.handler.getDungeonPortalEntity().getWaitingUuids().get(u), 102, 15));
                        }
                        context.drawTooltip(this.textRenderer, otherWaitingPlayerNames, mouseX, mouseY);
                    }
                }
                context.drawText(this.textRenderer, playerName, this.x + 14, j, 0x3F3F3F, false);
                j += 13;
            }
        }
        // Required items
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.required"), this.x + 139, this.y + 81, 0x3F3F3F, false);
        context.drawTexture(ICONS, this.x + 142 + this.textRenderer.getWidth(Text.translatable("text.dungeonz.required")), this.y + 78,
                52 + (InventoryHelper.hasRequiredItemStacks(this.playerEntity.getInventory(), this.handler.getRequiredItemStacks().get(this.handler.getDungeonPortalEntity().getDifficulty())) ? 0 : 14), 0, 14, 14);

        List<ItemStack> requiredItems = this.handler.getRequiredItemStacks().get(this.handler.getDungeonPortalEntity().getDifficulty());
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
            context.drawText(this.textRenderer, Text.translatable("text.dungeonz.nothing_required"), this.x + 144, this.y + 93, 0x3F3F3F, false);
        }

        // Possible loot
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.possible"), this.x + 139, this.y + 115, 0x3F3F3F, false);
        if (this.handler.getPossibleLootDifficultyItemStackMap().size() > 0 && this.handler.getPossibleLootDifficultyItemStackMap().containsKey(this.handler.getDungeonPortalEntity().getDifficulty())
                && this.handler.getPossibleLootDifficultyItemStackMap().get(this.handler.getDungeonPortalEntity().getDifficulty()).size() > 0) {
            int l = 0;
            int o = 0;
            for (int i = 0; i < this.handler.getPossibleLootDifficultyItemStackMap().get(this.handler.getDungeonPortalEntity().getDifficulty()).size() && i < 10; i++) {
                context.drawItem(this.handler.getPossibleLootDifficultyItemStackMap().get(this.handler.getDungeonPortalEntity().getDifficulty()).get(i), this.x + 144 + l, this.y + o + 127);
                context.drawItemInSlot(this.textRenderer, this.handler.getPossibleLootDifficultyItemStackMap().get(this.handler.getDungeonPortalEntity().getDifficulty()).get(i), this.x + 144 + l,
                        this.y + o + 127);

                if (this.isPointWithinBounds(144 + l, o + 127, 16, 16, mouseX, mouseY)) {
                    context.drawTooltip(this.textRenderer, this.handler.getPossibleLootDifficultyItemStackMap().get(this.handler.getDungeonPortalEntity().getDifficulty()).get(i).getName(), mouseX,
                            mouseY);
                }
                l += 18;
                if (i == 4) {
                    l = 0;
                    o = 18;
                }
            }
        }

        context.drawText(this.textRenderer, Text.translatable("dungeonz.difficulty"), this.x + 139, this.y + 24, 0x3F3F3F, false);
        context.drawText(this.textRenderer, Text.translatable("text.dungeonz.private"), this.x + 169, this.y + 65, 0x3F3F3F, false);
        // Min group size
        // if (this.handler.getDungeonPortalEntity().getDungeonPlayerCount() <= 0 && this.handler.getDungeonPortalEntity().getMinGroupSize() > 1) {
        //     context.drawText(this.textRenderer, Text.translatable("text.dungeonz.waiting_player_list", this.handler.getWaitingGroupSize(), this.handler.getDungeonPortalEntity().getMinGroupSize()),
        //             this.x + 9, this.y + 187, 0x3F3F3F, false);
        // }
        // LevelZ
        if (DungeonzMain.isLevelZLoaded) {
            context.drawText(this.textRenderer, Text.translatable("text.dungeonz.required_level", this.handler.getRequiredLevel()), this.x + 139, this.y + 180, 0x3F3F3F, false);
        }
        // Information
        if (this.isPointWithinBounds(230, 6, 20, 18, mouseX, mouseY)) {
            context.drawTexture(ICONS, this.x + 230, this.y+6, 20, 84, 20, 18);

            List<Text> dungeonInfo = new ArrayList<>();
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info"));
            for (int i = 1; i < 10; i++) {

                String dungeonInfoTooltip = "dungeon." + this.handler.getDungeonPortalEntity().getDungeonType() + ".description" + "." + i;
                Text dungeonInfoText = Text.translatable(dungeonInfoTooltip);

                if (dungeonInfoText.getString().equals(dungeonInfoTooltip)) {
                    break;
                }
                dungeonInfo.add(dungeonInfoText);
            }

            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.respawn" + (this.handler.isAllowRespawn() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.keep_inventory" + (this.handler.isKeepInventory() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.positive_effects" + (this.handler.isAllowPositiveEffects() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.ender_pearl" + (this.handler.isAllowEnderPearl() ? "" : ".disabled")));
            dungeonInfo.add(Text.translatable("dungeonz.dungeon.info.elytra" + (this.handler.isAllowElytra() ? "" : ".disabled")));

            context.drawTooltip(this.textRenderer, dungeonInfo, mouseX, mouseY);
        } else {
            context.drawTexture(ICONS, this.x + 230, this.y+6, 0, 84, 20, 18);
        }
        context.drawItem(INFO_ITEMSTACK,this.x + 232, this.y+7);
        
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(backgroundTexture, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onSlotUpdate(ScreenHandler var1, int var2, ItemStack var3) {
    }

    @Override
    public void onPropertyUpdate(ScreenHandler var1, int var2, int var3) {
    }

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
            context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, o | MathHelper.ceil(this.alpha * 255.0f) << 24);

            if (!this.active && this.isHovered()) {
                Text text = null;
                if (DungeonPortalScreen.this.handler.getDungeonPortalEntity().isOnCooldown((int) DungeonPortalScreen.this.client.world.getTime())) {
                    int cooldown = (DungeonPortalScreen.this.handler.getDungeonPortalEntity().getCooldownTime() - (int) DungeonPortalScreen.this.client.world.getTime()) / 20;
                    int seconds = cooldown % 60;
                    int minutes = cooldown / 60 % 60;
                    int hours = cooldown / 60 / 60;
                    text = Text.translatable("text.dungeonz.dungeon_cooldown_time", hours, minutes, seconds);
                } else if ((DungeonPortalScreen.this.handler.getDungeonPortalEntity().getDungeonPlayerUuids().size()
                        + DungeonPortalScreen.this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().size()) >= DungeonPortalScreen.this.handler.getDungeonPortalEntity()
                        .getMaxGroupSize()) {
                    text = Text.translatable("text.dungeonz.dungeon_full");
                } else if (client.player != null && !DungeonPortalScreen.this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().isEmpty()
                        && DungeonPortalScreen.this.handler.getDungeonPortalEntity().getDeadDungeonPlayerUuids().contains(client.player.getUuid())) {
                    text = Text.translatable("text.dungeonz.dead_player");
                } else if (!InventoryHelper.hasRequiredItemStacks(client.player.getInventory(), DungeonPortalScreen.this.handler.getRequiredItemStacks().get(DungeonPortalScreen.this.handler.getDungeonPortalEntity().getDifficulty()))) {
                    text = Text.translatable("text.dungeonz.missing");
                } else if (DungeonzMain.isLevelZLoaded) {
                    PlayerStatsManager PlayerStatsManager = ((PlayerStatsManagerAccess) DungeonPortalScreen.this.playerEntity).getPlayerStatsManager();
                    if (PlayerStatsManager.getOverallLevel() < DungeonPortalScreen.this.handler.getRequiredLevel()) {
                        text = Text.translatable("text.dungeonz.required_level", DungeonPortalScreen.this.handler.getRequiredLevel());
                    }
                } else if (DungeonPortalScreen.this.handler.getDungeonPortalEntity().getWaitingUuids().contains(DungeonPortalScreen.this.playerEntity.getUuid())) {
                    text = Text.translatable("text.dungeonz.dungeon_waiting");
                }
                if (text != null) {
                    context.drawTooltip(textRenderer, text, mouseX, mouseY);
                }
            }
        }

    }

    public class DungeonDifficultyButton extends ButtonWidget {
        private Text text;

        public DungeonDifficultyButton(int x, int y, Text text, ButtonWidget.PressAction onPress) {
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
            int j = this.getTextureY();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            context.drawTexture(ICONS, this.getX(), this.getY(), 0, j, this.width, this.height);
            int o = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredTextWithShadow(textRenderer, this.text, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, o | MathHelper.ceil(this.alpha * 255.0f) << 24);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            if (!this.active && this.isHovered()) {
                Text text = null;
                if (DungeonPortalScreen.this.handler.getDungeonPortalEntity().getWaitingUuids().size() >= 1) {
                    text = Text.translatable("text.dungeonz.dungeon_waiting");
                }
                if (text != null) {
                    context.drawTooltip(textRenderer, text, mouseX, mouseY);
                }
            }
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