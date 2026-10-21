package com.archmaxim.repository;

import com.archmaxim.entity.FileRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    Optional<FileRecord> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = {"course", "course.department", "uploadedBy"})
    Optional<FileRecord> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"course", "course.department", "uploadedBy"})
    List<FileRecord> findWithDetailsByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"course", "course.department", "uploadedBy"})
    Page<FileRecord> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"course", "course.department", "course.aliases", "uploadedBy"})
    @Query("select f from FileRecord f where f.id in :ids")
    List<FileRecord> findForIndexing(@Param("ids") Collection<Long> ids);

    @Query("select f.id from FileRecord f where f.active = true order by f.id")
    List<Long> findActiveIds();

    @Query("select f.id from FileRecord f where f.active = true and f.course.id in :courseIds order by f.id")
    List<Long> findActiveIdsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    @Transactional
    @Modifying
    @Query("update FileRecord f set f.downloadCount = f.downloadCount + 1 where f.id = :id")
    int incrementDownloadCount(@Param("id") Long id);
}
