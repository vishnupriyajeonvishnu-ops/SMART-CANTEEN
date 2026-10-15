package com.dmi.canteen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUser(User user);
    Optional<Staff> findByUserId(Long userId);
    Optional<Staff> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}
