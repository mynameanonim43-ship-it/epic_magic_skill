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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modern, high-depth RPG Character Screen opened by pressing 'O'.
 * Features:
 * - Ultra-Dark / Pitch Black obsidian aesthetics with rich layered depth, beveled borders, and gradients.
 * - Left Panel: 3D player preview with ground vignette, nameplate badge, and 3D glossy EXP progress bar.
 * - Right Panel: Tactile attribute cards with dynamic hover highlights, accent gems, and base/allocated breakdowns.
 * - Staged Allocation:
 *   * Points can be incremented [+] or decremented [-] freely before applying.
 *   * Custom modern widgets for [+], [-], Reset, and Apply buttons.
 *   * "Reset": Reverts all unapplied changes in current editing session.
 *   * "Apply": Permanently commits and locks points to the server.
 */
public class RpgCharacterScreen extends Screen {

    // UI Dimensions
    private static final int PANEL_WIDTH = 370;
    private static final int PANEL_HEIGHT = 262;

    private static final int LEFT_BOX_WIDTH = 105;
    private static final int RIGHT_BOX_WIDTH = 236;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = 3;

    // Palette Constants (ARGB)
    private static final int COLOR_WINDOW_BORDER_OUTER = 0xFF2D2D38;
    private static final int COLOR_WINDOW_BORDER_INNER_LIGHT = 0xFF3D3D4E;
    private static final int COLOR_WINDOW_BORDER_INNER_DARK = 0xFF121217;
    private static final int COLOR_WINDOW_BG_TOP = 0xF9121217;
    private static final int COLOR_WINDOW_BG_BOTTOM = 0xFB070709;

    private static final int COLOR_HEADER_BG_TOP = 0xFF1C1C27;
    private static final int COLOR_HEADER_BG_BOTTOM = 0xFF101017;
    private static final int COLOR_FOOTER_BG_TOP = 0xFF14141C;
    private static final int COLOR_FOOTER_BG_BOTTOM = 0xFF0A0A0E;

    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SILVER = 0xFFD2D2D8;
    private static final int COLOR_TEXT_GRAY = 0xFFA0A0AA;
    private static final int COLOR_TEXT_POINTS = 0xFF55FF55;
    private static final int COLOR_TEXT_PENDING = 0xFF55FFFF;

    private int leftPos;
    private int topPos;

    private List<RpgAttributeType> visibleAttributes;
    private final Map<RpgAttributeType, ModernRpgButton> plusButtons = new EnumMap<>(RpgAttributeType.class);
    private final Map<RpgAttributeType, ModernRpgButton> minusButtons = new EnumMap<>(RpgAttributeType.class);
    private ModernRpgButton resetButton;
    private ModernRpgButton applyButton;

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

        int rightPanelX = this.leftPos + LEFT_BOX_WIDTH + 17;
        int listStartY = this.topPos + 32;

        // Create [-] and [+] modern custom buttons for each visible attribute row
        for (int i = 0; i < this.visibleAttributes.size(); i++) {
            RpgAttributeType type = this.visibleAttributes.get(i);
            int rowY = listStartY + i * (ROW_HEIGHT + ROW_SPACING);

            // [-] Decrement Button
            int minusBtnX = rightPanelX + RIGHT_BOX_WIDTH - 42;
            int buttonY = rowY + 3;

            ModernRpgButton minusBtn = new ModernRpgButton(
                    minusBtnX,
                    buttonY,
                    18,
                    18,
                    Component.literal("-").withStyle(ChatFormatting.BOLD),
                    btn -> {
                        int currentPending = pendingAllocations.getOrDefault(type, 0);
                        if (currentPending > 0) {
                            pendingAllocations.put(type, currentPending - 1);
                            refreshButtonStates();
                        }
                    },
                    Tooltip.create(Component.literal("Undo 1 staged point").withStyle(ChatFormatting.RED)),
                    ModernRpgButton.ButtonStyle.MINUS
            );

            // [+] Increment Button
            int plusBtnX = rightPanelX + RIGHT_BOX_WIDTH - 21;

            ModernRpgButton plusBtn = new ModernRpgButton(
                    plusBtnX,
                    buttonY,
                    18,
                    18,
                    Component.literal("+").withStyle(ChatFormatting.BOLD),
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
                    },
                    Tooltip.create(
                            Component.literal(type.getDisplayName()).withStyle(ChatFormatting.GOLD)
                                    .append(Component.literal("\n" + type.getDynamicSummary()).withStyle(ChatFormatting.GRAY))
                    ),
                    ModernRpgButton.ButtonStyle.PLUS
            );

