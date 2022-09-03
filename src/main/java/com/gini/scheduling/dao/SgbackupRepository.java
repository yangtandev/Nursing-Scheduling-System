package com.gini.scheduling.dao;

import com.gini.scheduling.model.Sgbackup;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface SgbackupRepository extends PagingAndSortingRepository<Sgbackup, String> {
    @Override
    List<Sgbackup> findAll();

    @Query(value = "SELECT COUNT(*) FROM sgbackup WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    int findCountByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sgbackup WHERE schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgbackup> findAllByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT * FROM sgbackup WHERE uno = :uno AND schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate",
            nativeQuery = true)
    List<Sgbackup> findAllByUnoAndDate(
            @Param("uno") String uno,
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM sgbackup WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    void deleteALLByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

}
