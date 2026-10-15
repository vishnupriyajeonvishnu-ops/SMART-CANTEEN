package com.dmi.canteen.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    String getGatewayName();
    boolean supportsMethod(PaymentMethod method);
    PaymentInitiationResult initiatePayment(String orderNumber, BigDecimal amount, String payeeUpiId, String payeeName);
    boolean verifyPayment(String utrReference, BigDecimal expectedAmount);

    class PaymentInitiationResult {
        private String paymentUrl;
        private String upiString;
        private String qrData;

        public PaymentInitiationResult(String paymentUrl, String upiString, String qrData) {
            this.paymentUrl = paymentUrl;
            this.upiString = upiString;
            this.qrData = qrData;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public String getUpiString() {
            return upiString;
        }

        public String getQrData() {
            return qrData;
        }
    }
}
