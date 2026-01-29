package com.utama.my_inventory.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.utama.my_inventory.dtos.request.multimedia.MultimediaCreateRequestDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import com.utama.my_inventory.entities.MultimediaFile;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.FileType;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.MultimediaFileMapper;
import com.utama.my_inventory.repositories.MultimediaFileRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.MultimediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultimediaServiceImpl implements MultimediaService {

    private final MultimediaFileRepository fileRepository;
    private final ProductRepository productRepository;
    private final MultimediaFileMapper fileMapper;
    private final Cloudinary cloudinary;

    // Constantes
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024; // 20MB
    private static final long DEFAULT_MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");
    private static final String INVALID_CHARS_REGEX = "[\\\\/:*?\"<>|]";

    @Override
    @Transactional
    public MultimediaUploadResponseDTO uploadFile(Long productId, MultipartFile file, String fileType) {
        log.info("Uploading file for product ID: {}, type: {}, filename: {}",
                productId, fileType, file.getOriginalFilename());

        // Validaciones
        Product product = findActiveProductById(productId);
        FileType type = validateAndParseFileType(fileType);
        validateFile(file, type);

        try {
            // Configuración para Cloudinary
            String folder = "uploads/utama/products/" + productId;
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "use_filename", true,
                    "unique_filename", false,
                    "overwrite", true,
                    "resource_type", "auto"
            );

            // Subir a Cloudinary con cast seguro
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

            // Extraer información del resultado
            String fileUrl = extractString(uploadResult, "url");
            String secureUrl = extractString(uploadResult, "secure_url");
            String publicId = extractString(uploadResult, "public_id");
            String format = extractString(uploadResult, "format");
            Long fileSize = file.getSize();
            Integer width = extractInteger(uploadResult, "width");
            Integer height = extractInteger(uploadResult, "height");

            // Crear y guardar entidad
            MultimediaFile multimediaFile = MultimediaFile.builder()
                    .product(product)
                    .fileType(type)
                    .fileUrl(secureUrl)
                    .fileName(file.getOriginalFilename())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            MultimediaFile savedFile = fileRepository.save(multimediaFile);

            log.info("File uploaded successfully. File ID: {}, URL: {}", savedFile.getId(), secureUrl);

            // Crear respuesta
            return MultimediaUploadResponseDTO.builder()
                    .fileId(savedFile.getId())
                    .fileUrl(fileUrl)
                    .secureUrl(secureUrl)
                    .fileName(file.getOriginalFilename())
                    .fileSize(fileSize)
                    .mimeType(file.getContentType())
                    .publicId(publicId)
                    .format(format)
                    .message("Archivo subido exitosamente")
                    .build();

        } catch (Exception e) {
            log.error("Error uploading file to Cloudinary: {}", e.getMessage());
            throw new BusinessException("Error al subir el archivo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MultimediaFileResponseDTO createFromUrl(Long productId, MultimediaCreateRequestDTO requestDTO) {
        log.info("Creating multimedia file from URL for product ID: {}", productId);

        // Validar producto
        Product product = findActiveProductById(productId);

        // Validar que la URL no exista ya para este producto
        if (fileRepository.existsByProductIdAndFileUrl(productId, requestDTO.fileUrl())) {
            throw new BusinessException("Ya existe un archivo con esta URL para este producto");
        }

        // Crear entidad
        MultimediaFile multimediaFile = MultimediaFile.builder()
                .product(product)
                .fileType(requestDTO.fileType())
                .fileUrl(requestDTO.fileUrl())
                .fileName(requestDTO.fileName())
                .uploadedAt(LocalDateTime.now())
                .build();

        // Validar tipo de archivo
        if (!multimediaFile.validateFileType()) {
            throw new BusinessException("La extensión del archivo no coincide con el tipo especificado");
        }

        // Guardar en base de datos
        MultimediaFile savedFile = fileRepository.save(multimediaFile);

        log.info("Multimedia file created from URL. File ID: {}", savedFile.getId());
        return fileMapper.toResponseDTO(savedFile);
    }

    @Override
    @Transactional(readOnly = true)
    public MultimediaFileResponseDTO getFileById(Long fileId) {
        log.info("Getting multimedia file by ID: {}", fileId);

        MultimediaFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo multimedia no encontrado con ID: " + fileId));

        return fileMapper.toResponseDTO(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultimediaFileResponseDTO> getProductFiles(Long productId) {
        log.info("Getting all multimedia files for product ID: {}", productId);

        findActiveProductById(productId); // Solo para validar que existe
        List<MultimediaFile> files = fileRepository.findByProductIdOrderByUploadedAtDesc(productId);
        return fileMapper.toResponseDTOList(files);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultimediaFileResponseDTO> getProductFilesByType(Long productId, String fileType) {
        log.info("Getting multimedia files for product ID: {}, type: {}", productId, fileType);

        findActiveProductById(productId); // Solo para validar que existe
        FileType type = FileType.valueOf(fileType.toUpperCase());
        List<MultimediaFile> files = fileRepository.findByProductIdAndFileTypeOrderByUploadedAtDesc(productId, type);
        return fileMapper.toResponseDTOList(files);
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        log.info("Deleting multimedia file with ID: {}", fileId);

        MultimediaFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo multimedia no encontrado con ID: " + fileId));

        // Eliminar de Cloudinary si corresponde
        if (file.getFileUrl().contains("cloudinary.com")) {
            try {
                String publicId = extractPublicIdFromUrl(file.getFileUrl());
                deleteFromCloudinary(publicId);
                log.info("File deleted from Cloudinary. Public ID: {}", publicId);
            } catch (Exception e) {
                log.warn("Could not delete file from Cloudinary: {}", e.getMessage());
                // Continuar con la eliminación de la base de datos
            }
        }

        fileRepository.delete(file);
        log.info("Multimedia file deleted. ID: {}", fileId);
    }

    @Override
    @Transactional
    public void deleteProductFiles(Long productId) {
        log.info("Deleting all multimedia files for product ID: {}", productId);

        List<MultimediaFile> files = fileRepository.findByProductIdOrderByUploadedAtDesc(productId);

        // Eliminar archivos de Cloudinary
        files.forEach(file -> {
            if (file.getFileUrl().contains("cloudinary.com")) {
                try {
                    String publicId = extractPublicIdFromUrl(file.getFileUrl());
                    deleteFromCloudinary(publicId);
                    log.debug("File deleted from Cloudinary. Public ID: {}", publicId);
                } catch (Exception e) {
                    log.warn("Could not delete file from Cloudinary: {}", e.getMessage());
                }
            }
        });

        fileRepository.deleteAllByProductId(productId);
        log.info("All multimedia files deleted for product ID: {}", productId);
    }

    @Override
    public boolean isValidFileType(MultipartFile file, String fileType) {
        if (file == null || file.getOriginalFilename() == null) return false;

        String fileName = file.getOriginalFilename().toLowerCase();
        String extension = getFileExtension(fileName);

        return switch (FileType.valueOf(fileType.toUpperCase())) {
            case IMAGE -> IMAGE_EXTENSIONS.contains(extension);
            case VIDEO -> VIDEO_EXTENSIONS.contains(extension);
            case DOCUMENT -> DOCUMENT_EXTENSIONS.contains(extension);
        };
    }

    @Override
    public boolean isValidFileSize(MultipartFile file) {
        if (file == null) return false;

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName != null ? fileName : "");

        long maxSize = switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> MAX_IMAGE_SIZE;
            case "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm" -> MAX_VIDEO_SIZE;
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt" -> MAX_DOCUMENT_SIZE;
            default -> DEFAULT_MAX_SIZE;
        };

        return file.getSize() <= maxSize;
    }

    @Override
    public boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        return !fileName.matches(".*" + INVALID_CHARS_REGEX + ".*");
    }

    @Override
    @Transactional(readOnly = true)
    public MultimediaFileResponseDTO toResponseDTOWithMetadata(Long fileId) {
        MultimediaFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));

        MultimediaFileResponseDTO response = fileMapper.toResponseDTO(file);

        // Obtener metadata adicional de Cloudinary si es necesario
        if (file.getFileUrl().contains("cloudinary.com")) {
            try {
                String publicId = extractPublicIdFromUrl(file.getFileUrl());
                Map<String, Object> metadata = getCloudinaryFileInfo(publicId);
                log.debug("Cloudinary metadata for file {}: {}", fileId, metadata);
            } catch (Exception e) {
                log.warn("Could not fetch Cloudinary metadata: {}", e.getMessage());
            }
        }

        return response;
    }

    @Override
    public Map<String, Object> uploadToCloudinary(MultipartFile file, String folder) {
        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "use_filename", true,
                    "unique_filename", false,
                    "overwrite", true,
                    "resource_type", "auto"
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return uploadResult;

        } catch (Exception e) {
            log.error("Error uploading to Cloudinary: {}", e.getMessage());
            throw new BusinessException("Error al subir archivo a Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getCloudinaryFileInfo(String publicId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fileInfo = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            return fileInfo;
        } catch (Exception e) {
            log.error("Error getting Cloudinary file info: {}", e.getMessage());
            throw new BusinessException("Error al obtener información del archivo: " + e.getMessage());
        }
    }

    @Override
    public void deleteFromCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("File deleted from Cloudinary. Public ID: {}", publicId);
        } catch (Exception e) {
            log.error("Error deleting from Cloudinary: {}", e.getMessage());
            throw new BusinessException("Error al eliminar archivo de Cloudinary: " + e.getMessage());
        }
    }

    // ========== MÉTODOS PRIVADOS HELPER ==========

    private Product findActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado o inactivo con ID: " + id
                ));
    }

    private FileType validateAndParseFileType(String fileType) {
        try {
            return FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de archivo inválido: " + fileType);
        }
    }

    private void validateFile(MultipartFile file, FileType fileType) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("El archivo no tiene nombre");
        }

        if (!isValidFileName(originalFilename)) {
            throw new BusinessException("El nombre del archivo contiene caracteres no permitidos");
        }

        if (!isValidFileType(file, fileType.name())) {
            throw new BusinessException("El tipo de archivo no es compatible con el tipo especificado");
        }

        if (!isValidFileSize(file)) {
            throw new BusinessException("El tamaño del archivo excede el límite permitido");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                if (path.startsWith("v")) {
                    path = path.substring(path.indexOf("/") + 1);
                }
                if (path.contains(".")) {
                    path = path.substring(0, path.lastIndexOf("."));
                }
                return path;
            }
        } catch (Exception e) {
            log.warn("Could not extract public ID from URL: {}", url);
        }
        throw new BusinessException("No se pudo extraer el ID público de la URL");
    }

    // Métodos helper para extraer datos de Map de forma segura
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}