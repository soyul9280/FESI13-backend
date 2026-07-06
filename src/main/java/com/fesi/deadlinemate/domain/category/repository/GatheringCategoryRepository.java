package com.fesi.deadlinemate.domain.category.repository;

import com.fesi.deadlinemate.domain.category.entity.GatheringCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringCategoryRepository extends JpaRepository<GatheringCategory, Long> {

    List<GatheringCategory> findByGatheringId(Long gatheringId);

    List<GatheringCategory> findByGatheringIdIn(List<Long> gatheringIds);

    @Modifying
    @Query("DELETE FROM GatheringCategory gc WHERE gc.gatheringId = :gatheringId")
    void deleteByGatheringId(@Param("gatheringId") Long gatheringId);
}
