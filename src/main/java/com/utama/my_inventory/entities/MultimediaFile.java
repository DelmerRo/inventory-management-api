package com.utama.my_inventory.entities;

import com.utama.my_inventory.entities.enums.FileType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "multimedia_files",
        indexes = {
                @Index(name = "idx_file_product", columnList = "product_id"),
                @Index(name = "idx_file_type", columnList = "file_type")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimediaFile {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_file_product"))
    private Product product;

    @NotNull(message = "Tipo de archivo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @NotBlank(message = "URL es obligatoria")
    @Pattern(regexp = "^(https?|ftp|file)://.+$|^/[^/].*$",
            message = "URL debe ser válida (http://, https://, /ruta/)")
    @Size(max = 500, message = "URL no puede exceder 500 caracteres")
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @NotBlank(message = "Nombre de archivo es obligatorio")
    @Size(max = 255, message = "Nombre no puede exceder 255 caracteres")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public String getMimeType() {
        String ext = getFileExtension();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }

    public boolean validateFileType() {
        String ext = getFileExtension();
        return switch (fileType) {
            case IMAGE -> IMAGE_EXTENSIONS.contains(ext);
            case VIDEO -> VIDEO_EXTENSIONS.contains(ext);
            case DOCUMENT -> DOCUMENT_EXTENSIONS.contains(ext);
            case OTHER -> true;
        };
    }
}
