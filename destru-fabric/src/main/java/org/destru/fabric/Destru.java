package org.destru.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.destru.DestruAPI;
import org.destru.Region;
import org.destru.Section;
import org.destru.SimpleDestruAPI;
import org.destru.fabric.event.InputCallback;
import org.destru.fabric.ext.ClothConfig;
import org.destru.fabric.listener.CommandListener;
import org.destru.fabric.listener.InputListener;
import org.destru.fabric.listener.RenderListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Destru implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("destru");
    public static DestruAPI<BlockPos, Section<BlockPos>, List<Region<BlockPos, List<Pair<CompoundTag, ResourceLocation>>>>> API;
    public static boolean ACTIVE;
    public static boolean CONFIG;

    @Override
    public void onInitializeClient() {
        DestruAPI.Provider.set(new SimpleDestruAPI<>());
        API = DestruAPI.Provider.get();

        var dir = FabricLoader.getInstance().getConfigDir().resolve("destru");
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                LOGGER.error("Failed to create directory {}", dir);
            }
        }
        var config = dir.resolve("config.properties");
        Config.load(config);

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Config.save(config));
        InputCallback.EVENT.register(new InputListener());
        WorldRenderEvents.AFTER_ENTITIES.register(new RenderListener());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (CONFIG) {
                mc.setScreen(ClothConfig.get(mc.screen));
                CONFIG = false;
            }
        });
        ClientCommandRegistrationCallback.EVENT.register(new CommandListener(dir.resolve("structures")));
    }

    private static void sendMessage(@NotNull Component message) {
        message = Component.literal("[destru] ").withStyle(ChatFormatting.AQUA).append(message);
        Minecraft mc = Minecraft.getInstance();
        mc.gui.getChat().addMessage(message);
        mc.getNarrator().sayNow(message);
    }

    public static void sendFeedback(@NotNull Component message) {
        sendMessage(Component.empty().append(message).withStyle(ChatFormatting.WHITE));
    }

    public static void sendError(@NotNull Component message) {
        sendMessage(Component.empty().append(message).withStyle(ChatFormatting.RED));
    }

    public static void sendPos(@NotNull String translate, @NotNull BlockPos pos, int color) {
        sendFeedback(Component.translatable(translate, coloredPos(pos.getX(), color), coloredPos(pos.getY(), color), coloredPos(pos.getZ(), color)));
    }

    private static @NotNull Component coloredPos(int value, int color) {
        return Component.literal(String.valueOf(value)).withColor(color);
    }
}
