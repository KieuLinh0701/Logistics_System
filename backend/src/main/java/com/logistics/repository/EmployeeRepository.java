package com.logistics.repository;

import com.logistics.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByCode(String code);
    List<Employee> findByOfficeId(Integer officeId);
    List<Employee> findByUserId(Integer userId);
    boolean existsByCode(String code);
    List<Employee> findAllByAccountRoleId(Integer accountRoleId);
    
    @Query("select e from Employee e where e.accountRole.account.id = :accountId")
    List<Employee> findAllByAccountId(@Param("accountId") Integer accountId);

    // nếu hữu ích: tìm employee theo accountRole (single optional)
    Optional<Employee> findByAccountRoleId(Integer accountRoleId);
}



