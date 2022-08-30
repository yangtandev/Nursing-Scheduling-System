package com.gini.scheduling.dao;

import com.gini.scheduling.model.Sgrroom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

public interface SgrroomRepository extends PagingAndSortingRepository<Sgrroom, String> {

    @Query(value = "SELECT * FROM sg.sgrroom WHERE schdate = ?1 ",
            nativeQuery = true)
    List<Sgrroom> findAllByDate(LocalDate schdate);

}
