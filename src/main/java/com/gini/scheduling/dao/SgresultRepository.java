package com.gini.scheduling.dao;

import com.gini.scheduling.config.SchemaConfig;
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
    @Query(value = "SELECT schuuid,schdate,schweek,clsno,uno,clsnohid,clspr,overtime,hid FROM "+SchemaConfig.schema+".sgresult WITH UR",
    nativeQuery = true)
    List<Sgresult> findAll();

    @Query(value = "SELECT COUNT(*) FROM "+ SchemaConfig.schema +".sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate WITH UR",
            nativeQuery = true)
    int findCountByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT schuuid,schdate,schweek,clsno,uno,clsnohid,clspr,overtime,hid FROM "+SchemaConfig.schema+".sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate WITH UR",
            nativeQuery = true)
    List<Sgresult> findAllByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT schuuid,schdate,schweek,clsno,uno,clsnohid,clspr,overtime,hid FROM "+SchemaConfig.schema+".sgresult WHERE uno = :uno AND schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate WITH UR",
            nativeQuery = true)
    List<Sgresult> findAllByUnoAndDate(
            @Param("uno") String uno,
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM "+SchemaConfig.schema+".sgresult WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    void deleteALLByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );


}
