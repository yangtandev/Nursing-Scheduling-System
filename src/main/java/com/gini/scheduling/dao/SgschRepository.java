package com.gini.scheduling.dao;

import java.time.LocalDate;
import java.util.List;

import com.gini.scheduling.model.Sgsch;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SgschRepository extends PagingAndSortingRepository<Sgsch, String> {
    @Override
    List<Sgsch> findAll();

    @Query(value = "SELECT * FROM sg.sgsch WHERE schdate BETWEEN ?1 AND ?2 ORDER BY schdate",
            nativeQuery = true)
    List<Sgsch> findAllByDate(LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM sg.sgsch WHERE useruno = ?1 AND schdate BETWEEN ?2 AND ?3 ORDER BY schdate",
            nativeQuery = true)
    List<Sgsch> findAllByUnoAndDate(String uno, LocalDate startDate, LocalDate endDate);

}
