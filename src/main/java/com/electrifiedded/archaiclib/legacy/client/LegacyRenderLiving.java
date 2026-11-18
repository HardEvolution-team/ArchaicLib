package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Replacement for the countless 1.7 "RenderXxx extends RenderLiving { texture; getEntityTexture }" classes.
 * Register with {@link #register} in client preInit.
 */
@SideOnly(Side.CLIENT)
public class LegacyRenderLiving<T extends EntityLiving> extends RenderLiving<T> {
    private final Function<T, ResourceLocation> texture;

    public LegacyRenderLiving(RenderManager manager, ModelBase model, float shadow, Function<T, ResourceLocation> texture) {
        super(manager, model, shadow);
        this.texture = texture;
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) { return texture.apply(entity); }

    /** 1.7 texture strings like "MyMod:textures/mobs/Foo.png" are lower-cased (1.12 requirement). */
    public static ResourceLocation tex(String legacy) {
        return new ResourceLocation(legacy.toLowerCase(Locale.ROOT));
    }

    public static <T extends EntityLiving> void register(Class<T> cls, Supplier<? extends ModelBase> model, float shadow, String texture) {
        ResourceLocation rl = tex(texture);
        register(cls, model, shadow, e -> rl);
    }

    public static <T extends EntityLiving> void register(Class<T> cls, Supplier<? extends ModelBase> model, float shadow, Function<T, ResourceLocation> texture) {
        IRenderFactory<T> f = m -> new LegacyRenderLiving<>(m, model.get(), shadow, texture);
        RenderingRegistry.registerEntityRenderingHandler(cls, f);
    }
}
