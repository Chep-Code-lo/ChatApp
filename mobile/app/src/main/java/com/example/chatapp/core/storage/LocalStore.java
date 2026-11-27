package com.example.chatapp.core.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.chatapp.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Local offline data cache. Networking is not configured. */
public final class LocalStore {
    private static LocalStore instance;
    private final SharedPreferences preferences;
    private final List<Person> contacts = new ArrayList<>();
    private final LinkedHashMap<String, Chat> conversations = new LinkedHashMap<>();
    private final Map<String, List<Message>> history = new LinkedHashMap<>();
    private final Set<String> pendingFriends = new LinkedHashSet<>();
    private String profileName = "Người dùng", profileBio = "", profileEmail = "", photo = "";
    private boolean signedIn, dark = true;

    public static synchronized LocalStore get(Context context) {
        if (instance == null) instance = new LocalStore(context.getApplicationContext());
        return instance;
    }

    LocalStore(Context context) {
        preferences = context.getSharedPreferences("luma_local_v3", Context.MODE_PRIVATE);
        restore();
    }

    public synchronized List<Person> people() { return new ArrayList<>(contacts); }
    public synchronized Person person(String id) {
        for (Person person : contacts) if (person.id.equals(id)) return person;
        return null;
    }
    public synchronized List<Person> searchPeople(String query) {
        String normalized = normalize(query);
        String digits = normalizePhone(query);
        List<Person> result = new ArrayList<>();
        for (Person person : contacts) {
            if (normalized.isEmpty() || normalize(person.name + " " + person.handle).contains(normalized)
                    || (!digits.isEmpty() && normalizePhone(person.phone).contains(digits))) result.add(person);
        }
        return result;
    }
    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
    public static String normalizePhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 10) digits = "0" + digits.substring(2);
        return digits;
    }
    public synchronized List<Chat> chats() {
        List<Chat> result = new ArrayList<>(conversations.values());
        Collections.sort(result, (a, b) -> {
            if (a.isPinned != b.isPinned) return a.isPinned ? -1 : 1;
            return Long.compare(lastTime(b.id), lastTime(a.id));
        });
        return result;
    }
    private long lastTime(String id) {
        List<Message> list = history.get(id);
        return list == null || list.isEmpty() ? 0 : list.get(list.size() - 1).timestamp;
    }
    public synchronized Chat chat(String id) { return conversations.get(id); }
    public synchronized List<Message> messages(String chatId) {
        List<Message> list = history.get(chatId);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
    public synchronized String ensureDirect(String personId) {
        Person person = person(personId);
        if (person == null) return null;
        String id = "chat-" + personId;
        if (!conversations.containsKey(id)) {
            conversations.put(id, new Chat(id, person.name, false, Arrays.asList(personId), person.avatarRes));
            history.put(id, new ArrayList<>());
            save();
        }
        return id;
    }
    public synchronized boolean sendText(String chatId, String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty() || value.length() > 10000 || !conversations.containsKey(chatId)) return false;
        append(chatId, new Message(UUID.randomUUID().toString(), value, "text", "", "", profileName, true, System.currentTimeMillis()));
        return true;
    }
    public synchronized boolean sendAttachment(String chatId, String uri, String kind, String name) {
        if (!conversations.containsKey(chatId) || uri == null || !uri.startsWith("content://")) return false;
        if (!Arrays.asList("image", "video", "file", "audio").contains(kind)) return false;
        append(chatId, new Message(UUID.randomUUID().toString(), "", kind, uri,
                name == null || name.trim().isEmpty() ? "Tệp đính kèm" : name.trim(), profileName, true, System.currentTimeMillis()));
        return true;
    }
    private void append(String chatId, Message message) {
        history.computeIfAbsent(chatId, key -> new ArrayList<>()).add(message);
        save();
    }
    public synchronized void markRead(String chatId) {
        Chat chat = conversations.get(chatId);
        if (chat != null && chat.unreadCount != 0) { chat.unreadCount = 0; save(); }
    }
    public synchronized void togglePinned(String chatId) {
        Chat chat = conversations.get(chatId);
        if (chat != null) { chat.isPinned = !chat.isPinned; save(); }
    }
    public synchronized void toggleMuted(String chatId) {
        Chat chat = conversations.get(chatId);
        if (chat != null) { chat.isMuted = !chat.isMuted; save(); }
    }
    public synchronized String createGroup(String name, List<String> memberIds) {
        List<String> members = validMembers(memberIds);
        String title = name == null ? "" : name.trim();
        if (title.isEmpty() || title.length() > 60 || members.size() < 2) return null;
        String id = "group-" + UUID.randomUUID();
        conversations.put(id, new Chat(id, title, true, members, R.drawable.lucide_users));
        history.put(id, new ArrayList<>());
        save();
        return id;
    }
    private List<String> validMembers(List<String> ids) {
        Set<String> result = new LinkedHashSet<>();
        if (ids != null) for (String id : ids) if (person(id) != null) result.add(id);
        return new ArrayList<>(result);
    }
    public synchronized void addMembers(String chatId, List<String> ids) {
        Chat chat = conversations.get(chatId);
        if (chat == null || !chat.isGroup) return;
        for (String id : validMembers(ids)) if (!chat.memberIds.contains(id)) chat.memberIds.add(id);
        save();
    }
    public synchronized void removeMember(String chatId, String personId) {
        Chat chat = conversations.get(chatId);
        if (chat != null && chat.isGroup && chat.memberIds.remove(personId)) save();
    }
    public synchronized void leaveGroup(String chatId) {
        Chat chat = conversations.get(chatId);
        if (chat != null && chat.isGroup) { conversations.remove(chatId); history.remove(chatId); save(); }
    }
    public synchronized void requestFriend(String personId) {
        if (person(personId) != null && pendingFriends.add(personId)) save();
    }
    public synchronized boolean isFriendRequestPending(String personId) { return pendingFriends.contains(personId); }
    public synchronized void updateProfile(String name, String bio) {
        if (name != null && !name.trim().isEmpty()) profileName = name.trim();
        profileBio = bio == null ? "" : bio.trim();
        save();
    }
    public synchronized String name() { return profileName; }
    public synchronized String bio() { return profileBio; }
    public synchronized String email() { return profileEmail; }
    public synchronized void setProfilePhoto(String uri) { photo = uri == null ? "" : uri; save(); }
    public synchronized String profilePhoto() { return photo; }
    public synchronized boolean isSignedIn() { return signedIn; }
    public synchronized void startSession(String name, String email) {
        if (name != null && !name.trim().isEmpty()) profileName = name.trim();
        profileEmail = email == null ? "" : email.trim();
        signedIn = true;
        save();
    }
    public synchronized void logout() { signedIn = false; save(); }
    public synchronized void setDark(boolean value) { dark = value; save(); }
    public synchronized boolean isDark() { return dark; }
    private boolean restore() {
        String raw = preferences.getString("state", null);
        if (raw == null) return false;
        try {
            JSONObject root = new JSONObject(raw);
            profileName = root.optString("name", profileName); profileBio = root.optString("bio", profileBio);
            profileEmail = root.optString("email", ""); photo = root.optString("photo", "");
            signedIn = root.optBoolean("signedIn"); dark = root.optBoolean("dark", true);
            JSONArray pending = root.optJSONArray("pendingFriends");
            if (pending != null) for (int i = 0; i < pending.length(); i++) pendingFriends.add(pending.getString(i));
            JSONArray chats = root.getJSONArray("chats");
            for (int i = 0; i < chats.length(); i++) {
                JSONObject value = chats.getJSONObject(i);
                List<String> ids = new ArrayList<>();
                JSONArray members = value.getJSONArray("members");
                for (int j = 0; j < members.length(); j++) ids.add(members.getString(j));
                boolean group = value.getBoolean("group");
                Person person = ids.isEmpty() ? null : person(ids.get(0));
                Chat chat = new Chat(value.getString("id"), value.getString("title"), group, validMembers(ids),
                        group || person == null ? R.drawable.lucide_users : person.avatarRes);
                chat.isPinned = value.optBoolean("pinned"); chat.isMuted = value.optBoolean("muted"); chat.unreadCount = value.optInt("unread");
                conversations.put(chat.id, chat);
                List<Message> list = new ArrayList<>();
                JSONArray messages = value.getJSONArray("messages");
                for (int j = 0; j < messages.length(); j++) {
                    JSONObject m = messages.getJSONObject(j);
                    list.add(new Message(m.getString("id"), m.optString("text"), m.optString("kind", "text"), m.optString("uri"), m.optString("fileName"), m.optString("sender"), m.optBoolean("outgoing"), m.optLong("time")));
                }
                history.put(chat.id, list);
            }
            return true;
        } catch (JSONException ignored) {
            conversations.clear(); history.clear(); pendingFriends.clear();
            return false;
        }
    }
    private void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("name", profileName).put("bio", profileBio).put("email", profileEmail).put("photo", photo)
                    .put("signedIn", signedIn).put("dark", dark).put("pendingFriends", new JSONArray(pendingFriends));
            JSONArray chats = new JSONArray();
            for (Chat chat : conversations.values()) {
                JSONObject c = new JSONObject();
                c.put("id", chat.id).put("title", chat.title).put("group", chat.isGroup).put("members", new JSONArray(chat.memberIds))
                        .put("pinned", chat.isPinned).put("muted", chat.isMuted).put("unread", chat.unreadCount);
                JSONArray messages = new JSONArray();
                for (Message m : messages(chat.id)) messages.put(new JSONObject().put("id", m.id).put("text", m.text)
                        .put("kind", m.kind).put("uri", m.uri).put("fileName", m.fileName).put("sender", m.senderName)
                        .put("outgoing", m.outgoing).put("time", m.timestamp));
                c.put("messages", messages); chats.put(c);
            }
            root.put("chats", chats);
            preferences.edit().putString("state", root.toString()).apply();
        } catch (JSONException error) { throw new IllegalStateException("Cannot serialize local data", error); }
    }

    public static final class Person {
        public final String id, name, handle, bio, phone;
        public final int avatarRes;
        public final boolean online;
        Person(String id, String name, String handle, String bio, String phone, int avatarRes, boolean online) {
            this.id = id; this.name = name; this.handle = handle; this.bio = bio; this.phone = phone; this.avatarRes = avatarRes; this.online = online;
        }
    }
    public static final class Chat {
        public final String id, title;
        public final boolean isGroup;
        public boolean isPinned, isMuted;
        public int unreadCount;
        public final List<String> memberIds;
        public final int avatarRes;
        Chat(String id, String title, boolean group, List<String> memberIds, int avatarRes) {
            this.id = id; this.title = title; this.isGroup = group; this.memberIds = new ArrayList<>(memberIds); this.avatarRes = avatarRes;
        }
    }
    public static final class Message {
        public final String id, text, kind, uri, fileName, senderName;
        public final boolean outgoing;
        public final long timestamp;
        public Message(String id, String text, String kind, String uri, String fileName, String senderName, boolean outgoing, long timestamp) {
            this.id = id; this.text = text; this.kind = kind; this.uri = uri; this.fileName = fileName;
            this.senderName = senderName; this.outgoing = outgoing; this.timestamp = timestamp;
        }
    }
}
