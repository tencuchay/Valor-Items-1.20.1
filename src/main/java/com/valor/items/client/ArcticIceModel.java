package com.valor.items.client;

import com.valor.items.ValorItems;
import com.valor.items.entity.ArcticIceEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Files: geo/arctic_ice.geo.json, animations/arctic_ice.animation.json, textures/entity/arctic_ice.png. */
public class ArcticIceModel extends GeoModel<ArcticIceEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ValorItems.MODID, "geo/arctic_ice.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ValorItems.MODID, "textures/entity/arctic_ice.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ValorItems.MODID, "animations/arctic_ice.animation.json");

    @Override
    public ResourceLocation getModelResource(ArcticIceEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArcticIceEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArcticIceEntity animatable) {
        return ANIMATION;
    }
}
