package org.destru.fabric.ext;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.destru.fabric.Config;
import org.jetbrains.annotations.Nullable;

public class ClothConfig {
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("cloth-config");

    public static @Nullable Screen get(Screen screen) {
        var builder = ConfigBuilder.create()
                .setParentScreen(screen)
                .setTitle(Component.literal("destru").withStyle(ChatFormatting.AQUA));
        var entry = builder.entryBuilder();
        builder.getOrCreateCategory(Component.translatable("destru.category.render"))
                .addEntry(entry.startAlphaColorField(Component.translatable("destru.option.render.pos1.color"), Config.RENDER_POS1_COLOR.getValue())
                        .setDefaultValue(Config.RENDER_POS1_COLOR.getDefaultValue())
                        .setSaveConsumer(Config.RENDER_POS1_COLOR::setValue)
                        .build())
                .addEntry(entry.startAlphaColorField(Component.translatable("destru.option.render.pos2.color"), Config.RENDER_POS2_COLOR.getValue())
                        .setDefaultValue(Config.RENDER_POS2_COLOR.getDefaultValue())
                        .setSaveConsumer(Config.RENDER_POS2_COLOR::setValue)
                        .build())
                .addEntry(entry.startAlphaColorField(Component.translatable("destru.option.render.section.color"), Config.RENDER_SECTION_COLOR.getValue())
                        .setDefaultValue(Config.RENDER_SECTION_COLOR.getDefaultValue())
                        .setSaveConsumer(Config.RENDER_SECTION_COLOR::setValue)
                        .build())
                .addEntry(entry.startAlphaColorField(Component.translatable("destru.option.render.region.color"), Config.RENDER_REGION_COLOR.getValue())
                        .setDefaultValue(Config.RENDER_REGION_COLOR.getDefaultValue())
                        .setSaveConsumer(Config.RENDER_REGION_COLOR::setValue)
                        .build())
                .addEntry(entry.startAlphaColorField(Component.translatable("destru.option.render.clipboard.color"), Config.RENDER_CLIPBOARD_COLOR.getValue())
                        .setDefaultValue(Config.RENDER_CLIPBOARD_COLOR.getDefaultValue())
                        .setSaveConsumer(Config.RENDER_CLIPBOARD_COLOR::setValue)
                        .build());

        var activeCategory = entry.startSubCategory(Component.translatable("destru.option.control.active"));
        activeCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.control.trigger"), Config.CONTROL_ACTIVE_TOGGLE.getValue())
                .setDefaultValue(Config.CONTROL_ACTIVE_TOGGLE.getDefaultValue())
                .setSaveConsumer(Config.CONTROL_ACTIVE_TOGGLE::setValue)
                .setYesNoTextSupplier(flag -> flag ? Component.translatable("options.key.toggle") : Component.translatable("options.key.hold"))
                .build());
        activeCategory.add(entry.startKeyCodeField(Component.translatable("destru.option.control.key"), Config.CONTROL_ACTIVE_KEY.getValue())
                .setDefaultValue(Config.CONTROL_ACTIVE_KEY.getDefaultValue())
                .setKeySaveConsumer(Config.CONTROL_ACTIVE_KEY::setValue)
                .build());
        activeCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.control.pass"), Config.CONTROL_ACTIVE_PASS.getValue())
                .setDefaultValue(Config.CONTROL_ACTIVE_PASS.getDefaultValue())
                .setSaveConsumer(Config.CONTROL_ACTIVE_PASS::setValue)
                .build());
        var pos1Category = entry.startSubCategory(Component.translatable("destru.option.control.pos1"));
        pos1Category.add(entry.startKeyCodeField(Component.translatable("destru.option.control.key"), Config.CONTROL_POS1_KEY.getValue())
                .setDefaultValue(Config.CONTROL_POS1_KEY.getDefaultValue())
                .setKeySaveConsumer(Config.CONTROL_POS1_KEY::setValue)
                .build());
        pos1Category.add(entry.startBooleanToggle(Component.translatable("destru.option.control.pass"), Config.CONTROL_POS1_PASS.getValue())
                .setDefaultValue(Config.CONTROL_POS1_PASS.getDefaultValue())
                .setSaveConsumer(Config.CONTROL_POS1_PASS::setValue)
                .build());
        var pos2Category = entry.startSubCategory(Component.translatable("destru.option.control.pos2"));
        pos2Category.add(entry.startKeyCodeField(Component.translatable("destru.option.control.key"), Config.CONTROL_POS2_KEY.getValue())
                .setDefaultValue(Config.CONTROL_POS2_KEY.getDefaultValue())
                .setKeySaveConsumer(Config.CONTROL_POS2_KEY::setValue)
                .build());
        pos2Category.add(entry.startBooleanToggle(Component.translatable("destru.option.control.pass"), Config.CONTROL_POS2_PASS.getValue())
                .setDefaultValue(Config.CONTROL_POS2_PASS.getDefaultValue())
                .setSaveConsumer(Config.CONTROL_POS2_PASS::setValue)
                .build());
        var clipboardCategory = entry.startSubCategory(Component.translatable("destru.option.control.clipboard"));
        clipboardCategory.add(entry.startKeyCodeField(Component.translatable("destru.option.control.key"), Config.CONTROL_CLIPBOARD_KEY.getValue())
                .setDefaultValue(Config.CONTROL_CLIPBOARD_KEY.getDefaultValue())
                .setKeySaveConsumer(Config.CONTROL_CLIPBOARD_KEY::setValue)
                .build());
        clipboardCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.control.pass"), Config.CONTROL_CLIPBOARD_PASS.getValue())
                .setDefaultValue(Config.CONTROL_CLIPBOARD_PASS.getDefaultValue())
                .setSaveConsumer(Config.CONTROL_CLIPBOARD_PASS::setValue)
                .build());
        builder.getOrCreateCategory(Component.translatable("destru.category.control"))
                .addEntry(activeCategory.setExpanded(true).build())
                .addEntry(pos1Category.setExpanded(true).build())
                .addEntry(pos2Category.setExpanded(true).build());

        var pushCategory = entry.startSubCategory(Component.translatable("destru.option.flags.push"));
        pushCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.flags.biome"), Config.FLAGS_PUSH_BIOME.getValue())
                .setDefaultValue(Config.FLAGS_PUSH_BIOME.getDefaultValue())
                .setSaveConsumer(Config.FLAGS_PUSH_BIOME::setValue)
                .build());
        pushCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.flags.entity"), Config.FLAGS_PUSH_ENTITY.getValue())
                .setDefaultValue(Config.FLAGS_PUSH_ENTITY.getDefaultValue())
                .setSaveConsumer(Config.FLAGS_PUSH_ENTITY::setValue)
                .build());
        var loadCategory = entry.startSubCategory(Component.translatable("destru.option.flags.load"));
        loadCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.flags.biome"), Config.FLAGS_LOAD_BIOME.getValue())
                .setDefaultValue(Config.FLAGS_LOAD_BIOME.getDefaultValue())
                .setSaveConsumer(Config.FLAGS_LOAD_BIOME::setValue)
                .build());
        loadCategory.add(entry.startBooleanToggle(Component.translatable("destru.option.flags.entity"), Config.FLAGS_LOAD_ENTITY.getValue())
                .setDefaultValue(Config.FLAGS_LOAD_ENTITY.getDefaultValue())
                .setSaveConsumer(Config.FLAGS_LOAD_ENTITY::setValue)
                .build());
        builder.getOrCreateCategory(Component.translatable("destru.category.flags"))
                .addEntry(pushCategory.setExpanded(true).build())
                .addEntry(loadCategory.setExpanded(true).build());

        return builder.build();
    }
}
