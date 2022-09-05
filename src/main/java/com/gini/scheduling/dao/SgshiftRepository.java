package com.gini.scheduling.dao;

import com.gini.scheduling.model.Sgshift;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SgshiftRepository extends PagingAndSortingRepository<Sgshift, String> {
    @Override
    List<Sgshift> findAll();



}
