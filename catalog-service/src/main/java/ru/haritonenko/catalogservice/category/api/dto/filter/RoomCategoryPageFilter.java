package ru.haritonenko.catalogservice.category.api.dto.filter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Setter;
import ru.haritonenko.commonlibs.utils.pages.PageFilter;

@Setter
public class RoomCategoryPageFilter implements PageFilter {

    @Min(value = 0, message = "Min number of page is 0")
    private Integer pageNumber;
    @Min(value = 1, message = "Min size of page is 1")
    @Max(value = 100, message = "Max size of page is 100")
    private Integer pageSize;

    @Override
    public Integer getPageNumber() {
        return pageNumber;
    }

    @Override
    public Integer getPageSize() {
        return pageSize;
    }
}


