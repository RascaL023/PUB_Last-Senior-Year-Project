package id.my.rascal.common.seed;

public interface Seeder {

    /** Which seed flavour this seeder belongs to. */
    SeedType seedType();

    void seed();

}
