package org.destru.fabric.listener;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.destru.Region;
import org.destru.Section;
import org.destru.fabric.Config;
import org.destru.fabric.Destru;
import org.destru.fabric.ext.ClothConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos;
import static org.destru.fabric.listener.CommandListener.PathArgumentType.path;

public class CommandListener implements ClientCommandRegistrationCallback {
    private final Path dir;

    public CommandListener(Path dir) {
        this.dir = dir;
    }

    @Override
    public void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        var root = literal("destru");

        root
                .then(literal("clipboard")
                        .then(literal("clear")
                                .executes(context -> clipboard$clear())
                        )
                        .then(literal("load")
                                .then(argument("path", path(dir))
                                        .executes(context -> clipboard$load(context, context.getArgument("path", Path.class)))
                                )
                        )
                        .then(literal("paste")
                                .then(argument("pos", blockPos())
                                        .executes(context -> clipboard$paste(context, context.getArgument("pos", Coordinates.class)))
                                )
                        )
                )
                .then(literal("close")
                        .executes(context -> close())
                )
                .then(literal("save")
                        .then(argument("name", string())
                                .executes(context -> save(context.getArgument("name", String.class)))
                        )
                )
                .then(literal("section")
                        .then(literal("clear")
                                .executes(context -> {
                                    section$clear("pos1");
                                    section$clear("pos2");
                                    return 0;
                                })
                                .then(literal("pos1")
                                        .executes(context -> section$clear("pos1"))
                                )
                                .then(literal("pos2")
                                        .executes(context -> section$clear("pos2"))
                                )
                        )
                        .then(literal("modify")
                                .then(argument("value", blockPos())
                                        .executes(context -> {
                                            var coordinates = context.getArgument("value", Coordinates.class);
                                            section$modify(context, "pos1", coordinates);
                                            section$modify(context, "pos2", coordinates);
                                            return 0;
                                        })
                                )
                                .then(literal("pos1")
                                        .then(argument("value", blockPos())
                                                .executes(context -> section$modify(context, "pos1", context.getArgument("value", Coordinates.class)))
                                        )
                                )
                                .then(literal("pos2")
                                        .then(argument("value", blockPos())
                                                .executes(context -> section$modify(context, "pos2", context.getArgument("value", Coordinates.class)))
                                        )
                                )
                        )
                        .then(literal("query")
                                .executes(context -> {
                                    section$query("pos1");
                                    section$query("pos2");
                                    return 0;

                                })
                                .then(literal("pos1")
                                        .executes(context -> section$query("pos1"))
                                )
                                .then(literal("pos2")
                                        .executes(context -> section$query("pos2"))
                                )
                        )
                )
                .then(literal("structures")
                        .executes(context -> structures())
                )
                .then(literal("new")
                        .then(argument("pos", blockPos())
                                .executes(context -> n3w(context, context.getArgument("pos", Coordinates.class)))
                        )
                )
                .then(literal("push")
                        .executes(context -> push(context, false))
                        .then(argument("biome", bool())
                                .executes(context -> push(context, context.getArgument("biome", Boolean.class)))

                        )
                );

        if (ClothConfig.LOADED) {
            root.then(literal("config")
                    .executes(context -> config())
            );
        }

