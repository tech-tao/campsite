package com.techtao.campsite.service;

import com.techtao.campsite.domain.exception.NotAvailableException;
import com.techtao.campsite.domain.model.DateRange;

import java.time.LocalDate;
import java.util.List;

/**
 * An interface defines services for reservation.
 *
 * @author rantao
 */
public interface ReservationService {

    /**
     * This method will take a range of dates and find the available ranges for reservations.
     *
     * @param startFrom the reservation start date
     * @param endTo the reservation end date
     * @return a list of {@link DateRange} which are still available
     */
    List<DateRange> searchForReservation(LocalDate startFrom, LocalDate endTo);

    /**
     * This method will try reserve the campsite for the given user and return an unique reservation id.
     *
     * @param userName the user's name
     * @param email the user's email
     * @param startFrom reserve starting date
     * @param endTo reserve ending date
     * @return the unique id
     * @throws NotAvailableException when the date range is not reservable
     */
    String reserve(String userName, String email, LocalDate startFrom, LocalDate endTo) throws NotAvailableException;

    /**
     * This method will try update an existing reservation.
     *
     * @param id the unique id for the reservation
     * @param email the user's email as a validation
     * @param startFrom reserve starting date
     * @param endTo reserve ending date
     * @return the unique id
     * @throws NotAvailableException when the date range is not reservable
     */
    String update(String id, String email, LocalDate startFrom, LocalDate endTo) throws NotAvailableException;


    /**
     * This method will try cancel an existing reservation.
     *
     * @param id the unique id for the reservation
     * @param email the user's email as a validation
     */
    void cancel(String id, String email);

}
