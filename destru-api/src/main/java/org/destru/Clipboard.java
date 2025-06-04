package org.destru;

import java.util.List;

public record Clipboard<Pos, Blocks, Entity>(List<Region<Pos, Blocks>> blocks, List<Entity> entities) {
}
