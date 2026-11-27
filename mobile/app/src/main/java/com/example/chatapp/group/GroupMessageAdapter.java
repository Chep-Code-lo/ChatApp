package com.example.chatapp.group;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.core.storage.LocalStore;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Message bubbles and attachment previews for group conversations. */
final class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.Holder> {
    private static final ExecutorService IMAGE_WORKER = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> IMAGES = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    private final Context context;
    private final List<LocalStore.Message> messages = new ArrayList<>();

    GroupMessageAdapter(Context context, List<LocalStore.Message> initialMessages) {
        this.context = context;
        messages.addAll(initialMessages);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }


    public void submit(List<LocalStore.Message> items) {
        messages.clear();
        messages.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_item_message, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        LocalStore.Message message = messages.get(position);
        boolean outgoing = message.outgoing;
        int foreground = ContextCompat.getColor(context,
                outgoing ? R.color.group_ui_on_accent : R.color.group_ui_text);
        int secondary = ContextCompat.getColor(context,
                outgoing ? R.color.group_ui_on_accent : R.color.group_ui_secondary);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.cluster.getLayoutParams();
        params.gravity = outgoing ? Gravity.END : Gravity.START;
        holder.cluster.setLayoutParams(params);
        holder.cluster.setGravity(outgoing ? Gravity.END : Gravity.START);
        holder.bubble.setBackgroundResource(outgoing
                ? R.drawable.group_bubble_outgoing : R.drawable.group_bubble_incoming);
        holder.sender.setVisibility(!outgoing && !TextUtils.isEmpty(message.senderName)
                ? View.VISIBLE : View.GONE);
        holder.sender.setText(message.senderName);
        holder.text.setTextColor(foreground);
        holder.attachmentName.setTextColor(foreground);
        holder.attachmentAction.setTextColor(secondary);
        ImageViewCompat.setImageTintList(holder.attachmentIcon, ColorStateList.valueOf(foreground));
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.timestamp));
        holder.time.setText(outgoing ? context.getString(R.string.group_ui_your_message_time, time) : time);
        holder.boundToken = message.id + ":" + message.uri;
        holder.image.setVisibility(View.GONE);
        holder.attachment.setVisibility(View.GONE);
        holder.text.setVisibility(View.GONE);
        holder.text.setTextIsSelectable(false);
        holder.bubble.setOnClickListener(null);
        holder.bubble.setClickable(false);
        holder.bubble.setFocusable(false);
        holder.bubble.setContentDescription(null);

        if ("image".equals(message.kind)) {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageResource(R.drawable.lucide_image);
            ImageViewCompat.setImageTintList(holder.image, ColorStateList.valueOf(secondary));
            holder.image.setContentDescription(TextUtils.isEmpty(message.fileName)
                    ? context.getString(R.string.group_ui_image_file) : message.fileName);
            bindImage(holder, message.uri, holder.boundToken);
            bindAttachmentClick(holder, message);
        } else if ("audio".equals(message.kind) || "file".equals(message.kind) || "video".equals(message.kind)) {
            holder.attachment.setVisibility(View.VISIBLE);
            boolean audio = "audio".equals(message.kind);
            boolean video = "video".equals(message.kind);
            holder.attachmentIcon.setImageResource(audio ? R.drawable.lucide_archive
                    : video ? R.drawable.lucide_image : R.drawable.lucide_archive);
            holder.attachmentName.setText(TextUtils.isEmpty(message.fileName)
                    ? context.getString(audio ? R.string.group_ui_attach_audio : video ? R.string.group_ui_video_file
                    : R.string.group_ui_file) : message.fileName);
            holder.attachmentAction.setText(R.string.group_ui_open_attachment);
            bindAttachmentClick(holder, message);
        } else {
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(message.text == null ? "" : message.text);
            holder.text.setTextIsSelectable(true);
        }
    }

    private void bindAttachmentClick(Holder holder, LocalStore.Message message) {
        holder.bubble.setFocusable(true);
        holder.bubble.setContentDescription(context.getString(R.string.group_ui_open_attachment)
                + ": " + (TextUtils.isEmpty(message.fileName) ? message.kind : message.fileName));
        holder.bubble.setOnClickListener(v -> {
            if (TextUtils.isEmpty(message.uri)) {
                Toast.makeText(context, R.string.group_ui_attachment_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = Uri.parse(message.uri);
            try {
                String mimeType = context.getContentResolver().getType(uri);
                context.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, mimeType == null ? "*/*" : mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (ActivityNotFoundException | SecurityException ignored) {
                Toast.makeText(context, R.string.group_ui_attachment_unavailable, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindImage(Holder holder, String uri, String token) {
        if (TextUtils.isEmpty(uri)) {
            showUnavailable(holder);
            return;
        }
        Bitmap cached = IMAGES.get(uri);
        if (cached != null) {
            ImageViewCompat.setImageTintList(holder.image, null);
            holder.image.setImageBitmap(cached);
            return;
        }
        Context application = context.getApplicationContext();
        IMAGE_WORKER.execute(() -> {
            Bitmap bitmap = decodeSampled(application, uri);
            if (bitmap != null) IMAGES.put(uri, bitmap);
            MAIN.post(() -> {
                if (!token.equals(holder.boundToken)) return;
                if (bitmap == null) {
                    showUnavailable(holder);
                } else {
                    ImageViewCompat.setImageTintList(holder.image, null);
                    holder.image.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap decodeSampled(Context context, String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 900
                    || bounds.outHeight / options.inSampleSize > 900) {
                options.inSampleSize *= 2;
            }
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                return input == null ? null : BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Exception ignored) {
            // Persisted attachment permissions can be revoked or their source files removed.
            return null;
        }
    }

    private void showUnavailable(Holder holder) {
        holder.image.setVisibility(View.GONE);
        holder.attachment.setVisibility(View.VISIBLE);
        holder.attachmentIcon.setImageResource(R.drawable.lucide_image);
        holder.attachmentName.setText(R.string.group_ui_image_file);
        holder.attachmentAction.setText(R.string.group_ui_attachment_unavailable);
    }

    @Override public int getItemCount() { return messages.size(); }

    @Override public void onViewRecycled(@NonNull Holder holder) {
        holder.boundToken = "";
        holder.image.setImageDrawable(null);
        super.onViewRecycled(holder);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout cluster, bubble;
        final TextView sender, text, time, attachmentName, attachmentAction;
        final ImageView image, attachmentIcon;
        final View attachment;
        String boundToken = "";
        Holder(View view) {
            super(view);
            cluster = view.findViewById(R.id.groupBubbleCluster);
            bubble = view.findViewById(R.id.groupBubbleBubble);
            sender = view.findViewById(R.id.groupBubbleSender);
            text = view.findViewById(R.id.groupBubbleText);
            time = view.findViewById(R.id.groupBubbleTime);
            image = view.findViewById(R.id.groupBubbleImage);
            attachment = view.findViewById(R.id.groupBubbleAttachment);
            attachmentIcon = view.findViewById(R.id.groupBubbleAttachmentIcon);
            attachmentName = view.findViewById(R.id.groupBubbleAttachmentName);
            attachmentAction = view.findViewById(R.id.groupBubbleAttachmentAction);
        }
    }
}
