package com.valor.items.client;

import com.valor.items.ValorItems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Which players have the Death Sickle circling around them (it is ready to use), and the setup of its render layer. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SickleOrbit {
    public static final ResourceLocation MODEL = new ResourceLocation(ValorItems.MODID, "item/death_sickle_entity");

    /** The server repeats its message now and then; if it stops (player left the range...), the sickle disappears. */
    private static final int TIMEOUT_TICKS = 260;
    private static final Map<Integer, Long> SHOWN = new ConcurrentHashMap<>();

    public static void set(int entityId, boolean show) {
        Minecraft mc = Minecraft.getInstance();
        if (!show || mc.level == null) {
            SHOWN.remove(entityId);
        } else {
            SHOWN.put(entityId, mc.level.getGameTime() + TIMEOUT_TICKS);
        }
    }

    static boolean isShown(Player player) {
        Minecraft mc = Minecraft.getInstance();
        Long end = SHOWN.get(player.getId());
        if (end == null || mc.level == null) {
            return false;
        }
        if (end <= mc.level.getGameTime()) {
            SHOWN.remove(player.getId());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void registerModel(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : new String[] {"default", "slim"}) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new SickleOrbitLayer(playerRenderer));
                playerRenderer.addLayer(new ThornLayer(playerRenderer));
            }
        }
    }

    private SickleOrbit() {}
}
