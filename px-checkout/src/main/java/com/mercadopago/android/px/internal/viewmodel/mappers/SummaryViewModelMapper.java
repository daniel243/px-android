package com.mercadopago.android.px.internal.viewmodel.mappers;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ChargeRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.view.AmountDescriptorView;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.view.SummaryDetailDescriptorFactory;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.viewmodel.AmountLocalized;
import com.mercadopago.android.px.internal.viewmodel.SummaryViewDefaultColor;
import com.mercadopago.android.px.internal.viewmodel.TotalLocalized;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule;
import com.mercadopago.android.px.model.internal.ExpressPaymentMethod;
import com.mercadopago.android.px.model.internal.SummaryInfo;
import java.util.List;

public class SummaryViewModelMapper extends CacheableMapper<ExpressPaymentMethod, SummaryView.Model,
    Pair<DiscountConfigurationModel, PaymentTypeChargeRule>> {

    @NonNull private final Currency currency;
    @NonNull private final DiscountRepository discountRepository;
    @NonNull private final AmountRepository amountRepository;
    @NonNull private final ElementDescriptorView.Model elementDescriptorModel;
    @NonNull private final AmountDescriptorView.OnClickListener listener;
    @NonNull private final SummaryInfo summaryInfo;
    @NonNull private final ChargeRepository chargeRepository;

    public SummaryViewModelMapper(@NonNull final Currency currency,
        @NonNull final DiscountRepository discountRepository, @NonNull final AmountRepository amountRepository,
        @NonNull final ElementDescriptorView.Model elementDescriptorModel,
        @NonNull final AmountDescriptorView.OnClickListener listener,
        @NonNull final SummaryInfo summaryInfo, @NonNull final ChargeRepository chargeRepository) {
        this.currency = currency;
        this.discountRepository = discountRepository;
        this.amountRepository = amountRepository;
        this.elementDescriptorModel = elementDescriptorModel;
        this.listener = listener;
        this.summaryInfo = summaryInfo;
        this.chargeRepository = chargeRepository;
    }

    @Override
    protected Pair<DiscountConfigurationModel, PaymentTypeChargeRule> getKey(
        @NonNull final ExpressPaymentMethod expressPaymentMethod) {
        return new Pair<>(discountRepository.getConfigurationFor(expressPaymentMethod.getCustomOptionId()),
            chargeRepository.getChargeRule(expressPaymentMethod.getPaymentTypeId()));
    }

    @Override
    public SummaryView.Model map(@NonNull final ExpressPaymentMethod expressPaymentMethod) {
        return createModel(expressPaymentMethod.getPaymentTypeId(),
            discountRepository.getConfigurationFor(expressPaymentMethod.getCustomOptionId()));
    }

    @NonNull
    private SummaryView.Model createModel(@NonNull final String paymentTypeId,
        @NonNull final DiscountConfigurationModel discountModel) {
        final PaymentTypeChargeRule chargeRule = chargeRepository.getChargeRule(paymentTypeId);
        final List<AmountDescriptorView.Model> summaryDetailList =
            new SummaryDetailDescriptorFactory(listener, discountModel, amountRepository, summaryInfo, currency,
                chargeRule).create();

        final AmountDescriptorView.Model totalRow = new AmountDescriptorView.Model(
            new TotalLocalized(),
            new AmountLocalized(amountRepository.getAmountToPay(paymentTypeId, discountModel), currency),
            new SummaryViewDefaultColor());

        return new SummaryView.Model(elementDescriptorModel, summaryDetailList, totalRow);
    }
}