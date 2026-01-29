package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.entities.MultimediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MultimediaFileMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "secureUrl", source = "fileUrl")
    @Mapping(target = "fileSize", ignore = true) // Se calculará en el servicio
    @Mapping(target = "width", ignore = true) // Se obtendrá de Cloudinary
    @Mapping(target = "height", ignore = true) // Se obtendrá de Cloudinary
    MultimediaFileResponseDTO toResponseDTO(MultimediaFile file);

    List<MultimediaFileResponseDTO> toResponseDTOList(List<MultimediaFile> files);
}