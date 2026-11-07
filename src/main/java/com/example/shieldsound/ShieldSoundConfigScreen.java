package com.example.shieldsound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;

/**
 * Экран настроек (открывается из Mod Menu).
 */
public class ShieldSoundConfigScreen extends Screen {

    private final Screen parent;
    private final ShieldSoundConfig cfg = ShieldSoundConfig.get();

    private ButtonWidget soundButton;
    private TextFieldWidget soundField;
    private int fieldLabelY;

    public ShieldSoundConfigScreen(Screen parent) {
        super(Text.literal("Shield Sound — настройки"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 200;
        int x = this.width / 2 - w / 2;
        int step = 24;
        int y = Math.max(28, (this.height - 260) / 2 + 20);

        // --- переключатели ---
        ButtonWidget enabledBtn = ButtonWidget.builder(toggleText("Мод включён", cfg.enabled), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(toggleText("Мод включён", cfg.enabled));
        }).dimensions(x, y, w, 20).build();
        this.addDrawableChild(enabledBtn);
        y += step;

        ButtonWidget selfBtn = ButtonWidget.builder(toggleText("Звук для моего щита", cfg.playForSelf), b -> {
            cfg.playForSelf = !cfg.playForSelf;
            b.setMessage(toggleText("Звук для моего щита", cfg.playForSelf));
        }).dimensions(x, y, w, 20).build();
        this.addDrawableChild(selfBtn);
        y += step;

        ButtonWidget othersBtn = ButtonWidget.builder(toggleText("Звук для других игроков", cfg.playForOthers), b -> {
            cfg.playForOthers = !cfg.playForOthers;
            b.setMessage(toggleText("Звук для других игроков", cfg.playForOthers));
        }).dimensions(x, y, w, 20).build();
        this.addDrawableChild(othersBtn);
        y += step;

        ButtonWidget oppBtn = ButtonWidget.builder(toggleText("Только мой противник", cfg.onlyOpponents), b -> {
            cfg.onlyOpponents = !cfg.onlyOpponents;
            b.setMessage(toggleText("Только мой противник", cfg.onlyOpponents));
        }).dimensions(x, y, w, 20).build();
        this.addDrawableChild(oppBtn);
        y += step;

        // --- выбор звука: [<] [название — нажми, чтобы прослушать] [>] ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> cycleSound(-1))
                .dimensions(x, y, 20, 20).build());
        soundButton = ButtonWidget.builder(Text.empty(), b -> ShieldSoundClient.playPreview(this.client))
                .dimensions(x + 22, y, w - 44, 20).build();
        this.addDrawableChild(soundButton);
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> cycleSound(1))
                .dimensions(x + w - 20, y, 20, 20).build());
        y += step;

        // --- свой ID звука ---
        fieldLabelY = y;
        y += 11;
        soundField = new TextFieldWidget(this.textRenderer, x, y, w, 20, Text.literal("ID звука"));
        soundField.setMaxLength(128);
        soundField.setText(cfg.sound);
        soundField.setChangedListener(text -> {
            cfg.sound = text.trim();
            refreshSoundButton();
        });
        this.addDrawableChild(soundField);
        y += step;

        // --- ползунки ---
        this.addDrawableChild(new ValueSlider(x, y, w, 20, "Громкость", 0.0, 4.0, cfg.volume, true, v -> cfg.volume = (float) v));
        y += step;
        this.addDrawableChild(new ValueSlider(x, y, w, 20, "Высота тона", 0.5, 2.0, cfg.pitch, false, v -> cfg.pitch = (float) v));
        y += step;

        // --- низ ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Проверить звук"),
                b -> ShieldSoundClient.playPreview(this.client)).dimensions(x, y, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> this.close())
                .dimensions(x + 102, y, 98, 20).build());

        refreshSoundButton();
    }

    private void cycleSound(int dir) {
        int n = ShieldSoundConfig.PRESETS.length;
        int idx = ShieldSoundConfig.presetIndexOf(cfg.sound);
        if (idx < 0) {
            idx = dir > 0 ? -1 : 0;
        }
        idx = Math.floorMod(idx + dir, n);
        cfg.sound = ShieldSoundConfig.PRESETS[idx][0];
        soundField.setText(cfg.sound);
        refreshSoundButton();
        ShieldSoundClient.playPreview(this.client);
    }

    private void refreshSoundButton() {
        if (soundButton == null) {
            return;
        }
        int idx = ShieldSoundConfig.presetIndexOf(cfg.sound);
        String name = idx >= 0 ? ShieldSoundConfig.PRESETS[idx][1] : "Свой звук";
        soundButton.setMessage(Text.literal(name));
    }

    private static Text toggleText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "ДА" : "НЕТ"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("ID звука (например block.decorated_pot.shatter):"),
                this.width / 2 - 100, fieldLabelY, 0xFFAAAAAA);
    }

    @Override
    public void removed() {
        ShieldSoundConfig.save();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    /** Ползунок со значением в диапазоне [min, max]. */
    private static class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final boolean percent;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int width, int height, String label, double min, double max,
                    double current, boolean percent, DoubleConsumer onChange) {
            super(x, y, width, height, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.percent = percent;
            this.onChange = onChange;
            this.updateMessage();
        }

        private double actual() {
            // округляем до 0.05, чтобы значения были «красивые»
            double v = min + (max - min) * this.value;
            return Math.round(v * 20.0) / 20.0;
        }

        @Override
        protected void updateMessage() {
            if (label == null) {
                return; // вызов из конструктора родителя, поля ещё не заполнены
            }
            double v = actual();
            String text = percent ? Math.round(v * 100.0) + "%" : String.format("%.2f", v);
            this.setMessage(Text.literal(label + ": " + text));
        }

        @Override
        protected void applyValue() {
            onChange.accept(actual());
        }
    }
}
