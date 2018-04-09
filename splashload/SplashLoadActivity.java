package ru.splashload;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import ru.airwatch.AirWatchCompatibleActivity;
import ru.App;
import ru.R;
import ru.changepassword.ChangePassActivity;
import ru.sendlog.SendLogService;

public class SplashLoadActivity extends AirWatchCompatibleActivity implements SplashLoadContract.View {

    private static final String CHANGE_PASS_ERROR = "changePassError";

    @Inject
    SplashLoadContract.Presenter presenter;
    private TextView statusLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_login);
        ImageView imageView = (ImageView) findViewById(R.id.image_logo);
        statusLoading = (TextView) findViewById(R.id.status_load);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_splash_login);
        imageView.startAnimation(animation);
        setupActivityComponent();
        presenter.onStartGettingNetworkData();
    }

    @Override
    public void onReady() {

    }

    private void setupActivityComponent() {
        DaggerSplashLoadComponent.builder()
                .appComponent(App.get(this).getAppComponent())
                .splashLoadModule(new SplashLoadModule(this))
                .build().inject(this);
    }

    @Override
    public void showToastMessage() {
        Toast.makeText(this, getString(R.string.toast_message_wait_load),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showMessage(@NonNull String message) {
        AlertDialog.Builder dialogMessage = new AlertDialog.Builder(this);
        dialogMessage.setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> finish())
                .setCancelable(false);
        dialogMessage.create();
        dialogMessage.show();
    }

    @Override
    public void setStatusLoading(@NonNull String status) {
        statusLoading.setText(status);
    }

    @Override
    public void finishActivity() {
        finish();
    }

    @Override
    public void startChangePassActivity() {
        Intent intent = new Intent(this, ChangePassActivity.class);
        intent.putExtra(CHANGE_PASS_ERROR, true);
        startActivity(intent);
    }

    @Override
    public void startAppLogService() {
        startService(new Intent(this, SendLogService.class));
    }

    @Override
    public void onBackPressed() {
        presenter.backPressed();
    }
}