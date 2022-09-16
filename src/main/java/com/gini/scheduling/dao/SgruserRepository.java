package com.gini.scheduling.dao;

import java.time.LocalDate;
import java.util.List;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgruser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;


public interface SgruserRepository extends PagingAndSortingRepository<Sgruser, String> {
    @Override
    @Query(value = "SELECT uno,uname,uteam,urole,uopno,uisbn,uissn,hid FROM "+ SchemaConfig.schema+".sgruser WITH UR",
    nativeQuery = true)
    List<Sgruser> findAll();

    @Query(value = "SELECT COUNT(*) FROM "+ SchemaConfig.schema+".sgruser WITH UR",
            nativeQuery = true)
    int findCount();
}
