package com.example.shieldsound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки мода. Хранятся в config/shieldsound.json.
 */
public final class ShieldSoundConfig {

    /** Готовые звуки Minecraft: {id, название для меню}. */
    public static final String[][] PRESETS = {
            {"block.decorated_pot.shatter", "Кувшин: разбился"},
            {"block.decorated_pot.break", "Кувшин: сломан"},
            {"block.decorated_pot.hit", "Кувшин: удар"},
            {"block.lily_pad.place", "Кувшинка"},
            {"block.glass.break", "Стекло: разбилось"},
            {"item.shield.block", "Щит: блок"},
            {"item.shield.break", "Щит: сломан"},
            {"block.anvil.land", "Наковальня"},
            {"block.bell.use", "Колокол"},
            {"block.note_block.bell", "Нота: колокольчик"},
            {"block.note_block.pling", "Нота: пинг"},
            {"block.amethyst_block.chime", "Аметист: звон"},
            {"block.amethyst_cluster.break", "Аметист: разбит"},
            {"entity.experience_orb.pickup", "Шарик опыта"},
            {"entity.player.levelup", "Новый уровень"},
            {"entity.item.pickup", "Подбор предмета"},
            {"entity.arrow.hit_player", "Попадание стрелы"},
            {"entity.arrow.shoot", "Выстрел из лука"},
            {"entity.chicken.egg", "Курица снесла яйцо"},
            {"entity.villager.no", "Житель: нет"},
            {"block.beacon.activate", "Маяк: включение"},
            {"block.stone_button.click_on", "Каменная кнопка"},
            {"block.wooden_button.click_on", "Деревянная кнопка"},
            {"ui.button.click", "Клик интерфейса"},
    };

    public static final String DEFAULT_SOUND = "block.decorated_pot.shatter";

    /** Главный выключатель. */
    public boolean enabled = true;
    /** Играть звук, когда опускаю щит я. */
    public boolean playForSelf = true;
    /** Играть звук, когда щит опускают другие игроки. */
    public boolean playForOthers = true;
    /** ID звука Minecraft (можно любой, в т.ч. из ресурспака). */
    public String sound = DEFAULT_SOUND;
    /** 0.0 – 4.0. Всё что выше 1.0 накладывается слоями, т.е. реально громче. */
    public float volume = 2.0f;
    /** 0.5 – 2.0 */
    public float pitch = 1.0f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ShieldSoundConfig instance = new ShieldSoundConfig();

    private ShieldSoundConfig() {
    }

    public static ShieldSoundConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("shieldsound.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader reader = Files.newBufferedReader(p)) {
                ShieldSoundConfig loaded = GSON.fromJson(reader, ShieldSoundConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                ShieldSoundClient.LOGGER.warn("Не удалось прочитать shieldsound.json, беру значения по умолчанию", e);
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        instance.sanitize();
        try (Writer writer = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            ShieldSoundClient.LOGGER.warn("Не удалось сохранить shieldsound.json", e);
        }
    }

    private void sanitize() {
        if (sound == null || sound.isBlank()) {
            sound = DEFAULT_SOUND;
        }
        sound = sound.trim();
        volume = Math.max(0.0f, Math.min(4.0f, volume));
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));
    }

    /** Индекс звука в PRESETS или -1, если указан свой. */
    public static int presetIndexOf(String id) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i][0].equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
