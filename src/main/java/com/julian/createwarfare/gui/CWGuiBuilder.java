package com.julian.createwarfare.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class CWGuiBuilder {
    private static final Map<String, Registration> HANDLERS = new ConcurrentHashMap<>();

    private final String id;
    private final String title;
    private final List<NumberField> fields = new ArrayList<>();
    private final List<GuiButton> buttons = new ArrayList<>();
    private final List<VisualElement> visuals = new ArrayList<>();

    private ButtonShape nextButtonShape = ButtonShape.RECTANGLE;
    private int nextButtonRadius = 6;
    private int nextButtonBorderThickness = 1;
    private int nextButtonFillColor = 0xFF6B6B6B;
    private int nextButtonHoverColor = 0xFF858585;
    private int nextButtonPressedColor = 0xFF454545;
    private int nextButtonDisabledColor = 0xFF303030;
    private int nextButtonBorderColor = 0xFF101010;
    private boolean nextButtonTextOverlay = true;
    private boolean nextButtonEnabled = true;
    private boolean nextButtonCloseOnClick;

    private int panelWidth = 300;
    private int panelHeight;
    private int panelX = Integer.MIN_VALUE;
    private int panelY = Integer.MIN_VALUE;
    private int minimumPanelHeight = 120;
    private int padding = 12;
    private int headerHeight = 30;
    private int buttonHeight = 20;

    private int backgroundTopColor = 0xD0101010;
    private int backgroundBottomColor = 0xE0080808;
    private int panelColor = 0xFF252525;
    private int panelHighlightColor = 0xFF8A8A8A;
    private int panelShadowColor = 0xFF101010;
    private int headerColor = 0xFF3C3C3C;
    private int accentColor = 0xFF6E6E6E;
    private int stripeColor = 0xFF555555;
    private int textColor = 0xFFFFFFFF;
    private int borderColor = 0xFF101010;
    private int shadowColor = 0x88000000;
    private int shadowSize = 4;

    private int inputColor = 0xFF202020;
    private int inputHoverColor = 0xFF4B4B4B;
    private int inputActiveColor = 0xFF303030;
    private int inputBorderColor = 0xFF101010;
    private int inputHighlightColor = 0xFF777777;
    private int cursorColor = 0xFFFFFFFF;

    private int buttonColor = 0xFF6B6B6B;
    private int buttonHoverColor = 0xFF858585;
    private int buttonBorderColor = 0xFF101010;
    private int buttonHighlightColor = 0xFFB0B0B0;
    private int buttonShadowColor = 0xFF373737;
    private int buttonTextColor = 0xFFFFFFFF;

    private int slotBorderColor = 0xFF101010;
    private int slotHighlightColor = 0xFF8B8B8B;
    private int slotColor = 0xFF373737;
    private int slotInsetColor = 0xFF202020;

    private int checkerColor1 = 0xFF82D9F5;
    private int checkerColor2 = 0xFF328BC4;
    private int checkerPixelSize = 1;
    private boolean checkerEnabled = true;
    private boolean backgroundDimEnabled = true;
    private boolean sideBarsEnabled = true;
    private boolean stripesEnabled = true;
    private boolean shadowsEnabled = true;

    private int sideBarWidth = 24;
    private int sideBarHeight = 18;
    private int sideBarGap = 6;
    private int sideBarBaseColor = 0xFF777777;
    private int sideBarStripeColor = 0xFF555555;
    private int sideBarShadowColor = 0xFF303030;

    private String fontId = "minecraft:default";
    private float textScale = 1.0F;
    private float titleScale = 1.0F;
    private int textAlignment;

    private String defaultButtonSound = "minecraft:ui.button.click";
    private ActionHandler actionHandler = (player, action, values) -> {};

    private CWGuiBuilder(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public static CWGuiBuilder create(String id, String title) {
        return new CWGuiBuilder(id, title);
    }

    public CWGuiBuilder panelWidth(int value) {
        panelWidth = Math.max(80, value);
        return this;
    }

    public CWGuiBuilder panelHeight(int value) {
        panelHeight = Math.max(0, value);
        return this;
    }

    public CWGuiBuilder panelPosition(int x, int y) {
        panelX = x;
        panelY = y;
        return this;
    }

    public CWGuiBuilder centeredPanel() {
        panelX = Integer.MIN_VALUE;
        panelY = Integer.MIN_VALUE;
        return this;
    }

    public CWGuiBuilder minimumPanelHeight(int value) {
        minimumPanelHeight = Math.max(1, value);
        return this;
    }

    public CWGuiBuilder padding(int value) {
        padding = Math.max(0, value);
        return this;
    }

    public CWGuiBuilder headerHeight(int value) {
        headerHeight = Math.max(0, value);
        return this;
    }

    public CWGuiBuilder buttonHeight(int value) {
        buttonHeight = Math.max(1, value);
        return this;
    }

    public CWGuiBuilder backgroundColors(int top, int bottom) {
        backgroundTopColor = top;
        backgroundBottomColor = bottom;
        return this;
    }

    public CWGuiBuilder backgroundDimEnabled(boolean value) {
        backgroundDimEnabled = value;
        return this;
    }

    public CWGuiBuilder panelColor(int value) {
        panelColor = value;
        return this;
    }

    public CWGuiBuilder panelHighlightColor(int value) {
        panelHighlightColor = value;
        return this;
    }

    public CWGuiBuilder panelShadowColor(int value) {
        panelShadowColor = value;
        return this;
    }

    public CWGuiBuilder headerColor(int value) {
        headerColor = value;
        return this;
    }

    public CWGuiBuilder accentColor(int value) {
        accentColor = value;
        return this;
    }

    public CWGuiBuilder stripeColor(int value) {
        stripeColor = value;
        return this;
    }

    public CWGuiBuilder textColor(int value) {
        textColor = value;
        return this;
    }

    public CWGuiBuilder borderColor(int value) {
        borderColor = value;
        return this;
    }

    public CWGuiBuilder shadowColor(int value) {
        shadowColor = value;
        return this;
    }

    public CWGuiBuilder shadowSize(int value) {
        shadowSize = Math.max(0, value);
        return this;
    }

    public CWGuiBuilder shadowsEnabled(boolean value) {
        shadowsEnabled = value;
        return this;
    }

    public CWGuiBuilder inputColors(int normal, int hovered, int active) {
        inputColor = normal;
        inputHoverColor = hovered;
        inputActiveColor = active;
        return this;
    }

    public CWGuiBuilder inputBorderColor(int value) {
        inputBorderColor = value;
        return this;
    }

    public CWGuiBuilder inputHighlightColor(int value) {
        inputHighlightColor = value;
        return this;
    }

    public CWGuiBuilder cursorColor(int value) {
        cursorColor = value;
        return this;
    }

    public CWGuiBuilder buttonColor(int value) {
        buttonColor = value;
        return this;
    }

    public CWGuiBuilder buttonHoverColor(int value) {
        buttonHoverColor = value;
        return this;
    }

    public CWGuiBuilder buttonBorderColor(int value) {
        buttonBorderColor = value;
        return this;
    }

    public CWGuiBuilder buttonHighlightColor(int value) {
        buttonHighlightColor = value;
        return this;
    }

    public CWGuiBuilder buttonShadowColor(int value) {
        buttonShadowColor = value;
        return this;
    }

    public CWGuiBuilder buttonTextColor(int value) {
        buttonTextColor = value;
        return this;
    }

    public CWGuiBuilder slotColors(int border, int highlight, int fill, int inset) {
        slotBorderColor = border;
        slotHighlightColor = highlight;
        slotColor = fill;
        slotInsetColor = inset;
        return this;
    }

    public CWGuiBuilder checkerColors(int first, int second) {
        checkerColor1 = first;
        checkerColor2 = second;
        return this;
    }

    public CWGuiBuilder checkerPixelSize(int value) {
        checkerPixelSize = Math.max(1, value);
        return this;
    }

    public CWGuiBuilder checkerEnabled(boolean value) {
        checkerEnabled = value;
        return this;
    }

    public CWGuiBuilder stripesEnabled(boolean value) {
        stripesEnabled = value;
        return this;
    }

    public CWGuiBuilder sideBarsEnabled(boolean value) {
        sideBarsEnabled = value;
        return this;
    }

    public CWGuiBuilder sideBars(int width, int height, int gap) {
        sideBarWidth = Math.max(0, width);
        sideBarHeight = Math.max(0, height);
        sideBarGap = Math.max(0, gap);
        return this;
    }

    public CWGuiBuilder sideBarColors(int base, int stripes, int shadow) {
        sideBarBaseColor = base;
        sideBarStripeColor = stripes;
        sideBarShadowColor = shadow;
        return this;
    }

    public CWGuiBuilder font(String resourceLocation) {
        fontId = resourceLocation == null || resourceLocation.isBlank()
                ? "minecraft:default" : resourceLocation;
        return this;
    }

    public CWGuiBuilder textScale(float value) {
        textScale = clampScale(value);
        return this;
    }

    public CWGuiBuilder titleScale(float value) {
        titleScale = clampScale(value);
        return this;
    }

    public CWGuiBuilder textAlignment(int value) {
        textAlignment = Math.max(0, Math.min(2, value));
        return this;
    }

    private static float clampScale(float value) {
        return Math.max(0.5F, Math.min(3.0F, value));
    }

    public CWGuiBuilder numberField(String id, String label, int defaultValue) {
        fields.add(new NumberField(id, label, defaultValue, false, 0, 0, 0));
        return this;
    }

    public CWGuiBuilder numberFieldAt(String id, String label, int defaultValue, int x, int y, int width) {
        fields.add(new NumberField(id, label, defaultValue, true, x, y, width));
        return this;
    }

    public CWGuiBuilder sliderAt(String id, String label, int defaultValue, int x, int y, int width) {
        visuals.add(new VisualElement(
                VisualType.SLIDER,
                id,
                label,
                x,
                y,
                Math.max(40, width),
                25,
                Math.max(0, Math.min(100, defaultValue)),
                0xFFB18A4A,
                true,
                null
        ));
        return this;
    }

    public CWGuiBuilder textButton(String label, String actionId) {
        return textButton(label, actionId, 80, buttonHeight, defaultButtonSound);
    }

    public CWGuiBuilder textButton(String label, String actionId, int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, "", Math.max(1, width), Math.max(1, height),
                soundId, false, 0, 0, "", "", "", null, null, null, null));
        return this;
    }

    public CWGuiBuilder textButtonAt(String label, String actionId, int x, int y, int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, "", Math.max(1, width), Math.max(1, height),
                soundId, true, x, y, "", "", "", null, null, null, null));
        return this;
    }

    public CWGuiBuilder imageButton(String label, String actionId, String texture) {
        return imageButton(label, actionId, texture, 80, buttonHeight, defaultButtonSound);
    }

    public CWGuiBuilder imageButton(String label, String actionId, String texture, int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, texture, Math.max(1, width), Math.max(1, height),
                soundId, false, 0, 0, "", "", "", null, null, null, null));
        return this;
    }

    public CWGuiBuilder imageButtonAt(String label, String actionId, String texture, int x, int y,
                                      int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, texture, Math.max(1, width), Math.max(1, height),
                soundId, true, x, y, "", "", "", null, null, null, null));
        return this;
    }

    public CWGuiBuilder buttonShape(ButtonShape shape, int radius, int borderThickness) {
        nextButtonShape = shape == null ? ButtonShape.RECTANGLE : shape;
        nextButtonRadius = Math.max(0, Math.min(64, radius));
        nextButtonBorderThickness = Math.max(0, Math.min(8, borderThickness));
        return this;
    }

    public CWGuiBuilder buttonStateColors(int normal, int hovered, int pressed, int disabled, int border) {
        nextButtonFillColor = normal;
        nextButtonHoverColor = hovered;
        nextButtonPressedColor = pressed;
        nextButtonDisabledColor = disabled;
        nextButtonBorderColor = border;
        return this;
    }

    public CWGuiBuilder buttonTextOverlay(boolean value) {
        nextButtonTextOverlay = value;
        return this;
    }

    public CWGuiBuilder buttonEnabled(boolean value) {
        nextButtonEnabled = value;
        return this;
    }

    public CWGuiBuilder closeOnClick(boolean value) {
        nextButtonCloseOnClick = value;
        return this;
    }

    public CWGuiBuilder image(String texture, int x, int y, int width, int height) {
        visuals.add(new VisualElement(VisualType.IMAGE, "", texture, x, y, width, height, 0, textColor, false, null));
        return this;
    }

    public CWGuiBuilder animation(Animation animation, int x, int y, int width, int height) {
        if (animation != null && !animation.frames().isEmpty()) {
            visuals.add(new VisualElement(VisualType.ANIMATION, "", "", x, y, width, height, 0, textColor, false, animation));
        }
        return this;
    }

    public CWGuiBuilder animatedButtonAt(String label, String actionId, Animation normal, Animation hovered,
                                         Animation pressed, Animation disabled, int x, int y,
                                         int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, "", width, height, soundId, true, x, y,
                "", "", "", normal, hovered, pressed, disabled));
        return this;
    }

    public CWGuiBuilder imageButtonStatesAt(String label, String actionId, String normal, String hovered,
                                            String pressed, String disabled, int x, int y,
                                            int width, int height, String soundId) {
        buttons.add(createButton(label, actionId, normal, width, height, soundId, true, x, y,
                hovered, pressed, disabled, null, null, null, null));
        return this;
    }

    public CWGuiBuilder animatedImageButtonAt(String label, String actionId, Animation animation,
                                              int x, int y, int width, int height, String soundId) {
        return animatedButtonAt(label, actionId, animation, animation, animation, animation,
                x, y, width, height, soundId);
    }

    private GuiButton createButton(String label, String actionId, String texture, int width, int height,
                                   String soundId, boolean positioned, int x, int y,
                                   String hoverTexture, String pressedTexture, String disabledTexture,
                                   Animation normalAnimation, Animation hoverAnimation,
                                   Animation pressedAnimation, Animation disabledAnimation) {
        GuiButton button = new GuiButton(
                label == null ? "" : label,
                actionId == null ? "" : actionId,
                texture == null ? "" : texture,
                width,
                height,
                soundId == null ? "" : soundId,
                buttonTextColor,
                positioned,
                x,
                y,
                hoverTexture == null ? "" : hoverTexture,
                pressedTexture == null ? "" : pressedTexture,
                disabledTexture == null ? "" : disabledTexture,
                normalAnimation,
                hoverAnimation,
                pressedAnimation,
                disabledAnimation,
                nextButtonEnabled,
                nextButtonTextOverlay,
                nextButtonShape,
                nextButtonRadius,
                nextButtonBorderThickness,
                nextButtonFillColor,
                nextButtonHoverColor,
                nextButtonPressedColor,
                nextButtonDisabledColor,
                nextButtonBorderColor,
                nextButtonCloseOnClick
        );

        nextButtonShape = ButtonShape.RECTANGLE;
        nextButtonRadius = 6;
        nextButtonBorderThickness = 1;
        nextButtonFillColor = buttonColor;
        nextButtonHoverColor = buttonHoverColor;
        nextButtonPressedColor = 0xFF454545;
        nextButtonDisabledColor = 0xFF303030;
        nextButtonBorderColor = buttonBorderColor;
        nextButtonTextOverlay = true;
        nextButtonEnabled = true;
        nextButtonCloseOnClick = false;

        return button;
    }

    public CWGuiBuilder buttonSound(String soundId) {
        defaultButtonSound = soundId;
        return this;
    }

    public CWGuiBuilder displayItem(String itemId, int x, int y, int size) {
        return displayItem(itemId, x, y, size, 1);
    }

    public CWGuiBuilder displayItem(String itemId, int x, int y, int size, int count) {
        visuals.add(new VisualElement(VisualType.ITEM, itemId, "", x, y, size, size,
                Math.max(1, count), textColor, false, null));
        return this;
    }

    public CWGuiBuilder itemSlot(int x, int y, String itemId) {
        visuals.add(new VisualElement(VisualType.SLOT, itemId, "", x, y, 18, 18, 1, textColor, false, null));
        return this;
    }

    public CWGuiBuilder largeItemSlot(int x, int y, String itemId) {
        visuals.add(new VisualElement(VisualType.LARGE_SLOT, itemId, "", x, y, 36, 36, 1, textColor, false, null));
        return this;
    }

    public CWGuiBuilder label(String text, int x, int y, int color) {
        visuals.add(new VisualElement(VisualType.TEXT, text, "", x, y, 0, 0, 0, color, false, null));
        return this;
    }

    public CWGuiBuilder divider(int x, int y, int width, int color) {
        visuals.add(new VisualElement(VisualType.LINE, "", "", x, y, Math.max(0, width), 1, 0, color, false, null));
        return this;
    }

    public CWGuiBuilder arrowRight(int x, int y, int size, int color) {
        visuals.add(new VisualElement(VisualType.ARROW_RIGHT, "", "", x, y, size, size, 0, color, false, null));
        return this;
    }

    public CWGuiBuilder arrowLeft(int x, int y, int size, int color) {
        visuals.add(new VisualElement(VisualType.ARROW_LEFT, "", "", x, y, size, size, 0, color, false, null));
        return this;
    }

    public CWGuiBuilder visualBar(int x, int y, int width, int height, int color) {
        visuals.add(new VisualElement(VisualType.BAR, "", "", x, y, width, height, 0, color, false, null));
        return this;
    }

    public CWGuiBuilder onSubmit(BiConsumer<ServerPlayer, Map<String, Integer>> handler) {
        actionHandler = (player, action, values) -> {
            if (handler != null) {
                handler.accept(player, values);
            }
        };
        return this;
    }

    public CWGuiBuilder onAction(ActionHandler handler) {
        actionHandler = handler == null ? (player, action, values) -> {} : handler;
        return this;
    }

    public CWGuiBuilder build() {
        return this;
    }

    public void openFor(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Set<String> allowedActions = buttons.stream()
                .map(GuiButton::actionId)
                .filter(action -> action != null && !action.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        HANDLERS.put(handlerKey(player, id), new Registration(actionHandler, allowedActions));

        PacketDistributor.sendToPlayer(player, new OpenPayload(
                id, title, List.copyOf(fields), List.copyOf(buttons), List.copyOf(visuals),
                panelWidth, panelHeight, panelX, panelY, minimumPanelHeight, padding, headerHeight, buttonHeight,
                backgroundTopColor, backgroundBottomColor, backgroundDimEnabled,
                panelColor, panelHighlightColor, panelShadowColor, headerColor, accentColor, stripeColor,
                textColor, borderColor, shadowColor, shadowSize, shadowsEnabled,
                inputColor, inputHoverColor, inputActiveColor, inputBorderColor, inputHighlightColor, cursorColor,
                buttonColor, buttonHoverColor, buttonBorderColor, buttonHighlightColor, buttonShadowColor, buttonTextColor,
                slotBorderColor, slotHighlightColor, slotColor, slotInsetColor,
                checkerColor1, checkerColor2, checkerPixelSize, checkerEnabled, stripesEnabled,
                sideBarsEnabled, sideBarWidth, sideBarHeight, sideBarGap,
                sideBarBaseColor, sideBarStripeColor, sideBarShadowColor,
                fontId, textScale, titleScale, textAlignment
        ));
    }

    private static String handlerKey(ServerPlayer player, String guiId) {
        return player.getUUID() + ":" + guiId;
    }

    public static void handleSubmit(ServerPlayer player, CWGuiSyncPayload payload) {
        if (player == null || payload == null || player.isRemoved()) {
            return;
        }

        Registration registration = HANDLERS.get(handlerKey(player, payload.guiId()));

        if (registration == null || !registration.allowedActions().contains(payload.actionId())) {
            return;
        }

        Map<String, Integer> safeValues = new LinkedHashMap<>();

        if (payload.values() != null) {
            payload.values().forEach((key, value) -> {
                if (key != null && key.length() <= 128 && value != null) {
                    safeValues.put(key, value);
                }
            });
        }

        registration.handler().handle(player, payload.actionId(), Map.copyOf(safeValues));
    }

    @FunctionalInterface
    public interface ActionHandler {
        void handle(ServerPlayer player, String actionId, Map<String, Integer> values);
    }

    private record Registration(ActionHandler handler, Set<String> allowedActions) {}

    public enum VisualType {
        ITEM, SLOT, LARGE_SLOT, TEXT, LINE, ARROW_RIGHT, ARROW_LEFT, BAR, IMAGE, ANIMATION, SLIDER
    }

    public enum ButtonShape {
        RECTANGLE, ROUNDED, PILL, CIRCLE
    }

    public record Animation(List<String> frames, int frameDurationTicks, boolean loop) {
        public Animation {
            frames = frames == null ? List.of() : List.copyOf(frames.stream()
                    .filter(frame -> frame != null && !frame.isBlank())
                    .limit(128)
                    .toList());
            frameDurationTicks = Math.max(1, Math.min(1200, frameDurationTicks));
        }
    }

    public record NumberField(String id, String label, int defaultValue,
                              boolean positioned, int x, int y, int width) {}

    public record GuiButton(
            String label, String actionId, String texture, int width, int height,
            String soundId, int textColor, boolean positioned, int x, int y,
            String hoverTexture, String pressedTexture, String disabledTexture,
            Animation animation, Animation hoverAnimation, Animation pressedAnimation,
            Animation disabledAnimation, boolean enabled, boolean textOverlay,
            ButtonShape shape, int radius, int borderThickness, int fillColor,
            int hoverColor, int pressedColor, int disabledColor, int borderColor,
            boolean closeOnClick
    ) {}

    public record VisualElement(
            VisualType type, String value, String texture, int x, int y,
            int width, int height, int count, int color, boolean positioned,
            Animation animation
    ) {}

    public record OpenPayload(
            String guiId,
            String title,
            List<NumberField> fields,
            List<GuiButton> buttons,
            List<VisualElement> visuals,
            int panelWidth,
            int panelHeight,
            int panelX,
            int panelY,
            int minimumPanelHeight,
            int padding,
            int headerHeight,
            int buttonHeight,
            int backgroundTopColor,
            int backgroundBottomColor,
            boolean backgroundDimEnabled,
            int panelColor,
            int panelHighlightColor,
            int panelShadowColor,
            int headerColor,
            int accentColor,
            int stripeColor,
            int textColor,
            int borderColor,
            int shadowColor,
            int shadowSize,
            boolean shadowsEnabled,
            int inputColor,
            int inputHoverColor,
            int inputActiveColor,
            int inputBorderColor,
            int inputHighlightColor,
            int cursorColor,
            int buttonColor,
            int buttonHoverColor,
            int buttonBorderColor,
            int buttonHighlightColor,
            int buttonShadowColor,
            int buttonTextColor,
            int slotBorderColor,
            int slotHighlightColor,
            int slotColor,
            int slotInsetColor,
            int checkerColor1,
            int checkerColor2,
            int checkerPixelSize,
            boolean checkerEnabled,
            boolean stripesEnabled,
            boolean sideBarsEnabled,
            int sideBarWidth,
            int sideBarHeight,
            int sideBarGap,
            int sideBarBaseColor,
            int sideBarStripeColor,
            int sideBarShadowColor,
            String fontId,
            float textScale,
            float titleScale,
            int textAlignment
    ) implements CustomPacketPayload {

        public static final Type<OpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("createwarfare", "open_gui"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC =
                StreamCodec.of(OpenPayload::write, OpenPayload::read);

        private static void write(RegistryFriendlyByteBuf b, OpenPayload p) {
            b.writeUtf(p.guiId);
            b.writeUtf(p.title);

            b.writeVarInt(p.fields.size());
            for (NumberField f : p.fields) {
                b.writeUtf(f.id);
                b.writeUtf(f.label);
                b.writeInt(f.defaultValue);
                b.writeBoolean(f.positioned);
                b.writeInt(f.x);
                b.writeInt(f.y);
                b.writeInt(f.width);
            }

            b.writeVarInt(p.buttons.size());
            for (GuiButton v : p.buttons) {
                b.writeUtf(v.label);
                b.writeUtf(v.actionId);
                b.writeUtf(v.texture);
                b.writeInt(v.width);
                b.writeInt(v.height);
                b.writeUtf(v.soundId);
                b.writeInt(v.textColor);
                b.writeBoolean(v.positioned);
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeUtf(v.hoverTexture);
                b.writeUtf(v.pressedTexture);
                b.writeUtf(v.disabledTexture);
                writeAnimation(b, v.animation);
                writeAnimation(b, v.hoverAnimation);
                writeAnimation(b, v.pressedAnimation);
                writeAnimation(b, v.disabledAnimation);
                b.writeBoolean(v.enabled);
                b.writeBoolean(v.textOverlay);
                b.writeEnum(v.shape);
                b.writeInt(v.radius);
                b.writeInt(v.borderThickness);
                b.writeInt(v.fillColor);
                b.writeInt(v.hoverColor);
                b.writeInt(v.pressedColor);
                b.writeInt(v.disabledColor);
                b.writeInt(v.borderColor);
                b.writeBoolean(v.closeOnClick);
            }

            b.writeVarInt(p.visuals.size());
            for (VisualElement v : p.visuals) {
                b.writeEnum(v.type);
                b.writeUtf(v.value == null ? "" : v.value);
                b.writeUtf(v.texture == null ? "" : v.texture);
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.width);
                b.writeInt(v.height);
                b.writeInt(v.count);
                b.writeInt(v.color);
                b.writeBoolean(v.positioned);
                writeAnimation(b, v.animation);
            }

            b.writeInt(p.panelWidth);
            b.writeInt(p.panelHeight);
            b.writeInt(p.panelX);
            b.writeInt(p.panelY);
            b.writeInt(p.minimumPanelHeight);
            b.writeInt(p.padding);
            b.writeInt(p.headerHeight);
            b.writeInt(p.buttonHeight);

            b.writeInt(p.backgroundTopColor);
            b.writeInt(p.backgroundBottomColor);
            b.writeBoolean(p.backgroundDimEnabled);

            b.writeInt(p.panelColor);
            b.writeInt(p.panelHighlightColor);
            b.writeInt(p.panelShadowColor);
            b.writeInt(p.headerColor);
            b.writeInt(p.accentColor);
            b.writeInt(p.stripeColor);

            b.writeInt(p.textColor);
            b.writeInt(p.borderColor);
            b.writeInt(p.shadowColor);
            b.writeInt(p.shadowSize);
            b.writeBoolean(p.shadowsEnabled);

            b.writeInt(p.inputColor);
            b.writeInt(p.inputHoverColor);
            b.writeInt(p.inputActiveColor);
            b.writeInt(p.inputBorderColor);
            b.writeInt(p.inputHighlightColor);
            b.writeInt(p.cursorColor);

            b.writeInt(p.buttonColor);
            b.writeInt(p.buttonHoverColor);
            b.writeInt(p.buttonBorderColor);
            b.writeInt(p.buttonHighlightColor);
            b.writeInt(p.buttonShadowColor);
            b.writeInt(p.buttonTextColor);

            b.writeInt(p.slotBorderColor);
            b.writeInt(p.slotHighlightColor);
            b.writeInt(p.slotColor);
            b.writeInt(p.slotInsetColor);

            b.writeInt(p.checkerColor1);
            b.writeInt(p.checkerColor2);
            b.writeInt(p.checkerPixelSize);
            b.writeBoolean(p.checkerEnabled);

            b.writeBoolean(p.stripesEnabled);
            b.writeBoolean(p.sideBarsEnabled);

            b.writeInt(p.sideBarWidth);
            b.writeInt(p.sideBarHeight);
            b.writeInt(p.sideBarGap);
            b.writeInt(p.sideBarBaseColor);
            b.writeInt(p.sideBarStripeColor);
            b.writeInt(p.sideBarShadowColor);

            b.writeUtf(p.fontId);
            b.writeFloat(p.textScale);
            b.writeFloat(p.titleScale);
            b.writeInt(p.textAlignment);
        }

        private static void writeAnimation(RegistryFriendlyByteBuf b, Animation animation) {
            b.writeBoolean(animation != null);
            if (animation == null) return;

            b.writeVarInt(animation.frames().size());
            for (String frame : animation.frames()) {
                b.writeUtf(frame, 256);
            }

            b.writeVarInt(animation.frameDurationTicks());
            b.writeBoolean(animation.loop());
        }

        private static Animation readAnimation(RegistryFriendlyByteBuf b) {
            if (!b.readBoolean()) return null;

            int count = Math.max(0, Math.min(128, b.readVarInt()));
            List<String> frames = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                frames.add(b.readUtf(256));
            }

            return new Animation(frames, b.readVarInt(), b.readBoolean());
        }

        private static OpenPayload read(RegistryFriendlyByteBuf b) {
            String id = b.readUtf();
            String title = b.readUtf();

            int fieldCount = Math.max(0, Math.min(128, b.readVarInt()));
            List<NumberField> fields = new ArrayList<>(fieldCount);

            for (int i = 0; i < fieldCount; i++) {
                fields.add(new NumberField(
                        b.readUtf(), b.readUtf(), b.readInt(),
                        b.readBoolean(), b.readInt(), b.readInt(), b.readInt()
                ));
            }

            int buttonCount = Math.max(0, Math.min(128, b.readVarInt()));
            List<GuiButton> buttons = new ArrayList<>(buttonCount);

            for (int i = 0; i < buttonCount; i++) {
                String label = b.readUtf();
                String action = b.readUtf();
                String texture = b.readUtf();
                int width = b.readInt();
                int height = b.readInt();
                String sound = b.readUtf();
                int textColor = b.readInt();
                boolean positioned = b.readBoolean();
                int x = b.readInt();
                int y = b.readInt();
                String hoverTexture = b.readUtf();
                String pressedTexture = b.readUtf();
                String disabledTexture = b.readUtf();

                Animation animation = readAnimation(b);
                Animation hoverAnimation = readAnimation(b);
                Animation pressedAnimation = readAnimation(b);
                Animation disabledAnimation = readAnimation(b);

                boolean enabled = b.readBoolean();
                boolean textOverlay = b.readBoolean();
                ButtonShape shape = b.readEnum(ButtonShape.class);
                int radius = b.readInt();
                int borderThickness = b.readInt();
                int fill = b.readInt();
                int hover = b.readInt();
                int pressed = b.readInt();
                int disabled = b.readInt();
                int border = b.readInt();
                boolean closeOnClick = b.readBoolean();

                buttons.add(new GuiButton(
                        label, action, texture, width, height, sound, textColor,
                        positioned, x, y, hoverTexture, pressedTexture, disabledTexture,
                        animation, hoverAnimation, pressedAnimation, disabledAnimation,
                        enabled, textOverlay, shape, radius, borderThickness,
                        fill, hover, pressed, disabled, border, closeOnClick
                ));
            }

            int visualCount = Math.max(0, Math.min(512, b.readVarInt()));
            List<VisualElement> visuals = new ArrayList<>(visualCount);

            for (int i = 0; i < visualCount; i++) {
                visuals.add(new VisualElement(
                        b.readEnum(VisualType.class),
                        b.readUtf(),
                        b.readUtf(),
                        b.readInt(),
                        b.readInt(),
                        b.readInt(),
                        b.readInt(),
                        b.readInt(),
                        b.readInt(),
                        b.readBoolean(),
                        readAnimation(b)
                ));
            }

            int panelWidth = b.readInt();
            int panelHeight = b.readInt();
            int panelX = b.readInt();
            int panelY = b.readInt();
            int minimumPanelHeight = b.readInt();
            int padding = b.readInt();
            int headerHeight = b.readInt();
            int buttonHeight = b.readInt();

            int backgroundTopColor = b.readInt();
            int backgroundBottomColor = b.readInt();
            boolean backgroundDimEnabled = b.readBoolean();

            int panelColor = b.readInt();
            int panelHighlightColor = b.readInt();
            int panelShadowColor = b.readInt();
            int headerColor = b.readInt();
            int accentColor = b.readInt();
            int stripeColor = b.readInt();

            int textColor = b.readInt();
            int borderColor = b.readInt();
            int shadowColor = b.readInt();
            int shadowSize = b.readInt();
            boolean shadowsEnabled = b.readBoolean();

            int inputColor = b.readInt();
            int inputHoverColor = b.readInt();
            int inputActiveColor = b.readInt();
            int inputBorderColor = b.readInt();
            int inputHighlightColor = b.readInt();
            int cursorColor = b.readInt();

            int buttonColor = b.readInt();
            int buttonHoverColor = b.readInt();
            int buttonBorderColor = b.readInt();
            int buttonHighlightColor = b.readInt();
            int buttonShadowColor = b.readInt();
            int buttonTextColor = b.readInt();

            int slotBorderColor = b.readInt();
            int slotHighlightColor = b.readInt();
            int slotColor = b.readInt();
            int slotInsetColor = b.readInt();

            int checkerColor1 = b.readInt();
            int checkerColor2 = b.readInt();
            int checkerPixelSize = b.readInt();
            boolean checkerEnabled = b.readBoolean();

            boolean stripesEnabled = b.readBoolean();
            boolean sideBarsEnabled = b.readBoolean();

            int sideBarWidth = b.readInt();
            int sideBarHeight = b.readInt();
            int sideBarGap = b.readInt();
            int sideBarBaseColor = b.readInt();
            int sideBarStripeColor = b.readInt();
            int sideBarShadowColor = b.readInt();

            String fontId = b.readUtf();
            float textScale = b.readFloat();
            float titleScale = b.readFloat();
            int textAlignment = b.readInt();

            return new OpenPayload(
                    id, title, fields, buttons, visuals,
                    panelWidth, panelHeight, panelX, panelY,
                    minimumPanelHeight, padding, headerHeight, buttonHeight,
                    backgroundTopColor, backgroundBottomColor, backgroundDimEnabled,
                    panelColor, panelHighlightColor, panelShadowColor, headerColor,
                    accentColor, stripeColor, textColor, borderColor, shadowColor,
                    shadowSize, shadowsEnabled,
                    inputColor, inputHoverColor, inputActiveColor, inputBorderColor,
                    inputHighlightColor, cursorColor,
                    buttonColor, buttonHoverColor, buttonBorderColor, buttonHighlightColor,
                    buttonShadowColor, buttonTextColor,
                    slotBorderColor, slotHighlightColor, slotColor, slotInsetColor,
                    checkerColor1, checkerColor2, checkerPixelSize, checkerEnabled,
                    stripesEnabled, sideBarsEnabled,
                    sideBarWidth, sideBarHeight, sideBarGap,
                    sideBarBaseColor, sideBarStripeColor, sideBarShadowColor,
                    fontId, textScale, titleScale, textAlignment
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(
                OpenPayload payload,
                net.neoforged.neoforge.network.handling.IPayloadContext context
        ) {
            context.enqueueWork(() ->
                    Minecraft.getInstance().setScreen(CWGuiScreen.fromPayload(payload))
            );
        }
    }
}