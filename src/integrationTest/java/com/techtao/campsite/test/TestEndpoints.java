package com.techtao.campsite.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import com.techtao.campsite.Application;
import com.techtao.campsite.domain.model.DateRange;
import com.techtao.campsite.persistence.entity.Reservation;
import com.techtao.campsite.persistence.repository.ReservationRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class})
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class TestEndpoints {
    private final static String EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE = "The given dates are unavailable:There are reservations already in this date range";
    private final static String EXPECTED_ERROR_MESSAGE_DATE_RANGE_TOO_LONG = "The given dates are unavailable:User could only reserve for maximum 3 days";

    @Autowired
    private MockMvc mockMvc;

    //TODO normally this should be using the TEST DB or in memory DB like H2
    @Autowired
    private ReservationRepository reservationRepository;

    private HttpHeaders defaultHttpHeaders;

    private ObjectMapper objectMapper;
    private LocalDate currentDate;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE;

    @Before
    public void setUp() throws Exception {
        currentDate = LocalDate.now();

        defaultHttpHeaders = new HttpHeaders();
        defaultHttpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        defaultHttpHeaders.add(HttpHeaders.ACCEPT, "application/json");

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

    }

    @After
    public void clean() throws Exception {
        reservationRepository.deleteAll();
    }

    @BeforeEach
    public void init() throws Exception {
        reservationRepository.deleteAll();
    }


    @Test
    public void testGetAvailableDates() throws Exception {
        prepareData();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("startFrom", currentDate.plusDays(1).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(30).format(dateTimeFormatter));
        String results = mockMvc.perform(get("/api/search").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assert.assertNotNull(results);

        ArrayList<DateRange> dateRanges = objectMapper.readValue(results, new TypeReference<ArrayList<DateRange>>() {
        });

        Assert.assertNotNull(dateRanges);
        Assert.assertTrue(dateRanges.size() == 3);
        Assert.assertTrue(dateRanges.stream().anyMatch(dateRange -> dateRange.endTo.isEqual(currentDate.plusDays(9))));
        Assert.assertTrue(dateRanges.stream().anyMatch(dateRange -> dateRange.startFrom.isEqual(currentDate.plusDays(11))));

    }

    @Test
    public void testAddAReservationSuccessfully() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.plusDays(5).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(7).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Optional<Reservation> reservation = reservationRepository.findById(Long.parseLong(result));
        Assert.assertTrue(reservation.isPresent());
        Assert.assertTrue(reservation.get().getUsername().equals("Ran"));
        Assert.assertEquals(currentDate.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                reservation.get().getStartFrom().toInstant());
        Assert.assertEquals(currentDate.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                reservation.get().getEndTo().toInstant());
    }

    @Test
    public void testUpdateAReservationSuccessfully() throws Exception {
        prepareData();
        Reservation reservation = reservationRepository.findAll().get(0);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("email", reservation.getEmail());
        params.set("startFrom", currentDate.plusDays(5).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(7).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/update/" + String.valueOf(reservation.getId())).params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Optional<Reservation> oldReservation = reservationRepository.findById(reservation.getId());
        Optional<Reservation> newReservation = reservationRepository.findById(Long.parseLong(result));
        Assert.assertFalse(oldReservation.isPresent());
        Assert.assertTrue(newReservation.isPresent());
        Assert.assertEquals(currentDate.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                newReservation.get().getStartFrom().toInstant());
        Assert.assertEquals(currentDate.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                newReservation.get().getEndTo().toInstant());
    }

    @Test
    public void testUpdateAReservationWithAnInvalidDateRange() throws Exception {
        prepareData();
        Reservation reservation = reservationRepository.findAll().get(0);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("email", reservation.getEmail());
        params.set("startFrom", currentDate.plusDays(5).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(10).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/update/" + String.valueOf(reservation.getId())).params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_DATE_RANGE_TOO_LONG, result);
        Optional<Reservation> oldReservation = reservationRepository.findById(reservation.getId());
        Assert.assertTrue(oldReservation.isPresent());
    }

    @Test
    public void testUpdateAReservationToAnExistingReservationDates() throws Exception {
        prepareData();
        Reservation reservation1 = reservationRepository.findAll().get(0);
        Reservation reservation2 = reservationRepository.findAll().get(1);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("email", reservation1.getEmail());
        params.set("startFrom", reservation2.getStartFrom().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .plusDays(1).format(dateTimeFormatter));
        params.set("endTo", reservation2.getEndTo().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .plusDays(1).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/update/" + String.valueOf(reservation1.getId())).params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE, result);
        Optional<Reservation> oldReservation = reservationRepository.findById(reservation1.getId());
        Assert.assertTrue(oldReservation.isPresent());
    }

    @Test
    public void testCancelAReservationSuccessfully() throws Exception {
        prepareData();
        Reservation reservation1 = reservationRepository.findAll().get(0);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("email", reservation1.getEmail());
        mockMvc.perform(delete("/api/cancel/" + String.valueOf(reservation1.getId())).params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Optional<Reservation> oldReservation = reservationRepository.findById(reservation1.getId());
        Assert.assertFalse(oldReservation.isPresent());
    }

    @Test
    public void testAddAReservationOnUnavailableDates() throws Exception {
        prepareData();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.plusDays(9).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(11).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE, result);

        params.set("startFrom", currentDate.plusDays(8).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(10).format(dateTimeFormatter));
        result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE, result);

        params.set("startFrom", currentDate.plusDays(10).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(12).format(dateTimeFormatter));
        result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE, result);

        params.set("startFrom", currentDate.plusDays(10).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(10).format(dateTimeFormatter));
        result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_INVALID_DATE_RANGE, result);

    }

    @Test
    public void testAddAReservationMoreThanThreeDays() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.plusDays(9).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(12).format(dateTimeFormatter));
        String result = mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertEquals(EXPECTED_ERROR_MESSAGE_DATE_RANGE_TOO_LONG, result);
    }

    // Have no time to implement my own ExceptionHandler which then we could expect IllegalArgumentException here
    @Test(expected = NestedServletException.class)
    public void testAddAReservationMoreThanAMonth() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.plusDays(60).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(61).format(dateTimeFormatter));
        mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test(expected = NestedServletException.class)
    public void testAddAReservationOnToday() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(2).format(dateTimeFormatter));
        mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test(expected = NestedServletException.class)
    public void testAddAReservationWithInvalidRange() throws Exception {
        prepareData();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("userName", "Ran");
        params.set("email", "zeronetao@gmail.com");
        params.set("startFrom", currentDate.plusDays(5).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(3).format(dateTimeFormatter));
        mockMvc.perform(put("/api/reserve").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testSimulateMultipleUserReserving() throws Exception {
        List<CompletableFuture<Void>> futures = Lists.newArrayList();
        Set<String> results = new CopyOnWriteArraySet<>();

        for (int i = 0 ; i < 100; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                int randomNum = ThreadLocalRandom.current().nextInt(1, 28);

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.set("userName", "userName");
                params.set("email", "email");
                params.set("startFrom", currentDate.plusDays(randomNum).format(dateTimeFormatter));
                params.set("endTo", currentDate.plusDays(randomNum + 2).format(dateTimeFormatter));
                try {
                    return mockMvc.perform(put("/api/reserve").params(params)
                            .headers(defaultHttpHeaders)).andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString();
                } catch (Exception e) {
                   return e.getMessage();
                }
            }).thenAccept(result -> results.add(result)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        Assert.assertNotNull(results);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("startFrom", currentDate.plusDays(1).format(dateTimeFormatter));
        params.set("endTo", currentDate.plusDays(30).format(dateTimeFormatter));
        String searchResults = mockMvc.perform(get("/api/search").params(params)
                .headers(defaultHttpHeaders)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assert.assertNotNull(searchResults);

        ArrayList<DateRange> dateRanges = objectMapper.readValue(searchResults, new TypeReference<ArrayList<DateRange>>() {
        });

        Assert.assertNotNull(dateRanges);

    }

    private void prepareData() {
        Reservation reservation1 = new Reservation();
        reservation1.setUsername("test1");
        reservation1.setEmail("test1@test.com");
        reservation1.setStartFrom(Date.from(currentDate.plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reservation1.setEndTo(Date.from(currentDate.plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Reservation reservation2 = new Reservation();
        reservation2.setUsername("test2");
        reservation2.setEmail("test2@test.com");
        reservation2.setStartFrom(Date.from(currentDate.plusDays(20).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reservation2.setEndTo(Date.from(currentDate.plusDays(22).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reservationRepository.saveAll(Arrays.asList(reservation1, reservation2));
    }
}
