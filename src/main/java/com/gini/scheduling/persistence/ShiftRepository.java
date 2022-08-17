package com.gini.scheduling.persistence;

import java.util.List;
import java.util.UUID;

import com.gini.scheduling.domain.Shift;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ShiftRepository extends PagingAndSortingRepository<Shift, String> {
    @Override
    List<Shift> findAll();
}
