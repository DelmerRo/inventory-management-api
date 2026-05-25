package com.utama.my_inventory.entities;

import com.utama.my_inventory.entities.enums.FileType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "multimedia_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;

    @Column(name = "cloudinary_public_id", length = 150)
    private String cloudinaryPublicId;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)  // ✅ Especificar el nombre real de la columna
    private LocalDateTime createdAt;
}