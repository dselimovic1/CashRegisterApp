package ba.unsa.etf.si.notifications.topics;

import ba.unsa.etf.si.interfaces.PaymentObserver;
import ba.unsa.etf.si.notifications.models.PaymentNotification;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class PaymentNotificationTopic implements Topic {

    private PaymentObserver paymentObserver;

    private final Consumer<Object> action = payload -> {
        String payloadString = (String) payload;
        PaymentNotification paymentNotification = new PaymentNotification(new JSONObject(payloadString));
        paymentObserver.onPaymentProcessed(paymentNotification);
    };

    @Override
    public Type getType() {
        return String.class;
    }

    @Override
    public String getTopic() {
        return "/topic/receipt_status_update";
    }

    @Override
    public Consumer<Object> getAction() {
        return action;
    }

    public void setPaymentObserver(PaymentObserver paymentObserver) {
        this.paymentObserver = paymentObserver;
    }
}
