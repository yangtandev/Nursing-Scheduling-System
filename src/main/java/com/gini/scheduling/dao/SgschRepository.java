package com.gini.scheduling.dao;

import java.time.LocalDate;
import java.util.List;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgsch;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SgschRepository extends PagingAndSortingRepository<Sgsch, String> {
    @Override
    @Query(value = "SELECT uno,schdate,clsno,clspr,overtime,remark,hid FROM "+SchemaConfig.schema+".sgsch WITH UR",
    nativeQuery = true)
    List<Sgsch> findAll();

    @Query(value = "SELECT COUNT(*) FROM "+SchemaConfig.schema+".sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate WITH UR",
            nativeQuery = true)
    int findCountByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT uno,schdate,clsno,clspr,overtime,remark,hid FROM "+SchemaConfig.schema+".sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate WITH UR",
            nativeQuery = true)
    List<Sgsch> findAllByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Query(value = "SELECT uno,schdate,clsno,clspr,overtime,remark,hid FROM "+ SchemaConfig.schema+".sgsch WHERE uno = :uno AND schdate BETWEEN :startSchdate AND :endSchdate ORDER BY schdate WITH UR",
            nativeQuery = true)
    List<Sgsch> findAllByUnoAndDate(
            @Param("uno") String uno,
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM "+SchemaConfig.schema+".sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate",
            nativeQuery = true)
    void deleteALLByDate(
            @Param("startSchdate") LocalDate startSchdate,
            @Param("endSchdate") LocalDate endSchdate
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM "+SchemaConfig.schema+".sgsch WHERE schdate BETWEEN :startSchdate AND :endSchdate AND uno = :uno",
        nativeQuery = true)
    void deleteALLByDateAndUno(
        @Param("startSchdate") LocalDate startSchdate,
        @Param("endSchdate") LocalDate endSchdate,
        @Param("uno") String uno
    );
}
