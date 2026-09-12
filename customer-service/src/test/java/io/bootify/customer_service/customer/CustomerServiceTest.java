package io.bootify.customer_service.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.server.ResponseStatusException;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
    }

    @Test
    void createShouldNormalizeLocalNumberUsingCountry() {
        final CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstname("Jane");
        customerDTO.setLastname("Doe");
        customerDTO.setPhone("0712345678");
        customerDTO.setCountry("za");
        customerDTO.setEmail("jane@example.com");

        final Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        customerService.create(customerDTO);

        final ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals("+27712345678", customerCaptor.getValue().getPhone());
        assertEquals("ZA", customerCaptor.getValue().getCountry());
    }

    @Test
    void createShouldInferCountryFromInternationalNumberWhenCountryMissing() {
        final CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstname("Jane");
        customerDTO.setLastname("Doe");
        customerDTO.setPhone("+27712345678");
        customerDTO.setEmail("jane@example.com");

        final Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        customerService.create(customerDTO);

        final ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals("+27712345678", customerCaptor.getValue().getPhone());
        assertEquals("ZA", customerCaptor.getValue().getCountry());
    }

    @Test
    void createShouldRejectLocalNumberWithoutCountry() {
        final CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstname("Jane");
        customerDTO.setLastname("Doe");
        customerDTO.setPhone("0712345678");
        customerDTO.setEmail("jane@example.com");

        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.create(customerDTO));
        assertEquals("400 BAD_REQUEST \"Country is required when phone number is not in international format\"",
                exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void createShouldRejectInvalidPhoneForCountry() {
        final CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstname("Jane");
        customerDTO.setLastname("Doe");
        customerDTO.setPhone("0712345678");
        customerDTO.setCountry("US");
        customerDTO.setEmail("jane@example.com");

        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.create(customerDTO));
        assertEquals("400 BAD_REQUEST \"Invalid phone number for country\"", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

}
