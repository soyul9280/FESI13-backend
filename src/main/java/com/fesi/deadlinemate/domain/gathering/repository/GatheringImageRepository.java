package com.fesi.deadlinemate.domain.gathering.repository;

import com.fesi.deadlinemate.domain.gathering.entity.GatheringImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringImageRepository extends JpaRepository<GatheringImage, Long> {

    interface ImageRow {
        Long getGatheringId();
        String getImageUrl();
        Integer getDisplayOrder();
    }

    void deleteByGatheringId(Long gatheringId);

    @Query("SELECT gi.gatheringId AS gatheringId, gi.imageUrl AS imageUrl, gi.displayOrder AS displayOrder " +
           "FROM GatheringImage gi WHERE gi.gatheringId = :gatheringId ORDER BY gi.displayOrder ASC")
    List<ImageRow> findImageRowsByGatheringId(@Param("gatheringId") Long gatheringId);

    @Query("SELECT gi.gatheringId AS gatheringId, gi.imageUrl AS imageUrl, gi.displayOrder AS displayOrder " +
           "FROM GatheringImage gi WHERE gi.gatheringId IN :ids " +
           "ORDER BY gi.gatheringId ASC, gi.displayOrder ASC")
    List<ImageRow> findImageRowsByGatheringIdIn(@Param("ids") Collection<Long> ids);
}
