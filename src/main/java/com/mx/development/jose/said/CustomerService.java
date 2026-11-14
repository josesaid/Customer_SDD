package com.mx.development.jose.said;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    /** CUST-01 */
    public CustomerResponse create(CustomerRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Customer customer = new Customer(
                request.firstName().trim(), request.lastName().trim(), email, blankToNull(request.phone()));
        return CustomerResponse.from(repository.save(customer));
    }

    /** CUST-02 */
    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(find(id));
    }

    /** CUST-03: active == null returns all customers. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(Boolean active) {
        List<Customer> customers = (active == null)
                ? repository.findAllByOrderByLastNameAsc()
                : repository.findAllByActiveOrderByLastNameAsc(active);
        return customers.stream().map(CustomerResponse::from).toList();
    }

    /** CUST-04 */
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        customer.update(
                request.firstName().trim(), request.lastName().trim(), email, blankToNull(request.phone()));
        return CustomerResponse.from(customer);
    }

    /** CUST-05: logical delete, idempotent. */
    public void deactivate(Long id) {
        find(id).deactivate();
    }

    private Customer find(Long id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
