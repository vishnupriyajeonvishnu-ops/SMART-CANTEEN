package com.dmi.canteen.billing;

import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.order.Order;
import com.dmi.canteen.order.OrderItem;
import com.dmi.canteen.order.OrderStatus;
import com.dmi.canteen.payment.Payment;
import com.dmi.canteen.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private final BillRepository billRepository;

    public BillingService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @Transactional
    public Bill generateBillForOrder(Order order, Payment payment) {
        // Idempotency check: bill generated once per order
        Optional<Bill> existing = billRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Must be paid
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS && payment.getPaymentStatus() != PaymentStatus.VERIFIED) {
            throw new BusinessException("Cannot generate bill for unpaid order. Current payment status: " + payment.getPaymentStatus());
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long countToday = billRepository.countBillsForDay(startOfDay, endOfDay) + 1;

        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String billNumber = String.format("DMI-%s-%04d", dateStr, countToday);

        BigDecimal billAmount = order.getTotalAmount();
        BigDecimal taxAmount = BigDecimal.ZERO; // Tax exempt / included in college canteen pricing
        BigDecimal grandTotal = billAmount;

        Bill bill = new Bill(billNumber, order, order.getUser(), payment, billAmount, taxAmount, grandTotal);
        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public List<Bill> getBillsForUser(Long userId) {
        return billRepository.findByUserIdOrderByBillDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        return billRepository.findAllByOrderByBillDateDesc();
    }

    @Transactional(readOnly = true)
    public Bill getBillById(Long billId, Long userId, boolean isStaffOrManager) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + billId));

        if (!isStaffOrManager && !bill.getUser().getId().equals(userId)) {
            throw new BusinessException("You do not have permission to view this bill.");
        }
        return bill;
    }

    @Transactional(readOnly = true)
    public Optional<Bill> getBillByOrderId(Long orderId) {
        return billRepository.findByOrderId(orderId);
    }

    /**
     * Generates an 80mm thermal receipt formatted HTML document for direct browser printing.
     */
    @Transactional(readOnly = true)
    public String generatePrintableBillHtml(Long billId, Long userId, boolean isStaffOrManager) {
        Bill bill = getBillById(billId, userId, isStaffOrManager);
        Order order = bill.getOrder();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Receipt - ").append(bill.getBillNumber()).append("</title>");
        sb.append("<style>");
        sb.append("@page { size: 80mm auto; margin: 0; }");
        sb.append("body { font-family: 'Courier New', monospace; font-size: 12px; width: 72mm; margin: 4mm auto; color: #000; }");
        sb.append(".center { text-align: center; }");
        sb.append(".bold { font-weight: bold; }");
        sb.append(".divider { border-top: 1px dashed #000; margin: 6px 0; }");
        sb.append(".double-divider { border-top: 2px dashed #000; margin: 6px 0; }");
        sb.append(".flex-row { display: flex; justify-content: space-between; margin-bottom: 3px; }");
        sb.append(".item-name { width: 50%; } .item-qty { width: 15%; text-align: center; } .item-price { width: 35%; text-align: right; }");
        sb.append("@media print { .no-print { display: none; } }");
        sb.append("</style></head><body>");

        sb.append("<div class='no-print' style='text-align:center; margin-bottom: 15px;'>");
        sb.append("<button onclick='window.print()' style='padding: 6px 16px; background: #2563eb; color: #fff; border:none; border-radius: 4px; cursor:pointer;'>Print 80mm Receipt</button>");
        sb.append("</div>");

        sb.append("<div class='center'>");
        sb.append("<div class='bold' style='font-size: 15px;'>DMI COLLEGE OF ENGINEERING</div>");
        sb.append("<div>Smart Canteen Management System</div>");
        sb.append("<div>Palanchur, Chennai - 600123</div>");
        sb.append("<div>FSSAI Lic No: 12421008000456</div>");
        sb.append("</div>");

        sb.append("<div class='divider'></div>");
        sb.append("<div class='flex-row'><span>Bill No:</span><span class='bold'>").append(bill.getBillNumber()).append("</span></div>");
        sb.append("<div class='flex-row'><span>Order No:</span><span>").append(order.getOrderNumber()).append("</span></div>");
        sb.append("<div class='flex-row'><span>Date:</span><span>").append(bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))).append("</span></div>");
        sb.append("<div class='flex-row'><span>Customer:</span><span>").append(bill.getUser().getFullName()).append("</span></div>");
        sb.append("<div class='flex-row'><span>Type:</span><span class='bold'>").append(order.getOrderType()).append("</span></div>");
        sb.append("<div class='flex-row'><span>Payment:</span><span>").append(bill.getPayment().getPaymentMethod()).append(" (").append(bill.getPayment().getPaymentStatus()).append(")</span></div>");
        if (bill.getPayment().getUtrReference() != null) {
            sb.append("<div class='flex-row'><span>UTR Ref:</span><span>").append(bill.getPayment().getUtrReference()).append("</span></div>");
        }

        sb.append("<div class='divider'></div>");
        sb.append("<div class='flex-row bold'><span class='item-name'>Item</span><span class='item-qty'>Qty</span><span class='item-price'>Amount</span></div>");
        sb.append("<div class='divider'></div>");

        for (OrderItem item : order.getItems()) {
            sb.append("<div class='flex-row'>");
            sb.append("<span class='item-name'>").append(item.getItemName()).append("</span>");
            sb.append("<span class='item-qty'>").append(item.getQuantity()).append("</span>");
            sb.append("<span class='item-price'>₹").append(item.getSubtotal().setScale(2)).append("</span>");
            sb.append("</div>");
        }

        sb.append("<div class='double-divider'></div>");
        sb.append("<div class='flex-row bold' style='font-size: 14px;'><span>TOTAL AMOUNT:</span><span>₹").append(bill.getGrandTotal().setScale(2)).append("</span></div>");
        sb.append("<div class='divider'></div>");

        sb.append("<div class='center' style='margin-top: 10px;'>");
        sb.append("<div>Thank you for dining at DMI Canteen!</div>");
        sb.append("<div>Please collect your food at designated counters.</div>");
        sb.append("<div style='margin-top: 5px; font-size: 10px;'>*** Powered by Antigravity Smart Canteen ***</div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }
}
