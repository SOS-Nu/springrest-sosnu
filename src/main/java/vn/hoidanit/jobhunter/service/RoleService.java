package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.dto.PermissionDTO;
import vn.hoidanit.jobhunter.domain.dto.RoleDTO;
import vn.hoidanit.jobhunter.domain.entity.Permission;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.util.mapper.RoleMapper;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
            RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleMapper = roleMapper;
    }

    @Cacheable(cacheNames = "role-by-id-v1", key = "#a0", unless = "#result == null")
    @Transactional(readOnly = true)
    public RoleDTO fetchById(Long id) {
        return roleRepository.findOneWithPermissionsById(id)
                .map(roleMapper::toDto)
                .orElse(null);
    }

    @Cacheable(cacheNames = "role-exists-by-name-v1")
    public boolean existByName(String name) {
        return this.roleRepository.existsByName(name);
    }

    @Transactional // ghi thì có @Transactional mặc định readOnly=false
    @CacheEvict(value = { "role-by-id-v1", "role-exists-by-name-v1", "user-permissions-v1" }, allEntries = true)
    public RoleDTO create(Role r) {
        List<Permission> dbPermissions = permissionRepository.findByIdIn(
                r.getPermissions().stream().map(Permission::getId).collect(Collectors.toList()));
        r.setPermissions(dbPermissions);
        Role saved = roleRepository.save(r);
        return roleMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = { "role-by-id-v1", "role-exists-by-name-v1", "user-permissions-v1" }, allEntries = true)
    public RoleDTO update(Role r) {
        Role roleDB = roleRepository.findById(r.getId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        roleDB.setName(r.getName());
        roleDB.setDescription(r.getDescription());
        roleDB.setActive(r.isActive());
        List<Permission> dbPermissions = permissionRepository.findByIdIn(
                r.getPermissions().stream().map(Permission::getId).collect(Collectors.toList()));
        roleDB.setPermissions(dbPermissions);
        return roleMapper.toDto(roleRepository.save(roleDB));
    }

    @Transactional
    @CacheEvict(cacheNames = { "role-by-id-v1", "role-exists-by-name-v1", "user-permissions-v1" }, allEntries = true)
    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    // paging: trả Entity hay DTO đều được; theo JHipster thì trả DTO
    public ResultPaginationDTO getRoles(Specification<Role> spec, Pageable pageable) {
        Page<Role> pRole = this.roleRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pRole.getTotalPages());
        mt.setTotal(pRole.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(
                pRole.getContent().stream().map(roleMapper::toDto).collect(Collectors.toList()));
        return rs;
    }
}
