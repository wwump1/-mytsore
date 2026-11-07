import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import java.util.HashMap;
import java.util.Map;
// остальные импорты остаются как были
private Set<Integer> blockingLastTick = new HashSet<>();
/** id противника -> тик, до которого он считается противником */
private final Map<Integer, Long> opponents = new HashMap<>();
private ClientWorld lastWorld;
private long tickCounter;
private int lastHurtTime;

@Override
public void onInitializeClient() {
    ShieldSoundConfig.load();
    ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

    AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
        if (world.isClient() && entity instanceof PlayerEntity target
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
        if (p == me || p.isSpectator()) continue;
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
        return;
    }
    if (world != lastWorld) {
        blockingLastTick.clear();
        opponents.clear();
        lastWorld = world;
    }
    tickCounter++;
    ShieldSoundConfig cfg = ShieldSoundConfig.get();

    // Меня ударили -> ближайший игрок рядом считается противником
    int hurt = client.player.hurtTime;
    if (hurt > 0 && lastHurtTime == 0) {
        PlayerEntity attacker = nearestPlayer(world, client.player, 6.0);
        if (attacker != null) markOpponent(attacker.getId());
    }
    lastHurtTime = hurt;

    // Забываем тех, с кем давно не дрались
    opponents.values().removeIf(until -> until < tickCounter);

    Set<Integer> blockingNow = new HashSet<>();

    for (PlayerEntity player : world.getPlayers()) {
        int id = player.getId();
        boolean blocking = player.isBlocking();
        if (blocking) blockingNow.add(id);
        boolean loweredJustNow = blockingLastTick.contains(id) && !blocking;
        if (!loweredJustNow || !cfg.enabled || player.isSpectator()) continue;

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
            client.getSoundManager().play(PositionedSoundInstance.ui(event, cfg.pitch, layer));
        }
    }

    private static SoundEvent eventFor(String soundId) {
        Identifier id = Identifier.tryParse(soundId);
        return id == null ? null : SoundEvent.of(id);
    }
}
