package org.destru;

public record Section<Pos>(Pos pos1, Pos pos2) {
    public Section<Pos> pos1(Pos pos1) {
        return new Section<>(pos1, pos2);
    }

    public Section<Pos> pos2(Pos pos2) {
        return new Section<>(pos1, pos2);
    }
}
