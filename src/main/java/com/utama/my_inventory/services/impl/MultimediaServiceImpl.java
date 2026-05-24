package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import com.utama.my_inventory.entities.MultimediaFile;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.FileType;
import com.utama.my_inventory.repositories.MultimediaFileRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.MultimediaService;
import com.utama.my_inventory.services.api.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultimediaServiceImpl implements MultimediaService {

    private final MultimediaFileRepository multimediaFileRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public MultimediaUploadResponseDTO uploadFile(Long productId, MultipartFile file, String fileType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // ✅ Eliminar imágenes anteriores del producto (si quieres solo una imagen)
        FileType type = FileType.valueOf(fileType.toUpperCase());
        List<MultimediaFile> existingFiles = multimediaFileRepository.findByProductIdAndFileType(productId, type);

        // Eliminar imágenes anteriores de Cloudinary
        for (MultimediaFile existing : existingFiles) {
            if (existing.getCloudinaryPublicId() != null) {
                cloudinaryService.deleteFile(existing.getCloudinaryPublicId());
            }
            multimediaFileRepository.delete(existing);
        }

        Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "products/" + productId);

        MultimediaFile multimediaFile = MultimediaFile.builder()
                .product(product)
                .fileName(file.getOriginalFilename())
                .fileType(type)
                .cloudinaryPublicId(uploadResult.get("publicId"))
                .fileUrl(uploadResult.get("url"))
                .fileFormat(file.getContentType())
                .build();

        multimediaFile = multimediaFileRepository.save(multimediaFile);

        return new MultimediaUploadResponseDTO(
                multimediaFile.getId(),
                multimediaFile.getFileUrl(),
                multimediaFile.getCloudinaryPublicId(),
                "Imagen subida exitosamente"
        );
    }

    @Override
    public List<MultimediaFileResponseDTO> getProductFilesByType(Long productId, String fileType) {
        FileType type = FileType.valueOf(fileType.toUpperCase());
        List<MultimediaFile> files = multimediaFileRepository.findByProductIdAndFileType(productId, type);

        return files.stream()
                .map(file -> new MultimediaFileResponseDTO(
                        file.getId(),               // id
                        file.getFileName(),         // fileName
                        file.getFileUrl(),          // fileUrl
                        file.getFileType().name(),  // fileType
                        file.getCreatedAt()         // createdAt
                ))
                .toList();
    }
}