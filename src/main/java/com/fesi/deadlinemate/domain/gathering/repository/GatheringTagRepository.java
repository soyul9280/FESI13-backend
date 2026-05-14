package com.fesi.deadlinemate.domain.gathering.repository;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringTag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringTagRepository extends JpaRepository<GatheringTag, Long> {

    interface TagRow {
        Long getGatheringId();
        String getTag();
    }

    void deleteByGatheringId(Long gatheringId);

    @Query("SELECT gt.tag FROM GatheringTag gt WHERE gt.gatheringId = :gatheringId ORDER BY gt.id ASC")
    List<String> findTagsByGatheringId(@Param("gatheringId") Long gatheringId);

    @Query("SELECT gt.gatheringId AS gatheringId, gt.tag AS tag " +
           "FROM GatheringTag gt WHERE gt.gatheringId IN :ids " +
           "ORDER BY gt.gatheringId ASC, gt.id ASC")
    List<TagRow> findTagRowsByGatheringIdIn(@Param("ids") Collection<Long> ids);
}
