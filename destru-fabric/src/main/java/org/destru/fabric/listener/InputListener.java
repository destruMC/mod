package org.destru.fabric.listener;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.destru.fabric.Config;
import org.destru.fabric.Destru;
import org.destru.fabric.event.InputCallback;

import java.util.Objects;

import static org.destru.fabric.Destru.ACTIVE;
import static org.destru.fabric.Destru.API;

public class InputListener implements InputCallback.AllowInput {
    @Override
    public boolean allowInput(InputConstants.Type type, int key, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();

        if (Objects.nonNull(mc.screen)) {
            return true;
        }

        var activeKey = Config.CONTROL_ACTIVE_KEY.getValue();
        boolean active = activeKey.getType().equals(type) && activeKey.getValue() == key;
        if (Config.CONTROL_ACTIVE_TOGGLE.getValue()) {
            if (pressed && active) {
                ACTIVE = !ACTIVE;
            }
        } else {
            if (active) {
                ACTIVE = pressed;
            }
        }
        if (!Config.CONTROL_ACTIVE_PASS.getValue() && active) {
            return false;
        }

        if (!ACTIVE) {
            return true;
        }

        var pos1Key = Config.CONTROL_POS1_KEY.getValue();
        boolean pos1 = pos1Key.getType().equals(type) && pos1Key.getValue() == key;
        if (pressed && pos1) {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                var pos = hitResult.getBlockPos();
                if (mc.level != null && !mc.level.getBlockState(pos).isAir()) {
                    ClientCommandInternals.executeCommand(String.format("destru section modify pos1 %s %s %s", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        }
        if (!Config.CONTROL_POS1_PASS.getValue() && pos1) {
            return false;
        }

        var pos2Key = Config.CONTROL_POS2_KEY.getValue();
        boolean pos2 = pos2Key.getType().equals(type) && pos2Key.getValue() == key;
        if (pressed && pos2) {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                var pos = hitResult.getBlockPos();
                if (mc.level != null && !mc.level.getBlockState(pos).isAir()) {
                    ClientCommandInternals.executeCommand(String.format("destru section modify pos2 %s %s %s", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        }
        if (!Config.CONTROL_POS2_PASS.getValue() && pos2) {
            return false;
        }

        var clipboardKey = Config.CONTROL_CLIPBOARD_KEY.getValue();
        boolean clipboard = clipboardKey.getType().equals(type) && clipboardKey.getValue() == key;
        if (pressed && clipboard) {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                var pos = hitResult.getBlockPos();
                if (mc.level != null && !mc.level.getBlockState(pos).isAir()) {
                    pos = pos.above();
                    ClientCommandInternals.executeCommand(String.format("destru clipboard paste %s %s %s", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        }
        if (!Config.CONTROL_CLIPBOARD_PASS.getValue() && clipboard) {
            return false;
        }

        return true;
    }
}
