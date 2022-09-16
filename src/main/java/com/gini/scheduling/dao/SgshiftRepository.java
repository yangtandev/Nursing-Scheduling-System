package com.gini.scheduling.dao;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgshift;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SgshiftRepository extends PagingAndSortingRepository<Sgshift, String> {
    @Override
    @Query(value = "SELECT clsno,hid FROM "+SchemaConfig.schema+".sgshift WITH UR",
    nativeQuery = true)
    List<Sgshift> findAll();

    @Query(value = "SELECT COUNT(*) FROM "+SchemaConfig.schema+".sgshift WITH UR",
            nativeQuery = true)
    int findCount();

}
