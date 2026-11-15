package com.valor.items.event;

import com.valor.items.ValorItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Which projectiles shot by a player count as his normal (basic) attack, so that critical hits and the effects of the
 * relics work with them: every arrow / trident (AbstractArrow), and any other projectile (or entity with an owner,
 * TraceableEntity) whose entity type is in the tag
 * valor_items:basic_projectiles (data/valor_items/tags/entity_types/basic_projectiles.json), for example the
 * projectiles of Sun's Bow.
 */
public final class BasicProjectiles {
    public static final TagKey<EntityType<?>> TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(ValorItems.MODID, "basic_projectiles"));

    /**
     * Same as {@link #TAG}, but these projectiles can never be a critical hit (they still trigger every other effect
     * of the relics). An entity type only needs to be in this tag, not in both.
     */
    public static final TagKey<EntityType<?>> NO_CRIT_TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(ValorItems.MODID, "no_crit_projectiles"));

    /** True if the projectile can never be a critical hit. */
    public static boolean cannotCrit(Entity direct) {
        return direct != null && direct.getType().is(NO_CRIT_TAG);
    }

    /** True if {@code direct} is a projectile of {@code player} that counts as his normal attack. */
    public static boolean isOwnedBasic(Entity direct, Player player) {
        if (direct instanceof AbstractArrow arrow) {
            return arrow.getOwner() == player;
        }
        // any entity that knows its owner (projectiles, and custom entities that implement TraceableEntity)
        return direct instanceof net.minecraft.world.entity.TraceableEntity traceable && traceable.getOwner() == player
                && (direct.getType().is(TAG) || direct.getType().is(NO_CRIT_TAG));
    }

    private BasicProjectiles() {}
}
