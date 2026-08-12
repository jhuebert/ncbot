package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMemoryFailure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemoryFailureRepository extends JpaRepository<ChatMemoryFailure, Long> {

    Page<ChatMemoryFailure> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ChatMemoryFailure> findAllByChatChannelIdOrderByCreatedAtDesc(Long chatChannelId, Pageable pageable);

}