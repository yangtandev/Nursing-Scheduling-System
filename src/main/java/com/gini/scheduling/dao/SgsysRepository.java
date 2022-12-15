package com.gini.scheduling.dao;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgsys;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SgsysRepository extends PagingAndSortingRepository<Sgsys, String> {

    @Override
    @Query(value = "SELECT skey,val,hid FROM "+SchemaConfig.schema+".sgsys ORDER BY skey WITH UR",
            nativeQuery = true)
    List<Sgsys> findAll();


    @Query(value = "SELECT val FROM "+SchemaConfig.schema+".sgsys WHERE skey like 'impVDs%' ORDER BY skey WITH UR",
        nativeQuery = true)
    List<String> findAllNationalHolidays();
    
    @Query(value = "SELECT COUNT(*) FROM "+SchemaConfig.schema+".sgsys WITH UR",
            nativeQuery = true)
    int findCount();
}
