package com.example.chatapp.group;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

public class GroupDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.group_detail_title);
        setContentView(R.layout.group_activity_detail);

        findViewById(R.id.groupDetailAddMemberButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupAddFriendActivity.class)));
        findViewById(R.id.groupDetailInviteButton).setOnClickListener(view ->
                Toast.makeText(this, R.string.group_mock_invite_message, Toast.LENGTH_SHORT).show());
        findViewById(R.id.groupDetailLeaveButton).setOnClickListener(view ->
                Toast.makeText(this, R.string.group_mock_leave_message, Toast.LENGTH_SHORT).show());
    }
}
