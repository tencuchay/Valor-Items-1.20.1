package com.valor.items.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;

@OnlyIn(Dist.CLIENT)
public final class ModKeybinds {
    public static final KeyMapping OPEN_RELIC = new KeyMapping(
            "key.valor_items.open_relic",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.valor_items");

    public static final KeyMapping USE_ACTIVE = new KeyMapping(
            "key.valor_items.use_active",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.valor_items");

    /** The second active ability worn (the one above the first on the screen): Y; the first one is H. */
    public static final KeyMapping USE_ACTIVE_2 = new KeyMapping(
            "key.valor_items.use_active_2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y,
            "key.categories.valor_items");

    private ModKeybinds() {}
}
