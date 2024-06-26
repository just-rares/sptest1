package nl.tudelft.sem.template.example.service;

import static nl.tudelft.sem.template.model.Order.StatusEnum;

import java.util.Optional;
import nl.tudelft.sem.template.example.exception.IllegalOrderStatusException;
import nl.tudelft.sem.template.example.exception.MicroserviceCommunicationException;
import nl.tudelft.sem.template.example.exception.OrderNotFoundException;
import nl.tudelft.sem.template.example.external.OrdersMicroservice;
import nl.tudelft.sem.template.example.repository.OrderRepository;
import nl.tudelft.sem.template.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    OrderRepository orderRepository;

    OrdersMicroservice ordersMicroservice;

    /**
     * Simple constructor handling dependency injection.
     *
     * @param ordersMicroservice microservice responsible for handling orders
     * @param orderRepository JPA repository holding the orders
     */
    @Autowired
    public OrderService(OrderRepository orderRepository, OrdersMicroservice ordersMicroservice) {
        this.orderRepository = orderRepository;
        this.ordersMicroservice = ordersMicroservice;
    }

    /**
     * Returns status of a given error.
     *
     * @param orderId Unique identifier of the order (required)
     * @return StatusEnum containing the status of the order
     * @throws OrderNotFoundException if the order was not found
     */
    public Order.StatusEnum getOrderStatus(Integer orderId) throws OrderNotFoundException {
        Optional<Order> orderOptional = orderRepository.findById(Long.valueOf(orderId));
        if (orderOptional.isEmpty()) {
            throw new OrderNotFoundException("Order id not found");
        }
        return orderOptional.get().getStatus();
    }

    /**
     * Checks whether the new status follows a desired flow, respecting
     * certain rules, and if so changes and updates the order in the repository.
     * If not, it throws an exception.
     *
     * @param orderId Unique identifier of the order (required)
     * @param authorizationId Unique identifier of the user making the request.
     * @param orderStatusString String format of the new status
     * @throws IllegalOrderStatusException if status doesn't respect the flow
     *     or status string is not available
     * @throws OrderNotFoundException if order was not found
     */
    public void setOrderStatus(Integer orderId, Integer authorizationId, String orderStatusString)
            throws IllegalOrderStatusException, OrderNotFoundException, MicroserviceCommunicationException {
        Optional<Order> orderOptional = orderRepository.findById(Long.valueOf(orderId));
        if (orderOptional.isEmpty()) {
            throw new OrderNotFoundException("Order id not found");
        }
        if (!ordersMicroservice.putOrderStatus((long) orderId, (long) authorizationId, orderStatusString)) {
            throw new MicroserviceCommunicationException("Order status could not be updated");
        }
        Order order = orderOptional.get();
        StatusEnum newStatus = StatusEnum.fromValue(orderStatusString);
        assertStatusFlowIsCorrect(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    /**
     * Checks, given the current status of the order, if the order can
     * or can't reach the second status, following a logical set of rules.
     * A status can go from:
     * Pending -> Accepted
     * Pending -> Rejected
     * Accepted -> Preparing -> Given to courier -> On transit -> Delivered
     *
     * @param oldStatus Current status of an order
     * @param newStatus New status of an order
     * @throws IllegalOrderStatusException if status doesn't respect the flow
     */
    private void assertStatusFlowIsCorrect(StatusEnum oldStatus, StatusEnum newStatus)
            throws IllegalOrderStatusException {
        switch (oldStatus) {
            case PENDING -> {
                if (newStatus != StatusEnum.ACCEPTED && newStatus != StatusEnum.REJECTED) {
                    throw new IllegalOrderStatusException("Error! Order status cant go from PENDING to "
                            + newStatus.toString().toUpperCase() + ".");
                }
            }
            case REJECTED -> {
                throw new IllegalOrderStatusException("Error! Order status can't change after being REJECTED");
            }
            case ACCEPTED -> {
                if (newStatus != StatusEnum.PREPARING) {
                    throw new IllegalOrderStatusException("Error! Order status can't change from ACCEPTED to "
                            + newStatus.toString().toUpperCase() + ".");
                }
            }
            case PREPARING -> {
                if (newStatus != StatusEnum.GIVEN_TO_COURIER) {
                    throw new IllegalOrderStatusException("Error! Order status can't change from PREPARING to "
                            + newStatus.toString().toUpperCase() + ".");
                }
            }
            case GIVEN_TO_COURIER -> {
                if (newStatus != StatusEnum.ON_TRANSIT) {
                    throw new IllegalOrderStatusException("Error! Order status can't change from GIVEN_TO_COURIER to "
                            + newStatus.toString().toUpperCase() + ".");
                }
            }
            case ON_TRANSIT -> {
                if (newStatus != StatusEnum.DELIVERED) {
                    throw new IllegalOrderStatusException("Error! Order status can't change from ON_TRANSIT to "
                            + newStatus.toString().toUpperCase() + ".");
                }
            }
            case DELIVERED -> {
                throw new IllegalOrderStatusException("Error! Order status can't change from DELIVERED to "
                        + newStatus.toString().toUpperCase() + ".");
            }
            default -> { }
        }
    }
}
