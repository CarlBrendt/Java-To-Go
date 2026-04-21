package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mts.ip.workflow.engine.entity.StarterExclusionEntity;

import java.util.List;
import java.util.UUID;

public interface StarterExclusionRepository extends JpaRepository<StarterExclusionEntity, UUID> {
  List<StarterExclusionEntity> getByStarterId(UUID starterId);
}
