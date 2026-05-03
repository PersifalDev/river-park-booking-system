package ru.haritonenko.bookingservice.api.dto.filter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.haritonenko.commonlibs.utils.pages.PageFilter;

@Getter
@Setter
@NoArgsConstructor
public class BookingPageFilter implements PageFilter {

    @Min(value = 0, message = "Min number of page is 0")
    private Integer pageNumber = 0;

    @Min(value = 1, message = "Min size of page is 1")
    @Max(value = 100, message = "Max size of page is 100")
    private Integer pageSize = 10;
}
