package com.techtao.campsite.controller;

import com.google.common.base.Strings;
import com.techtao.campsite.domain.exception.NotAvailableException;
import com.techtao.campsite.domain.model.DateRange;
import com.techtao.campsite.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller class for REST APIs.
 *
 * @author rantao
 */
@RestController
@RequestMapping("/api")
public class CampSiteController {

    @Autowired
    ReservationService reservationService;

    @GetMapping(value = "/search", produces = "application/json")
    @ResponseBody
    public List<DateRange> getAvailableDateRanges(@Nullable @RequestParam String startFrom, @Nullable @RequestParam String endTo) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = Strings.isNullOrEmpty(startFrom) ? today.plusDays(1) : LocalDate.parse(startFrom);
        LocalDate endDate = Strings.isNullOrEmpty(endTo) ? today.plusMonths(1) : LocalDate.parse(endTo);


        return reservationService.searchForReservation(startDate, endDate);
    }

    @PutMapping(value = "/reserve", produces = "application/json")
    public String reserve(@RequestParam String email, @RequestParam String userName,
                          @RequestParam String startFrom, @RequestParam String endTo) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = Strings.isNullOrEmpty(startFrom) ? today.plusDays(1) : LocalDate.parse(startFrom);
        LocalDate endDate = Strings.isNullOrEmpty(endTo) ? today.plusMonths(1) : LocalDate.parse(endTo);

        if (startDate.isBefore(today) || endDate.isBefore(today)) {
            throw new IllegalArgumentException("You cannot reserve in history");
        }

        try {
            String id = reservationService.reserve(userName, email, startDate, endDate);
            return id;
        } catch (NotAvailableException ex) {
            return ex.getErrorMessage();
        }
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public String update(@PathVariable String id, @RequestParam String email,
                         @RequestParam String startFrom, @RequestParam String endTo) {
        try {
            String newId = reservationService.update(id, email, LocalDate.parse(startFrom), LocalDate.parse(endTo));
            return newId;
        } catch (NotAvailableException ex) {
            return ex.getErrorMessage();
        }
    }

    @DeleteMapping(value = "/cancel/{id}", produces = "application/json")
    public String cancel(@PathVariable String id, @RequestParam String email) {
        reservationService.cancel(id, email);
        return "SUCCESS";
    }

}
