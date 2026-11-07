package com.attributerpg.client.gui;

import com.attributerpg.attribute.RpgAttributeType;
import com.attributerpg.capability.IRpgStats;
import com.attributerpg.capability.RpgStats;
import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.client.KeyBindings;
import com.attributerpg.config.RpgConfig;
import com.attributerpg.network.ModNetworking;
import com.attributerpg.network.packet.PacketApplyAllocations;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modern, dark-themed RPG Character Screen opened by pressing 'O'.
 * Features:
 * - Left Panel: 3D interactive preview model of the player character + Level & Peak Level stats.
 * - Right Panel: List of RPG attributes with [-] (decrement) and [+] (increment) buttons.
 * - Staged Allocation:
 *   * Points can be incremented [+] or decremented [-] freely before applying.
 *   * "Reset": Reverts all unapplied changes made during the current editing session.
 *   * "Apply": Permanently saves and locks the points to the server. Once applied, they cannot be reset!
 * - Removed +1 Max Stamina for Epic Fight.
 */
public class RpgCharacterScreen extends Screen {

    // UI Dimensions
    private static final int PANEL_WIDTH = 370;
    private static final int PANEL_HEIGHT = 250;

    private static final int LEFT_BOX_WIDTH = 105;
    private static final int RIGHT_BOX_WIDTH = 235;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = 3;

    // Pure Ultra-Dark / Pitch Black Theme Palette (ARGB)
    private static final int COLOR_BG_OUTER_BORDER = 0xFF222222; // Minimalist dark gray outer border
    private static final int COLOR_BG_MAIN = 0xFA08080A;         // Deep near-pitch black body
    private static final int COLOR_BG_HEADER = 0xFF0D0D10;       // Dark black header & footer
    private static final int COLOR_BG_SUBPANEL = 0xFF0A0A0D;     // Dark inner containers
    private static final int COLOR_BG_ROW_EVEN = 0xFF101014;     // Pitch black row alternate 1
    private static final int COLOR_BG_ROW_ODD = 0xFF141418;      // Pitch black row alternate 2
    private static final int COLOR_BORDER_SUBTLE = 0xFF1F1F24;   // Subtle separator lines
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;    // Brilliant pure white text
    private static final int COLOR_TEXT_SECONDARY = 0xFFE0E0E0;  // Bright clean white-silver text
    private static final int COLOR_TEXT_VALUE = 0xFFFFFFFF;      // Brilliant pure white numbers
    private static final int COLOR_TEXT_POINTS = 0xFF55FF55;     // Bright neon green
    private static final int COLOR_TEXT_PENDING = 0xFF55FFFF;    // Bright neon cyan

    private int leftPos;
    private int topPos;

    private List<RpgAttributeType> visibleAttributes;
    private final Map<RpgAttributeType, Button> plusButtons = new EnumMap<>(RpgAttributeType.class);
    private final Map<RpgAttributeType, Button> minusButtons = new EnumMap<>(RpgAttributeType.class);
    private Button resetButton;
    private Button applyButton;

    // Pending allocations in current GUI session (staged changes)
    private final Map<RpgAttributeType, Integer> pendingAllocations = new EnumMap<>(RpgAttributeType.class);