            this.minusButtons.put(type, minusBtn);
            this.plusButtons.put(type, plusBtn);
            this.addRenderableWidget(minusBtn);
            this.addRenderableWidget(plusBtn);
        }

        // Footer Action Buttons: Reset (Left) & Apply (Right)
        int footerY = this.topPos + PANEL_HEIGHT - 26;

        this.resetButton = new ModernRpgButton(
                this.leftPos + PANEL_WIDTH - 152,
                footerY,
                70,
                20,
                Component.literal("\u21BA Reset").withStyle(ChatFormatting.BOLD),
                btn -> {
                    // Reset pending unapplied changes
                    for (RpgAttributeType type : RpgAttributeType.values()) {
                        pendingAllocations.put(type, 0);
                    }
                    refreshButtonStates();
                },
                Tooltip.create(Component.translatable("gui.attributerpg.reset_points.tooltip")),
                ModernRpgButton.ButtonStyle.RESET
        );

        this.applyButton = new ModernRpgButton(
                this.leftPos + PANEL_WIDTH - 76,
                footerY,
                70,
                20,
                Component.literal("\u2714 Apply").withStyle(ChatFormatting.BOLD),
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
                },
                Tooltip.create(Component.translatable("gui.attributerpg.apply_points.tooltip")),
                ModernRpgButton.ButtonStyle.APPLY
        );

        this.addRenderableWidget(this.resetButton);
        this.addRenderableWidget(this.applyButton);

        refreshButtonStates();
    }

    /**
     * Calculates remaining available status points considering unapplied allocations.
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
     * Updates active and enabled states for buttons.
     */
    public void refreshButtonStates() {
        IRpgStats stats = getClientStats();
        int remainingPoints = getEffectiveAvailablePoints();
        boolean hasPoints = remainingPoints > 0;
        int totalPending = getTotalPendingAllocations();

        for (Map.Entry<RpgAttributeType, ModernRpgButton> entry : this.plusButtons.entrySet()) {
            RpgAttributeType type = entry.getKey();
            ModernRpgButton plusBtn = entry.getValue();
            int current = stats.getAttributeValue(type) + pendingAllocations.getOrDefault(type, 0);
            boolean belowCap = current < RpgConfig.getMaxAttributeCap();
            plusBtn.active = hasPoints && belowCap;
        }

        for (Map.Entry<RpgAttributeType, ModernRpgButton> entry : this.minusButtons.entrySet()) {
            RpgAttributeType type = entry.getKey();
            ModernRpgButton minusBtn = entry.getValue();
            int pending = pendingAllocations.getOrDefault(type, 0);
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

        // 1. Soft Drop Shadow behind the main window
        renderDropShadow(graphics, this.leftPos, this.topPos, PANEL_WIDTH, PANEL_HEIGHT);

        // 2. Main Beveled Window Container
        drawElevatedBox(
                graphics,
                this.leftPos,
                this.topPos,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                COLOR_WINDOW_BG_TOP,
                COLOR_WINDOW_BG_BOTTOM,
                COLOR_WINDOW_BORDER_INNER_LIGHT,
                COLOR_WINDOW_BORDER_INNER_DARK
        );
        // Outer crisp charcoal rim
        drawBoxOutline(graphics, this.leftPos, this.topPos, PANEL_WIDTH, PANEL_HEIGHT, COLOR_WINDOW_BORDER_OUTER);

        // 3. Header Bar with Gradient & Tapered Glowing Divider
        graphics.fillGradient(
                this.leftPos + 1,
                this.topPos + 1,
                this.leftPos + PANEL_WIDTH - 1,
                this.topPos + 25,
                COLOR_HEADER_BG_TOP,
                COLOR_HEADER_BG_BOTTOM
        );
        renderHeaderAccentLine(graphics, this.leftPos + 8, this.topPos + 25, PANEL_WIDTH - 16);

        // Header Title
        graphics.drawCenteredString(
                this.font,
                Component.literal("\u2726 ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("gui.attributerpg.character_screen.title").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                        .append(Component.literal(" \u2726").withStyle(ChatFormatting.GOLD)),
                this.leftPos + PANEL_WIDTH / 2,
                this.topPos + 8,
                COLOR_TEXT_WHITE
        );

        // 4. Left Section: 3D Character Preview & Glossy EXP Card
        renderLeftCharacterSection(graphics, player, stats, mouseX, mouseY);

        // 5. Right Section: Attribute Allocation Cards
        renderRightAttributeList(graphics, stats, mouseX, mouseY);

        // 6. Footer Section: Status Points Badge & Actions
        renderFooterSection(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Renders a soft radial-like drop shadow around the panel for elevation.
     */
    private void renderDropShadow(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x - 5, y - 5, x + w + 5, y + h + 5, 0x14000000);
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x2A000000);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x50000000);
    }

    /**
     * Renders a glowing divider under the header that softly tapers at the ends.
     */
    private void renderHeaderAccentLine(GuiGraphics graphics, int x, int y, int width) {
        int half = width / 2;
        // Left gradient: transparent to bright gold
        graphics.fillGradient(x, y, x + half, y + 1, 0x00FFD700, 0xC0FFD700);
        // Right gradient: bright gold to transparent
        graphics.fillGradient(x + half, y, x + width, y + 1, 0xC0FFD700, 0x00FFD700);
        // Sub-shadow line
        graphics.fill(x, y + 1, x + width, y + 2, 0xFF08080C);
    }

    private void renderLeftCharacterSection(GuiGraphics graphics, Player player, IRpgStats stats, int mouseX, int mouseY) {
        int boxX = this.leftPos + 9;
        int boxY = this.topPos + 32;
        int boxHeight = 154;

        // Left Container with elevated bevel
        drawElevatedBox(
                graphics,
                boxX,
                boxY,
                LEFT_BOX_WIDTH,
                boxHeight,
                0xFF14141E,
                0xFF09090D,
                0xFF2D2D3E,
                0xFF050508
        );

        if (player != null) {
            // Player Nameplate Header Badge
            int nameplateY = boxY + 5;
            int nameplateH = 15;
            drawElevatedBox(
                    graphics,
                    boxX + 5,
                    nameplateY,
                    LEFT_BOX_WIDTH - 10,
                    nameplateH,
                    0xFF222230,
                    0xFF14141E,
                    0xFF38384D,
                    0xFF0A0A0E
            );

            graphics.drawCenteredString(
                    this.font,
                    player.getName().getString(),
                    boxX + LEFT_BOX_WIDTH / 2,
                    nameplateY + 4,
                    COLOR_TEXT_WHITE
            );

            // 3D Player Entity Preview
            int modelCenterX = boxX + LEFT_BOX_WIDTH / 2;
            int modelBottomY = boxY + boxHeight - 14;
            int modelScale = 52;

            // Ground Vignette/Shadow beneath entity feet
            graphics.fill(modelCenterX - 24, modelBottomY - 2, modelCenterX + 24, modelBottomY + 2, 0x40000000);
            graphics.fill(modelCenterX - 16, modelBottomY - 1, modelCenterX + 16, modelBottomY + 1, 0x55000000);

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    modelCenterX,
                    modelBottomY,
                    modelScale,
                    (float) modelCenterX - mouseX,
                    (float) (modelBottomY - 75) - mouseY,
                    player
            );

            // RPG Level & EXP Card below 3D preview
            int infoY = boxY + boxHeight + 5;
            int infoHeight = 36;
            drawElevatedBox(
                    graphics,
                    boxX,
                    infoY,
                    LEFT_BOX_WIDTH,
                    infoHeight,
                    0xFF151520,
                    0xFF0C0C12,
                    0xFF2E2E3F,
                    0xFF060609
            );

            int rpgLvl = stats.getRpgLevel();
            int curExp = stats.getRpgExp();
            int neededExp = IRpgStats.getExpNeededForLevel(rpgLvl);

            // RPG Level Pill
            graphics.drawString(this.font, "\u2B50 Lv. " + rpgLvl, boxX + 7, infoY + 5, COLOR_TEXT_WHITE, true);

            // 3D Recessed EXP Bar with Gloss Sheen
            int barX = boxX + 7;
            int barY = infoY + 16;
            int barWidth = LEFT_BOX_WIDTH - 14;
            int barHeight = 6;
            float progress = Math.min(1.0F, Math.max(0.0F, (float) curExp / (float) neededExp));
            int filledWidth = (int) (barWidth * progress);

            // Recessed groove track
            drawRecessedGroove(graphics, barX, barY, barWidth, barHeight);

            // Glowing gradient fill + gloss highlight
            if (filledWidth > 0) {
                graphics.fillGradient(barX + 1, barY + 1, barX + filledWidth, barY + barHeight - 1, 0xFF00FF88, 0xFF009944);
                // 1px top gloss highlight
                graphics.fill(barX + 1, barY + 1, barX + filledWidth, barY + 2, 0x90FFFFFF);
            }

            // EXP Text readout
            String expText = curExp + " / " + neededExp;
            graphics.drawString(this.font, expText, boxX + 7, infoY + 25, COLOR_TEXT_SILVER, false);

            // Tooltip when hovering over EXP section
            if (mouseX >= boxX && mouseX <= boxX + LEFT_BOX_WIDTH && mouseY >= infoY && mouseY <= infoY + infoHeight) {
                int percent = (int) (progress * 100.0F);
                graphics.renderTooltip(
                        this.font,
                        List.of(
                                Component.literal("RPG Level: " + rpgLvl).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                Component.literal("EXP: " + curExp + " / " + neededExp + " (" + percent + "%)").withStyle(ChatFormatting.WHITE),
                                Component.literal("EXP Needed for Next Level: " + Math.max(0, neededExp - curExp)).withStyle(ChatFormatting.AQUA),
                                Component.literal("Progression: Scales steeper with each level").withStyle(ChatFormatting.DARK_GRAY)
                        ),
                        java.util.Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private void renderRightAttributeList(GuiGraphics graphics, IRpgStats stats, int mouseX, int mouseY) {
        int rightPanelX = this.leftPos + LEFT_BOX_WIDTH + 17;
        int listStartY = this.topPos + 32;

        for (int i = 0; i < this.visibleAttributes.size(); i++) {
            RpgAttributeType type = this.visibleAttributes.get(i);
            int rowY = listStartY + i * (ROW_HEIGHT + ROW_SPACING);

            boolean isHovered = mouseX >= rightPanelX && mouseX <= rightPanelX + RIGHT_BOX_WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;

            int bgTop = isHovered ? 0xFF1E1E2A : (i % 2 == 0 ? 0xFF14141C : 0xFF161620);
            int bgBottom = isHovered ? 0xFF121219 : (i % 2 == 0 ? 0xFF0A0A0E : 0xFF0D0D12);
            int borderLight = isHovered ? 0xFF46465D : 0xFF2A2A38;
            int borderDark = isHovered ? 0xFF0F0F16 : 0xFF050508;

            // Elevated Card Box
            drawElevatedBox(graphics, rightPanelX, rowY, RIGHT_BOX_WIDTH, ROW_HEIGHT, bgTop, bgBottom, borderLight, borderDark);

            // Left Accent Gem Strip with bright top cap
            int accent = type.getAccentColor();
            graphics.fillGradient(rightPanelX + 1, rowY + 1, rightPanelX + 4, rowY + ROW_HEIGHT - 1, accent, darkenColor(accent, 0.5f));
            graphics.fill(rightPanelX + 1, rowY + 1, rightPanelX + 4, rowY + 2, brightenColor(accent, 1.3f));

            int appliedTotal = stats.getAttributeValue(type);
            int baseValue = stats.getBaseAttributeValue(type);
            int allocatedValue = stats.getAllocatedPoints(type);
            int pending = pendingAllocations.getOrDefault(type, 0);

            String rowHeader = type.getIconPlaceholder() + " " + type.getDisplayName() + ": ";
            int textX = rightPanelX + 8;
            int textY = rowY + 4;

            // Attribute Header Name
            graphics.drawString(this.font, rowHeader, textX, textY, COLOR_TEXT_WHITE, true);
            int valueX = textX + this.font.width(rowHeader);

            // Value Display
            graphics.drawString(this.font, String.valueOf(appliedTotal), valueX, textY, COLOR_TEXT_WHITE, true);

            // Display pending addition "(+N)" in bright cyan if present
            int afterValX = valueX + this.font.width(String.valueOf(appliedTotal));
            if (pending > 0) {
                graphics.drawString(this.font, " (+" + pending + ")", afterValX, textY, COLOR_TEXT_PENDING, true);
                afterValX += this.font.width(" (+" + pending + ")");
            }

            // Display base starting stat tag if configured
            if (baseValue > 0) {
                graphics.drawString(this.font, " [Base " + baseValue + "]", afterValX, textY, 0xFF88A0B8, false);
            }

            // Subtitle: live dynamic scaling description
            String summary = type.getDynamicSummary();
            graphics.drawString(this.font, summary, textX, rowY + 14, COLOR_TEXT_GRAY, false);

            // Hover tooltip with detailed breakdown
            if (isHovered && mouseX <= rightPanelX + RIGHT_BOX_WIDTH - 46) {
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(Component.literal(type.getIconPlaceholder() + " " + type.getDisplayName())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                tooltipLines.add(Component.literal("Total Effective: " + (appliedTotal + pending))
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

                if (baseValue > 0) {
                    tooltipLines.add(Component.literal("  \u2022 Base Starting: " + baseValue)
                            .withStyle(ChatFormatting.BLUE));
                }
                tooltipLines.add(Component.literal("  \u2022 Allocated Points: " + allocatedValue)
                        .withStyle(ChatFormatting.GRAY));

                if (pending > 0) {
                    tooltipLines.add(Component.literal("  \u2022 Staged Pending: +" + pending)
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                }
                tooltipLines.add(Component.literal("Bonus per Point: " + summary)
                        .withStyle(ChatFormatting.YELLOW));

                graphics.renderTooltip(
                        this.font,
                        tooltipLines,
                        java.util.Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private void renderFooterSection(GuiGraphics graphics) {
        int footerTop = this.topPos + PANEL_HEIGHT - 32;

        // Footer Gradient Background & Top Bevel
        graphics.fillGradient(
                this.leftPos + 1,
                footerTop,
                this.leftPos + PANEL_WIDTH - 1,
                this.topPos + PANEL_HEIGHT - 1,
                COLOR_FOOTER_BG_TOP,
                COLOR_FOOTER_BG_BOTTOM
        );
        graphics.fill(this.leftPos + 1, footerTop, this.leftPos + PANEL_WIDTH - 1, footerTop + 1, 0xFF2A2A38);
        graphics.fill(this.leftPos + 1, footerTop + 1, this.leftPos + PANEL_WIDTH - 1, footerTop + 2, 0xFF08080C);

        int remainingPoints = getEffectiveAvailablePoints();
        int totalPending = getTotalPendingAllocations();

        // Available Status Points Pill Badge
        int pointsBadgeX = this.leftPos + 9;
        int pointsBadgeY = footerTop + 5;
        int pointsBadgeW = 126;
        int pointsBadgeH = 22;

        drawElevatedBox(
                graphics,
                pointsBadgeX,
                pointsBadgeY,
                pointsBadgeW,
                pointsBadgeH,
                0xFF1C1C29,
                0xFF101018,
                0xFF2F2F42,
                0xFF08080D
        );

        String pointsLabel = "\u2B50 Points: ";
        graphics.drawString(this.font, pointsLabel, pointsBadgeX + 6, pointsBadgeY + 7, COLOR_TEXT_WHITE, true);
        int valX = pointsBadgeX + 6 + this.font.width(pointsLabel);
        graphics.drawString(
                this.font,
                String.valueOf(remainingPoints),
                valX,
                pointsBadgeY + 7,
                remainingPoints > 0 ? COLOR_TEXT_POINTS : COLOR_TEXT_SILVER,
                true
        );

        // Staged Pending Badge
        if (totalPending > 0) {
            int pendingBadgeX = pointsBadgeX + pointsBadgeW + 6;
            int pendingBadgeW = 76;
            drawElevatedBox(
                    graphics,
                    pendingBadgeX,
                    pointsBadgeY,
                    pendingBadgeW,
                    pointsBadgeH,
                    0xFF0F262E,
                    0xFF08151A,
                    0xFF225B6E,
                    0xFF050D11
            );
            graphics.drawCenteredString(
                    this.font,
                    "+" + totalPending + " Pending",
                    pendingBadgeX + pendingBadgeW / 2,
                    pointsBadgeY + 7,
                    COLOR_TEXT_PENDING
            );
        }
    }

    /**
     * Renders a 3D beveled elevated container with top/left highlight and bottom/right shadow.
     */
    private static void drawElevatedBox(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int bgTop,
            int bgBottom,
            int borderLight,
            int borderDark
    ) {
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, bgTop, bgBottom);
        graphics.fill(x, y, x + width, y + 1, borderLight);
        graphics.fill(x, y + 1, x + 1, y + height - 1, borderLight);
        graphics.fill(x, y + height - 1, x + width, y + height, borderDark);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderDark);
    }

    /**
     * Renders an inset / recessed groove for progress bars.
     */
    private static void drawRecessedGroove(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF08080B);
        graphics.fill(x, y, x + width, y + 1, 0xFF040406);
        graphics.fill(x, y + 1, x + 1, y + height - 1, 0xFF040406);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF1C1C26);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFF1C1C26);
    }

    private static void drawBoxOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (Math.min(255, Math.max(0, r)) << 16) | (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, b));
    }

    private static int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (Math.min(255, Math.max(0, r)) << 16) | (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, b));
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

    /**
     * Modern custom-rendered button widget that eliminates vanilla flat textures
     * and seamlessly integrates with the dark RPG aesthetic.
     */
    public static class ModernRpgButton extends Button {

        public enum ButtonStyle {
            PLUS, MINUS, RESET, APPLY
        }

        private final ButtonStyle style;

        public ModernRpgButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                Tooltip tooltip,
                ButtonStyle style
        ) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.style = style;
            if (tooltip != null) {
                this.setTooltip(tooltip);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hovered = this.isHoveredOrFocused();

            if (!this.active) {
                // Disabled State
                drawElevatedBox(graphics, x, y, w, h, 0xFF121217, 0xFF0B0B0E, 0xFF1B1B24, 0xFF07070A);
                graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + w / 2, y + (h - 8) / 2, 0xFF4A4A56);
                return;
            }

            int bgTop = 0xFF22222E;
            int bgBottom = 0xFF12121A;
            int borderLight = 0xFF44445A;
            int borderDark = 0xFF0C0C12;
            int textColor = 0xFFFFFFFF;

            switch (this.style) {
                case PLUS -> {
                    if (hovered) {
                        bgTop = 0xFF1A4526;
                        bgBottom = 0xFF0E2A18;
                        borderLight = 0xFF4AE074;
                        borderDark = 0xFF103F1E;
                        textColor = 0xFF99FFAA;
                    } else {
                        bgTop = 0xFF11301A;
                        bgBottom = 0xFF091E10;
                        borderLight = 0xFF2EB552;
                        borderDark = 0xFF082010;
                        textColor = 0xFF66FF88;
                    }
                }
                case MINUS -> {
                    if (hovered) {
                        bgTop = 0xFF4D181C;
                        bgBottom = 0xFF2D0B0E;
                        borderLight = 0xFFE64A57;
                        borderDark = 0xFF3D0F14;
                        textColor = 0xFFFF8888;
                    } else {
                        bgTop = 0xFF361013;
                        bgBottom = 0xFF1C0709;
                        borderLight = 0xFFB32F3A;
                        borderDark = 0xFF170608;
                        textColor = 0xFFFF5555;
                    }
                }
                case RESET -> {
                    if (hovered) {
                        bgTop = 0xFF3D2A12;
                        bgBottom = 0xFF24180A;
                        borderLight = 0xFFE0A43F;
                        borderDark = 0xFF33200A;
                        textColor = 0xFFFFD280;
                    } else {
                        bgTop = 0xFF2A1C0B;
                        bgBottom = 0xFF170E05;
                        borderLight = 0xFFA67123;
                        borderDark = 0xFF140D04;
                        textColor = 0xFFFFB347;
                    }
                }
                case APPLY -> {
                    if (hovered) {
                        bgTop = 0xFF184B2B;
                        bgBottom = 0xFF0C2B18;
                        borderLight = 0xFF3EEB77;
                        borderDark = 0xFF114221;
                        textColor = 0xFFAAFFBB;
                    } else {
                        bgTop = 0xFF0E361D;
                        bgBottom = 0xFF072111;
                        borderLight = 0xFF28BD58;
                        borderDark = 0xFF082312;
                        textColor = 0xFF55FF77;
                    }
                }
            }

            drawElevatedBox(graphics, x, y, w, h, bgTop, bgBottom, borderLight, borderDark);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
        }
    }
}
