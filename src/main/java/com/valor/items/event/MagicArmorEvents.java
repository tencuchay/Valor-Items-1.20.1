package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.registry.ModAttributes;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Magic armor works like armor (same formula) but only against magic damage. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class MagicArmorEvents {

    static boolean isMagic(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (!isMagic(event.getSource())) {
            return;
        }
        AttributeInstance magicArmor = event.getEntity().getAttribute(ModAttributes.MAGIC_ARMOR.get());
        if (magicArmor == null || magicArmor.getValue() <= 0.0D) {
            return;
        }
        event.setAmount(CombatRules.getDamageAfterAbsorb(event.getAmount(), (float) Math.min(magicArmor.getValue(), 100.0D), 0.0F));
    }

    private MagicArmorEvents() {}
}
