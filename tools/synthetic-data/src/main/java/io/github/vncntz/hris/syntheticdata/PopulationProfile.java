package io.github.vncntz.hris.syntheticdata;

/** Baseline workload targets, not measured application capacity. */
public enum PopulationProfile {
    S(2_000), M(10_000), L(30_000), XL(100_000);

    private final int activePopulation;

    PopulationProfile(int activePopulation) {
        this.activePopulation = activePopulation;
    }

    public int activePopulation() {
        return activePopulation;
    }
}
