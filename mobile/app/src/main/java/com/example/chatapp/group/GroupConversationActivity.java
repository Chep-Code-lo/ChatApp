package com.example.chatapp.group;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.os.Parcelable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class GroupConversationActivity extends GroupBaseActivity {
    private String chatId;
    private GroupMessageAdapter adapter;
    private RecyclerView list;
    private EditText messageInput;
    private LinearLayoutManager manager;
    private Parcelable pendingScroll;
    private String messageAnchor;
    private boolean firstRender = true;
    private final ActivityResultLauncher<String[]> chooseAttachment = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::sendAttachment);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.group_activity_conversation);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        chatId = savedInstanceState == null ? getIntent().getStringExtra("chat_id")
                : savedInstanceState.getString("chat_id");
        messageAnchor = savedInstanceState == null ? getIntent().getStringExtra("message_id") : null;
        if (!validGroup()) return;
        messageInput = findViewById(R.id.groupMessageInput);
        list = findViewById(R.id.groupMessagesRecycler);
        manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        list.setLayoutManager(manager);
        adapter = new GroupMessageAdapter(this, store.messages(chatId));
        list.setAdapter(adapter);
        if (savedInstanceState != null) {
            messageInput.setText(savedInstanceState.getString("draft", ""));
            int cursor = Math.max(0, Math.min(messageInput.length(),
                    savedInstanceState.getInt("draft_cursor", messageInput.length())));
            messageInput.setSelection(cursor);
            pendingScroll = savedInstanceState.getParcelable("message_scroll", Parcelable.class);
        }
        findViewById(R.id.groupSendButton).setOnClickListener(view -> send());
        findViewById(R.id.groupAttachmentButton).setOnClickListener(view -> attachments());
        refresh(false);
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) refresh(false);
    }

    private boolean validGroup() {
        LocalStore.Chat group = store.chat(chatId);
        if (group == null || !group.isGroup) {
            Toast.makeText(this, R.string.group_ui_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void refresh(boolean forceEnd) {
        if (!validGroup()) return;
        LocalStore.Chat group = store.chat(chatId);
        setupToolbar(group.title);
        ((TextView) findViewById(R.id.groupConversationStatus)).setText(getString(R.string.group_ui_members_count, group.memberIds.size() + 1));
        View details = findViewById(R.id.groupToolbarAction);
        details.setVisibility(View.VISIBLE);
        details.setContentDescription(getString(R.string.group_ui_details_title));
        details.setOnClickListener(view -> startActivity(new Intent(this, GroupDetailActivity.class).putExtra("chat_id", chatId)));
        store.markRead(chatId);
        int oldCount = adapter.getItemCount();
        boolean atBottom = oldCount == 0 || manager.findLastVisibleItemPosition() >= oldCount - 2;
        List<LocalStore.Message> messages = store.messages(chatId);
        adapter.submit(messages);
        if (pendingScroll != null) {
            manager.onRestoreInstanceState(pendingScroll);
            pendingScroll = null;
            messageAnchor = null;
        } else {
            int anchorPosition = -1;
            if (messageAnchor != null) {
                for (int index = 0; index < messages.size(); index++) {
                    if (messageAnchor.equals(messages.get(index).id)) {
                        anchorPosition = index;
                        break;
                    }
                }
                messageAnchor = null;
            }
            if (anchorPosition >= 0) {
                manager.scrollToPositionWithOffset(anchorPosition,
                        Math.round(16 * getResources().getDisplayMetrics().density));
            } else if (!messages.isEmpty() && (forceEnd || firstRender
                    || (atBottom && oldCount != messages.size()))) {
                list.scrollToPosition(messages.size() - 1);
            }
        }
        firstRender = false;
    }

    private void send() {
        if (!validGroup()) return;
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            messageInput.setError(getString(R.string.group_ui_message_invalid));
            return;
        }
        if (store.sendText(chatId, text)) {
            messageInput.setText("");
            refresh(true);
        }
    }

    private void attachments() {
        if (!validGroup()) return;
        CharSequence[] choices = {
                getString(R.string.group_ui_attach_photo),
                getString(R.string.group_ui_attach_file),
                getString(R.string.group_ui_attach_audio)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.group_ui_attach_title)
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) chooseAttachment.launch(new String[]{"image/*", "video/*"});
                    else if (which == 1) chooseAttachment.launch(new String[]{"*/*"});
                    else chooseAttachment.launch(new String[]{"audio/*"});
                }).show();
    }

    private void sendAttachment(Uri uri) {
        if (uri == null || !validGroup()) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = getContentResolver().getType(uri);
            String kind = "file";
            if (type != null && type.startsWith("image/")) kind = "image";
            else if (type != null && type.startsWith("video/")) kind = "video";
            else if (type != null && type.startsWith("audio/")) kind = "audio";
            String name = getString(R.string.group_ui_file);
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                    name = cursor.getString(0);
                }
            }
            if (store.sendAttachment(chatId, uri.toString(), kind, name)) refresh(true);
        } catch (SecurityException ignored) {
            Toast.makeText(this, R.string.group_ui_attachment_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("chat_id", chatId);
        if (messageInput != null) {
            outState.putString("draft", messageInput.getText().toString());
            outState.putInt("draft_cursor", messageInput.getSelectionStart());
            outState.putParcelable("message_scroll", manager.onSaveInstanceState());
        }
        super.onSaveInstanceState(outState);
    }
}
