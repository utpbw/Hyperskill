package com.example.feedback.accounting;

import com.example.feedback.auth.AccountUser;
import com.example.feedback.auth.AccountUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class PayrollService {

    private static final DateTimeFormatter PERIOD_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("MM-uuuu")
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter PERIOD_RESPONSE_FORMATTER = new DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendPattern("uuuu")
            .toFormatter(Locale.ENGLISH);

    private final PayrollRecordRepository payrollRecordRepository;
    private final AccountUserRepository accountUserRepository;

    public PayrollService(PayrollRecordRepository payrollRecordRepository, AccountUserRepository accountUserRepository) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.accountUserRepository = accountUserRepository;
    }

    @Transactional
    public void uploadPayrolls(List<PayrollRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll list must not be empty");
        }

        List<PayrollRecord> recordsToSave = new ArrayList<>(requests.size());
        Set<String> uniqueKeys = new HashSet<>();

        for (PayrollRequest request : requests) {
            PayrollRecord record = mapRequestToRecord(request);
            String key = buildKey(record.getEmployee().getEmail(), record.getPeriod());
            if (!uniqueKeys.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate employee and period combination in request");
            }
            if (payrollRecordRepository.existsByEmployeeAndPeriod(record.getEmployee(), record.getPeriod())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll for this employee and period already exists");
            }
            recordsToSave.add(record);
        }

        payrollRecordRepository.saveAll(recordsToSave);
    }

    @Transactional
    public void updatePayroll(PayrollRequest request) {
        PayrollRecord record = mapRequestToRecord(request);
        PayrollRecord existing = payrollRecordRepository.findByEmployeeAndPeriod(record.getEmployee(), record.getPeriod())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll record not found"));
        existing.setSalary(record.getSalary());
        payrollRecordRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<EmployeePayrollResponse> getPayrollsFor(AccountUser user) {
        List<PayrollRecord> records = payrollRecordRepository.findAllByEmployeeOrderByPeriodDesc(user);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(record -> toResponse(user, record))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmployeePayrollResponse> getPayrollFor(AccountUser user, String periodValue) {
        YearMonth period = parsePeriod(periodValue);
        return payrollRecordRepository.findByEmployeeAndPeriod(user, period)
                .map(record -> toResponse(user, record));
    }

    private PayrollRecord mapRequestToRecord(PayrollRequest request) {
        String employeeEmail = Optional.ofNullable(request.employee())
                .map(String::trim)
                .map(String::toLowerCase)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee email must not be blank"));

        if (employeeEmail.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee email must not be blank");
        }

        long salary = Optional.ofNullable(request.salary())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary must be provided"));

        if (salary < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary must be non-negative");
        }

        YearMonth period = parsePeriod(request.period());

        AccountUser employee = accountUserRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found"));

        PayrollRecord record = new PayrollRecord();
        record.setEmployee(employee);
        record.setPeriod(period);
        record.setSalary(salary);
        return record;
    }

    private String buildKey(String employeeEmail, YearMonth period) {
        return employeeEmail + ":" + period;
    }

    private YearMonth parsePeriod(String value) {
        String trimmed = Optional.ofNullable(value)
                .map(String::trim)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period must be provided"));

        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period must be provided");
        }

        try {
            return YearMonth.parse(trimmed, PERIOD_PARSER);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong period format");
        }
    }

    private EmployeePayrollResponse toResponse(AccountUser user, PayrollRecord record) {
        String formattedPeriod = record.getPeriod().format(PERIOD_RESPONSE_FORMATTER);
        long salary = record.getSalary();
        long dollars = salary / 100;
        long cents = salary % 100;
        String salaryString = String.format(Locale.ENGLISH, "%d dollar(s) %d cent(s)", dollars, cents);
        return new EmployeePayrollResponse(user.getName(), user.getLastname(), formattedPeriod, salaryString);
    }
}
