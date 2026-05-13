package com.everybuddy.global.firebase;

import com.google.firebase.database.FirebaseDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViewingSyncService 단위 테스트")
class ViewingSyncServiceTest {

    @Mock
    private FirebaseDatabase firebaseDatabase;

    private ViewingSyncService viewingSyncService;

    @BeforeEach
    void setUp() {
        viewingSyncService = new ViewingSyncService(firebaseDatabase);
    }

    @Nested
    @DisplayName("isViewing")
    class IsViewing {

        @Test
        @DisplayName("유저가 해당 채팅방을 보고 있으면 true")
        void returnsTrueWhenUserViewsRoom() {
            viewingSyncService.putViewing(1L, 100L);

            assertTrue(viewingSyncService.isViewing(1L, 100L));
        }

        @Test
        @DisplayName("유저가 보고 있지 않으면 false")
        void returnsFalseWhenUserNotViewing() {
            assertFalse(viewingSyncService.isViewing(1L, 100L));
        }

        @Test
        @DisplayName("유저가 다른 채팅방을 보고 있으면 false")
        void returnsFalseWhenUserViewsDifferentRoom() {
            viewingSyncService.putViewing(1L, 100L);

            assertFalse(viewingSyncService.isViewing(1L, 200L));
        }

        @Test
        @DisplayName("userId가 null이면 false")
        void returnsFalseWhenUserIdNull() {
            assertFalse(viewingSyncService.isViewing(null, 100L));
        }

        @Test
        @DisplayName("chatRoomId가 null이면 false")
        void returnsFalseWhenChatRoomIdNull() {
            viewingSyncService.putViewing(1L, 100L);

            assertFalse(viewingSyncService.isViewing(1L, null));
        }
    }

    @Nested
    @DisplayName("방 전환")
    class RoomSwitch {

        @Test
        @DisplayName("같은 유저가 다른 방으로 이동하면 새 방만 isViewing true")
        void switchingRoomReflectsNewRoomOnly() {
            viewingSyncService.putViewing(1L, 100L);
            viewingSyncService.putViewing(1L, 200L);

            assertFalse(viewingSyncService.isViewing(1L, 100L));
            assertTrue(viewingSyncService.isViewing(1L, 200L));
        }
    }

    @Nested
    @DisplayName("removeViewing")
    class RemoveViewing {

        @Test
        @DisplayName("제거 후에는 isViewing false")
        void removedUserNoLongerViewing() {
            viewingSyncService.putViewing(1L, 100L);
            viewingSyncService.removeViewing(1L);

            assertFalse(viewingSyncService.isViewing(1L, 100L));
        }
    }

    @Nested
    @DisplayName("clearViewing")
    class ClearViewing {

        @Test
        @DisplayName("clear 후 모든 isViewing false")
        void clearRemovesAllEntries() {
            viewingSyncService.putViewing(1L, 100L);
            viewingSyncService.putViewing(2L, 200L);

            viewingSyncService.clearViewing();

            assertFalse(viewingSyncService.isViewing(1L, 100L));
            assertFalse(viewingSyncService.isViewing(2L, 200L));
        }
    }

    @Nested
    @DisplayName("putViewing 방어 로직")
    class PutGuard {

        @Test
        @DisplayName("userId가 null이면 저장하지 않음")
        void doesNotStoreWhenUserIdNull() {
            viewingSyncService.putViewing(null, 100L);

            assertFalse(viewingSyncService.isViewing(null, 100L));
        }

        @Test
        @DisplayName("chatRoomId가 null이면 저장하지 않음 (ConcurrentHashMap NPE 방지)")
        void doesNotStoreWhenChatRoomIdNull() {
            viewingSyncService.putViewing(1L, null);

            assertFalse(viewingSyncService.isViewing(1L, 100L));
        }
    }
}
