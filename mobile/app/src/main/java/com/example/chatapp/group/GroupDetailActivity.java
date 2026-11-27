package com.example.chatapp.group;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailActivity extends GroupBaseActivity {
    private String chatId;
    private GroupPersonAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.group_activity_detail);
        setupToolbar(getString(R.string.group_ui_details_title));
        chatId = getIntent().getStringExtra("chat_id");
        RecyclerView members = findViewById(R.id.groupDetailMembersRecycler);
        members.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupPersonAdapter(this, null, null, "", null);
        adapter.setRemoveAction(this::confirmRemove);
        members.setAdapter(adapter);
        findViewById(R.id.groupDetailAddMemberButton).setOnClickListener(view ->
                startActivity(new Intent(this, GroupAddFriendActivity.class).putExtra("chat_id", chatId)));
        findViewById(R.id.groupDetailInviteButton).setOnClickListener(view -> share());
        findViewById(R.id.groupDetailLeaveButton).setOnClickListener(view -> confirmLeave());
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private LocalStore.Chat group() {
        LocalStore.Chat group = store.chat(chatId);
        if (group == null || !group.isGroup) {
            Toast.makeText(this, R.string.group_ui_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return null;
        }
        return group;
    }

    private void refresh() {
        LocalStore.Chat group = group();
        if (group == null) return;
        ((TextView) findViewById(R.id.groupDetailName)).setText(group.title);
        ((TextView) findViewById(R.id.groupDetailCount)).setText(getString(R.string.group_ui_members_count, group.memberIds.size() + 1));
        ((TextView) findViewById(R.id.groupDetailSelfName)).setText(getString(R.string.group_ui_you, store.name()));
        List<LocalStore.Person> members = new ArrayList<>();
        for (String id : group.memberIds) {
            LocalStore.Person person = store.person(id);
            if (person != null) members.add(person);
        }
        adapter.submit(members);
    }

    private void share() {
        LocalStore.Chat group = group();
        if (group == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.group_ui_share_message, group.title));
        startActivity(Intent.createChooser(share, getString(R.string.group_ui_share_chooser)));
    }

    private void confirmRemove(LocalStore.Person person) {
        LocalStore.Chat group = group();
        if (group == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.group_ui_remove_member_title, person.name))
                .setMessage(getString(R.string.group_ui_remove_member_message, group.title))
                .setNegativeButton(R.string.group_ui_cancel, null)
                .setPositiveButton(R.string.group_ui_remove_member, (dialog, which) -> {
                    store.removeMember(chatId, person.id);
                    refresh();
                }).show();
    }

    private void confirmLeave() {
        LocalStore.Chat group = group();
        if (group == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.group_ui_leave_title, group.title))
                .setMessage(R.string.group_ui_leave_message)
                .setNegativeButton(R.string.group_ui_cancel, null)
                .setPositiveButton(R.string.group_ui_leave, (dialog, which) -> {
                    store.leaveGroup(chatId);
                    startActivity(new Intent(this, GroupListActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    finish();
                }).show();
    }
}
