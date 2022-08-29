package com.gini.scheduling.dao;

import java.util.List;

import com.gini.scheduling.model.Sgruser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SgruserRepository extends PagingAndSortingRepository<Sgruser, String> {
    @Override
    List<Sgruser> findAll();
    @Query(value = "SELECT * FROM sg.sgrroom WHERE uno = ?1 ",
            nativeQuery = true)
    List<Sgruser> findAllByUno(String uno);
}
