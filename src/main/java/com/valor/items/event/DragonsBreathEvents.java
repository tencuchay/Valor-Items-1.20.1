package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.FafnirsTalonItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fafnir's Talon: "Dragon's Breath". A normal attack that hits deals an additional physical damage (a separate hit
 * with its own damage type, armor applies, never a crit) equal to a percent of the CURRENT health of the target.
 * The passive has a cooldown, shared by all the targets.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class DragonsBreathEvents {
    /** Physical damage of the passive; its data is in data/valor_items/damage_type. */
    public static final ResourceKey<DamageType> DRAGONS_BREATH =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ValorItems.MODID, "dragons_breath"));

    private static final String COOLDOWN_END_TAG = "ValorDragonCooldownEnd";

    /** Prevents the extra damage from triggering itself. */
    private static boolean applying = false;

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (applying || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !LifestealEvents.isBasicAttack(event.getSource(), player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player) {
            return;
        }
        int level = FafnirsTalonItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.getLong(COOLDOWN_END_TAG) > now) {
            return;
        }
        // the health the target has now, before this hit
        float extra = (float) (target.getHealth() * ValorConfig.dragonPercent(level) / 100.0D);
        if (extra <= 0.0F) {
            return;
        }
        data.putLong(COOLDOWN_END_TAG, now + Math.max(0, (int) Math.round(ValorConfig.dragonCooldownSeconds() * 20.0D)));
        var damageTypes = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        applying = true;
        try {
            BlitzBladeEvents.dealDamage(target, new DamageSource(damageTypes.getHolderOrThrow(DRAGONS_BREATH), player),
                    extra, false);
        } finally {
            applying = false;
        }
    }

    private DragonsBreathEvents() {}
}
