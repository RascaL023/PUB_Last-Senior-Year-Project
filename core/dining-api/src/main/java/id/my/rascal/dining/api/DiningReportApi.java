package id.my.rascal.dining.api;

public interface DiningReportApi {

    long countOpenDinings();
    long countOccupiedTables();
    long countAvailableTables();

}
