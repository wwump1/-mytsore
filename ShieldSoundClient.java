package com.example.shieldsound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Клиентский мод: когда любой игрок рядом (в том числе ты сам) опускает щит,
 * проигрывается звук Minecraft, выбранный в меню настроек.
 * Звук слышит только твой клиент, на сервер ничего не отправляется.
 *
 * Состояние «поднят щит» у других игроков сервер и так рассылает всем клиентам
 * (это то же самое, что рисует анимацию щита), поэтому мод читает его локально.
 */
public class ShieldSoundClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("shieldsound");

    /** ID сущностей, которые на прошлом тике держали щит поднятым. */
    private Set<Integer> blockingLastTick = new HashSet<>();
    private ClientWorld lastWorld;

    @Override
    public void onInitializeClient() {
        ShieldSoundConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            blockingLastTick.clear();
            lastWorld = null;
            return;
        }
        if (world != lastWorld) {
            // смена мира/измерения — id сущностей больше не актуальны
            blockingLastTick.clear();
            lastWorld = world;
        }

        ShieldSoundConfig cfg = ShieldSoundConfig.get();
        Set<Integer> blockingNow = new HashSet<>();

        for (PlayerEntity player : world.getPlayers()) {
            int id = player.getId();
            boolean blocking = player.isBlocking();
            if (blocking) {
                blockingNow.add(id);
            }

            boolean loweredJustNow = blockingLastTick.contains(id) && !blocking;
            if (!loweredJustNow || !cfg.enabled || player.isSpectator()) {
                continue;
            }

            boolean isMe = player == client.player;
            if (isMe ? cfg.playForSelf : cfg.playForOthers) {
                playAt(client, cfg, player.getX(), player.getEyeY(), player.getZ());
            }
        }

        blockingLastTick = blockingNow;
    }

    /** Звук в точке мира (для себя и для других игроков — с затуханием по расстоянию). */
    private static void playAt(MinecraftClient client, ShieldSoundConfig cfg, double x, double y, double z) {
        SoundEvent event = eventFor(cfg.sound);
        if (event == null) {
            return;
        }
        // Громкость >1 в Minecraft не делает звук громче, а только увеличивает дальность.
        // Поэтому «громче 100%» делаем наложением нескольких одинаковых звуков.
        float remaining = cfg.volume;
        while (remaining > 0.01f) {
            float layer = Math.min(1.0f, remaining);
            remaining -= 1.0f;
            client.getSoundManager().play(new PositionedSoundInstance(
                    event, SoundCategory.MASTER, layer, cfg.pitch, Random.create(), x, y, z));
        }
    }

    /** Предпрослушивание из меню: играет прямо в уши, без привязки к миру. */
    public static void playPreview(MinecraftClient client) {
        ShieldSoundConfig cfg = ShieldSoundConfig.get();
        SoundEvent event = eventFor(cfg.sound);
        if (event == null) {
            return;
        }
        float remaining = cfg.volume;
        while (remaining > 0.01f) {
            float layer = Math.min(1.0f, remaining);
            remaining -= 1.0f;
            client.getSoundManager().play(PositionedSoundInstance.master(event, cfg.pitch, layer));
        }
    }

    private static SoundEvent eventFor(String soundId) {
        Identifier id = Identifier.tryParse(soundId);
        return id == null ? null : SoundEvent.of(id);
    }
}
