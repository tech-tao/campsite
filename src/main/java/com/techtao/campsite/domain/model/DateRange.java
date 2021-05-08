package com.techtao.campsite.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * This is a class to represent a date range which is available for booking the campsite.
 *
 * @author rantao
 */
@AllArgsConstructor
@NoArgsConstructor
public class DateRange implements Serializable {

    @JsonProperty
    public LocalDate startFrom;

    @JsonProperty
    public LocalDate endTo;
}
