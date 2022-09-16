package com.gini.scheduling.dao;

import com.gini.scheduling.config.SchemaConfig;
import com.gini.scheduling.model.Sgrroom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

public interface SgrroomRepository extends PagingAndSortingRepository<Sgrroom, String> {

    @Query(value = "SELECT uno,schdate,rmname,hid FROM "+ SchemaConfig.schema+".sgrroom WHERE schdate = ?1 WITH UR",
            nativeQuery = true)
    List<Sgrroom> findAllByDate(LocalDate schdate);

}