        dispatcher.register(root);
    }

    private int clipboard$clear() {
        Destru.API.clipboard().clear();
        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.clear"));
        return 0;
    }

    private int clipboard$load(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull Path path) {
        try {
            Destru.sendFeedback(Component.translatable("destru.commands.clipboard.load.started"));
            var nbt = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

            int count = 0;
            var optional = nbt.getList("regions");
            if (optional.isPresent()) {
                var regions = optional.get();
                var palette = nbt.getListOrEmpty("palette");

                var biomeRegistry = registryAccess(context.getSource().getWorld()).lookup(Registries.BIOME);

                for (var r : regions) {
                    if (r instanceof CompoundTag region) {
                        int[] size = region.getIntArray("size").orElse(null);
                        if (Objects.isNull(size) || size.length != 3) {
                            continue;
                        }
                        int[] pos = region.getIntArray("pos").orElse(null);
                        if (Objects.isNull(pos) || pos.length != 3) {
                            continue;
                        }

                        int s = size[0] * size[1] * size[2];
                        if (s == 0) {
                            continue;
                        }

                        int[] blocks = region.getIntArray("blocks").orElse(null);
                        if (Objects.nonNull(blocks) && blocks.length != s) {
                            continue;
                        }
                        int[] biomes = region.getIntArray("biomes").orElse(null);
                        if (Objects.nonNull(biomes) && biomes.length != s) {
                            continue;
                        }

                        var pairs = new ArrayList<Pair<CompoundTag, Holder<Biome>>>();
                        for (int i = 0; i < s; i++) {
                            CompoundTag block = null;
                            Holder<Biome> biome = null;

                            if (Objects.nonNull(blocks)) {
                                int index = blocks[i];
                                if (index != -1 && palette.get(index) instanceof CompoundTag tag) {
                                    block = tag;
                                }
                            }

                            if (Objects.nonNull(biomes)) {
                                int index = biomes[i];
                                if (index != -1 && palette.get(index) instanceof CompoundTag tag) {
                                    biome = tag.getString("id").flatMap(id -> biomeRegistry.flatMap(registry -> registry.get(ResourceLocation.tryParse(id)))).orElse(null);
                                }
                            }

                            pairs.add(new Pair<>(block, biome));
                        }

                        var pos1 = new BlockPos(pos[0], pos[1], pos[2]);
                        Destru.API.clipboard().add(new Region<>(new Section<>(pos1, pos1.offset(size[0] - 1, size[1] - 1, size[2] - 1)), pairs));

                        count++;
                    }
                }
            }

            Destru.sendFeedback(Component.translatable("destru.commands.clipboard.load.success", Component.literal(String.valueOf(count)).withStyle(ChatFormatting.AQUA)));
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.clipboard.load.failed", e.getLocalizedMessage()));
            Destru.LOGGER.error("Failed to load structure", e);
        }
        return 0;
    }

    private static int clipboard$paste(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull Coordinates coordinates) {
        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.paste.started"));
        var pos = coordinates2BlockPos(coordinates, context.getSource());
        var level = finalLevel(context.getSource().getWorld());
        var registryAccess = registryAccess(level);
        var blockRegistry = registryAccess.lookup(Registries.BLOCK).orElse(BuiltInRegistries.BLOCK);
        int count = 0;
        for (var region : Destru.API.clipboard()) {
            var section = region.section();
            var pairs = region.blocks();
            for (int i = 0; i < pairs.size(); i++) {
                var pair = pairs.get(i);
                BlockPos.betweenClosedStream(section.pos1(), section.pos2()).skip(i).findFirst().ifPresent(blockPos -> {
                    var finalPos = blockPos.offset(pos);

                    if (pair.getA() instanceof CompoundTag blockNbt) {
                        blockNbt.getString("id").flatMap(id -> blockRegistry.get(ResourceLocation.tryParse(id))).ifPresent(block -> {
                            blockNbt.getCompound("properties").ifPresent(properties -> properties.forEach((key, value) -> {
                                var blockState = block.value().defaultBlockState();
                                for (var property : blockState.getProperties()) {
                                    if (property.getName().equals(key)) {
                                        value.asString().ifPresent(s -> level.setBlock(finalPos, setValue(blockState, property, s), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE, 0));
                                        break;
                                    }
                                }
                            }));

                            blockNbt.getCompound("components").ifPresent(components -> {
                                var blockEntity = level.getChunkAt(finalPos).getBlockEntity(finalPos);
                                if (Objects.nonNull(blockEntity)) {
                                    blockEntity.loadCustomOnly(components, registryAccess);
                                }
                            });
                        });
                    }
//                    if (pair.getB() instanceof Holder<Biome> biome) {
//
//                    }
                });
            }
            count++;
        }
        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.paste.success", Component.literal(String.valueOf(count)).withStyle(ChatFormatting.AQUA)));
        return 0;
    }

    private static int close() {
        Destru.API.pos(null);
        Destru.API.regions().clear();
        Destru.sendFeedback(Component.translatable("destru.commands.close"));
        return 0;
    }

    private static int config() {
        Destru.CONFIG = true;
        return 0;
    }

    private int save(@NotNull String name) {
        var pos = Destru.API.pos();
        if (Objects.isNull(pos)) {
            Destru.sendError(Component.translatable("destru.commands.save.failed"));
            return 0;
        }
        try {
            name = name + ".destru";
            var path = dir.resolve(name);
            if (Files.exists(path)) {
                Destru.sendError(Component.translatable("destru.commands.save.exists", pathComponent(name, path)));
                return 0;
            }

            Destru.sendFeedback(Component.translatable("destru.commands.save.started"));

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            var root = new CompoundTag();
            var palette = new ListTag();
            var regions = new ListTag();

            for (var region : Destru.API.regions()) {
                var nbt = new CompoundTag();
                var section = region.section();
                var box = BoundingBox.fromCorners(section.pos1(), section.pos2());
                nbt.put("pos", new IntArrayTag(new int[] {box.minX() - pos.getX(), box.minY() - pos.getY(), box.minZ() - pos.getZ()}));
                nbt.put("size", new IntArrayTag(new int[] {box.getXSpan(), box.getYSpan(), box.getZSpan()}));

                var pairs = region.blocks();
                int size = pairs.size();
                int[] blocks = new int[size];
                int[] biomes = new int[size];
                for (int i = 0; i < size; i++) {
                    var pair = pairs.get(i);

                    var block = pair.getA();
                    if (Objects.isNull(block)) {
                        blocks[i] = -1;
                    } else {
                        if (palette.contains(block)) {
                            blocks[i] = palette.indexOf(block);
                        } else {
                            palette.add(block);
                            blocks[i] = palette.size() - 1;
                        }
                    }

                    var biome = pair.getB();
                    if (Objects.isNull(biome)) {
                        biomes[i] = -1;
                    } else {
                        var biomeNbt = new CompoundTag();
                        biomeNbt.putString("id", biome.getRegisteredName());
                        if (palette.contains(biomeNbt)) {
                            biomes[i] = palette.indexOf(biomeNbt);
                        } else {
                            palette.add(biomeNbt);
                            biomes[i] = palette.size() - 1;
                        }
                    }
                }
                if (!Arrays.stream(blocks).allMatch(i -> i == -1)) {
                    nbt.put("blocks", new IntArrayTag(blocks));
                }
                if (!Arrays.stream(biomes).allMatch(i -> i == -1)) {
                    nbt.put("biomes", new IntArrayTag(biomes));
                }

                regions.add(nbt);
            }

            if (!palette.isEmpty()) {
                root.put("palette", palette);
            }
            if (!regions.isEmpty()) {
                root.put("regions", regions);
            }

            NbtIo.writeCompressed(root, path);

            Destru.sendFeedback(Component.translatable("destru.commands.save.success", pathComponent(name, path)));
            return 1;
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.structures", e.getLocalizedMessage()));
            Destru.LOGGER.error("Failed to save structure", e);
        }
        return 0;
    }

    private static int section$clear(@NotNull String index) {
        switch (index) {
            case "pos1" -> {
                Destru.API.section(Destru.API.section().pos1(null));
                Destru.sendFeedback(Component.translatable("destru.commands.section.clear.pos1"));
            }
            case "pos2" -> {
                Destru.API.section(Destru.API.section().pos2(null));
                Destru.sendFeedback(Component.translatable("destru.commands.section.clear.pos2"));
            }
        }
        return 0;
    }

    private static int section$modify(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull String index, Coordinates coordinates) {
        var pos = coordinates2BlockPos(coordinates, context.getSource());
        switch (index) {
            case "pos1" -> {
                Destru.API.section(Destru.API.section().pos1(pos));
                Destru.sendPos("destru.commands.section.modify.pos1", pos, Config.RENDER_POS1_COLOR.getValue());
            }
            case "pos2" -> {
                Destru.API.section(Destru.API.section().pos2(pos));
                Destru.sendPos("destru.commands.section.modify.pos2", pos, Config.RENDER_POS2_COLOR.getValue());
            }
        }
        return 0;
    }

    private static int section$query(@NotNull String index) {
        switch (index) {
            case "pos1" -> {
                var pos = Destru.API.section().pos1();
                if (Objects.isNull(pos)) {
                    Destru.sendFeedback(Component.translatable("destru.commands.section.query.pos1.null"));
                } else {
                    Destru.sendPos("destru.commands.section.query.pos1", pos, Config.RENDER_POS1_COLOR.getValue());
                }
            }
            case "pos2" -> {
                var pos = Destru.API.section().pos2();
                if (Objects.isNull(pos)) {
                    Destru.sendFeedback(Component.translatable("destru.commands.section.query.pos2.null"));
                } else {
                    Destru.sendPos("destru.commands.section.query.pos2", pos, Config.RENDER_POS2_COLOR.getValue());
                }
            }
        }
        return 0;
    }

    private int structures() {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Util.getPlatform().openPath(dir);
            return 1;
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.save.exception", e.getLocalizedMessage()));
            Destru.LOGGER.error("Failed to open structures directory", e);
        }
        return 0;
    }

    private static int n3w(@NotNull CommandContext<FabricClientCommandSource> context, Coordinates coordinates) {
        var pos = coordinates2BlockPos(coordinates, context.getSource());
        Destru.API.pos(pos);
        Destru.API.regions().clear();
        Destru.sendPos("destru.commands.new", pos, Config.RENDER_REGION_COLOR.getValue());
        return 0;
    }

    private static int push(@NotNull CommandContext<FabricClientCommandSource> context, boolean biome) {
        var section = Destru.API.section();
        var pos1 = section.pos1();
        var pos2 = section.pos2();
        if (Objects.isNull(pos1) || Objects.isNull(pos2)) {
            Destru.sendError(Component.translatable("destru.commands.push.failed"));
            return 0;
        }

        var level = finalLevel(context.getSource().getWorld());
        var registryAccess = registryAccess(level);
        var blockRegistry = registryAccess.lookup(Registries.BLOCK).orElse(BuiltInRegistries.BLOCK);

        var blocks = new ArrayList<Pair<CompoundTag, Holder<Biome>>>();
        Destru.sendFeedback(Component.translatable("destru.commands.push.started"));
        BlockPos.betweenClosedStream(BoundingBox.fromCorners(pos1, pos2)).forEach(pos -> {
            CompoundTag nbt = null;
            var blockState = level.getBlockState(pos);
            var block = blockState.getBlock();

            var id = blockRegistry.getKey(block);
            if (Objects.nonNull(id)) {
                nbt = new CompoundTag();
                nbt.putString("id", id.toString());

                var properties = new CompoundTag();
                blockState.getValues().forEach((property, value) -> properties.putString(property.getName(), value.toString()));
                if (!properties.isEmpty()) {
                    nbt.put("properties", properties);
                }

                var components = new CompoundTag();
                var blockEntity = level.getChunkAt(pos).getBlockEntity(pos);
                if (Objects.nonNull(blockEntity)) {
                    blockEntity.saveAdditional(components, registryAccess);
                }
                if (!components.isEmpty()) {
                    nbt.put("components", components);
                }
            }

            blocks.add(new Pair<>(nbt, biome ? level.getBiome(pos) : null));
        });
        Destru.API.regions().add(new Region<>(section, blocks));
        Destru.sendFeedback(Component.translatable("destru.commands.push.success", Component.literal(String.valueOf(blocks.size())).withStyle(ChatFormatting.AQUA)));
        return 1;
    }

    private static @NotNull BlockPos coordinates2BlockPos(Coordinates coordinates, @NotNull FabricClientCommandSource source) {
        var vec3 = source.getPosition();
        if (coordinates instanceof WorldCoordinates worldCoordinates) {
            vec3 = new Vec3(worldCoordinates.x.get(vec3.x), worldCoordinates.y.get(vec3.y), worldCoordinates.z.get(vec3.z));
        } else if (coordinates instanceof LocalCoordinates localCoordinates) {
            var vec2 = source.getRotation();
            vec3 = EntityAnchorArgument.Anchor.EYES.apply(source.getPlayer());
            float f = Mth.cos((vec2.y + 90.0F) * ((float)Math.PI / 180F));
            float g = Mth.sin((vec2.y + 90.0F) * ((float)Math.PI / 180F));
            float h = Mth.cos(-vec2.x * ((float)Math.PI / 180F));
            float i = Mth.sin(-vec2.x * ((float)Math.PI / 180F));
            float j = Mth.cos((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
            float k = Mth.sin((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
            var vec32 = new Vec3(f * h, i, g * h);
            var vec33 = new Vec3(f * j, k, g * j);
            var vec34 = vec32.cross(vec33).scale(-1.0F);
            double d = vec32.x * localCoordinates.forwards + vec33.x * localCoordinates.up + vec34.x * localCoordinates.left;
            double e = vec32.y * localCoordinates.forwards + vec33.y * localCoordinates.up + vec34.y * localCoordinates.left;
            double l = vec32.z * localCoordinates.forwards + vec33.z * localCoordinates.up + vec34.z * localCoordinates.left;
            vec3 = new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
        }
        return BlockPos.containing(vec3);
    }

    private static @NotNull Component pathComponent(@NotNull String name, @NotNull Path path) {
        var component = Component.literal(name);
        return component.withStyle(component.getStyle().withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(path.toAbsolutePath())));
    }

    private static @NotNull Level finalLevel(ClientLevel clientLevel) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (Objects.nonNull(server)) {
            var serverLevel = server.getLevel(clientLevel.dimension());
            if (Objects.nonNull(serverLevel)) {
                return serverLevel;
            }
        }
        return clientLevel;
    }

    private static @NotNull RegistryAccess registryAccess(@NotNull Level level) {
        return level.registryAccess();
    }

    private static <T extends Comparable<T>> @Nullable BlockState setValue(BlockState block, @NotNull Property<T> property, String string) {
        return property.getValue(string).map(t -> block.trySetValue(property, t)).orElse(block);
    }

    static class PathArgumentType implements ArgumentType<Path> {
        private final StringArgumentType stringArgumentType;
        private final Path dir;

        private PathArgumentType(Path dir) {
            this.stringArgumentType = string();
            this.dir = dir;
        }

        @Contract(value = "_ -> new", pure = true)
        public static @NotNull PathArgumentType path(Path dir) {
            return new PathArgumentType(dir);
        }

        @Override
        public @Nullable Path parse(StringReader reader) {
            try {
                if (Files.notExists(dir)) {
                    Files.createDirectories(dir);
                }
                return dir.resolve(stringArgumentType.parse(reader));
            } catch (Exception e) {
                return null;
            }
        }

        @Contract("_, _ -> new")
        @Override
        public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    if (Files.notExists(dir)) {
                        Files.createDirectories(dir);
                    }
                    try (var list = Files.walk(dir)) {
                        list.skip(1).forEach(path -> builder.suggest(StringArgumentType.escapeIfRequired(dir.relativize(path).toString())));
                    }
                } catch (Exception ignored) {}
                return builder.build();
            });
        }
    }
}
