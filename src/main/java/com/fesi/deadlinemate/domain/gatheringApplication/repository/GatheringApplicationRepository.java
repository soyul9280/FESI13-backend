package com.fesi.deadlinemate.domain.gatheringApplication.repository;

import com.fesi.deadlinemate.domain.gatheringApplication.entity.ApplicationStatus;
import com.fesi.deadlinemate.domain.gatheringApplication.entity.GatheringApplication;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringApplicationRepository extends JpaRepository<GatheringApplication, Long> {

    boolean existsByGatheringIdAndApplicantId(Long gatheringId, Long applicantId);
    Optional<GatheringApplication> findByGatheringIdAndApplicantId(Long gatheringId, Long applicantId);
    Optional<GatheringApplication> findByIdAndGatheringId(Long applicationId, Long gatheringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ga
        from GatheringApplication ga
        where ga.id = :applicationId
          and ga.gatheringId = :gatheringId
    """)
    Optional<GatheringApplication> findByIdAndGatheringIdForUpdate(
            @Param("applicationId") Long applicationId,
            @Param("gatheringId") Long gatheringId
    );
    List<GatheringApplication> findByGatheringIdOrderByCreatedAtAsc(Long gatheringId);
    List<GatheringApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    @Query("SELECT ga.gatheringId, COUNT(ga) FROM GatheringApplication ga WHERE ga.gatheringId IN :gatheringIds AND ga.status = :status GROUP BY ga.gatheringId")
    List<Object[]> countByGatheringIdInAndStatus(@Param("gatheringIds") List<Long> gatheringIds, @Param("status") ApplicationStatus status);

    List<GatheringApplication> findByGatheringIdAndStatusAndIdNot(
            Long gatheringId, ApplicationStatus status, Long excludeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE GatheringApplication ga
        SET ga.status = :rejected
        WHERE ga.gatheringId = :gatheringId
          AND ga.status = :pending
          AND ga.id <> :acceptedApplicationId
    """)
    int rejectAllPendingExcept(
            @Param("gatheringId") Long gatheringId,
            @Param("acceptedApplicationId") Long acceptedApplicationId,
            @Param("pending") ApplicationStatus pending,
            @Param("rejected") ApplicationStatus rejected
    );
}
