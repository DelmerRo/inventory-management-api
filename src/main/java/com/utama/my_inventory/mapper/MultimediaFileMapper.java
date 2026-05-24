package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.entities.MultimediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MultimediaFileMapper {

    @Mapping(target = "fileType", expression = "java(file.getFileType() != null ? file.getFileType().name() : null)")
    MultimediaFileResponseDTO toResponseDTO(MultimediaFile file);

    List<MultimediaFileResponseDTO> toResponseDTOList(List<MultimediaFile> files);
}