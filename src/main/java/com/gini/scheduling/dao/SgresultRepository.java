package com.gini.scheduling.dao;

import com.gini.scheduling.model.Sgresult;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface SgresultRepository extends PagingAndSortingRepository<Sgresult, String> {
    @Override
    List<Sgresult> findAll();

    @Query(value = "SELECT COUNT(*) FROM sg.sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    int findCountByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sg.sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgresult> findAllByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sg.sgresult WHERE uno = :uno AND schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgresult> findAllByUnoAndDate(
            @Param("uno") String uno,
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM sg.sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    void deleteALLByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

}
