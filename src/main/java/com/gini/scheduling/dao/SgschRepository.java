package com.gini.scheduling.dao;

import java.time.LocalDate;
import java.util.List;

import com.gini.scheduling.model.Sgsch;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SgschRepository extends PagingAndSortingRepository<Sgsch, String> {
    @Override
    List<Sgsch> findAll();

    @Query(value = "SELECT COUNT(*) FROM sg.sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    int findCountByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sg.sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgsch> findAllByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sg.sgsch WHERE uno = :uno AND schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgsch> findAllByUnoAndDate(
            @Param("uno") String uno,
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM sg.sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    void deleteALLByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

}
