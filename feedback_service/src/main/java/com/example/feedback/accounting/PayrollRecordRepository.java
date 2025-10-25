package com.example.feedback.accounting;

import com.example.feedback.auth.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    boolean existsByEmployeeAndPeriod(AccountUser employee, YearMonth period);

    Optional<PayrollRecord> findByEmployeeAndPeriod(AccountUser employee, YearMonth period);

    List<PayrollRecord> findAllByEmployeeOrderByPeriodDesc(AccountUser employee);
}
