package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.multimedia.MultimediaCreateRequestDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MultimediaService {

    // Subida de archivos
    MultimediaUploadResponseDTO uploadFile(Long productId, MultipartFile file, String fileType);
    MultimediaFileResponseDTO createFromUrl(Long productId, MultimediaCreateRequestDTO requestDTO);

    // Operaciones CRUD
    MultimediaFileResponseDTO getFileById(Long fileId);
    List<MultimediaFileResponseDTO> getProductFiles(Long productId);
    List<MultimediaFileResponseDTO> getProductFilesByType(Long productId, String fileType);
    void deleteFile(Long fileId);
    void deleteProductFiles(Long productId);

    // Validaciones
    boolean isValidFileType(MultipartFile file, String fileType);
    boolean isValidFileSize(MultipartFile file);
    boolean isValidFileName(String fileName);

    // Transformaciones
    MultimediaFileResponseDTO toResponseDTOWithMetadata(Long fileId);

    // Cloudinary operations
    Map<String, Object> uploadToCloudinary(MultipartFile file, String folder);
    Map<String, Object> getCloudinaryFileInfo(String publicId);
    void deleteFromCloudinary(String publicId);
}