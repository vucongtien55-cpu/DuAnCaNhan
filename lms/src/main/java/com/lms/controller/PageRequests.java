package com.lms.controller;

import com.lms.exception.InvalidSortException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Giới hạn tiêu chí sắp xếp mà client được phép yêu cầu. Mặc định Spring cho sắp xếp theo bất kỳ thuộc tính nào của
 * entity (kể cả {@code password} hay quan hệ lồng như {@code teacher.password}), nên mỗi API khai báo danh sách trường
 * công khai của riêng mình; trường khác bị từ chối bằng 400. Kích thước trang tối đa do
 * {@code spring.data.web.pageable.max-page-size} quyết định.
 */
final class PageRequests {

    private PageRequests() {
    }

    static Pageable restrict(Pageable pageable, Sort defaultSort, String... allowedProperties) {
        Set<String> allowed = new LinkedHashSet<>(Arrays.asList(allowedProperties));
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, defaultSort);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidSortException(order.getProperty(), allowed);
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
