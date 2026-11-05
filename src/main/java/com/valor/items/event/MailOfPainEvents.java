package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.MailOfPainItem;
import com.valor.items.network.MailStatePacket;
import com.valor.items.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Mail of Pain: "Riposte" reflects a percentage of the damage taken (the amount before any reduction) to the
 * attacker as true damage; the active "Countersiege" raises that percentage for a while.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class MailOfPainEvents {
    private static final String ACTIVE_END_TAG = "ValorMailActiveEnd";
    private static final String COOLDOWN_END_TAG = "ValorMailCooldownEnd";
    private static final String ACTIVE_TOTAL_TAG = "ValorMailActiveTotal";
    private static final String COOLDOWN_TOTAL_TAG = "ValorMailCooldownTotal";

    /** Damage type of the reflected damage; its data and tags (ignores armor, toughness, enchantments...) are in data/. */
    public static final ResourceKey<DamageType> TRUE_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ValorItems.MODID, "true_damage"));

    /** Prevents two players wearing the relic from reflecting the reflection forever. */
    private static boolean reflecting = false;

    /**
     * Fires with the damage as it was dealt, before armor, toughness, enchantments, effects and immunity.
     * It also receives hits that other handlers cancelled (immortality, etc.): the damage was still "taken".
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (reflecting || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(TRUE_DAMAGE) || !(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F || attacker == player || !attacker.isAlive() || attacker.isAlliedTo(player)) {
            return;
        }
        // a hit that the invulnerability frames of the player ignore is not damage taken
        if (player.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN) && !event.isCanceled()) {
            return;
        }
        // a hit that the shield blocks is not damage taken; hits that something else cancelled (the immortality of
        // the Death Sickle) still count
        if (!event.isCanceled() && blockedByShield(player, source)) {
            return;
        }
        int level = MailOfPainItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        boolean boosted = player.getPersistentData().getLong(ACTIVE_END_TAG) > now;
        double percent = boosted ? ValorConfig.mailCountersiegePercent(level) : ValorConfig.mailRipostePercent(level);
        float reflected = (float) (amount * percent / 100.0D);
        if (reflected <= 0.0F) {
            return;
        }
        var damageTypes = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        reflecting = true;
        try {
            attacker.hurt(new DamageSource(damageTypes.getHolderOrThrow(TRUE_DAMAGE), player), reflected);
        } finally {
            reflecting = false;
        }
    }

    /** Same test as vanilla: raising a shield towards the source of a damage that the shield can stop. */
    private static boolean blockedByShield(ServerPlayer player, DamageSource source) {
        if (!player.isBlocking() || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        net.minecraft.world.phys.Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) {
            return false;
        }
        net.minecraft.world.phys.Vec3 view = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 toPlayer = sourcePos.vectorTo(player.position()).normalize();
        toPlayer = new net.minecraft.world.phys.Vec3(toPlayer.x, 0.0D, toPlayer.z);
        return toPlayer.dot(view) < 0.0D;
    }

    /** The active ability: called when the player presses its key. */
    public static void tryActivate(ServerPlayer player) {
        if (!MailOfPainItem.isEquipped(player)) {
            return;
        }
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.getLong(COOLDOWN_END_TAG) > now) {
            Component message = Component.translatable("message.valor_items.skill_on_cooldown")
                    .withStyle(ChatFormatting.RED);
            player.displayClientMessage(message, true); // the small text above the hotbar
            return;
        }
        int duration = Math.max(1, (int) Math.round(ValorConfig.mailCountersiegeSeconds() * 20.0D));
        int cooldown = Math.max(duration, ValorConfig.mailCooldownSeconds() * 20);
        data.putLong(ACTIVE_END_TAG, now + duration);
        data.putInt(ACTIVE_TOTAL_TAG, duration);
        data.putLong(COOLDOWN_END_TAG, now + cooldown);
        data.putInt(COOLDOWN_TOTAL_TAG, cooldown);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        sync(player);
        // everybody who sees the player draws the thorns while Countersiege lasts
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new com.valor.items.network.MailAuraPacket(player.getId(), duration));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** The cooldown survives death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        var now = event.getEntity().getPersistentData();
        now.putLong(COOLDOWN_END_TAG, old.getLong(COOLDOWN_END_TAG));
        now.putInt(COOLDOWN_TOTAL_TAG, old.getInt(COOLDOWN_TOTAL_TAG));
    }

    private static void sync(ServerPlayer player) {
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        int activeLeft = (int) Math.max(0L, data.getLong(ACTIVE_END_TAG) - now);
        int cooldownLeft = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - now);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MailStatePacket(activeLeft, data.getInt(ACTIVE_TOTAL_TAG), cooldownLeft,
                        data.getInt(COOLDOWN_TOTAL_TAG)));
    }

    private MailOfPainEvents() {}
}
