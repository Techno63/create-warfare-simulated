package com.julian.createwarfare.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CWGuiScreen extends Screen {
    private static final int FIELD_HEIGHT = 34;
    private static final int FIELD_GAP = 6;
    private static final int INPUT_HEIGHT = 18;
    private static final int BUTTON_GAP = 6;
    private static final int MIN_PANEL_HEIGHT = 120;
    private static final int GRID_SIZE = 16;
    private static final int MAJOR_GRID_SIZE = 64;
    private static final int LABEL_BOX_HEIGHT = 18;
    private static final int LABEL_BOX_PADDING = 5;

    private final CWGuiBuilder.OpenPayload payload;
    private final Map<String, String> values = new LinkedHashMap<>();

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int activeField = -1;
    private int cursorPosition;
    private int scrollOffset;
    private long animationTicks;
    private String pressedActionId = "";
    private CWGuiBuilder.VisualElement draggingSlider;

    private CWGuiScreen(CWGuiBuilder.OpenPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;

        for (CWGuiBuilder.NumberField field : payload.fields()) {
            values.put(field.id(), Integer.toString(field.defaultValue()));
        }

        for (CWGuiBuilder.VisualElement visual : payload.visuals()) {
            if (visual.type() == CWGuiBuilder.VisualType.SLIDER) {
                values.put(visual.value(), Integer.toString(visual.count()));
            }
        }
    }

    public static CWGuiScreen fromPayload(CWGuiBuilder.OpenPayload payload) {
        return new CWGuiScreen(payload);
    }

    @Override
    protected void init() {
        updatePosition();
    }

    @Override
    public void tick() {
        super.tick();
        animationTicks++;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (payload.backgroundDimEnabled()) {
            graphics.fillGradient(
                    0, 0, width, height,
                    payload.backgroundTopColor(),
                    payload.backgroundBottomColor()
            );
        }
    }

    private void updatePosition() {
        panelHeight = calculatePanelHeight();
        panelX = payload.panelX() == Integer.MIN_VALUE
                ? (width - payload.panelWidth()) / 2
                : payload.panelX();
        panelY = payload.panelY() == Integer.MIN_VALUE
                ? (height - panelHeight) / 2
                : payload.panelY();
    }

    private int calculatePanelHeight() {
        int maxY = payload.headerHeight() + 10;

        for (int i = 0; i < payload.fields().size(); i++) {
            CWGuiBuilder.NumberField field = payload.fields().get(i);
            int y = field.positioned()
                    ? field.y()
                    : payload.headerHeight() + 18 + i * (FIELD_HEIGHT + FIELD_GAP);

            maxY = Math.max(maxY, y + (field.positioned() ? INPUT_HEIGHT + 14 : FIELD_HEIGHT));
        }

        int defaultButtonY = payload.headerHeight() + 20
                + payload.fields().size() * (FIELD_HEIGHT + FIELD_GAP);

        for (CWGuiBuilder.GuiButton button : payload.buttons()) {
            int y = button.positioned() ? button.y() : defaultButtonY;
            maxY = Math.max(maxY, y + button.height() + 10);
        }

        for (CWGuiBuilder.VisualElement visual : payload.visuals()) {
            int visualHeight = switch (visual.type()) {
                case ITEM, SLOT, LARGE_SLOT, BAR, ARROW_LEFT, ARROW_RIGHT, IMAGE, ANIMATION ->
                        visual.height();
                case LINE -> Math.max(1, visual.height());
                case TEXT -> LABEL_BOX_HEIGHT + 2;
                case SLIDER -> Math.max(25, visual.height());
            };

            maxY = Math.max(maxY, visual.y() + visualHeight + 8);
        }

        int barsHeight = payload.sideBarHeight() * 2 + payload.sideBarGap() * 2;
        return Math.max(
                MIN_PANEL_HEIGHT,
                Math.max(payload.minimumPanelHeight(), maxY + payload.padding() + barsHeight)
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePosition();
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelWidth = payload.panelWidth();
        int barWidth = panelWidth + payload.sideBarWidth() * 2;
        int barX = panelX - payload.sideBarWidth();
        int topBarY = panelY - payload.sideBarHeight() - payload.sideBarGap();
        int bottomBarY = panelY + panelHeight + payload.sideBarGap();

        drawShadow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawHorizontalBar(graphics, barX, topBarY, barWidth, payload.sideBarHeight());
        drawHorizontalBar(graphics, barX, bottomBarY, barWidth, payload.sideBarHeight());
        drawPanel(graphics);
        drawGrid(graphics);
        drawHeader(graphics);
        drawVisuals(graphics, mouseX, mouseY);
        drawFields(graphics, mouseX, mouseY);
        drawButtons(graphics, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphics graphics) {
        int x = panelX;
        int y = panelY;
        int right = x + payload.panelWidth();
        int bottom = y + panelHeight;

        graphics.fill(x - 2, y - 2, right + 2, bottom + 2, 0xFF080808);
        graphics.fill(x - 1, y - 1, right + 1, bottom + 1, payload.borderColor());
        graphics.fill(x, y, right, bottom, payload.panelColor());

        graphics.fill(x, y, right, y + 1, payload.accentColor());
        graphics.fill(x, y, x + 1, bottom, payload.accentColor());
        graphics.fill(x, bottom - 1, right, bottom, 0xFF101010);
        graphics.fill(right - 1, y, right, bottom, 0xFF101010);
    }

    private void drawGrid(GuiGraphics graphics) {
        int left = panelX + 1;
        int top = panelY + payload.headerHeight();
        int right = panelX + payload.panelWidth() - 1;
        int bottom = panelY + panelHeight - 1;

        int minorColor = withAlpha(payload.stripeColor(), 65);
        int majorColor = withAlpha(payload.stripeColor(), 125);

        int firstX = left + GRID_SIZE - Math.floorMod(left, GRID_SIZE);

        for (int x = firstX; x < right; x += GRID_SIZE) {
            int color = Math.floorMod(x, MAJOR_GRID_SIZE) == 0 ? majorColor : minorColor;
            graphics.fill(x, top, x + 1, bottom, color);
        }

        int firstY = top + GRID_SIZE - Math.floorMod(top, GRID_SIZE);

        for (int y = firstY; y < bottom; y += GRID_SIZE) {
            int color = Math.floorMod(y, MAJOR_GRID_SIZE) == 0 ? majorColor : minorColor;
            graphics.fill(left, y, right, y + 1, color);
        }

        graphics.fill(left, top, right, top + 1, payload.borderColor());
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private void drawShadow(GuiGraphics graphics, int x, int y, int panelWidth, int panelHeight) {
        if (!payload.shadowsEnabled() || payload.shadowSize() <= 0) return;

        int shadow = payload.shadowSize();

        graphics.fill(x + shadow, y + panelHeight, x + panelWidth + shadow,
                y + panelHeight + shadow, payload.shadowColor());

        graphics.fill(x + panelWidth, y + shadow, x + panelWidth + shadow,
                y + panelHeight + shadow, payload.shadowColor());
    }

    private void drawHorizontalBar(GuiGraphics graphics, int x, int y, int barWidth, int barHeight) {
        if (!payload.sideBarsEnabled() || barWidth <= 0 || barHeight <= 0) return;

        int shadow = payload.shadowSize();

        graphics.fill(x + shadow, y + barHeight, x + barWidth + shadow,
                y + barHeight + Math.max(1, shadow), payload.sideBarShadowColor());

        graphics.fill(x, y, x + barWidth, y + barHeight, payload.sideBarBaseColor());

        for (int start = -barHeight; start < barWidth; start += 8) {
            for (int offset = 0; offset < barHeight; offset++) {
                int px = x + start + offset;
                int py = y + barHeight - 1 - offset;

                if (px >= x && px < x + barWidth) {
                    graphics.fill(px, py, px + 1, py + 1, payload.sideBarStripeColor());
                }
            }
        }

        graphics.fill(x, y, x + barWidth, y + 1, payload.accentColor());
        graphics.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, 0xFF383838);
        graphics.fill(x, y, x + 1, y + barHeight, 0xFF969696);
        graphics.fill(x + barWidth - 1, y, x + barWidth, y + barHeight, 0xFF303030);
    }

    private void drawHeader(GuiGraphics graphics) {
        int right = panelX + payload.panelWidth();
        int bottom = panelY + payload.headerHeight();

        graphics.fill(panelX + 1, panelY + 1, right - 1, bottom, payload.headerColor());
        graphics.fill(panelX + 1, bottom - 2, right - 1, bottom, payload.accentColor());
        graphics.fill(panelX + 1, panelY + 1, right - 1, panelY + 2, payload.accentColor());

        int titleWidth = font.width(payload.title());
        int titleX = switch (payload.textAlignment()) {
            case 1 -> panelX + (payload.panelWidth() - titleWidth) / 2;
            case 2 -> panelX + payload.panelWidth() - payload.padding() - titleWidth;
            default -> panelX + payload.padding();
        };

        graphics.drawString(
                font,
                payload.title(),
                titleX,
                panelY + (payload.headerHeight() - font.lineHeight) / 2,
                payload.textColor(),
                false
        );
    }

    private void drawVisuals(GuiGraphics graphics, int mouseX, int mouseY) {
        for (CWGuiBuilder.VisualElement visual : payload.visuals()) {
            int x = panelX + visual.x();
            int y = panelY + visual.y();

            switch (visual.type()) {
                case ITEM -> drawItem(graphics, visual, x, y);
                case SLOT -> drawSlot(graphics, visual, x, y, false);
                case LARGE_SLOT -> drawSlot(graphics, visual, x, y, true);

                case TEXT -> drawTextBox(
                        graphics, visual.value(), x, y, visual.color(),
                        payload.headerColor(), payload.borderColor(), payload.accentColor(), true
                );

                case LINE -> graphics.fill(x, y, x + visual.width(),
                        y + Math.max(1, visual.height()), visual.color());

                case BAR -> graphics.fill(x, y, x + visual.width(),
                        y + visual.height(), visual.color());

                case ARROW_RIGHT -> drawArrow(
                        graphics, x, y, visual.width(), visual.height(), visual.color(), true
                );

                case ARROW_LEFT -> drawArrow(
                        graphics, x, y, visual.width(), visual.height(), visual.color(), false
                );

                case IMAGE -> drawTexture(
                        graphics, visual.texture(), x, y, visual.width(), visual.height()
                );

                case ANIMATION -> drawAnimation(
                        graphics, visual.animation(), x, y, visual.width(), visual.height()
                );

                case SLIDER -> drawSlider(graphics, visual, x, y, mouseX, mouseY);
            }
        }
    }

    private void drawSlider(
            GuiGraphics graphics,
            CWGuiBuilder.VisualElement slider,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        int value = parseValue(values.get(slider.value()), slider.count());
        value = Math.max(0, Math.min(100, value));

        String label = slider.texture();
        String valueText = value + "%";

        graphics.drawString(font, label, x, y, payload.textColor(), false);
        graphics.drawString(
                font,
                valueText,
                x + slider.width() - font.width(valueText),
                y,
                payload.accentColor(),
                false
        );

        int trackY = y + 15;
        int trackHeight = 6;
        int thumbX = x + value * Math.max(1, slider.width() - 8) / 100;

        graphics.fill(x, trackY, x + slider.width(), trackY + trackHeight, 0xFF101710);
        graphics.fill(x, trackY, thumbX + 4, trackY + trackHeight, 0xFF8C713F);
        graphics.fill(x, trackY, x + slider.width(), trackY + 1, 0xFF52604F);
        graphics.fill(thumbX, trackY - 4, thumbX + 8, trackY + trackHeight + 4, 0xFF101510);
        graphics.fill(thumbX + 1, trackY - 3, thumbX + 7, trackY + trackHeight + 3, 0xFFE5C486);
        graphics.fill(thumbX + 2, trackY - 2, thumbX + 6, trackY + trackHeight + 2, 0xFFB18A4A);
    }

    private int parseValue(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void drawTextBox(
            GuiGraphics graphics,
            String text,
            int x,
            int y,
            int textColor,
            int backgroundColor,
            int borderColor,
            int accentColor,
            boolean compact
    ) {
        int padX = compact ? LABEL_BOX_PADDING : 5;
        int padY = 4;
        int textHeight = font.lineHeight;
        int boxWidth = font.width(text) + padX * 2 + 2;
        int boxHeight = Math.max(LABEL_BOX_HEIGHT, textHeight + padY * 2 + 2);
        int textX = x + padX + 1;
        int textY = y + (boxHeight - textHeight) / 2;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, borderColor);
        graphics.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, backgroundColor);
        graphics.fill(x + 1, y + 1, x + boxWidth - 1, y + 3, accentColor);
        graphics.drawString(font, text, textX, textY, textColor, false);
    }

    private void drawItem(GuiGraphics graphics, CWGuiBuilder.VisualElement visual, int x, int y) {
        ItemStack stack = getItemStack(visual.value(), visual.count());
        if (stack.isEmpty()) return;

        float scale = Math.max(1.0F, visual.width() / 16.0F);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();

        if (visual.count() > 1) {
            String count = Integer.toString(visual.count());
            graphics.drawString(font, count, x + visual.width() - font.width(count),
                    y + visual.height() - 8, 0xFFFFFFFF, true);
        }
    }

    private void drawSlot(GuiGraphics graphics, CWGuiBuilder.VisualElement visual, int x, int y, boolean large) {
        int size = large ? 36 : 18;

        graphics.fill(x, y, x + size, y + size, 0xFF101010);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B8B8B);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + size - 2, y + 3, 0xFF202020);
        graphics.fill(x + 2, y + 2, x + 3, y + size - 2, 0xFF202020);

        ItemStack stack = getItemStack(visual.value(), visual.count());

        if (!stack.isEmpty()) {
            int inset = large ? 2 : 1;
            float scale = large ? 2.0F : 1.0F;

            graphics.pose().pushPose();
            graphics.pose().translate(x + inset, y + inset, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }
    }

    private ItemStack getItemStack(String itemId, int count) {
        if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;

        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) return ItemStack.EMPTY;

        var item = BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR);
        if (item == Items.AIR) return ItemStack.EMPTY;

        return new ItemStack(item, Math.max(1, Math.min(99, count)));
    }

    private void drawArrow(
            GuiGraphics graphics,
            int x,
            int y,
            int arrowWidth,
            int arrowHeight,
            int color,
            boolean right
    ) {
        if (arrowWidth <= 0 || arrowHeight <= 0) return;

        int midY = y + arrowHeight / 2;
        int startX = right ? x : x + arrowWidth;
        int endX = right ? x + arrowWidth : x;

        graphics.fill(Math.min(startX, endX), midY - 1, Math.max(startX, endX), midY + 1, color);

        int headWidth = Math.max(3, arrowWidth / 3);
        int headHeight = Math.max(3, arrowHeight / 2);

        for (int i = 0; i < headWidth; i++) {
            int px = right ? x + arrowWidth - i - 1 : x + i;
            int offset = i * headHeight / headWidth;
            graphics.fill(px, midY - offset, px + 1, midY + offset + 1, color);
        }
    }

    private void drawFields(GuiGraphics graphics, int mouseX, int mouseY) {
        int defaultX = panelX + payload.padding();
        int defaultWidth = payload.panelWidth() - payload.padding() * 2;
        int defaultY = panelY + payload.headerHeight() + 18;

        for (int i = 0; i < payload.fields().size(); i++) {
            CWGuiBuilder.NumberField field = payload.fields().get(i);

            int x = field.positioned() ? panelX + field.x() : defaultX;
            int y = field.positioned()
                    ? panelY + field.y()
                    : defaultY + i * (FIELD_HEIGHT + FIELD_GAP) - scrollOffset;

            int fieldWidth = field.positioned() && field.width() > 0 ? field.width() : defaultWidth;
            int inputY = y + LABEL_BOX_HEIGHT + 2;

            drawTextBox(graphics, field.label(), x, y, payload.textColor(),
                    payload.headerColor(), payload.borderColor(), payload.accentColor(), false);

            boolean hovered = mouseX >= x && mouseX < x + fieldWidth
                    && mouseY >= inputY && mouseY < inputY + INPUT_HEIGHT;

            int background = i == activeField
                    ? payload.inputActiveColor()
                    : hovered ? payload.inputHoverColor() : payload.inputColor();

            graphics.fill(x, inputY, x + fieldWidth, inputY + INPUT_HEIGHT, payload.inputBorderColor());
            graphics.fill(x + 1, inputY + 1, x + fieldWidth - 1, inputY + INPUT_HEIGHT - 1, background);
            graphics.fill(x + 1, inputY + 1, x + fieldWidth - 1, inputY + 2, payload.inputHighlightColor());

            String value = values.getOrDefault(field.id(), "");
            int textX = x + 4;
            int textY = inputY + (INPUT_HEIGHT - font.lineHeight) / 2;

            graphics.drawString(font, value, textX, textY, payload.textColor(), false);

            if (i == activeField && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int safeCursor = Math.max(0, Math.min(cursorPosition, value.length()));
                int cursorX = textX + font.width(value.substring(0, safeCursor));
                graphics.fill(cursorX, inputY + 4, cursorX + 1,
                        inputY + INPUT_HEIGHT - 4, payload.cursorColor());
            }
        }
    }

    private void drawButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int defaultCount = (int) payload.buttons().stream().filter(b -> !b.positioned()).count();

        int totalWidth = payload.buttons().stream()
                .filter(b -> !b.positioned())
                .mapToInt(CWGuiBuilder.GuiButton::width)
                .sum() + Math.max(0, defaultCount - 1) * BUTTON_GAP;

        int nextX = panelX + (payload.panelWidth() - totalWidth) / 2;

        int defaultY = panelY + panelHeight - payload.padding()
                - payload.buttons().stream()
                .filter(b -> !b.positioned())
                .mapToInt(CWGuiBuilder.GuiButton::height)
                .max().orElse(20);

        for (CWGuiBuilder.GuiButton button : payload.buttons()) {
            int x;
            int y;

            if (button.positioned()) {
                x = panelX + button.x();
                y = panelY + button.y();
            } else {
                x = nextX;
                y = defaultY;
                nextX += button.width() + BUTTON_GAP;
            }

            boolean hovered = mouseX >= x && mouseX < x + button.width()
                    && mouseY >= y && mouseY < y + button.height();

            drawStyledButton(graphics, button, x, y, hovered,
                    button.actionId().equals(pressedActionId));
        }
    }

    private void drawStyledButton(
            GuiGraphics graphics,
            CWGuiBuilder.GuiButton button,
            int x,
            int y,
            boolean hovered,
            boolean pressed
    ) {
        String texture = button.texture();
        CWGuiBuilder.Animation animation = button.animation();

        if (!button.enabled()) {
            if (!button.disabledTexture().isBlank()) texture = button.disabledTexture();
            if (button.disabledAnimation() != null) animation = button.disabledAnimation();
        } else if (pressed) {
            if (!button.pressedTexture().isBlank()) texture = button.pressedTexture();
            if (button.pressedAnimation() != null) animation = button.pressedAnimation();
        } else if (hovered) {
            if (!button.hoverTexture().isBlank()) texture = button.hoverTexture();
            if (button.hoverAnimation() != null) animation = button.hoverAnimation();
        }

        int fill = !button.enabled() ? button.disabledColor()
                : pressed ? button.pressedColor()
                  : hovered ? button.hoverColor() : button.fillColor();

        drawButtonShape(graphics, x, y, button.width(), button.height(), fill,
                button.borderColor(), button.borderThickness(), button.shape(), button.radius());

        if (animation != null && !animation.frames().isEmpty()) {
            drawAnimation(graphics, animation, x, y, button.width(), button.height());
        } else if (texture != null && !texture.isBlank()) {
            drawTexture(graphics, texture, x, y, button.width(), button.height());
        }

        if (button.textOverlay() && !button.label().isBlank()) {
            drawButtonLabel(graphics, button, x, y);
        }
    }

    private void drawButtonShape(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int fill,
            int border,
            int thickness,
            CWGuiBuilder.ButtonShape shape,
            int radius
    ) {
        int t = Math.max(0, Math.min(thickness, Math.min(w, h) / 2));

        if (shape == CWGuiBuilder.ButtonShape.RECTANGLE || shape == null) {
            graphics.fill(x, y, x + w, y + h, border);
            graphics.fill(x + t, y + t, x + w - t, y + h - t, fill);
            return;
        }

        int r = shape == CWGuiBuilder.ButtonShape.PILL || shape == CWGuiBuilder.ButtonShape.CIRCLE
                ? Math.min(w, h) / 2
                : Math.min(Math.max(0, radius), Math.min(w, h) / 2);

        for (int row = 0; row < h; row++) {
            int inset = 0;

            if (row < r) {
                inset = (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - (r - row) * (r - row))));
            } else if (row >= h - r) {
                int d = row - (h - r - 1);
                inset = (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - (r - d) * (r - d))));
            }

            graphics.fill(x + inset, y + row, x + w - inset, y + row + 1, border);

            if (w - inset * 2 > t * 2 && row >= t && row < h - t) {
                graphics.fill(x + inset + t, y + row, x + w - inset - t, y + row + 1, fill);
            }
        }
    }

    private void drawTexture(GuiGraphics graphics, String textureId, int x, int y, int width, int height) {
        ResourceLocation texture = ResourceLocation.tryParse(textureId);
        if (texture == null) return;

        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    private void drawAnimation(
            GuiGraphics graphics,
            CWGuiBuilder.Animation animation,
            int x,
            int y,
            int width,
            int height
    ) {
        if (animation == null || animation.frames().isEmpty()) return;

        int frameCount = animation.frames().size();
        long frame = animationTicks / Math.max(1, animation.frameDurationTicks());

        int index = animation.loop()
                ? (int) (frame % frameCount)
                : (int) Math.min(frameCount - 1, frame);

        drawTexture(graphics, animation.frames().get(index), x, y, width, height);
    }

    private void drawButtonLabel(GuiGraphics graphics, CWGuiBuilder.GuiButton button, int x, int y) {
        int labelX = x + (button.width() - font.width(button.label())) / 2;
        int labelY = y + (button.height() - font.lineHeight) / 2;
        graphics.drawString(font, button.label(), labelX, labelY, button.textColor(), false);
    }

    private void updateSlider(CWGuiBuilder.VisualElement slider, double mouseX) {
        int x = panelX + slider.x();
        int trackWidth = Math.max(1, slider.width() - 8);
        int value = (int) Math.round((mouseX - x - 4) * 100.0D / trackWidth);
        values.put(slider.value(), Integer.toString(Math.max(0, Math.min(100, value))));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        updatePosition();

        for (CWGuiBuilder.VisualElement visual : payload.visuals()) {
            if (visual.type() != CWGuiBuilder.VisualType.SLIDER) continue;

            int x = panelX + visual.x();
            int y = panelY + visual.y();

            if (mouseX >= x && mouseX <= x + visual.width()
                    && mouseY >= y && mouseY <= y + visual.height()) {
                draggingSlider = visual;
                updateSlider(visual, mouseX);
                activeField = -1;
                return true;
            }
        }

        int defaultX = panelX + payload.padding();
        int defaultWidth = payload.panelWidth() - payload.padding() * 2;
        int defaultY = panelY + payload.headerHeight() + 18;

        for (int i = 0; i < payload.fields().size(); i++) {
            CWGuiBuilder.NumberField field = payload.fields().get(i);

            int x = field.positioned() ? panelX + field.x() : defaultX;
            int y = field.positioned()
                    ? panelY + field.y()
                    : defaultY + i * (FIELD_HEIGHT + FIELD_GAP) - scrollOffset;

            int fieldWidth = field.positioned() && field.width() > 0 ? field.width() : defaultWidth;
            int inputY = y + LABEL_BOX_HEIGHT + 2;

            if (mouseX >= x && mouseX < x + fieldWidth
                    && mouseY >= inputY && mouseY < inputY + INPUT_HEIGHT) {
                activeField = i;
                cursorPosition = values.getOrDefault(field.id(), "").length();
                return true;
            }
        }

        int defaultCount = (int) payload.buttons().stream().filter(b -> !b.positioned()).count();
        int totalWidth = payload.buttons().stream()
                .filter(b -> !b.positioned())
                .mapToInt(CWGuiBuilder.GuiButton::width)
                .sum() + Math.max(0, defaultCount - 1) * BUTTON_GAP;

        int nextX = panelX + (payload.panelWidth() - totalWidth) / 2;

        int defaultButtonY = panelY + panelHeight - payload.padding()
                - payload.buttons().stream()
                .filter(b -> !b.positioned())
                .mapToInt(CWGuiBuilder.GuiButton::height)
                .max().orElse(20);

        for (CWGuiBuilder.GuiButton guiButton : payload.buttons()) {
            int x;
            int y;

            if (guiButton.positioned()) {
                x = panelX + guiButton.x();
                y = panelY + guiButton.y();
            } else {
                x = nextX;
                y = defaultButtonY;
                nextX += guiButton.width() + BUTTON_GAP;
            }

            if (guiButton.enabled() && mouseX >= x && mouseX < x + guiButton.width()
                    && mouseY >= y && mouseY < y + guiButton.height()) {
                pressedActionId = guiButton.actionId();
                playButtonSound(guiButton.soundId());
                handleButton(guiButton);
                return true;
            }
        }

        activeField = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingSlider != null) {
            updateSlider(draggingSlider, mouseX);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingSlider = null;
        pressedActionId = "";
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void playButtonSound(String soundId) {
        if (soundId == null || soundId.isBlank() || minecraft == null || minecraft.player == null) return;

        ResourceLocation location = ResourceLocation.tryParse(soundId);
        if (location == null) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(location).orElse(null);
        if (sound != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
        }
    }

    private void handleButton(CWGuiBuilder.GuiButton button) {
        Map<String, Integer> submittedValues = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            try {
                submittedValues.put(entry.getKey(), Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException ignored) {
                submittedValues.put(entry.getKey(), 0);
            }
        }

        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new CWGuiSyncPayload(
                    payload.guiId(),
                    button.actionId(),
                    submittedValues
            ));
        }

        if (button.closeOnClick()) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeField >= 0 && activeField < payload.fields().size()) {
            CWGuiBuilder.NumberField field = payload.fields().get(activeField);
            String value = values.getOrDefault(field.id(), "");

            if (keyCode == 256) {
                activeField = -1;
                return true;
            }

            if (keyCode == 259 && cursorPosition > 0) {
                values.put(field.id(), value.substring(0, cursorPosition - 1) + value.substring(cursorPosition));
                cursorPosition--;
                return true;
            }

            if (keyCode == 261 && cursorPosition < value.length()) {
                values.put(field.id(), value.substring(0, cursorPosition) + value.substring(cursorPosition + 1));
                return true;
            }

            if (keyCode == 263 && cursorPosition > 0) {
                cursorPosition--;
                return true;
            }

            if (keyCode == 262 && cursorPosition < value.length()) {
                cursorPosition++;
                return true;
            }

            if (keyCode == 268) {
                cursorPosition = 0;
                return true;
            }

            if (keyCode == 269) {
                cursorPosition = value.length();
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                activeField = -1;
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeField < 0 || activeField >= payload.fields().size()) {
            return super.charTyped(codePoint, modifiers);
        }

        if (!Character.isDigit(codePoint) && codePoint != '-') return true;

        CWGuiBuilder.NumberField field = payload.fields().get(activeField);
        String value = values.getOrDefault(field.id(), "");

        if (codePoint == '-' && (cursorPosition != 0 || value.contains("-"))) return true;

        String updated = value.substring(0, cursorPosition)
                + codePoint
                + value.substring(cursorPosition);

        if (updated.length() > 11) return true;

        values.put(field.id(), updated);
        cursorPosition++;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(
                0,
                payload.headerHeight() + 18
                        + payload.fields().size() * (FIELD_HEIGHT + FIELD_GAP)
                        - (panelHeight - payload.padding() * 2 - 40)
        );

        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * 12)));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}