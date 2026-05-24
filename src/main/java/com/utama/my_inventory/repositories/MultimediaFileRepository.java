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
    List<MultimediaFile> findByProductIdAndFileType(Long productId, FileType fileType);
}