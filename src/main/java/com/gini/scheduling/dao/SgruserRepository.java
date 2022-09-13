package com.gini.scheduling.dao;

import java.util.List;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgruser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface SgruserRepository extends PagingAndSortingRepository<Sgruser, String> {
    @Override
    List<Sgruser> findAll();

    @Query(value = "SELECT COUNT(*) FROM "+ SchemaConfig.schema+".sgruser",
            nativeQuery = true)
    int findCount();
}
