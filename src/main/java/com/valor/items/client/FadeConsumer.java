package com.valor.items.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Passes everything to another consumer but multiplies the alpha of every vertex. */
@OnlyIn(Dist.CLIENT)
public final class FadeConsumer implements VertexConsumer {
    private final VertexConsumer inner;
    private final float alpha;

    public FadeConsumer(VertexConsumer inner, float alpha) {
        this.inner = inner;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        inner.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        inner.color(r, g, b, Math.round(a * alpha));
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        inner.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        inner.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        inner.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        inner.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        inner.endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        inner.defaultColor(r, g, b, Math.round(a * alpha));
    }

    @Override
    public void unsetDefaultColor() {
        inner.unsetDefaultColor();
    }
}
