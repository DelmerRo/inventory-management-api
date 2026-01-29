package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.MultimediaFile;
import com.utama.my_inventory.entities.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultimediaFileRepository extends JpaRepository<MultimediaFile, Long> {

    // Búsquedas por producto
    List<MultimediaFile> findByProductIdOrderByUploadedAtDesc(Long productId);
    List<MultimediaFile> findByProductIdAndFileTypeOrderByUploadedAtDesc(Long productId, FileType fileType);

    // Búsquedas por tipo
    List<MultimediaFile> findByFileTypeOrderByUploadedAtDesc(FileType fileType);

    // Búsqueda por URL
    Optional<MultimediaFile> findByFileUrl(String fileUrl);

    // Verificaciones
    boolean existsByProductIdAndFileName(Long productId, String fileName);
    boolean existsByProductIdAndFileUrl(Long productId, String fileUrl);

    // Consultas personalizadas
    @Query("SELECT COUNT(mf) FROM MultimediaFile mf WHERE mf.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM MultimediaFile mf WHERE mf.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}