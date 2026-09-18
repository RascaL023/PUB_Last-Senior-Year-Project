package id.my.rascal.dining.internal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.GuestOrderRequest;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.model.response.GuestOrderSummary;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;

@Service
public class GuestDiningService {

    private final DiningRepository diningRepository;
    private final DiningService diningService;
    private final OrderApi orderApi;
    private final InvoiceApi invoiceApi;
    private final CustomerApi customerApi;

    public GuestDiningService(
        DiningRepository diningRepository,
        DiningService diningService,
        OrderApi orderApi,
        InvoiceApi invoiceApi,
        CustomerApi customerApi
    ) {
        this.diningRepository = diningRepository;
        this.diningService = diningService;
        this.orderApi = orderApi;
        this.invoiceApi = invoiceApi;
        this.customerApi = customerApi;
    }

    @Transactional(readOnly = true)
    public GuestDiningResponse getByToken(String token) {
        Dining dining = findDiningByToken(token);
        return buildGuestResponse(dining);
    }

    @Transactional(readOnly = true)
    public GuestDiningResponse getByCode(String code) {
        Dining dining = diningRepository.findByGuestCodeAndStatus(code, DiningStatus.OPEN)
            .orElseThrow(() -> new NotFoundException("Session not found"));
        return buildGuestResponse(dining);
    }

    @Transactional
    public GuestDiningResponse addOrder(String token, GuestOrderRequest request) {
        Dining dining = requireOpenSession(token);
        diningService.addOrder(dining.getId(), toInternalRequest(request, null, null));
        return buildGuestResponse(dining);
    }

    @Transactional
    public GuestDiningResponse addOrderAsMember(String token, Long userAuthId, GuestOrderRequest request) {
        Dining dining = requireOpenSession(token);
        CustomerApiResponse member = customerApi.getByUserAuthId(userAuthId)
            .orElseThrow(() -> new ForbiddenException("Customer profile not found for this account"));
        diningService.addOrder(dining.getId(), toInternalRequest(request, member.id(), member.name()));
        return buildGuestResponse(dining);
    }

    private Dining requireOpenSession(String token) {
        Dining dining = findDiningByToken(token);
        if (dining.getStatus() != DiningStatus.OPEN)
            throw new BadRequestException("Cannot add order to a closed dining");
        return dining;
    }

    private Dining findDiningByToken(String token) {
        return diningRepository.findByGuestToken(token)
            .orElseThrow(() -> new NotFoundException("Session not found"));
    }

    private CreateDiningOrderRequest toInternalRequest(GuestOrderRequest request, Long customerId, String profileName) {
        String customerName = StringUtil.safeIsBlank(request.customerName())
            ? profileName
            : StringUtil.normalizeSpaces(request.customerName());
        return new CreateDiningOrderRequest(customerId, customerName, request.notes(), request.items());
    }

    private GuestDiningResponse buildGuestResponse(Dining dining) {
        List<OrderApiResponse> orders = orderApi.getOrders(diningService.getOrderIds(dining.getId()));

        List<GuestOrderSummary> summaries = orders.stream()
            .map(o -> new GuestOrderSummary(o.status(), o.totalPrice(), o.createdAt()))
            .toList();
        int totalPrice = orders.stream()
            .filter(o -> !"CANCELLED".equals(o.status()))
            .mapToInt(OrderApiResponse::totalPrice)
            .sum();

        InvoiceApiResponse invoice = invoiceApi.getDiningInvoice(dining.getId());

        return new GuestDiningResponse(
            diningService.getTableNumber(dining.getTableId()),
            dining.getStatus().name(),
            totalPrice,
            invoice != null ? invoice.status() : null,
            summaries
        );
    }

}
