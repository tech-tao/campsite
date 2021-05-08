package com.techtao.campsite.service;

import com.google.common.collect.Lists;
import com.techtao.campsite.domain.exception.NotAvailableException;
import com.techtao.campsite.domain.model.DateRange;
import com.techtao.campsite.domain.validator.DateRangeValidator;
import com.techtao.campsite.persistence.entity.Reservation;
import com.techtao.campsite.persistence.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A service class to support reservation.
 *
 * @author rantao
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DateRangeValidator dateRangeValidator;

    private final static ReentrantLock addLock = new ReentrantLock();
    private final static ReentrantLock updateLock = new ReentrantLock();

    public List<DateRange> searchForReservation(LocalDate startFrom, LocalDate endTo) {
        List<DateRange> results = Lists.newArrayList();

        List<Reservation> reservations =
                reservationRepository.findAllByStartFromLessThanEqualAndEndToGreaterThanEqual(
                        convertToDate(endTo), convertToDate(startFrom));
        if (CollectionUtils.isEmpty(reservations)) {
            results.add(new DateRange(startFrom, endTo));
        } else {
            reservations.sort(Comparator.comparing(reservation -> reservation.getStartFrom()));
            LocalDate startDate = startFrom;
            for (Reservation reservation : reservations) {
                LocalDate convertedStartDate = convertToLocalDateFrom(reservation.getStartFrom());
                LocalDate convertedEndDate = convertToLocalDateFrom(reservation.getEndTo());
                if (convertedStartDate.isEqual(startDate)) {
                    startDate = convertedEndDate.plusDays(1);
                    continue;
                }

                if (startDate.isBefore(endTo)) {
                    results.add(new DateRange(startDate, convertedStartDate.minusDays(1)));
                    startDate = convertedEndDate.plusDays(1);
                }
            }

            if (startDate.isBefore(endTo)) {
                results.add(new DateRange(startDate, endTo));
            }
        }


        return results;
    }

    @Override
    public String reserve(String userName, String email, LocalDate startFrom, LocalDate endTo) throws NotAvailableException {
        if(!dateRangeValidator.validateDateRange(startFrom, endTo)) {
            throw new NotAvailableException("User could only reserve for maximum 3 days");
        }
        Reservation reservation = new Reservation();
        reservation.setEmail(email);
        reservation.setUsername(userName);
        reservation.setStartFrom(Date.from(startFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reservation.setEndTo(Date.from(endTo.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        try {
            if (addLock.tryLock(5, TimeUnit.SECONDS)) {
                List<Reservation> reservations =
                        reservationRepository.findAllByStartFromLessThanEqualAndEndToGreaterThanEqual(
                                convertToDate(endTo), convertToDate(startFrom));
                if (!CollectionUtils.isEmpty(reservations)) {
                    throw new NotAvailableException("There are reservations already in this date range");
                }

                Long id = reservationRepository.save(reservation).getId();
                return String.valueOf(id);
            } else {
                throw new NotAvailableException("Timeout, please try again.");
            }
        } catch (InterruptedException e) {
            throw new NotAvailableException("System error, please try again.");
        } finally {
            addLock.unlock();
        }
    }

    @Override
    public String update(String id, String email, LocalDate startFrom, LocalDate endTo) throws NotAvailableException {
        if(!dateRangeValidator.validateDateRange(startFrom, endTo)) {
            throw new NotAvailableException("User could only reserve for maximum 3 days");
        }

        Reservation reservation = reservationRepository.findByIdAndEmail(Long.parseLong(id), email);
        if (Objects.isNull(reservation)) {
            throw new NotAvailableException("Cannot find the reservation");
        }

        Reservation newReservation = new Reservation();
        newReservation.setEmail(email);
        newReservation.setUsername(reservation.getUsername());
        newReservation.setStartFrom(Date.from(startFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        newReservation.setEndTo(Date.from(endTo.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        try {
            if (updateLock.tryLock(5, TimeUnit.SECONDS)) {
                reservationRepository.delete(reservation);
                List<Reservation> reservations =
                        reservationRepository.findAllByStartFromLessThanEqualAndEndToGreaterThanEqual(
                                convertToDate(endTo), convertToDate(startFrom));
                if (!CollectionUtils.isEmpty(reservations)) {
                    throw new NotAvailableException("There are reservations already in this date range");
                }

                Long newId = reservationRepository.save(newReservation).getId();
                return String.valueOf(newId);
            } else {
                throw new NotAvailableException("Timeout, please try again.");
            }
        } catch (InterruptedException e) {
            throw new NotAvailableException("System error, please try again.");
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void cancel(String id, String email) {
        Reservation reservation = reservationRepository.findByIdAndEmail(Long.parseLong(id), email);
        if (Objects.isNull(reservation)) {
            return;
        }

        reservationRepository.delete(reservation);
    }

    private LocalDate convertToLocalDateFrom(Date inputDate) {
        return inputDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date convertToDate(LocalDate inputDate) {
        return Date.from(inputDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
