package org.destru;

public interface DestruAPI<Pos, Section, Regions, Entities, Clipboard> {
    Pos pos();

    void pos(Pos pos);

    Section section();

    Regions regions();

    Entities entities();

    Clipboard clipboard();

    void section(Section section);

    class Provider {
        private static DestruAPI<?, ?, ?, ?, ?> API;

        @SuppressWarnings("unchecked")
        public static <Pos, Section, Regions, Entities, Clipboard> DestruAPI<Pos, Section, Regions, Entities, Clipboard> get() {
            return (DestruAPI<Pos, Section, Regions, Entities, Clipboard>) API;
        }

        public static <Pos, Section, Regions, Entities, Clipboard> void set(DestruAPI<Pos, Section, Regions, Entities, Clipboard> API) {
            Provider.API = API;
        }
    }
}
