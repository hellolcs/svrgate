package com.nicednb.svrgate.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 페이지 객체 변환 유틸리티 클래스
 * Entity 페이지를 DTO 페이지로 변환하는 메서드를 제공합니다.
 */
public class PageConversionUtil {

    /**
     * Entity 페이지를 DTO 페이지로 변환
     *
     * @param entityPage Entity 페이지 객체
     * @param converter Entity를 DTO로 변환하는 함수
     * @param <E> Entity 타입
     * @param <D> DTO 타입
     * @return DTO 페이지 객체
     */
    public static <E, D> Page<D> convertEntityPageToDtoPage(Page<E> entityPage, Function<E, D> converter) {
        List<D> dtoList = entityPage.getContent()
                .stream()
                .map(converter)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, entityPage.getPageable(), entityPage.getTotalElements());
    }
}