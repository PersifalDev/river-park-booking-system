package ru.haritonenko.commonlibs.utils.pages;

import org.springframework.data.domain.Pageable;

import static java.util.Objects.nonNull;

public final class CommonPageable {

    private CommonPageable(){}

    public static Pageable getPageable(
            PageFilter filter,
            int defaultPageNumber,
            int defaultPageSize
    ){
        int pageNumber = nonNull(filter.getPageNumber()) ? filter.getPageNumber() : defaultPageNumber;
        int pageSize =  nonNull(filter.getPageSize()) ? filter.getPageSize() : defaultPageSize;
        if(pageSize > 100){
            pageSize = 100;
        }
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }

}
