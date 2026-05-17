package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {

    @Query("""
            SELECT c
            FROM ChatChannel c
            WHERE c.isDm = :isDm AND c.channelKey = :channelKey
            """)
    Optional<ChatChannel> findChannel(boolean isDm, String channelKey);

}
