package org.destru;

import java.util.ArrayList;
import java.util.List;

public class SimpleDestruAPI<Pos, Blocks> implements DestruAPI<Pos, Section<Pos>, List<Region<Pos, Blocks>>> {
    private Pos pos;
    private Section<Pos> section;
    private final List<Region<Pos, Blocks>> regions;
    private final List<Region<Pos, Blocks>> clipboard;

    public SimpleDestruAPI() {
        this.section = new Section<>(null, null);
        this.regions = new ArrayList<>();
        this.clipboard = new ArrayList<>();
    }

    @Override
    public Pos pos() {
        return pos;
    }

    @Override
    public void pos(Pos pos) {
        this.pos = pos;
    }

    @Override
    public Section<Pos> section() {
        return section;
    }

    @Override
    public List<Region<Pos, Blocks>> regions() {
        return regions;
    }

    @Override
    public List<Region<Pos, Blocks>> clipboard() {
        return clipboard;
    }

    @Override
    public void section(Section<Pos> section) {
        this.section = section;
    }
}
