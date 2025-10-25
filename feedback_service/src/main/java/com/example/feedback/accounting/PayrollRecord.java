package com.example.feedback.accounting;

import com.example.feedback.auth.AccountUser;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.YearMonth;

/**
 * JPA entity representing a single payroll entry for an employee and period.
 */
@Entity
@Table(name = "payroll_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_employee_period", columnNames = {"employee_id", "period"})
})
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private AccountUser employee;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth period;

    @Column(nullable = false)
    private long salary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountUser getEmployee() {
        return employee;
    }

    public void setEmployee(AccountUser employee) {
        this.employee = employee;
    }

    public YearMonth getPeriod() {
        return period;
    }

    public void setPeriod(YearMonth period) {
        this.period = period;
    }

    public long getSalary() {
        return salary;
    }

    public void setSalary(long salary) {
        this.salary = salary;
    }
}
