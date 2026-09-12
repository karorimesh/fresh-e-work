package io.bootify.customer_service.customer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.bootify.customer_service.util.NotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class CustomerService {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private final CustomerRepository customerRepository;

    public CustomerService(final CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerDTO> findAll() {
        final List<Customer> customers = customerRepository.findAll(Sort.by("id"));
        return customers.stream()
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .toList();
    }

    public CustomerDTO get(final Long id) {
        return customerRepository.findById(id)
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final CustomerDTO customerDTO) {
        final Customer customer = new Customer();
        mapToEntity(customerDTO, customer);
        return customerRepository.save(customer).getId();
    }

    public void update(final Long id, final CustomerDTO customerDTO) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(customerDTO, customer);
        customerRepository.save(customer);
    }

    public void delete(final Long id) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        customerRepository.delete(customer);
    }

    private CustomerDTO mapToDTO(final Customer customer, final CustomerDTO customerDTO) {
        customerDTO.setId(customer.getId());
        customerDTO.setFirstname(customer.getFirstname());
        customerDTO.setLastname(customer.getLastname());
        customerDTO.setPhone(customer.getPhone());
        customerDTO.setCountry(customer.getCountry());
        customerDTO.setEmail(customer.getEmail());
        return customerDTO;
    }

    private Customer mapToEntity(final CustomerDTO customerDTO, final Customer customer) {
        final NormalizedPhone normalizedPhone = normalizePhone(customerDTO.getPhone(), customerDTO.getCountry());
        customer.setFirstname(customerDTO.getFirstname());
        customer.setLastname(customerDTO.getLastname());
        customer.setPhone(normalizedPhone.phoneNumber());
        customer.setCountry(normalizedPhone.countryCode());
        customer.setEmail(customerDTO.getEmail());
        return customer;
    }

    private NormalizedPhone normalizePhone(final String phone, final String country) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        final String countryCode = normalizeCountryCode(country);
        if (countryCode == null && !phone.startsWith("+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Country is required when phone number is not in international format");
        }
        if (countryCode != null && !PHONE_NUMBER_UTIL.getSupportedRegions().contains(countryCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported country code: " + countryCode);
        }
        try {
            final Phonenumber.PhoneNumber parsedPhoneNumber = PHONE_NUMBER_UTIL.parse(phone, countryCode);
            final String parsedRegion = PHONE_NUMBER_UTIL.getRegionCodeForNumber(parsedPhoneNumber);
            if (parsedRegion == null || !PHONE_NUMBER_UTIL.getSupportedRegions().contains(parsedRegion)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Phone number does not map to a supported country");
            }
            final boolean validForCountry = countryCode == null
                    ? PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber)
                    : phone.startsWith("+")
                            ? PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber)
                                    && countryCode.equals(parsedRegion)
                            : PHONE_NUMBER_UTIL.isValidNumberForRegion(parsedPhoneNumber, countryCode);
            if (!validForCountry) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number for country");
            }
            return new NormalizedPhone(
                    PHONE_NUMBER_UTIL.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164),
                    parsedRegion);
        } catch (final NumberParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number", ex);
        }
    }

    private String normalizeCountryCode(final String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return country.toUpperCase(Locale.ROOT);
    }

    private record NormalizedPhone(String phoneNumber, String countryCode) {
    }

}
