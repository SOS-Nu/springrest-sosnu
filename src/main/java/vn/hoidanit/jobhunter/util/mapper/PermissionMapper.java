package vn.hoidanit.jobhunter.util.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import vn.hoidanit.jobhunter.domain.dto.PermissionDTO;
import vn.hoidanit.jobhunter.domain.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionDTO toDto(Permission permission);

    // List<PermissionDTO> toDto(List<Permission> permission);

}