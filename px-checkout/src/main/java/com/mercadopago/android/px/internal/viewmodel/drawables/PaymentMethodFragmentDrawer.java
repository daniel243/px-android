package com.mercadopago.android.px.internal.viewmodel.drawables;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

public interface PaymentMethodFragmentDrawer {

    Fragment draw(@NonNull final DrawableFragmentItem drawableFragmentItem);

    Fragment draw(@NonNull final OtherPaymentMethodFragmentItem drawableItem);

    Fragment draw(@NonNull final SavedCardDrawableFragmentItem drawableItem);

    Fragment draw(@NonNull final AccountMoneyDrawableFragmentItem drawableItem);

    Fragment draw(@NonNull final ConsumerCreditsDrawableFragmentItem drawableItem);
}
