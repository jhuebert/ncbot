package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigItemRepository extends JpaRepository<ConfigItem, String> {
}
