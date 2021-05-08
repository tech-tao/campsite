package com.techtao.campsite.persistence.repository;

import com.techtao.campsite.persistence.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * The JPA repository class for {@link Reservation}.
 *
 * @author rantao
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByStartFromLessThanEqualAndEndToGreaterThanEqual(Date endTo, Date startFrom);

    Reservation findByIdAndEmail(Long id, String email);

}
