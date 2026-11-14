package com.example.chatapp.group;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

public class GroupCreateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.group_create_title);
        setContentView(R.layout.group_activity_create);

        findViewById(R.id.groupCreateSubmitButton).setOnClickListener(view ->
                Toast.makeText(this, R.string.group_mock_create_message, Toast.LENGTH_SHORT).show());
    }
}
