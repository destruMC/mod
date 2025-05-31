package org.destru.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import org.destru.fabric.event.InputCallback;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"), cancellable = true)
    private void callEvent(long l, int i, int j, int k, CallbackInfo ci) {
        if (InputCallback.EVENT.invoker().allowInput(InputConstants.Type.MOUSE, i, GLFW.GLFW_RELEASE != j)) {
            return;
        }
        ci.cancel();
    }
}
