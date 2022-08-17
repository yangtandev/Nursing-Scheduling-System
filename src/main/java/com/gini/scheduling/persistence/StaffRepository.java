package com.gini.scheduling.persistence;

import java.util.List;
import java.util.UUID;

import com.gini.scheduling.domain.Staff;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StaffRepository extends PagingAndSortingRepository<Staff, String> {
    @Override
    List<Staff> findAll();
}
