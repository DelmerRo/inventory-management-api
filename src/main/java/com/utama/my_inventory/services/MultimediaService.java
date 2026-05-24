package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MultimediaService {

    MultimediaUploadResponseDTO uploadFile(Long productId, MultipartFile file, String fileType);

    List<MultimediaFileResponseDTO> getProductFilesByType(Long productId, String fileType);
}