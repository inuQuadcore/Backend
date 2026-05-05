package com.everybuddy.global.firebase;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewingSyncService {

    private final FirebaseDatabase firebaseDatabase;
    private final ConcurrentMap<Long, Long> viewingByUserId = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        DatabaseReference viewingRef = firebaseDatabase.getReference("viewing");

        viewingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                clearViewing();
                for (DataSnapshot child : snapshot.getChildren()) {
                    applyViewingFromSnapshot(child);
                }

                viewingRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        applyViewingFromSnapshot(snapshot);
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        removeViewing(Long.parseLong(snapshot.getKey()));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                        applyViewingFromSnapshot(snapshot);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                        // viewing 노드는 순서가 없으므로 이동 이벤트는 발생하지 않음
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("viewing 구독 오류: {}", error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("viewing 초기 동기화 실패: {}", error.getMessage());
            }
        });
    }

    public boolean isViewing(Long userId, Long chatRoomId) {
        if (userId == null || chatRoomId == null) {
            return false;
        }
        return chatRoomId.equals(viewingByUserId.get(userId));
    }

    void putViewing(Long userId, Long chatRoomId) {
        if (userId == null || chatRoomId == null) {
            return;
        }
        viewingByUserId.put(userId, chatRoomId);
    }

    void removeViewing(Long userId) {
        if (userId == null) {
            return;
        }
        viewingByUserId.remove(userId);
    }

    void clearViewing() {
        viewingByUserId.clear();
    }

    private void applyViewingFromSnapshot(DataSnapshot snapshot) {
        Long userId = Long.parseLong(snapshot.getKey());
        Long chatRoomId = snapshot.getValue(Long.class);
        putViewing(userId, chatRoomId);
    }
}
