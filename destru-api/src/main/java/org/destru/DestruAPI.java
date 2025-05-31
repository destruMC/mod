package org.destru;

public interface DestruAPI<Pos, Section, Regions> {
    Pos pos();

    void pos(Pos pos);

    Section section();

    Regions regions();

    Regions clipboard();

    void section(Section section);

    class Provider {
        private static DestruAPI<?, ?, ?> API;

        @SuppressWarnings("unchecked")
        public static <Pos, Section, Regions> DestruAPI<Pos, Section, Regions> get() {
            return (DestruAPI<Pos, Section, Regions>) API;
        }

        public static <Pos, Section, Regions> void set(DestruAPI<Pos, Section, Regions> API) {
            Provider.API = API;
        }
    }
}
