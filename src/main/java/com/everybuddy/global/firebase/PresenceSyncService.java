package com.everybuddy.global.firebase;

import com.everybuddy.domain.user.repository.UserPresenceRepository;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceSyncService {

    private final FirebaseDatabase firebaseDatabase;
    private final UserPresenceRepository userPresenceRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        DatabaseReference presenceRef = firebaseDatabase.getReference("presence");

        presenceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userPresenceRepository.resetAllOffline();

                List<Long> onlineIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    onlineIds.add(Long.parseLong(child.getKey()));
                }
                if (!onlineIds.isEmpty()) {
                    userPresenceRepository.markOnlineBatch(onlineIds);
                }

                presenceRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        userPresenceRepository.markOnline(Long.parseLong(snapshot.getKey()));
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        userPresenceRepository.markOffline(Long.parseLong(snapshot.getKey()));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) { }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) { }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("presence 구독 오류: {}", error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("presence 초기 동기화 실패: {}", error.getMessage());
            }
        });
    }
}
