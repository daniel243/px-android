package com.mercadopago.android.px.internal.features.payment_result;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import com.mercadolibre.android.ui.widgets.MeliSnackbar;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.addons.BehaviourProvider;
import com.mercadopago.android.px.internal.base.PXActivity;
import com.mercadopago.android.px.internal.di.Session;
import com.mercadopago.android.px.internal.features.pay_button.PayButton;
import com.mercadopago.android.px.internal.features.pay_button.PayButtonFragment;
import com.mercadopago.android.px.internal.features.payment_result.components.PaymentResultLegacyRenderer;
import com.mercadopago.android.px.internal.features.payment_result.remedies.RemediesFragment;
import com.mercadopago.android.px.internal.features.payment_result.remedies.RemediesModel;
import com.mercadopago.android.px.internal.features.payment_result.view.PaymentResultFooter;
import com.mercadopago.android.px.internal.features.payment_result.viewmodel.PaymentResultViewModel;
import com.mercadopago.android.px.internal.util.ErrorUtil;
import com.mercadopago.android.px.internal.util.Logger;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.view.BusinessActions;
import com.mercadopago.android.px.internal.view.PaymentResultBody;
import com.mercadopago.android.px.internal.view.PaymentResultHeader;
import com.mercadopago.android.px.internal.viewmodel.ChangePaymentMethodPostPaymentAction;
import com.mercadopago.android.px.internal.viewmodel.PaymentModel;
import com.mercadopago.android.px.internal.viewmodel.RecoverPaymentPostPaymentAction;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.internal.PaymentConfiguration;
import org.jetbrains.annotations.NotNull;

import static com.mercadopago.android.px.internal.features.Constants.RESULT_ACTION;
import static com.mercadopago.android.px.internal.features.Constants.RESULT_CUSTOM_EXIT;
import static com.mercadopago.android.px.internal.util.MercadoPagoUtil.getSafeIntent;

