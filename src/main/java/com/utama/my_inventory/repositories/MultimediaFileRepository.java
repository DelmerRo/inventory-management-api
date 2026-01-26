package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.MultimediaFile;
import com.utama.my_inventory.entities.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultimediaFileRepository extends JpaRepository<MultimediaFile, Long> {

    List<MultimediaFile> findByProductId(Long productId);

    List<MultimediaFile> findByProductIdAndFileType(Long productId, FileType fileType);

    @Query("SELECT mf FROM MultimediaFile mf WHERE mf.product.id = :productId " +
            "ORDER BY CASE WHEN mf.fileType = 'IMAGE' THEN 1 " +
            "WHEN mf.fileType = 'VIDEO' THEN 2 " +
            "WHEN mf.fileType = 'DOCUMENT' THEN 3 " +
            "ELSE 4 END, mf.uploadedAt DESC")
    List<MultimediaFile> findByProductIdOrdered(@Param("productId") Long productId);

    Optional<MultimediaFile> findFirstByProductIdAndFileTypeOrderByUploadedAtDesc(Long productId, FileType fileType);

    long countByProductId(Long productId);

    long countByProductIdAndFileType(Long productId, FileType fileType);

    void deleteByProductId(Long productId);
}
