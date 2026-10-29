package com.dongwhan.attendance.employee;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee(request.name(), request.employmentType(), request.hireDate(),
                request.toWorkCondition(currentYear()));
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public List<EmployeeResponse> list(boolean includeInactive) {
        List<Employee> employees = includeInactive
                ? employeeRepository.findAllByOrderByIdAsc()
                : employeeRepository.findByActiveTrueOrderByIdAsc();
        return employees.stream().map(EmployeeResponse::from).toList();
    }

    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(find(id));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);
        employee.update(request.name(), request.employmentType(), request.hireDate(),
                request.toWorkCondition(currentYear()));
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void deactivate(Long id) {
        find(id).deactivate();
    }

    private Employee find(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private int currentYear() {
        return Year.now(clock).getValue();
    }
}
