package com.gini.scheduling.dao;

import com.gini.scheduling.model.Sgsys;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SgsysRepository extends PagingAndSortingRepository<Sgsys, String> {
    @Override
    @Query(value = "SELECT * FROM sg.sgsys ORDER BY skey",
            nativeQuery = true)
    List<Sgsys> findAll();

}
