package com.valor.items.effect;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Soul Prison (Tróc Nã): reduces all the healing of the target. Does not stack: it is one effect. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public class SoulPrisonEffect extends MobEffect {
    public SoulPrisonEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8A33D);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.SOUL_PRISON.get())) {
            double factor = 1.0D - ValorConfig.soulPrisonReducePercent() / 100.0D;
            event.setAmount((float) (event.getAmount() * Math.max(0.0D, factor)));
        }
    }
}
