package org.destru.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import org.destru.fabric.event.InputCallback;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"), cancellable = true)
    private void callEvent(long l, int i, int j, int k, int m, CallbackInfo ci) {
        if (i == GLFW.GLFW_KEY_UNKNOWN) {
            if (InputCallback.EVENT.invoker().allowInput(InputConstants.Type.SCANCODE, j, GLFW.GLFW_RELEASE != k)) {
                return;
            }
        }
        else if (InputCallback.EVENT.invoker().allowInput(InputConstants.Type.KEYSYM, i, GLFW.GLFW_RELEASE != k)) {
            return;
        }
        ci.cancel();
    }
}
