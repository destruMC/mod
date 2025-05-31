package org.destru;

public record Region<Pos, Blocks, Biomes>(Section<Pos> section, Blocks blocks, Biomes biomes) {
}
