package dev.kunas.eldenblade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.kunas.eldenblade.BloodSlashEntity;
import dev.kunas.eldenblade.BloodSlashShape;
import dev.kunas.eldenblade.EldenBlade;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenBlade.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BloodSlashRenderer extends EntityRenderer<BloodSlashEntity> {
    private static final ResourceLocation TEXTURE = EldenBlade.id("textures/entity/blood_sweep.png");
    private static final float[] BANDS = {0, .18f, .6f, 1};
    private static final class BloodRenderType extends RenderType {
        private BloodRenderType() {
            super("blood_slash", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                256, false, true, () -> {}, () -> {});
        }
        // Emissive color with depth writes: clouds must not paint through the ribbon.
        private static final RenderType TYPE = create("eldenblade_blood_slash",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1024, false, true,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP)
                .setOverlayState(OVERLAY).setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false));
    }

    public BloodSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EldenBlade.BLOOD_SLASH.get(), BloodSlashRenderer::new);
    }

    @Override public ResourceLocation getTextureLocation(BloodSlashEntity entity) { return TEXTURE; }

    @Override public void render(BloodSlashEntity entity, float yaw, float partialTick,
                                 PoseStack poses, MultiBufferSource buffers, int light) {
        float age = entity.visualAge(partialTick);
        if (age >= BloodSlashEntity.LIFETIME) return;
        float reveal = BloodSlashShape.reveal(age);
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poses.mulPose(Axis.ZP.rotationDegrees(BloodSlashShape.roll(entity.isSecond())));
        // The fan lies along the forward axis and banks with the stroke.
        // From behind it is edge-on; from the side its crescent surface is visible.
        var vertices = buffers.getBuffer(BloodRenderType.TYPE);
        for (int i = 0; i < BloodSlashShape.SEGMENTS; i++) {
            float u0 = (float)i / BloodSlashShape.SEGMENTS;
            if (u0 >= reveal) break;
            float u1 = Math.min((float)(i + 1) / BloodSlashShape.SEGMENTS, reveal);
            for (int band = 0; band < BloodSlashShape.RADIAL_BANDS; band++) {
                point(vertices, poses.last(), u0, BANDS[band], entity.isSecond(), age);
                point(vertices, poses.last(), u0, BANDS[band + 1], entity.isSecond(), age);
                point(vertices, poses.last(), u1, BANDS[band + 1], entity.isSecond(), age);
                point(vertices, poses.last(), u1, BANDS[band], entity.isSecond(), age);
            }
        }
        // Eight small deterministic mesh flecks; no particle emitters or extra entities.
        for (int i = 0; i < BloodSlashShape.FLECKS; i++) {
            float u = .06f + i * .12f;
            float drift = age - u * BloodSlashShape.SWEEP_TICKS - 1;
            if (drift <= 0 || u >= reveal) continue;
            float angle = BloodSlashShape.angle(u, entity.isSecond());
            float radius = 4.9f + .075f * drift;
            float length = .08f + .035f * (i % 3);
            float width = .035f + .012f * (i % 2);
            float height = (i % 2 == 0 ? 1 : -1) * .035f * drift;
            int alpha = (int)(125 * (1 - BloodSlashShape.smooth(drift / 8)));
            fleck(vertices, poses.last(), angle - length / radius, radius - width, height, 0, 0, alpha);
            fleck(vertices, poses.last(), angle - length / radius, radius + width, height + .025f, 0, 1, alpha);
            fleck(vertices, poses.last(), angle + length / radius, radius + width, height + .025f, 1, 1, alpha);
            fleck(vertices, poses.last(), angle + length / radius, radius - width, height, 1, 0, alpha);
        }
        poses.popPose();
    }

    private static void point(VertexConsumer out, PoseStack.Pose pose, float u, float v,
                              boolean second, float age) {
        float angle = BloodSlashShape.angle(u, second);
        float radius = BloodSlashShape.radius(v, age);
        float x = (float)Math.sin(angle) * radius;
        float z = (float)Math.cos(angle) * radius;
        float y = .28f * (float)Math.sin(Math.PI * u) * (1 - v) * (1 - v)
            + .055f * (float)Math.sin(u * 12) * v;
        int alpha = (int)(220 * BloodSlashShape.opacity(u, v, age));
        vertex(out, pose, x, y, z, u, v, alpha);
    }

    private static void fleck(VertexConsumer out, PoseStack.Pose pose, float angle, float radius,
                              float y, float u, float v, int alpha) {
        vertex(out, pose, (float)Math.sin(angle) * radius, y,
            (float)Math.cos(angle) * radius, u, v, alpha);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int alpha) {
        out.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha)
            .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
    }
}
