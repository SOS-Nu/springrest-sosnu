package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.Permission;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PermissionRepository;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public boolean isPermissionExist(Permission p) {
        return permissionRepository.existsByModuleAndApiPathAndMethod(
                p.getModule(),
                p.getApiPath(),
                p.getMethod());
    }

    @Cacheable(value = "permissions", key = "#a0")
    public Permission fetchById(Long id) {
        if (id == null)
            throw new IllegalArgumentException("Permission id must not be null");
        return permissionRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "permissions", key = "'allPermissions'")
    public List<Permission> fetchAllPermissions() {
        return permissionRepository.findAll();
    }

    @CacheEvict(cacheNames = { "permissions", "user-permissions-v1" }, allEntries = true)
    public Permission create(Permission p) {
        return permissionRepository.save(p);
    }

    @CacheEvict(cacheNames = { "permissions", "user-permissions-v1" }, allEntries = true)
    public Permission update(Permission p) {
        if (p.getId() <= 0)
            throw new IllegalArgumentException("Permission id must be > 0");

        Permission permissionDB = fetchById(p.getId());
        permissionDB.setName(p.getName());
        permissionDB.setApiPath(p.getApiPath());
        permissionDB.setMethod(p.getMethod());
        permissionDB.setModule(p.getModule());
        return permissionRepository.save(permissionDB);
    }

    @CacheEvict(cacheNames = { "permissions", "user-permissions-v1" }, allEntries = true)
    public void delete(long id) {
        if (id <= 0)
            throw new IllegalArgumentException("Permission id must be > 0");
        permissionRepository.deleteById(id);
    }

    public ResultPaginationDTO getPermissions(Specification<Permission> spec, Pageable pageable) {
        Page<Permission> pPermissions = this.permissionRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pPermissions.getTotalPages());
        mt.setTotal(pPermissions.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pPermissions.getContent());
        return rs;
    }

    public boolean isSameName(Permission p) {
        Permission permissionDB = this.fetchById(p.getId());
        if (permissionDB != null) {
            if (permissionDB.getName().equals(p.getName()))
                return true;
        }
        return false;
    }

}
