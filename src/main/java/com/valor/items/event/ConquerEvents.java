package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.FenrirsFangItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fenrir's Fang: "Conquer" - the damage dealt to a target below half of its health is raised. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ConquerEvents {

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player) {
            return;
        }
        int level = FenrirsFangItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        // the health of the target before this hit
        double fraction = target.getHealth() / target.getMaxHealth();
        if (fraction < ValorConfig.conquerThresholdPercent() / 100.0D) {
            event.setAmount((float) (event.getAmount() * (1.0D + ValorConfig.conquerPercent(level) / 100.0D)));
        }
    }

    private ConquerEvents() {}
}
