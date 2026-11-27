package com.example.chatapp.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;

import java.util.ArrayList;
import java.util.List;

final class GroupCardAdapter extends RecyclerView.Adapter<GroupCardAdapter.Holder> {
    interface ChatAction { void run(String chatId); }

    private final Context context;
    private final LocalStore store;
    private final ChatAction openChat;
    private final ChatAction openDetail;
    private final List<LocalStore.Chat> groups = new ArrayList<>();

    GroupCardAdapter(Context context, LocalStore store, ChatAction openChat, ChatAction openDetail) {
        this.context = context;
        this.store = store;
        this.openChat = openChat;
        this.openDetail = openDetail;
    }

    void submit(List<LocalStore.Chat> next) {
        groups.clear();
        groups.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        LocalStore.Chat chat = groups.get(position);
        holder.name.setText(chat.title);
        holder.members.setText(context.getString(R.string.group_ui_member_count_with_you, chat.memberIds.size() + 1));
        holder.unread.setText(context.getString(R.string.group_ui_unread_count, chat.unreadCount));
        holder.unread.setVisibility(chat.unreadCount > 0 ? View.VISIBLE : View.GONE);
        List<LocalStore.Message> messages = store.messages(chat.id);
        String preview = context.getString(R.string.group_ui_first_message);
        if (!messages.isEmpty()) {
            LocalStore.Message last = messages.get(messages.size() - 1);
            String body = last.text;
            if (body == null || body.trim().isEmpty()) {
                body = last.fileName == null || last.fileName.isEmpty()
                        ? context.getString(R.string.group_ui_attachment_preview) : last.fileName;
            }
            String author = last.outgoing ? context.getString(R.string.group_ui_you_short) : last.senderName;
            preview = author == null || author.isEmpty() ? body : author + ": " + body;
        }
        holder.preview.setText(preview);
        holder.open.setOnClickListener(view -> openChat.run(chat.id));
        holder.detail.setOnClickListener(view -> openDetail.run(chat.id));
    }

    @Override public int getItemCount() { return groups.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, members, unread, preview;
        final View open, detail;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.groupCardName);
            members = view.findViewById(R.id.groupCardMembers);
            unread = view.findViewById(R.id.groupCardUnread);
            preview = view.findViewById(R.id.groupCardPreview);
            open = view.findViewById(R.id.groupOpenConversationButton);
            detail = view.findViewById(R.id.groupOpenDetailButton);
        }
    }
}
