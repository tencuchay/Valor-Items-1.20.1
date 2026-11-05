package com.valor.items.relic;

import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;

/**
 * One line of the relic UI.
 *
 * @param name        title of the ability
 * @param description text for a given relic level
 * @param upgradable  true if the description changes with the level (shows a "next level" preview)
 * @param changes     what changes when going from the given level to the next one (only the changed values)
 */
public record RelicAbility(Component name, IntFunction<Component> description, boolean upgradable,
                           IntFunction<List<Component>> changes) {
}
