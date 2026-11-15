package com.example.chatapp.group;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

public class GroupConversationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.group_conversation_title);
        setContentView(R.layout.group_activity_conversation);

        EditText messageInput = findViewById(R.id.groupMessageInput);
        findViewById(R.id.groupSendButton).setOnClickListener(view -> {
            messageInput.setText("");
            Toast.makeText(this, R.string.group_mock_send_message, Toast.LENGTH_SHORT).show();
        });
    }
}
