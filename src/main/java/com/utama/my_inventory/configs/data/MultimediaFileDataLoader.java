package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.MultimediaFile;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.FileType;
import com.utama.my_inventory.repositories.MultimediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class MultimediaFileDataLoader {

    private final MultimediaFileRepository multimediaFileRepository;
    private final ProductDataLoader productDataLoader;
    private static final Random random = new Random();

    // URLs de imágenes de placeholder para productos de decoración
    private static final String[] IMAGE_URLS = {
            "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6", // Almohadones
            "https://images.unsplash.com/photo-1586023492125-27b2c045efd7", // Alfombras
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85", // Cortinas
            "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92", // Mantas
            "https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0", // Jarrones
            "https://images.unsplash.com/photo-1618220179428-22790b461013", // Velas
            "https://images.unsplash.com/photo-1519710164239-da123dc03ef4", // Cuadros
            "https://images.unsplash.com/photo-1556228720-195a672e8a03", // Lámparas
            "https://images.unsplash.com/photo-1595433562696-a8b1cb8b60c6", // Textiles
            "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136"  // Decoración
    };

    private static final String[] FILE_NAMES = {
            "producto_principal.jpg",
            "detalle_textura.jpg",
            "color_variante.jpg",
            "uso_en_interior.jpg",
            "dimensiones.jpg",
            "materiales.jpg",
            "instalacion.jpg",
            "mantenimiento.jpg",
            "garantia.pdf",
            "especificaciones.pdf"
    };

    @Transactional
    public void load() {
        if (multimediaFileRepository.count() > 0) {
            return;
        }

        // Asegurar que productos existen
        if (productDataLoader.getProductRepository().count() == 0) {
            productDataLoader.load();
        }

        List<MultimediaFile> files = new ArrayList<>();
        List<Product> allProducts = productDataLoader.getProductRepository().findAll();

        // Crear 2-4 archivos por producto
        for (Product product : allProducts) {
            int fileCount = random.nextInt(3) + 2; // 2-4 archivos

            // Siempre al menos una imagen principal
            MultimediaFile mainImage = createMainImage(product);
            files.add(mainImage);

            // Archivos adicionales
            for (int i = 0; i < fileCount - 1; i++) {
                MultimediaFile additionalFile = createAdditionalFile(product, i + 1);
                files.add(additionalFile);
            }
        }

        multimediaFileRepository.saveAll(files);
        System.out.println("✅ Archivos multimedia creados: " + files.size());

        // Mostrar resumen
        printFileSummary(files);
    }

    @Transactional
    public void loadEssential() {
        if (multimediaFileRepository.count() > 0) {
            return;
        }

        // Solo archivos esenciales
        List<MultimediaFile> essentialFiles = new ArrayList<>();
        List<Product> products = productDataLoader.getProductRepository().findAll();

        // Tomar solo 10 productos para archivos esenciales
        int limit = Math.min(10, products.size());
        for (int i = 0; i < limit; i++) {
            Product product = products.get(i);

            MultimediaFile mainImage = MultimediaFile.builder()
                    .product(product)
                    .fileType(FileType.IMAGE)
                    .fileUrl(IMAGE_URLS[i % IMAGE_URLS.length])
                    .fileName("producto_principal.jpg")
                    .build();

            essentialFiles.add(mainImage);
        }

        multimediaFileRepository.saveAll(essentialFiles);
        System.out.println("✅ Archivos multimedia esenciales creados: " + essentialFiles.size());
    }

    private MultimediaFile createMainImage(Product product) {
        int imageIndex = random.nextInt(IMAGE_URLS.length);
        String productType = getProductTypeForImage(product);

        return MultimediaFile.builder()
                .product(product)
                .fileType(FileType.IMAGE)
                .fileUrl(IMAGE_URLS[imageIndex] + "?q=80&w=1200&auto=format&fit=crop&" + productType)
                .fileName(product.getSku().toLowerCase() + "_principal.jpg")
                .build();
    }

    private MultimediaFile createAdditionalFile(Product product, int fileNumber) {
        FileType fileType = getRandomFileType(fileNumber);

        return MultimediaFile.builder()
                .product(product)
                .fileType(fileType)
                .fileUrl(generateFileUrl(product, fileType, fileNumber))
                .fileName(generateFileName(product, fileType, fileNumber))
                .build();
    }

    private FileType getRandomFileType(int fileNumber) {
        // Primero más imágenes, luego documentos, raramente videos
        double randomValue = random.nextDouble();

        if (fileNumber <= 2 || randomValue < 0.7) {
            return FileType.IMAGE;
        } else if (randomValue < 0.9) {
            return FileType.DOCUMENT;
        } else {
            return FileType.VIDEO;
        }
    }

    private String generateFileUrl(Product product, FileType fileType, int fileNumber) {
        switch (fileType) {
            case IMAGE:
                int imageIndex = random.nextInt(IMAGE_URLS.length);
                String productType = getProductTypeForImage(product);
                return IMAGE_URLS[imageIndex] + "?q=80&w=800&auto=format&fit=crop&" +
                        productType + "&file=" + fileNumber;

            case DOCUMENT:
                return "/documents/" + product.getSku().toLowerCase() +
                        "_" + getDocumentType(fileNumber) + ".pdf";

            case VIDEO:
                return "https://videos.unsplash.com/video-" +
                        (random.nextInt(9000) + 1000) + ".mp4";

            default:
                return "/files/" + product.getSku().toLowerCase() + "_extra";
        }
    }

    private String generateFileName(Product product, FileType fileType, int fileNumber) {
        String baseName = product.getSku().toLowerCase();

        switch (fileType) {
            case IMAGE:
                return baseName + "_" + getImageType(fileNumber) + ".jpg";

            case DOCUMENT:
                return baseName + "_" + getDocumentType(fileNumber) + ".pdf";

            case VIDEO:
                return baseName + "_video_demo.mp4";

            default:
                return baseName + "_archivo_" + fileNumber;
        }
    }

    private String getProductTypeForImage(Product product) {
        String category = product.getSubcategory().getCategory().getName().toLowerCase();
        String subcategory = product.getSubcategory().getName().toLowerCase();

        if (subcategory.contains("alfombra")) return "carpet";
        if (subcategory.contains("almohadon")) return "cushion";
        if (subcategory.contains("cortina")) return "curtain";
        if (subcategory.contains("manta")) return "blanket";
        if (subcategory.contains("vela")) return "candle";
        if (subcategory.contains("jarrón")) return "vase";
        if (subcategory.contains("cuadro")) return "painting";
        if (subcategory.contains("lámpara")) return "lamp";
        return "home";
    }

    private String getImageType(int fileNumber) {
        String[] types = {"principal", "detalle", "color", "uso", "ambiente", "textura"};
        return types[Math.min(fileNumber - 1, types.length - 1)];
    }

    private String getDocumentType(int fileNumber) {
        String[] types = {"especificaciones", "garantia", "instrucciones", "medidas", "materiales"};
        return types[Math.min(fileNumber - 1, types.length - 1)];
    }

    private void printFileSummary(List<MultimediaFile> files) {
        long imageCount = files.stream()
                .filter(f -> f.getFileType() == FileType.IMAGE)
                .count();

        long documentCount = files.stream()
                .filter(f -> f.getFileType() == FileType.DOCUMENT)
                .count();

        long videoCount = files.stream()
                .filter(f -> f.getFileType() == FileType.VIDEO)
                .count();

        System.out.println("\n📊 RESUMEN DE ARCHIVOS MULTIMEDIA:");
        System.out.println("================================");
        System.out.println("Total archivos: " + files.size());
        System.out.println("Imágenes (IMAGE): " + imageCount);
        System.out.println("Documentos (DOCUMENT): " + documentCount);
        System.out.println("Videos (VIDEO): " + videoCount);
        System.out.println("================================");
    }
}