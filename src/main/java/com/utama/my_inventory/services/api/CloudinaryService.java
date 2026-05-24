package com.utama.my_inventory.services.api;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Sube un archivo a Cloudinary y retorna el public_id y la URL segura.
     * @param file archivo a subir
     * @param folder carpeta donde se guardará (ej: "products")
     * @return mapa con "publicId" y "url"
     */
    public Map<String, String> uploadFile(MultipartFile file, String folder) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    ));
            String publicId = (String) uploadResult.get("public_id");
            String url = (String) uploadResult.get("secure_url");
            log.info("Imagen subida a Cloudinary - publicId: {}, url: {}", publicId, url);
            return Map.of("publicId", publicId, "url", url);
        } catch (IOException e) {
            log.error("Error al subir archivo a Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo subir la imagen a Cloudinary", e);
        }
    }

    /**
     * Elimina una imagen de Cloudinary por su public_id.
     */
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Imagen eliminada de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Error al eliminar imagen de Cloudinary: {}", e.getMessage(), e);
        }
    }
}