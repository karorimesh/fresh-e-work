package io.bootify.customer_service.customer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CustomerDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String firstname;

    @NotNull
    @Size(max = 255)
    private String lastname;

    @NotNull
    @Size(max = 255)
    private String phone;

    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Za-z]{2}")
    private String country;

    @NotNull
    @Size(max = 255)
    private String email;

}
