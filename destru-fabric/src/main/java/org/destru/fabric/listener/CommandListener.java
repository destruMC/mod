package org.destru.fabric.listener;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
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
                                        .executes(context -> clipboard$load(context.getArgument("path", Path.class)))
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
                        .executes(this::structures)
                )
                .then(literal("new")
                        .then(argument("pos", blockPos())
                                .executes(context -> n3w(context, context.getArgument("pos", Coordinates.class)))
                        )
                )
                .then(literal("push")
                        .executes(context -> push(context, Config.FLAGS_PUSH_BIOME.getValue(), Config.FLAGS_PUSH_ENTITY.getValue()))
                        .then(argument("biome", bool())
                                .executes(context -> push(context, context.getArgument("biome", Boolean.class), Config.FLAGS_PUSH_ENTITY.getValue()))
                                .then(argument("entity", bool())
                                        .executes(context -> push(context, context.getArgument("biome", Boolean.class), context.getArgument("entity", Boolean.class)))
                                )
                        )
                        .then(literal("entities")
                                .then(argument("selector", entities())
                                        .executes(context -> push(context, context.getArgument("selector", EntitySelector.class)))
                                )
                        )
                );

        if (ClothConfig.LOADED) {
            root.then(literal("config")
                    .executes(context -> config())
            );
        }

        dispatcher.register(root);
    }

    private static int clipboard$clear() {
        Destru.API.clipboard().blocks().clear();
        Destru.API.clipboard().entities().clear();
        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.clear"));
        return 0;
    }

    private static int clipboard$load(@NotNull Path path) {
        try {
            Destru.sendFeedback(Component.translatable("destru.commands.clipboard.load.started"));
            var nbt = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

            int regions = 0;
            var palette = nbt.getListOrEmpty("palette");
            for (var r : nbt.getListOrEmpty("regions")) {
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

                    var pairs = new ArrayList<Pair<CompoundTag, ResourceLocation>>();
                    for (int i = 0; i < s; i++) {
                        CompoundTag block = null;
                        ResourceLocation biome = null;

                        if (Objects.nonNull(blocks)) {
                            int index = blocks[i];
                            if (index != -1 && palette.get(index) instanceof CompoundTag tag) {
                                block = tag;
                            }
                        }

                        if (Objects.nonNull(biomes)) {
                            int index = biomes[i];
                            if (index != -1 && palette.get(index) instanceof CompoundTag tag) {
                                var id = tag.getString("id");
                                if (id.isPresent()) {
                                    biome = ResourceLocation.tryParse(id.get());
                                }
                            }
                        }

                        pairs.add(new Pair<>(block, biome));
                    }

                    var pos1 = new BlockPos(pos[0], pos[1], pos[2]);
                    Destru.API.clipboard().blocks().add(new Region<>(new Section<>(pos1, pos1.offset(size[0] - 1, size[1] - 1, size[2] - 1)), pairs));

                    regions++;
                }
            }

            int entities = 0;
            for (var e : nbt.getListOrEmpty("entities")) {
                if (e instanceof CompoundTag entity) {
                    Destru.API.clipboard().entities().add(entity);
                    entities++;
                }
            }

            Destru.sendFeedback(Component.translatable("destru.commands.clipboard.load.success", Component.literal(String.valueOf(regions)).withStyle(ChatFormatting.AQUA), Component.literal(String.valueOf(entities)).withStyle(ChatFormatting.AQUA)));
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.clipboard.load.failed", e.getLocalizedMessage()));
            Destru.LOGGER.error("Failed to load structure", e);
        }
        return 0;
    }

    private static int clipboard$paste(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull Coordinates coordinates) {
        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.paste.started"));
        var pos = coordinates2BlockPos(coordinates, context.getSource());
        var level = level(context);
        var registryAccess = registryAccess(level);

        var blockRegistry = registryAccess.lookup(Registries.BLOCK).orElse(BuiltInRegistries.BLOCK);
        var biomeRegistry = registryAccess.lookup(Registries.BIOME);

        int blocks = 0;
        for (var region : Destru.API.clipboard().blocks()) {
            var section = region.section();
            var pairs = region.blocks();
            for (int i = 0; i < pairs.size(); i++) {
                var pair = pairs.get(i);
                BlockPos.betweenClosedStream(section.pos1(), section.pos2()).skip(i).findFirst().ifPresent(blockPos -> {
                    var finalPos = blockPos.offset(pos);

                    if (pair.getA() instanceof CompoundTag blockNbt) {
                        blockNbt.getString("id").flatMap(id -> blockRegistry.get(ResourceLocation.tryParse(id))).ifPresent(block -> {
                            final BlockState[] blockState = {block.value().defaultBlockState()};

                            blockNbt.getCompound("properties").ifPresent(properties -> properties.forEach((key, value) -> {
                                for (var property : blockState[0].getProperties()) {
                                    if (property.getName().equals(key)) {
                                        value.asString().ifPresent(s -> blockState[0] = setValue(blockState[0], property, s));
                                        break;
                                    }
                                }
                            }));

                            level.setBlock(finalPos, blockState[0], Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE, 0);

                            blockNbt.getCompound("components").ifPresent(components -> {
                                var blockEntity = level.getChunkAt(finalPos).getBlockEntity(finalPos);
                                if (Objects.nonNull(blockEntity)) {
                                    blockEntity.loadCustomOnly(components, registryAccess);
                                }
                            });
                        });
                    }
                    if (pair.getB() instanceof ResourceLocation location) {
                        biomeRegistry.flatMap(registry -> registry.get(location)).ifPresent(biome -> {
                            var chunk = level.getChunkAt(finalPos);
                            int[] biomePos = biomePos(level.getBiomeManager(), chunk, finalPos);
                            var chunkSection = chunk.getSection(biomePos[0]);
                            if (chunkSection.getBiomes() instanceof PalettedContainer<Holder<Biome>> biomes) {
                                biomes.set(biomePos[1], biomePos[2], biomePos[3], biome);
                                chunk.markUnsaved();
                                if (level instanceof ServerLevel serverLevel) {
                                    serverLevel.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
                                } else {
                                    context.getSource().getPlayer().connection.handleChunksBiomes(ClientboundChunksBiomesPacket.forChunks(List.of(chunk)));
                                }
                            }
                        });
                    }
                });
            }
            blocks++;
        }

        int entities = 0;
        for (var entity : Destru.API.clipboard().entities()) {
            var copied = entity.copy();
            var entityPos = new ArrayList<Double>();
            var list = copied.getListOrEmpty("Pos");
            for (var nbt : list) {
                nbt.asDouble().ifPresent(entityPos::add);
            }
            if (entityPos.size() == 3) {
                list.clear();
                list.add(DoubleTag.valueOf(pos.getX() + entityPos.get(0)));
                list.add(DoubleTag.valueOf(pos.getY() + entityPos.get(1)));
                list.add(DoubleTag.valueOf(pos.getZ() + entityPos.get(2)));
                var e = EntityType.create(copied, level, EntitySpawnReason.LOAD);
                if (e.isPresent()) {
                    if (level.addFreshEntity(e.get())) {
                        entities++;
                    }
                }
            }
        }

        Destru.sendFeedback(Component.translatable("destru.commands.clipboard.paste.success", Component.literal(String.valueOf(blocks)).withStyle(ChatFormatting.AQUA), Component.literal(String.valueOf(entities)).withStyle(ChatFormatting.AQUA)));
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
            Destru.sendError(Component.translatable("destru.commands.save.failed.new"));
            return 0;
        }
        try {
            name = name + ".destru";
            var path = dir.resolve(name);
            if (Files.exists(path)) {
                Destru.sendError(Component.translatable("destru.commands.save.failed.exists", pathComponent(name, path)));
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
                        biomeNbt.putString("id", biome.toString());
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

            var entities = new ListTag();
            entities.addAll(Destru.API.entities());
            if (!entities.isEmpty()) {
                root.put("entities", entities);
            }

            NbtIo.writeCompressed(root, path);

            Destru.sendFeedback(Component.translatable("destru.commands.save.success", pathComponent(name, path)));
            return 1;
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.save.failed.unknown", e.getLocalizedMessage()));
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

    private int structures(@NotNull CommandContext<FabricClientCommandSource> context) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Util.getPlatform().openPath(dir);
            Destru.sendFeedback(Component.translatable("destru.commands.structures.success", pathComponent(context.getSource().getClient().gameDirectory.toPath().relativize(dir).toString(), dir)));
            return 1;
        } catch (Exception e) {
            Destru.sendError(Component.translatable("destru.commands.structures.failed", e.getLocalizedMessage()));
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

    private static int push(@NotNull CommandContext<FabricClientCommandSource> context, boolean biome, boolean entity) {
        var pos = Destru.API.pos();
        if (Objects.isNull(pos)) {
            Destru.sendError(Component.translatable("destru.commands.push.failed.new"));
            return 0;
        }

        var section = Destru.API.section();
        var pos1 = section.pos1();
        var pos2 = section.pos2();
        if (Objects.isNull(pos1) || Objects.isNull(pos2)) {
            Destru.sendError(Component.translatable("destru.commands.push.failed.section"));
            return 0;
        }

        Destru.sendFeedback(Component.translatable("destru.commands.push.started"));

        var level = level(context);
        var registryAccess = registryAccess(level);
        var blockRegistry = registryAccess.lookup(Registries.BLOCK).orElse(BuiltInRegistries.BLOCK);

        var box = BoundingBox.fromCorners(pos1, pos2);

        var blocks = new ArrayList<Pair<CompoundTag, ResourceLocation>>();
        BlockPos.betweenClosedStream(box).forEach(blockPos -> {
            CompoundTag nbt = null;
            var blockState = level.getBlockState(blockPos);
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
                var blockEntity = level.getChunkAt(blockPos).getBlockEntity(blockPos);
                if (Objects.nonNull(blockEntity)) {
                    blockEntity.saveAdditional(components, registryAccess);
                }
                if (!components.isEmpty()) {
                    nbt.put("components", components);
                }
            }

            blocks.add(new Pair<>(nbt, biome ? level.getBiome(blockPos).unwrapKey().flatMap(key -> Optional.of(key.location())).orElse(null) : null));
        });

        var entities = new ArrayList<CompoundTag>();
        if (entity) {
            level.getEntities().get(AABB.of(box), e -> {
                if (!e.isPassenger()) {
                    var nbt = new CompoundTag();
                    if (e.saveAsPassenger(nbt)) {
                        nbt.remove("Pos");
                        nbt.store("Pos", Vec3.CODEC, e.position().subtract(pos.getX(), pos.getY(), pos.getZ()));
                        nbt.remove("UUID");
                        entities.add(nbt);
                    }
                }
            });
        }

        Destru.API.regions().add(new Region<>(section, blocks));
        if (entity) {
            Destru.API.entities().addAll(entities);
            Destru.sendFeedback(Component.translatable("destru.commands.push.success.all", Component.literal(String.valueOf(blocks.size())).withStyle(ChatFormatting.AQUA), Component.literal(String.valueOf(entities.size())).withStyle(ChatFormatting.AQUA)));
        } else {
            Destru.sendFeedback(Component.translatable("destru.commands.push.success.block", Component.literal(String.valueOf(blocks.size())).withStyle(ChatFormatting.AQUA)));
        }
        return 1;
    }

    private static int push(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull EntitySelector selector) {
        var pos = Destru.API.pos();
        if (Objects.isNull(pos)) {
            Destru.sendError(Component.translatable("destru.commands.push.failed.new"));
            return 0;
        }

        int count = 0;
        for (var entity : findEntities(context, selector)) {
            if (!entity.isPassenger()) {
                var nbt = new CompoundTag();
                if (entity.saveAsPassenger(nbt)) {
                    nbt.remove("Pos");
                    nbt.store("Pos", Vec3.CODEC, entity.position().subtract(pos.getX(), pos.getY(), pos.getZ()));
                    nbt.remove("UUID");
                    Destru.API.entities().add(nbt);
                    count++;
                }
            }
        }
        Destru.sendFeedback(Component.translatable("destru.commands.push.success.entity", Component.literal(String.valueOf(count)).withStyle(ChatFormatting.AQUA)));
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

    private static @NotNull Level level(@NotNull CommandContext<FabricClientCommandSource> context) {
        var clientLevel = context.getSource().getWorld();
        var server = context.getSource().getClient().getSingleplayerServer();
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

    private static <T extends Comparable<T>> BlockState setValue(@NotNull BlockState block, @NotNull Property<T> property, String value) {
        value = value.toLowerCase();
        return property.getValue(value).map(t -> block.trySetValue(property, t)).orElse(block);
    }

    @Contract("_, _, _ -> new")
    private static int @NotNull [] biomePos(@NotNull BiomeManager biomeManager, @NotNull ChunkAccess chunk, @NotNull BlockPos blockPos) {
        int i = blockPos.getX() - 2;
        int j = blockPos.getY() - 2;
        int k = blockPos.getZ() - 2;
        int l = i >> 2;
        int m = j >> 2;
        int n = k >> 2;
        double d = (double)(i & 3) / (double)4.0F;
        double e = (double)(j & 3) / (double)4.0F;
        double f = (double)(k & 3) / (double)4.0F;
        int o = 0;
        double g = Double.POSITIVE_INFINITY;

        for(int p = 0; p < 8; ++p) {
            boolean bl = (p & 4) == 0;
            boolean bl2 = (p & 2) == 0;
            boolean bl3 = (p & 1) == 0;
            int q = bl ? l : l + 1;
            int r = bl2 ? m : m + 1;
            int s = bl3 ? n : n + 1;
            double h = bl ? d : d - (double)1.0F;
            double t = bl2 ? e : e - (double)1.0F;
            double u = bl3 ? f : f - (double)1.0F;
            double v = BiomeManager.getFiddledDistance(biomeManager.biomeZoomSeed, q, r, s, h, t, u);
            if (g > v) {
                o = p;
                g = v;
            }
        }

        int p = (o & 4) == 0 ? l : l + 1;
        int w = (o & 2) == 0 ? m : m + 1;
        int x = (o & 1) == 0 ? n : n + 1;

        int a = QuartPos.fromBlock(chunk.getMinY());
        int b = a + QuartPos.fromBlock(chunk.getHeight()) - 1;
        int c = Mth.clamp(w, a, b);
        int z = chunk.getSectionIndex(QuartPos.toBlock(c));

        return new int[]{z, p & 3, c & 3, x & 3};
    }

    private static List<Entity> findEntities(@NotNull CommandContext<FabricClientCommandSource> context, @NotNull EntitySelector selector) {
        var level = level(context);

        if (level instanceof ServerLevel serverLevel) {
            if (!selector.includesEntities) {
                if (selector.playerName != null) {
                    var player = serverLevel.getServer().getPlayerList().getPlayerByName(selector.playerName);
                    return player == null ? List.of() : List.of(player);
                }
                else if (selector.entityUUID != null) {
                    var player = serverLevel.getServer().getPlayerList().getPlayer(selector.entityUUID);
                    return player == null ? List.of() : List.of(player);
                }
                else {
                    var vec3 = selector.position.apply(context.getSource().getPosition());
                    var aabb = selector.getAbsoluteAabb(vec3);
                    var predicate = selector.getPredicate(vec3, aabb, null);
                    if (selector.currentEntity) {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (predicate.test(player)) {
                                return List.of(player);
                            }
                        }
                        return List.of();
                    } else {
                        int i = selector.getResultLimit();
                        var list = new ObjectArrayList<Entity>();
                        if (selector.isWorldLimited()) {
                            list.addAll(serverLevel.getPlayers(predicate, i));
                        } else {
                            for(var player : serverLevel.getServer().getPlayerList().getPlayers()) {
                                if (predicate.test(player)) {
                                    list.add(player);
                                    if (list.size() >= i) {
                                        return list;
                                    }
                                }
                            }
                        }
                        return selector.sortAndLimit(vec3, list);
                    }
                }
            }
            else if (selector.playerName != null) {
                var player = serverLevel.getServer().getPlayerList().getPlayerByName(selector.playerName);
                return player == null ? List.of() : List.of(player);
            }
            else if (selector.entityUUID != null) {
                for(var sl : serverLevel.getServer().getAllLevels()) {
                    var entity = sl.getEntity(selector.entityUUID);
                    if (entity != null) {
                        if (entity.getType().isEnabled(serverLevel.enabledFeatures())) {
                            return List.of(entity);
                        }
                        break;
                    }
                }
                return List.of();
            }
            else {
                var vec3 = selector.position.apply(context.getSource().getPosition());
                var aabb = selector.getAbsoluteAabb(vec3);
                if (selector.currentEntity) {
                    var predicate = selector.getPredicate(vec3, aabb, null);
                    var entity = context.getSource().getEntity();
                    return entity != null && predicate.test(entity) ? List.of(entity) : List.of();
                } else {
                    var predicate = selector.getPredicate(vec3, aabb, serverLevel.enabledFeatures());
                    var list = new ObjectArrayList<Entity>();
                    if (selector.isWorldLimited()) {
                        selector.addEntities(list, serverLevel, aabb, predicate);
                    } else {
                        for(ServerLevel sl : serverLevel.getServer().getAllLevels()) {
                            selector.addEntities(list, sl, aabb, predicate);
                        }
                    }
                    return selector.sortAndLimit(vec3, list);
                }
            }
        }
        else {
            if (!selector.includesEntities) {
                if (selector.playerName != null) {
                    for (var player : level.players()) {
                        if (player.getScoreboardName().equals(selector.playerName)) {
                            return List.of(player);
                        }
                    }
                    return List.of();
                }
                else if (selector.entityUUID != null) {
                    var player = level.getPlayerByUUID(selector.entityUUID);
                    return player == null ? List.of() : List.of(player);
                }
                else {
                    var vec3 = selector.position.apply(context.getSource().getPosition());
                    var aabb = selector.getAbsoluteAabb(vec3);
                    var predicate = selector.getPredicate(vec3, aabb, null);
                    if (selector.currentEntity) {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (predicate.test(player)) {
                                return List.of(player);
                            }
                        }
                        return List.of();
                    } else {
                        int i = selector.getResultLimit();
                        var list = new ObjectArrayList<Entity>();
                        for (var player : level.players()) {
                            if (predicate.test(player)) {
                                list.add(player);
                                if (list.size() >= i) {
                                    return list;
                                }
                            }
                        }
                        return selector.sortAndLimit(vec3, list);
                    }
                }
            }
            else if (selector.playerName != null) {
                for (var player : level.players()) {
                    if (player.getScoreboardName().equals(selector.playerName)) {
                        return List.of(player);
                    }
                }
                return List.of();
            }
            else if (selector.entityUUID != null) {
                var entity = level.getEntity(selector.entityUUID);
                if (entity != null) {
                    if (entity.getType().isEnabled(level.enabledFeatures())) {
                        return List.of(entity);
                    }
                }
                return List.of();
            }
            else {
                var vec3 = selector.position.apply(context.getSource().getPosition());
                var aabb = selector.getAbsoluteAabb(vec3);
                if (selector.currentEntity) {
                    var predicate = selector.getPredicate(vec3, aabb, null);
                    var entity = context.getSource().getEntity();
                    return entity != null && predicate.test(entity) ? List.of(entity) : List.of();
                } else {
                    var predicate = selector.getPredicate(vec3, aabb, level.enabledFeatures());
                    var list = new ObjectArrayList<Entity>();
                    int i = selector.getResultLimit();
                    if (list.size() < i) {
                        if (aabb != null) {
                            level.getEntities(selector.type, aabb, predicate, list, i);
                        } else {
                            level.getEntities().get(selector.type, (entity) -> {
                                if (predicate.test(entity)) {
                                    list.add(entity);
                                    if (list.size() >= i) {
                                        return AbortableIterationConsumer.Continuation.ABORT;
                                    }
                                }
                                return AbortableIterationConsumer.Continuation.CONTINUE;
                            });
                        }
                    }
                    return selector.sortAndLimit(vec3, list);
                }
            }
        }
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
