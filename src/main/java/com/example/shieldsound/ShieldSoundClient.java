package com.example.shieldsound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Клиентский мод: когда игрок опускает щит (перестаёт блокировать),
 * проигрывается ванильный звук Minecraft. Звук слышит только сам игрок,
 * на сервер ничего не отправляется — геймплей не меняется.
 */
public class ShieldSoundClient implements ClientModInitializer {

    // Звук кувшинки. Можно заменить на любой другой из SoundEvents, например:
    // SoundEvents.UI_BUTTON_CLICK.value(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
    // SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()
    private static final SoundEvent SOUND = SoundEvents.BLOCK_LILY_PAD_PLACE;

    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.0f;

    private boolean wasBlocking = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            wasBlocking = false;
            return;
        }

        boolean blocking = client.player.isBlocking();

        if (wasBlocking && !blocking) {
            client.player.playSound(SOUND, VOLUME, PITCH);
        }

        wasBlocking = blocking;
    }
}
