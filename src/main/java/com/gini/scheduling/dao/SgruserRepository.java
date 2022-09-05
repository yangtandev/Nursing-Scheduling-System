package com.gini.scheduling.dao;

import java.time.LocalDate;
import java.util.List;

import com.gini.scheduling.model.Sgruser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


public interface SgruserRepository extends PagingAndSortingRepository<Sgruser, String> {
    @Override
    List<Sgruser> findAll();

    @Query(value = "SELECT COUNT(*) FROM sgruser",
            nativeQuery = true)
    int findCount();
}
