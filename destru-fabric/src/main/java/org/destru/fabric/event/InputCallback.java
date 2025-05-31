package org.destru.fabric.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class InputCallback {
    public static Event<AllowInput> EVENT = EventFactory.createArrayBacked(AllowInput.class, callbacks -> (type, key, pressed) -> {
        for (var callback : callbacks) {
            if (!callback.allowInput(type, key, pressed)) {
                return false;
            }
        }
        return true;
    });

    @FunctionalInterface
    public interface AllowInput {
        boolean allowInput(InputConstants.Type type, int key, boolean pressed);
    }
}
