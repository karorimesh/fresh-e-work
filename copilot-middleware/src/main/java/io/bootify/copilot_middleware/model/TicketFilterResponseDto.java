package io.bootify.copilot_middleware.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TicketFilterResponseDto {

    private List<TicketDto> tickets;

    private Integer total;

}