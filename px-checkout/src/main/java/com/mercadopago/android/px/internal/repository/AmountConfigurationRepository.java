package com.mercadopago.android.px.internal.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.mercadopago.android.px.model.AmountConfiguration;
import javax.annotation.Nonnull;

public interface AmountConfigurationRepository {

    /**
     * Obtains the payer costs configuration that applies in a particular moment of the flow
     * <p>
     * In the future, with a discount selector feature, the selected discount will define the associated payer cost.
     *
     * @return The current dominant configuration.
     */
    @NonNull
    AmountConfiguration getCurrentConfiguration() throws IllegalStateException;

    /**
     * Obtains the complete payer cost configuration for a specif custom option.
     *
     * @param customOptionId The {@link com.mercadopago.android.px.model.CustomSearchItem} ID.
     * @return The payer cost configuration, returns null if don't have a configuration or ID is invalid.
     */
    @Nullable
    AmountConfiguration getConfigurationFor(@Nonnull final String customOptionId);
}