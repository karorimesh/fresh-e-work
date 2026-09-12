package io.bootify.copilot_middleware.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AmbiguousTaskListResponseDto {

    private List<AmbiguousTaskDto> tasks;

    private Integer total;

}