public class PaymentResultActivity extends PXActivity<PaymentResultPresenter> implements
    PaymentResultContract.View, PayButton.Handler, RemediesFragment.Listener {

    private static final String TAG = PaymentResultActivity.class.getSimpleName();
    private static final String EXTRA_PAYMENT_MODEL = "extra_payment_model";
    private static final String EXTRA_PAYMENT_CONFIGURATION = "extra_payment_configuration";
    private static final String EXTRA_REQUEST_CODE = "extra_request_code";
    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    private PayButtonFragment payButtonFragment;
    private RemediesFragment remediesFragment;
    @Nullable private PaymentConfiguration paymentConfiguration;

    public static void start(@NonNull final Activity activity, final int requestCode, @NonNull final PaymentModel model,
        @NonNull final PaymentConfiguration paymentConfiguration) {
        final Intent intent = new Intent(activity, PaymentResultActivity.class);
        intent.putExtra(EXTRA_PAYMENT_MODEL, model);
        intent.putExtra(EXTRA_PAYMENT_CONFIGURATION, paymentConfiguration);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void start(@NonNull final Fragment fragment, final int requestCode, @NonNull final PaymentModel model,
        @NonNull final PaymentConfiguration paymentConfiguration) {
        final Intent intent = new Intent(fragment.getContext(), PaymentResultActivity.class);
        intent.putExtra(EXTRA_PAYMENT_MODEL, model);
        intent.putExtra(EXTRA_PAYMENT_CONFIGURATION, paymentConfiguration);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static Intent getIntent(@NonNull final Context context, @NonNull final PaymentModel paymentModel) {
        final Intent intent = new Intent(context, PaymentResultActivity.class);
        intent.putExtra(EXTRA_PAYMENT_MODEL, paymentModel);
        return intent;
    }

    @Override
    public void onCreated(@Nullable final Bundle savedInstanceState) {
        setContentView(R.layout.px_activity_payment_result);

        paymentConfiguration = getIntent().getParcelableExtra(EXTRA_PAYMENT_CONFIGURATION);
        presenter = createPresenter();
        presenter.attachView(this);
        if (savedInstanceState == null) {
            presenter.onFreshStart();
        }
        payButtonFragment = (PayButtonFragment) getSupportFragmentManager().findFragmentById(R.id.pay_button_container);
        payButtonFragment.disable();
    }

    @NonNull
    private PaymentResultPresenter createPresenter() {
        final PaymentModel paymentModel = getIntent().getParcelableExtra(EXTRA_PAYMENT_MODEL);
        final Session session = Session.getInstance();

        return new PaymentResultPresenter(session.getConfigurationModule().getPaymentSettings(),
            session.getInstructionsRepository(), paymentModel, BehaviourProvider.getFlowBehaviour());
    }

    @Override
    public void configureViews(@NonNull final PaymentResultViewModel model, @NonNull final BusinessActions callback) {
        findViewById(R.id.loading).setVisibility(View.GONE);
        final PaymentResultHeader header = findViewById(R.id.header);
        header.setModel(model.headerModel);
        final PaymentResultBody body = findViewById(R.id.body);

        if (hasRemedies(model.remediesModel)) {
            final PaymentResultFooter footer = findViewById(R.id.remedies_footer);
            footer.setVisibility(View.VISIBLE);
            footer.setQuietButtonListener(v -> changePaymentMethod());
            findViewById(R.id.remedies).setVisibility(View.VISIBLE);
            body.setVisibility(View.GONE);
            loadRemedies(model.remediesModel);
        } else {
            body.init(model.bodyModel, callback);
            //TODO migrate
            PaymentResultLegacyRenderer.render(findViewById(R.id.container), callback, model.legacyViewModel);
        }
    }

    private boolean hasRemedies(@NonNull final RemediesModel model) {
        //TODO: there will be more remedies
        return model.getCvvRemedyModel() != null;
    }

    private void loadRemedies(@NonNull final RemediesModel remediesModel) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            remediesFragment = (RemediesFragment) fragmentManager.findFragmentByTag(RemediesFragment.REMEDIES_TAG);
            if (remediesFragment == null) {
                remediesFragment = RemediesFragment.newInstance(remediesModel);
            }
            fragmentManager
                .beginTransaction()
                .replace(R.id.remedies,
                    remediesFragment,
                    RemediesFragment.REMEDIES_TAG)
                .commitAllowingStateLoss();
        }
    }

    @Override
    public void showApiExceptionError(@NonNull final ApiException exception, @NonNull final String requestOrigin) {
        ErrorUtil.showApiExceptionError(this, exception, requestOrigin);
    }

    @Override
    public void showInstructionsError() {
        ErrorUtil.startErrorActivity(this,
            new MercadoPagoError(getString(R.string.px_standard_error_message), false));
    }

    @Override
    public void onBackPressed() {
        presenter.onAbort();
    }

    @Override
    public void setStatusBarColor(@ColorRes final int color) {
        ViewUtils.setStatusBarColor(ContextCompat.getColor(this, color), getWindow());
    }

    @Override
    public void openLink(@NonNull final String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (final ActivityNotFoundException e) {
            Logger.debug(TAG, e);
        }
    }

    @Override
    public void finishWithResult(final int resultCode) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        setResult(RESULT_CUSTOM_EXIT, intent);
        finish();
    }

    @Override
    public void changePaymentMethod() {
        final Intent returnIntent = new Intent();
        new ChangePaymentMethodPostPaymentAction().addToIntent(returnIntent);
        setResult(RESULT_ACTION, returnIntent);
        finish();
    }

    @Override
    public void recoverPayment() {
        final Intent returnIntent = new Intent();
        new RecoverPaymentPostPaymentAction().addToIntent(returnIntent);
        setResult(RESULT_ACTION, returnIntent);
        finish();
    }

    @SuppressLint("Range")
    @Override
    public void copyToClipboard(@NonNull final String content) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("", content);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            MeliSnackbar.make(findViewById(R.id.container),
                getString(R.string.px_copied_to_clipboard_ack),
                Snackbar.LENGTH_SHORT, MeliSnackbar.SnackbarType.SUCCESS).show();
        }
    }

    @Override
    public void processBusinessAction(@NonNull final String deepLink) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)));
        } catch (final ActivityNotFoundException e) {
            Logger.debug(TAG, e);
        }
    }

    @Override
    public void processCrossSellingBusinessAction(@NonNull final String deepLink) {
        try {
            startActivity(getSafeIntent(this, Uri.parse(deepLink)));
        } catch (final ActivityNotFoundException e) {
            Logger.debug(TAG, e);
        }
    }

    @Override
    public void onPaymentFinished(@NotNull final IPaymentDescriptor payment) {
        remediesFragment.onPaymentFinished(payment);
    }

    @Override
    public void onPaymentError(@NotNull final MercadoPagoError error) {

    }

    @Override
    public void prePayment() {
        ViewUtils.hideKeyboard(this);
        remediesFragment.onPayButtonPressed();
    }

    @Override
    public void enablePayButton() {
        payButtonFragment.enable();
    }

    @Override
    public void disablePayButton() {
        payButtonFragment.disable();
    }

    @Override
    public void startPayment() {
        if (paymentConfiguration != null) {
            payButtonFragment.onReadyForPayment(paymentConfiguration, null);
        }
    }

    @Override
    public void showResult(@NotNull final PaymentModel paymentModel) {
        if (paymentConfiguration != null) {
            PaymentResultActivity.start(this, getIntent().getIntExtra(EXTRA_REQUEST_CODE, 666),
                paymentModel, paymentConfiguration);
            finish();
        }
    }
}