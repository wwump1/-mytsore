package com.example.shieldsound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Клиентский мод: когда игрок опускает щит, проигрывается звук Minecraft,
 * выбранный в меню настроек. Звук слышит только твой клиент.
 *
 * Режим «только противник»: звук играет только от игрока, с которым ты дерёшься
 * (ты его ударил или он ударил тебя за последние N секунд). Остальные
 * игроки вокруг молчат.
 */
public class ShieldSoundClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("shieldsound");

    /** Радиус, в котором ближайший игрок считается напавшим на меня. */
    private static final double ATTACKER_RADIUS = 6.0;

    /** ID сущностей, которые на прошлом тике держали щит поднятым. */
    private Set<Integer> blockingLastTick = new HashSet<>();
    /** id противника -> тик клиента, до которого он считается противником. */
    private final Map<Integer, Long> opponents = new HashMap<>();
    private ClientWorld lastWorld;
    private long tickCounter;
    private int lastHurtTime;

    @Override
    public void onInitializeClient() {
        ShieldSoundConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // Я ударил игрока -> он мой противник
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()
                    && entity instanceof PlayerEntity target
                    && player == MinecraftClient.getInstance().player) {
                markOpponent(target.getId());
            }
            return ActionResult.PASS;
        });
    }

    private void markOpponent(int id) {
        opponents.put(id, tickCounter + ShieldSoundConfig.get().combatSeconds * 20L);
    }

    private static PlayerEntity nearestPlayer(ClientWorld world, PlayerEntity me, double maxDist) {
        PlayerEntity best = null;
        double bestSq = maxDist * maxDist;
        for (PlayerEntity p : world.getPlayers()) {
            if (p == me || p.isSpectator()) {
                continue;
            }
            double d = p.squaredDistanceTo(me);
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    private void onTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            blockingLastTick.clear();
            opponents.clear();
            lastWorld = null;
            lastHurtTime = 0;
            return;
        }
        if (world != lastWorld) {
            // смена мира/измерения — id сущностей больше не актуальны
            blockingLastTick.clear();
            opponents.clear();
            lastWorld = world;
        }
        tickCounter++;

        ShieldSoundConfig cfg = ShieldSoundConfig.get();

        // Меня ударили -> ближайший игрок рядом считается противником
        int hurt = client.player.hurtTime;
        if (hurt > 0 && lastHurtTime == 0) {
            PlayerEntity attacker = nearestPlayer(world, client.player, ATTACKER_RADIUS);
            if (attacker != null) {
                markOpponent(attacker.getId());
            }
        }
        lastHurtTime = hurt;

        // Забываем тех, с кем давно не дрались
        opponents.values().removeIf(until -> until < tickCounter);

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
            boolean allowed = isMe
                    ? cfg.playForSelf
                    : cfg.playForOthers && (!cfg.onlyOpponents || opponents.containsKey(id));

            if (allowed) {
                playAt(client, cfg, player.getX(), player.getEyeY(), player.getZ());
            }
        }

        blockingLastTick = blockingNow;
    }

    /** Звук в точке мира (с затуханием по расстоянию). */
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
                    event, SoundCategory.PLAYERS, layer, cfg.pitch, Random.create(), x, y, z));
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
