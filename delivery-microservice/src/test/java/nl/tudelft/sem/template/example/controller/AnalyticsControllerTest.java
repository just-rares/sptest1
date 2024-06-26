package nl.tudelft.sem.template.example.controller;

import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.exception.*;
import nl.tudelft.sem.template.example.service.AnalyticsService;
import nl.tudelft.sem.template.model.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AnalyticsControllerTest {

    private AnalyticsService analyticsService;
    private AnalyticsController analyticsController;
    private AuthorizationService authorizationService;
    private Integer courierId;
    private Integer authorizationId;
    private Rating rating;
    private Integer orderId;
    private Integer vendorId;

    @BeforeEach
    void setUp() {
        analyticsService = Mockito.mock(AnalyticsService.class);
        authorizationService = Mockito.mock(AuthorizationService.class);
        analyticsController = new AnalyticsController(analyticsService, authorizationService);
        courierId = 1;
        authorizationId = 1;
        vendorId = 3;
        orderId = 10;
        rating = new Rating();
    }

    @Test
    void testSetRatingSuccess() throws Exception {
        rating.setGrade(5);
        rating.setComment("Excellent");

        when(authorizationService.canChangeOrderRating(1L, (long) 10)).thenReturn(true);
        when(analyticsService.saveRating(rating, (long) 10)).thenReturn(rating);

        ResponseEntity<Void> response = analyticsController.analyticsOrderOrderIdRatingPut(10, 1, rating);

        verify(authorizationService).canChangeOrderRating(1L, (long) 10);
        verify(analyticsService).saveRating(rating, (long) 10);
        assertEquals(200, response.getStatusCodeValue());
    }


    @Test
    void testSetRatingOrderNotFound() throws Exception {
        when(authorizationService.canChangeOrderRating(1L, (long) 10)).thenReturn(true);
        doThrow(new OrderNotFoundException("Order not found")).when(analyticsService).saveRating(rating, (long) 10);

        ResponseEntity<Void> response = analyticsController.analyticsOrderOrderIdRatingPut(10, 1, rating);

        verify(authorizationService).canChangeOrderRating(1L, (long) 10);
        verify(analyticsService).saveRating(rating, (long) 10);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    public void testRatingPutIllegalOrderStatusException() throws MicroserviceCommunicationException, OrderNotFoundException, IllegalOrderStatusException {
        when(authorizationService.canChangeOrderRating((long) authorizationId, (long) orderId)).thenReturn(true);
        doThrow(new IllegalOrderStatusException("Illegal order status")).when(analyticsService).saveRating(rating, (long) orderId);

        ResponseEntity<Void> response = analyticsController.analyticsOrderOrderIdRatingPut(orderId, authorizationId, rating);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testSetRatingUnauthorized() throws Exception {
        when(authorizationService.canChangeOrderRating((long) 1, (long) 10)).thenReturn(false);
        ResponseEntity<Void> response = analyticsController.analyticsOrderOrderIdRatingPut(10, 1, rating);

        verify(authorizationService).canChangeOrderRating((long) 1, (long) 10);
        verifyNoInteractions(analyticsService);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testSetRatingInternalServerError() throws Exception {
        when(authorizationService.canChangeOrderRating((long) 10, (long) 1)).thenThrow(MicroserviceCommunicationException.class);

        ResponseEntity<Void> response = analyticsController.analyticsOrderOrderIdRatingPut(1, 10, rating);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
    @Test
    void testGetRatingSuccess() throws RatingNotFoundException, OrderNotFoundException {
        rating.setGrade(4);
        rating.setComment("Good");
        when(analyticsService.getRatingByOrderId(1L)).thenReturn(rating);
        ResponseEntity<Rating> response = analyticsController.analyticsOrderOrderIdRatingGet(1, 1);

        verify(analyticsService).getRatingByOrderId(1L);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(rating, response.getBody());
    }

    @Test
    void testGetRatingRatingNotFound() throws RatingNotFoundException, OrderNotFoundException {
        Integer orderId = 5;
        Integer authorizationId = 1;

        when(analyticsService.getRatingByOrderId((long) orderId)).thenReturn(null);
        ResponseEntity<Rating> response = analyticsController.analyticsOrderOrderIdRatingGet(orderId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    @Test
    void testGetRatingOrderNotFound() throws RatingNotFoundException, OrderNotFoundException  {
        when(analyticsService.getRatingByOrderId((long) 1)).thenThrow(new OrderNotFoundException("Order not found"));

        ResponseEntity<Rating> response = analyticsController.analyticsOrderOrderIdRatingGet(1, 1);
        verify(analyticsService).getRatingByOrderId((long) 1);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetDeliveriesPerDaySuccess() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getDeliveriesPerDay((long) courierId)).thenReturn(5);

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdDeliveriesPerDayGet(courierId, authorizationId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody());
    }

    @Test
    void testGetDeliveriesPerDayUnauthorized() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(false);

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdDeliveriesPerDayGet(courierId, authorizationId);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetDeliveriesPerDayCourierNotFound() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getDeliveriesPerDay((long) courierId)).thenThrow(new CourierNotFoundException("Courier not found"));

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdDeliveriesPerDayGet(courierId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testDeliveriesPerDayMiscommunication() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics(anyLong(), anyLong())).thenThrow(MicroserviceCommunicationException.class);
        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdDeliveriesPerDayGet(anyInt(), anyInt());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetSuccessfulDeliveriesSuccess() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getSuccessfulDeliveries((long) courierId)).thenReturn(10);

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdSuccessfulDeliveriesGet(courierId, authorizationId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10, response.getBody());
    }

    @Test
    public void testSuccessfulDeliveriesCourierNotFoundException() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(false);
        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdSuccessfulDeliveriesGet(courierId, authorizationId);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testAnalyticsCourierCourierIdSuccessfulDeliveriesGetCourierNotFound() throws MicroserviceCommunicationException, CourierNotFoundException {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getSuccessfulDeliveries((long) courierId)).thenThrow(new CourierNotFoundException("Courier not found"));

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdSuccessfulDeliveriesGet(courierId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testSuccessfulDeliveriesMiscommunication() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics(anyLong(), anyLong())).thenThrow(MicroserviceCommunicationException.class);
        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdSuccessfulDeliveriesGet(anyInt(), anyInt());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetCourierIssuesSuccess() throws Exception {
        List<String> mockIssues = Arrays.asList("Issue 1", "Issue 2");
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getCourierIssues((long) courierId)).thenReturn(mockIssues);

        ResponseEntity<List<String>> response = analyticsController.analyticsCourierCourierIdCourierIssuesGet(courierId, authorizationId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockIssues, response.getBody());
    }

    @Test
    public void testAnalyticsCourierCourierIdCourierIssuesGetCourierNotFound() throws MicroserviceCommunicationException, CourierNotFoundException {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getCourierIssues((long) courierId)).thenThrow(new CourierNotFoundException("Courier not found"));

        ResponseEntity<List<String>> response = analyticsController.analyticsCourierCourierIdCourierIssuesGet(courierId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testAnalyticsCourierCourierIdCourierIssuesGetForbidden() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(false);

        ResponseEntity<List<String>> response = analyticsController.analyticsCourierCourierIdCourierIssuesGet(courierId, authorizationId);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testCourierIssuesMiscommunication() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics(anyLong(), anyLong())).thenThrow(MicroserviceCommunicationException.class);
        ResponseEntity<List<String>> response = analyticsController.analyticsCourierCourierIdCourierIssuesGet(anyInt(), anyInt());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetCourierEfficiencySuccess() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getCourierEfficiency((long) courierId)).thenReturn(80);

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdEfficiencyGet(courierId, authorizationId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(80, response.getBody());
    }

    @Test
    void testGetCourierEfficiencyUnauthorized() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(false);

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdEfficiencyGet(courierId, authorizationId);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetCourierEfficiencyCourierNotFound() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) courierId)).thenReturn(true);
        when(analyticsService.getCourierEfficiency((long) courierId)).thenThrow(new CourierNotFoundException("Courier not found"));

        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdEfficiencyGet(courierId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetCourierEfficiencyMiscommunication() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics(anyLong(), anyLong())).thenThrow(MicroserviceCommunicationException.class);
        ResponseEntity<Integer> response = analyticsController.analyticsCourierCourierIdEfficiencyGet(anyInt(), anyInt());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetVendorAverageSuccess() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) vendorId)).thenReturn(true);
        when(analyticsService.getVendorAverage((long) vendorId)).thenReturn(45);

        ResponseEntity<Integer> response = analyticsController.analyticsVendorVendorIdVendorAverageGet(vendorId, authorizationId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(45, response.getBody());
    }

    @Test
    void testGetVendorAverageUnauthorized() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) vendorId)).thenReturn(false);

        ResponseEntity<Integer> response = analyticsController.analyticsVendorVendorIdVendorAverageGet(vendorId, authorizationId);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetVendorAverageCourierNotFound() throws Exception {
        when(authorizationService.canViewCourierAnalytics((long) authorizationId, (long) vendorId)).thenReturn(true);
        when(analyticsService.getVendorAverage((long) vendorId)).thenThrow(new VendorNotFoundException("Vendor not found"));

        ResponseEntity<Integer> response = analyticsController.analyticsVendorVendorIdVendorAverageGet(vendorId, authorizationId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetVendorAverageMiscommunication() throws MicroserviceCommunicationException {
        when(authorizationService.canViewCourierAnalytics(anyLong(), anyLong())).thenThrow(MicroserviceCommunicationException.class);
        ResponseEntity<Integer> response = analyticsController.analyticsVendorVendorIdVendorAverageGet(anyInt(), anyInt());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
