package org.destru.fabric.listener;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.destru.fabric.Config;
import org.destru.fabric.Destru;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.OptionalDouble;

public class RenderListener implements WorldRenderEvents.AfterEntities {
    private static final RenderType.CompositeRenderType LINES;
    private static final RenderType.CompositeRenderType QUADS;

    @Override
    public void afterEntities(@NotNull WorldRenderContext context) {
        if (!Destru.ACTIVE) {
            return;
        }

        var profiler = Profiler.get();
        profiler.push("destru");

        var stack = context.matrixStack();
        var consumers = context.consumers();
        if (Objects.isNull(stack) || Objects.isNull(consumers)) {
            return;
        }

        var buffer = consumers.getBuffer(LINES);
        var camera = context.camera().getPosition().reverse();

        var pos1 = Destru.API.section().pos1();
        var pos2 = Destru.API.section().pos2();

        if (Objects.nonNull(pos1) || Objects.nonNull(pos2)) {
            if (Objects.isNull(pos1)) {
                drawOutlinedBox(stack, buffer, new AABB(pos2).move(camera), Config.RENDER_POS2_COLOR.getValue());
            }
            else if (Objects.isNull(pos2)) {
                drawOutlinedBox(stack, buffer, new AABB(pos1).move(camera), Config.RENDER_POS1_COLOR.getValue());
            }
            else {
                var aabb = AABB.of(BoundingBox.fromCorners(pos1, pos2));
                drawOutlinedBox(stack, buffer, aabb.move(camera), Config.RENDER_SECTION_COLOR.getValue());
                var aabb1 = new AABB(pos1);
                var aabb2 = new AABB(pos2);
                if (aabb1.maxY > aabb2.maxY) {
                    aabb1 = aabb1.setMinY(aabb1.maxY);
                    aabb2 = aabb2.setMaxY(aabb2.minY);
                } else {
                    aabb1 = aabb1.setMaxY(aabb1.minY);
                    aabb2 = aabb2.setMinY(aabb2.maxY);
                }
                drawOutlinedBox(stack, buffer, aabb1.move(camera), Config.RENDER_POS1_COLOR.getValue());
                drawOutlinedBox(stack, buffer, aabb2.move(camera), Config.RENDER_POS2_COLOR.getValue());
            }
        }

        buffer = consumers.getBuffer(QUADS);
        for (var region : Destru.API.regions()) {
            var section = region.section();
            drawSolidBox(stack, buffer, AABB.of(BoundingBox.fromCorners(section.pos1(), section.pos2())).move(camera), Config.RENDER_REGION_COLOR.getValue());
        }

        var clipboard = Destru.API.clipboard();
        if (!clipboard.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                var pos = blockHitResult.getBlockPos();
                if (!context.world().getBlockState(pos).isAir()) {
                    pos = pos.above(1);
                    var aabb = new AABB(pos);
                    drawSolidBox(stack, buffer, aabb.setMaxY(aabb.minY).move(camera), Config.RENDER_CLIPBOARD_COLOR.getValue());
                    buffer = consumers.getBuffer(LINES);
                    for (var region : clipboard) {
                        var section = region.section();
                        drawOutlinedBox(stack, buffer, AABB.of(BoundingBox.fromCorners(section.pos1(), section.pos2())).move(pos).move(camera), Config.RENDER_CLIPBOARD_COLOR.getValue());
                    }
                }
            }
        }

        profiler.pop();
    }

    private static void drawOutlinedBox(@NotNull PoseStack stack, @NotNull VertexConsumer buffer, @NotNull AABB aabb, int color) {
        var pose = stack.last();
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        buffer.addVertex(pose, minX, minY, minZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, minX, minY, minZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color).setNormal(pose, 1, 0, 0);

        buffer.addVertex(pose, minX, maxY, minZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setNormal(pose, 0, 0, 1);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color).setNormal(pose, 1, 0, 0);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setNormal(pose, 1, 0, 0);

        buffer.addVertex(pose, minX, minY, minZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color).setNormal(pose, 0, 1, 0);
    }

    private static void drawSolidBox(@NotNull PoseStack stack, @NotNull VertexConsumer buffer, @NotNull AABB aabb, int color) {
        var pose = stack.last();
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);

        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);

        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);

        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);
    }

    static {
        LINES = RenderType.create("destru:lines", 1536,
                RenderPipelines.register(
                        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                                .withLocation("pipeline/destru_lines")
                                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                .build()
                ),
                RenderType.CompositeState.builder()
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
                        .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                        .setOutputState(RenderType.ITEM_ENTITY_TARGET)
                        .createCompositeState(false)
        );
        QUADS = RenderType.create("destru:quads", 1536, false, true,
                RenderPipelines.register(
                        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                                .withLocation("pipeline/destru_quads")
                                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                .build()
                ),
                RenderType.CompositeState.builder()
                        .createCompositeState(false)
        );
    }
}