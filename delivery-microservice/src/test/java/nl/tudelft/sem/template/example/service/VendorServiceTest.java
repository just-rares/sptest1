package nl.tudelft.sem.template.example.service;

import nl.tudelft.sem.template.example.configuration.ConfigurationProperties;
import nl.tudelft.sem.template.example.exception.CourierNotFoundException;
import nl.tudelft.sem.template.example.exception.VendorHasNoCouriersException;
import nl.tudelft.sem.template.example.exception.VendorNotFoundException;
import nl.tudelft.sem.template.example.exception.MicroserviceCommunicationException;
import nl.tudelft.sem.template.example.external.UsersMicroservice;
import nl.tudelft.sem.template.example.repository.VendorRepository;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Vendor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class VendorServiceTest {

    private VendorRepository vendorRepository;

    private ConfigurationProperties configurationProperties;

    private UsersMicroservice usersMicroservice;

    private VendorService vendorService;
    private CourierService courierService;


    Vendor vendor;

    @BeforeEach
    void setup(){
        vendorRepository = Mockito.mock(VendorRepository.class);
        configurationProperties = new ConfigurationProperties();
        usersMicroservice = Mockito.mock(UsersMicroservice.class);
        courierService = Mockito.mock(CourierService.class);
        vendorService = new VendorService(vendorRepository, configurationProperties, usersMicroservice, courierService);

        Location address = new Location(0.0,0.0);
        vendor = new Vendor(1L, configurationProperties.getDefaultDeliveryZone(), address, new ArrayList<>());
        Vendor vendor1 = new Vendor(1L, 5L, address, null);
        List<Long> couriers = new ArrayList<>();
        couriers.add(2L);
        Vendor vendor2 = new Vendor(3L, 7L, address, couriers);
        Vendor vendor3 = new Vendor(11L, 7L, address, new ArrayList<>());
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor1));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(vendor2));
        when(vendorRepository.findById(11L)).thenReturn(Optional.of(vendor3));
        when(vendorRepository.existsById(1L)).thenReturn(true);
        when(vendorRepository.existsById(3L)).thenReturn(true);
        when(vendorRepository.existsById(2L)).thenReturn(false);
        when(vendorRepository.existsById(11L)).thenReturn(true);

    }

    @Test
    void testFindVendorOrCreateWhenVendorExists() throws MicroserviceCommunicationException {
        Long vendorId = 1L;
        when(vendorRepository.existsById(vendorId)).thenReturn(true);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.ofNullable(vendor));
        Vendor resultingVendor = vendorService.findVendorOrCreate(vendorId);

        verify(vendorRepository, never()).save(any());
        assertEquals(vendor, resultingVendor);
    }

    @Test
    void testFindVendorOrCreateWithNewVendor() throws MicroserviceCommunicationException {
        Long vendorId = 1L;

        when(vendorRepository.existsById(vendorId)).thenReturn(false);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.ofNullable(vendor));
        when(usersMicroservice.getVendorLocation(anyLong())).thenReturn(Optional.of(new Location(4.0, 5.0)));

        Vendor resultingVendor = vendorService.findVendorOrCreate(vendorId);

        verify(vendorRepository, times(1)).save(any());
        assertNotNull(resultingVendor);
    }

    @Test
    void getDeliveryZoneCorrectTest() throws VendorNotFoundException {
        Long vendorId = 1L;
        Long result = 5L;
        assertEquals(result, vendorService.getDeliveryZone(1L));
        assertDoesNotThrow(() -> vendorService.getDeliveryZone(vendorId));

    }

    @Test
    void getDeliveryZoneInvalidTest() throws VendorNotFoundException {
        Long vendorId = 2L;
        assertThrows(VendorNotFoundException.class, () -> vendorService.getDeliveryZone(vendorId));
    }

    @Test
    void updateDeliveryZoneInvalidTest() throws VendorNotFoundException {
        Long vendorId = 2L;
        Long newZone = 10L;
        assertThrows(VendorNotFoundException.class, () -> vendorService.updateDeliveryZone(vendorId, newZone));
    }

    @Test
    void updateDeliveryZoneCorrectTest() throws VendorNotFoundException, VendorHasNoCouriersException {
        Long newZone = 10L;
        Location address = new Location(0.0,0.0);
        Vendor newVendor = new Vendor(3L, 10L, address, new ArrayList<>());

        Vendor updated = vendorService.updateDeliveryZone(3L, newZone);
        assertEquals(newVendor.getDeliveryZone(), updated.getDeliveryZone());
        assertEquals(address, updated.getAddress());
    }

    @Test
    void updateDeliveryZoneNoCouriersTest() throws VendorNotFoundException, VendorHasNoCouriersException {
        Long vendorId = 1L;
        assertThrows(VendorHasNoCouriersException.class, () -> vendorService.updateDeliveryZone(vendorId, 30L));
    }

    @Test
    void updateDeliveryZoneNoCouriersTest2() throws VendorNotFoundException, VendorHasNoCouriersException {
        Long vendorId = 11L;
        assertThrows(VendorHasNoCouriersException.class, () -> vendorService.updateDeliveryZone(vendorId, 30L));
    }

    @Test
    void testFindVendorOrCreateWithNewVendorFaultyMicroserviceCommunication(){
        Long vendorId = 1L;

        when(vendorRepository.existsById(vendorId)).thenReturn(false);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.ofNullable(vendor));
        when(usersMicroservice.getVendorLocation(anyLong())).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> vendorService.findVendorOrCreate(vendorId))
                .isInstanceOf(MicroserviceCommunicationException.class);

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void assignCourierTest() throws VendorNotFoundException, CourierNotFoundException {
        Long vendorId = 33L;
        Vendor vendor = new Vendor();
        vendor.setCouriers(new ArrayList<>());
        vendor.setId(33L);

        when(vendorRepository.existsById(33L)).thenReturn(true);
        when(vendorRepository.findById(33L)).thenReturn(Optional.of(vendor));
        when(usersMicroservice.getUserType(2L)).thenReturn(Optional.of("courier"));
        when(usersMicroservice.getUserType(6L)).thenReturn(Optional.of("courier"));
        Vendor updated = vendorService.assignCourierToVendor(33L, 6L);
        List<Long> couriers = new ArrayList<>();
        couriers.add(6L);
        assertEquals(couriers, updated.getCouriers());
        assertDoesNotThrow(() -> vendorService.getAssignedCouriers(vendorId));
        verify(vendorRepository, times(1)).save(any());
        couriers.add(2L);
        updated = vendorService.assignCourierToVendor(33L, 2L);
        assertEquals(couriers, updated.getCouriers());



    }

    @Test
    void assignCouriersInvalidTest() throws VendorNotFoundException {
        assertThrows(VendorNotFoundException.class, () -> vendorService.assignCourierToVendor(2L, 5L));
    }
    @Test
    void assignCouriersCourierNotFoundTest() throws VendorNotFoundException {
        when(usersMicroservice.getUserType(55L)).thenReturn(Optional.of("vendor"));
        assertThrows(CourierNotFoundException.class, () -> vendorService.assignCourierToVendor(3L, 55L));
    }

    @Test
    void getCouriersTest() throws VendorNotFoundException {
        Long vendorId = 3L;
        List<Long> updated = vendorService.getAssignedCouriers(3L);
        List<Long> couriers = new ArrayList<>();
        couriers.add(2L);
        assertEquals(couriers, updated);
        assertDoesNotThrow(() -> vendorService.getAssignedCouriers(vendorId));
    }

    @Test
    void getCouriersInvalidTest() throws VendorNotFoundException {
        assertThrows(VendorNotFoundException.class, () -> vendorService.getAssignedCouriers(2L));
    }

    @Test
    void testFindVendorAddressWorks() throws VendorNotFoundException, MicroserviceCommunicationException {
        when(vendorRepository.findById(any())).thenReturn(Optional.of(vendor));
        Location address = vendorService.getVendorLocation(1);
        assertEquals(new Location(0.0,0.0), address);
    }

    @Test
    void testFindVendorAddressVendorIsNotFound() {
        when(vendorRepository.findById(any())).thenReturn(Optional.empty());
        Assertions.assertThatThrownBy(() -> vendorService.getVendorLocation(6))
                .isInstanceOf(VendorNotFoundException.class);
    }

    @Test
    void testFindVendorAddressLocationIsEmpty() {
        Vendor vendorNoAddress = new Vendor(1L, 5L, null, null);
        when(vendorRepository.findById(any())).thenReturn(Optional.of(vendorNoAddress));
        Assertions.assertThatThrownBy(() -> vendorService.getVendorLocation(6))
                .isInstanceOf(MicroserviceCommunicationException.class);
    }
}
