package com.mercadopago.android.px.internal.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import com.mercadopago.android.px.internal.callbacks.PaymentServiceHandler;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.PaymentData;
import com.mercadopago.android.px.model.PaymentRecovery;
import com.mercadopago.android.px.model.PaymentResult;
import com.mercadopago.android.px.model.internal.PaymentConfiguration;
import java.util.List;

public interface PaymentRepository {

    void startPayment();

    void startExpressPayment(@NonNull final PaymentConfiguration paymentConfiguration);

    void startExpressPaymentWithOffMethod(@NonNull final String paymentMethodId, @NonNull final String paymentTypeId);

    boolean isExplodingAnimationCompatible();

    @NonNull
    @Size(min = 1)
    List<PaymentData> getPaymentDataList();

    @NonNull
    PaymentResult createPaymentResult(@NonNull final IPaymentDescriptor genericPayment);

    int getPaymentTimeout();

    void attach(@NonNull final PaymentServiceHandler handler);

    void detach(@NonNull final PaymentServiceHandler handler);

    void storePayment(@NonNull final IPaymentDescriptor iPayment);

    @Nullable
    IPaymentDescriptor getPayment();

    boolean hasPayment();

    boolean hasRecoverablePayment();

    @NonNull
    PaymentRecovery createRecoveryForInvalidESC();

    @NonNull
    PaymentRecovery createPaymentRecovery();
}
