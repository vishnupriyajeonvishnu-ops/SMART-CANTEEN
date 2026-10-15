package com.dmi.canteen.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ManualUpiPaymentGateway implements PaymentGateway {

    @Override
    public String getGatewayName() {
        return "Manual UPI / Cash Staff Verification Gateway";
    }

    @Override
    public boolean supportsMethod(PaymentMethod method) {
        return method == PaymentMethod.GPAY_UPI || method == PaymentMethod.CASH;
    }

    @Override
    public PaymentInitiationResult initiatePayment(String orderNumber, BigDecimal amount, String payeeUpiId, String payeeName) {
        String encodedPayee = URLEncoder.encode(payeeName, StandardCharsets.UTF_8);
        String upiUri = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=Order-%s",
                payeeUpiId, encodedPayee, amount.toPlainString(), orderNumber);

        return new PaymentInitiationResult(upiUri, upiUri, upiUri);
    }

    @Override
    public boolean verifyPayment(String utrReference, BigDecimal expectedAmount) {
        // Staff verifies in their UPI App / Bank SMS and clicks Verify in the admin queue
        return utrReference != null && utrReference.trim().length() >= 6;
    }
}
