package com.example.chatapp.group;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;

abstract class GroupBaseActivity extends AppCompatActivity {
    protected LocalStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = LocalStore.get(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(mode != Configuration.UI_MODE_NIGHT_YES);
        controller.setAppearanceLightNavigationBars(mode != Configuration.UI_MODE_NIGHT_YES);
    }

    protected void setScreen(@LayoutRes int layout) {
        setContentView(layout);
        View root = findViewById(R.id.groupScreenRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    protected void setupToolbar(String title) {
        TextView label = findViewById(R.id.groupToolbarTitle);
        label.setText(title);
        findViewById(R.id.groupBackButton)
                .setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }
}
