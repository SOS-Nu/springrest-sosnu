package vn.hoidanit.jobhunter.util.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import vn.hoidanit.jobhunter.domain.dto.RoleDTO;
import vn.hoidanit.jobhunter.domain.entity.Role;

@Mapper(componentModel = "spring", uses = { PermissionMapper.class })
public interface RoleMapper {
    RoleDTO toDto(Role role);

    // List<RoleDTO> toDtoList(List<Role> roles);
}