    public RpgCharacterScreen() {
        super(Component.translatable("gui.attributerpg.character_screen.title"));
        for (RpgAttributeType type : RpgAttributeType.values()) {
            pendingAllocations.put(type, 0);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        this.visibleAttributes = RpgAttributeType.getVisibleAttributes();
        this.plusButtons.clear();
        this.minusButtons.clear();

        int rightPanelX = this.leftPos + LEFT_BOX_WIDTH + 18;
        int listStartY = this.topPos + 32;

        // Create [-] and [+] buttons for each visible attribute row
        for (int i = 0; i < this.visibleAttributes.size(); i++) {
            RpgAttributeType type = this.visibleAttributes.get(i);
            int rowY = listStartY + i * (ROW_HEIGHT + ROW_SPACING);

            // [-] Decrement Button
            int minusBtnX = rightPanelX + RIGHT_BOX_WIDTH - 38;
            int buttonY = rowY + 3;

            Button minusBtn = Button.builder(
                            Component.literal("-").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            btn -> {
                                int currentPending = pendingAllocations.getOrDefault(type, 0);
                                if (currentPending > 0) {
                                    pendingAllocations.put(type, currentPending - 1);
                                    refreshButtonStates();
                                }
                            }
                    )
                    .pos(minusBtnX, buttonY)
                    .size(16, 18)
                    .tooltip(Tooltip.create(Component.literal("Undo 1 point from this attribute").withStyle(ChatFormatting.RED)))
                    .build();

            // [+] Increment Button
            int plusBtnX = rightPanelX + RIGHT_BOX_WIDTH - 20;

            Button plusBtn = Button.builder(
                            Component.literal("+").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                            btn -> {
                                if (getEffectiveAvailablePoints() > 0) {
                                    int currentPending = pendingAllocations.getOrDefault(type, 0);
                                    IRpgStats stats = getClientStats();
                                    int totalEffective = stats.getAttributeValue(type) + currentPending;
                                    if (totalEffective < RpgConfig.getMaxAttributeCap()) {
                                        pendingAllocations.put(type, currentPending + 1);
                                        refreshButtonStates();
                                    }
                                }
                            }
                    )
                    .pos(plusBtnX, buttonY)
                    .size(16, 18)
                    .tooltip(Tooltip.create(
                            Component.literal(type.getDisplayName()).withStyle(ChatFormatting.GOLD)
                                    .append(Component.literal("\n" + type.getDynamicSummary()).withStyle(ChatFormatting.GRAY))
                    ))
                    .build();

            this.minusButtons.put(type, minusBtn);
            this.plusButtons.put(type, plusBtn);
            this.addRenderableWidget(minusBtn);
            this.addRenderableWidget(plusBtn);
        }

        // Footer Buttons: Reset (Left) & Apply (Right)
        int footerY = this.topPos + PANEL_HEIGHT - 27;

        this.resetButton = Button.builder(
                        Component.translatable("gui.attributerpg.reset_points"),
                        btn -> {
                            // Reset pending unapplied changes
                            for (RpgAttributeType type : RpgAttributeType.values()) {
                                pendingAllocations.put(type, 0);
                            }
                            refreshButtonStates();
                        }
                )
                .pos(this.leftPos + PANEL_WIDTH - 150, footerY)
                .size(68, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.attributerpg.reset_points.tooltip")))
                .build();

        this.applyButton = Button.builder(
                        Component.translatable("gui.attributerpg.apply_points").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                        btn -> {
                            // Send batch of pending allocations to server
                            Map<RpgAttributeType, Integer> toApply = new EnumMap<>(RpgAttributeType.class);
                            for (Map.Entry<RpgAttributeType, Integer> entry : pendingAllocations.entrySet()) {
                                if (entry.getValue() > 0) {
                                    toApply.put(entry.getKey(), entry.getValue());
                                }
                            }
                            if (!toApply.isEmpty()) {
                                ModNetworking.sendToServer(new PacketApplyAllocations(toApply));
                                // Clear pending allocations once sent
                                for (RpgAttributeType type : RpgAttributeType.values()) {
                                    pendingAllocations.put(type, 0);
                                }
                                refreshButtonStates();
                            }
                        }
                )
                .pos(this.leftPos + PANEL_WIDTH - 76, footerY)
                .size(68, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.attributerpg.apply_points.tooltip")))
                .build();

        this.addRenderableWidget(this.resetButton);
        this.addRenderableWidget(this.applyButton);

        refreshButtonStates();
    }

    /**
     * Calculates remaining available status points considering current unapplied allocations.
     */
    private int getEffectiveAvailablePoints() {
        IRpgStats stats = getClientStats();
        int basePoints = stats.getStatusPoints();
        int pendingTotal = 0;
        for (int amount : pendingAllocations.values()) {
            pendingTotal += amount;
        }
        return Math.max(0, basePoints - pendingTotal);
    }

    private int getTotalPendingAllocations() {
        int pendingTotal = 0;
        for (int amount : pendingAllocations.values()) {
            pendingTotal += amount;
        }
        return pendingTotal;
    }

    /**
     * Updates active and visible states for [-], [+], Reset, and Apply buttons.
     */
    public void refreshButtonStates() {
        IRpgStats stats = getClientStats();
        int remainingPoints = getEffectiveAvailablePoints();
        boolean hasPoints = remainingPoints > 0;
        int totalPending = getTotalPendingAllocations();

        for (Map.Entry<RpgAttributeType, Button> entry : this.plusButtons.entrySet()) {
            RpgAttributeType type = entry.getKey();
            Button plusBtn = entry.getValue();
            int current = stats.getAttributeValue(type) + pendingAllocations.getOrDefault(type, 0);
            boolean belowCap = current < RpgConfig.getMaxAttributeCap();
            plusBtn.active = hasPoints && belowCap;
        }

        for (Map.Entry<RpgAttributeType, Button> entry : this.minusButtons.entrySet()) {
            RpgAttributeType type = entry.getKey();
            Button minusBtn = entry.getValue();
            int pending = pendingAllocations.getOrDefault(type, 0);
            // [-] button is active only if there are pending unapplied points for this attribute
            minusBtn.active = pending > 0;
        }

        if (this.resetButton != null) {
            this.resetButton.active = totalPending > 0;
        }
        if (this.applyButton != null) {
            this.applyButton.active = totalPending > 0;
        }
    }

    private IRpgStats getClientStats() {
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getCapability(RpgStatsProvider.RPG_STATS).orElseGet(RpgStats::new);
        }
        return new RpgStats();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        refreshButtonStates();

        IRpgStats stats = getClientStats();
        Player player = this.minecraft != null ? this.minecraft.player : null;

        // 1. Main Dark Window Container & Header
        drawBorderedBox(graphics, this.leftPos, this.topPos, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BG_MAIN, COLOR_BG_OUTER_BORDER);
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + PANEL_WIDTH - 1, this.topPos + 24, COLOR_BG_HEADER);
        graphics.fill(this.leftPos + 1, this.topPos + 24, this.leftPos + PANEL_WIDTH - 1, this.topPos + 25, COLOR_BORDER_SUBTLE);

        // Window Title
        graphics.drawCenteredString(
                this.font,
                Component.literal("\u2726 ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("gui.attributerpg.character_screen.title").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                        .append(Component.literal(" \u2726").withStyle(ChatFormatting.GOLD)),
                this.leftPos + PANEL_WIDTH / 2,
                this.topPos + 8,
                COLOR_TEXT_PRIMARY
        );

        // 2. Left Section: 3D Player Preview & Level Information
        renderLeftCharacterSection(graphics, player, stats, mouseX, mouseY);

        // 3. Right Section: Attribute Allocation List
        renderRightAttributeList(graphics, stats, mouseX, mouseY);

        // 4. Footer Section: Available Points, Pending Badge & Action Buttons
        renderFooterSection(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLeftCharacterSection(GuiGraphics graphics, Player player, IRpgStats stats, int mouseX, int mouseY) {
        int boxX = this.leftPos + 10;
        int boxY = this.topPos + 32;
        int boxHeight = 148;

        drawBorderedBox(graphics, boxX, boxY, LEFT_BOX_WIDTH, boxHeight, COLOR_BG_SUBPANEL, COLOR_BORDER_SUBTLE);

        if (player != null) {
            graphics.drawCenteredString(
                    this.font,
                    player.getName().getString(),
                    boxX + LEFT_BOX_WIDTH / 2,
                    boxY + 6,
                    COLOR_TEXT_PRIMARY
            );

            int modelCenterX = boxX + LEFT_BOX_WIDTH / 2;
            int modelBottomY = boxY + boxHeight - 14;
            int modelScale = 52;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    modelCenterX,
                    modelBottomY,
                    modelScale,
                    (float) modelCenterX - mouseX,
                    (float) (modelBottomY - 75) - mouseY,
                    player
            );

            int infoY = boxY + boxHeight + 4;
            int infoHeight = 32;
            drawBorderedBox(graphics, boxX, infoY, LEFT_BOX_WIDTH, infoHeight, COLOR_BG_SUBPANEL, COLOR_BORDER_SUBTLE);

            String currentLvlText = "Current Lv: " + player.experienceLevel;
            String peakLvlText = "Peak Lv: " + stats.getHighestLevelAchieved();

            graphics.drawString(this.font, currentLvlText, boxX + 8, infoY + 6, COLOR_TEXT_PRIMARY, false);
            graphics.drawString(this.font, peakLvlText, boxX + 8, infoY + 18, COLOR_TEXT_SECONDARY, false);
        }
    }

    private void renderRightAttributeList(GuiGraphics graphics, IRpgStats stats, int mouseX, int mouseY) {
        int rightPanelX = this.leftPos + LEFT_BOX_WIDTH + 18;
        int listStartY = this.topPos + 32;

        for (int i = 0; i < this.visibleAttributes.size(); i++) {
            RpgAttributeType type = this.visibleAttributes.get(i);
            int rowY = listStartY + i * (ROW_HEIGHT + ROW_SPACING);
            int rowBg = (i % 2 == 0) ? COLOR_BG_ROW_EVEN : COLOR_BG_ROW_ODD;

            drawBorderedBox(graphics, rightPanelX, rowY, RIGHT_BOX_WIDTH, ROW_HEIGHT, rowBg, COLOR_BORDER_SUBTLE);
            graphics.fill(rightPanelX, rowY, rightPanelX + 3, rowY + ROW_HEIGHT, type.getAccentColor());

            int appliedValue = stats.getAttributeValue(type);
            int pending = pendingAllocations.getOrDefault(type, 0);

            String rowHeader = type.getIconPlaceholder() + " " + type.getDisplayName() + ": ";
            int textX = rightPanelX + 7;
            int textY = rowY + 4;

            graphics.drawString(this.font, rowHeader, textX, textY, COLOR_TEXT_PRIMARY, true);
            int valueX = textX + this.font.width(rowHeader);
            graphics.drawString(this.font, String.valueOf(appliedValue), valueX, textY, COLOR_TEXT_VALUE, true);

            // Display pending addition "+N" in cyan if present
            if (pending > 0) {
                int pendingX = valueX + this.font.width(String.valueOf(appliedValue)) + 2;
                graphics.drawString(this.font, "(+" + pending + ")", pendingX, textY, COLOR_TEXT_PENDING, true);
            }

            String summary = type.getDynamicSummary();
            graphics.drawString(this.font, summary, textX, rowY + 14, COLOR_TEXT_SECONDARY, false);

            if (mouseX >= rightPanelX && mouseX <= rightPanelX + RIGHT_BOX_WIDTH - 42
                    && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT) {
                graphics.renderTooltip(
                        this.font,
                        List.of(
                                Component.literal(type.getIconPlaceholder() + " " + type.getDisplayName())
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                Component.literal("Applied Points: " + appliedValue)
                                        .withStyle(ChatFormatting.WHITE),
                                Component.literal("Pending Unapplied: " + pending)
                                        .withStyle(pending > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                                Component.literal("Per Point: " + type.getDynamicSummary())
                                        .withStyle(ChatFormatting.YELLOW)
                        ),
                        java.util.Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private void renderFooterSection(GuiGraphics graphics) {
        int footerTop = this.topPos + PANEL_HEIGHT - 33;

        graphics.fill(this.leftPos + 1, footerTop, this.leftPos + PANEL_WIDTH - 1, this.topPos + PANEL_HEIGHT - 1, COLOR_BG_HEADER);
        graphics.fill(this.leftPos + 1, footerTop, this.leftPos + PANEL_WIDTH - 1, footerTop + 1, COLOR_BORDER_SUBTLE);

        int remainingPoints = getEffectiveAvailablePoints();
        int totalPending = getTotalPendingAllocations();

        String availableLabel = "Points: ";
        int labelX = this.leftPos + 10;
        int labelY = footerTop + 12;

        graphics.drawString(this.font, availableLabel, labelX, labelY, COLOR_TEXT_PRIMARY, true);
        int pointsValX = labelX + this.font.width(availableLabel);
        graphics.drawString(
                this.font,
                String.valueOf(remainingPoints),
                pointsValX,
                labelY,
                remainingPoints > 0 ? COLOR_TEXT_POINTS : COLOR_TEXT_SECONDARY,
                true
        );

        // Display pending unapplied count indicator
        if (totalPending > 0) {
            String pendingStr = " [Pending: " + totalPending + "]";
            int pendingX = pointsValX + this.font.width(String.valueOf(remainingPoints));
            graphics.drawString(this.font, pendingStr, pendingX, labelY, COLOR_TEXT_PENDING, true);
        }
    }

    private static void drawBorderedBox(GuiGraphics graphics, int x, int y, int width, int height, int bgColor, int borderColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x, y + 1, x + 1, y + height - 1, borderColor);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_CHARACTER_GUI_KEY.matches(keyCode, scanCode)
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
