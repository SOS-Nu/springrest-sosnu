package vn.hoidanit.jobhunter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.entity.Skill;
import vn.hoidanit.jobhunter.domain.entity.SkillBulkCreateDTO;
import vn.hoidanit.jobhunter.domain.response.ResBulkCreateSkillDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.SkillRepository;

@Service
public class SkillService {
    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public boolean isNameExist(String name) {
        return this.skillRepository.existsByName(name);
    }

    public Skill fetchSkillById(long id) {
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        if (skillOptional.isPresent())
            return skillOptional.get();
        return null;
    }

    public Skill createSkill(Skill s) {
        return this.skillRepository.save(s);
    }

    @Transactional
    public ResBulkCreateSkillDTO handleBulkCreateSkills(List<SkillBulkCreateDTO> skillDTOs) {
        int total = skillDTOs.size();
        int success = 0;
        List<String> failedSkills = new ArrayList<>();

        for (SkillBulkCreateDTO dto : skillDTOs) {
            try {
                // Kiểm tra skill trùng (dựa trên name)
                if (this.skillRepository.existsByName(dto.getName())) {
                    failedSkills.add(dto.getName() + " (Skill đã tồn tại)");
                    continue;
                }

                Skill skill = new Skill();
                skill.setName(dto.getName());

                // Lưu skill
                this.skillRepository.save(skill);
                success++;
            } catch (Exception e) {
                failedSkills.add(dto.getName() + " (Lỗi hệ thống: " + e.getMessage() + ")");
            }
        }

        return new ResBulkCreateSkillDTO(total, success, total - success, failedSkills);
    }

    public Skill updateSkill(Skill s) {
        return this.skillRepository.save(s);
    }

    public void deleteSkill(long id) {
        // delete job (inside job_skill table)
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        Skill currentSkill = skillOptional.get();
        currentSkill.getJobs().forEach(job -> job.getSkills().remove(currentSkill));

        // delete subscriber (inside subscriber_skill table)
        currentSkill.getSubscribers().forEach(subs -> subs.getSkills().remove(currentSkill));

        // delete skill
        this.skillRepository.delete(currentSkill);
    }

    public ResultPaginationDTO fetchAllSkills(Specification<Skill> spec, Pageable pageable) {
        Page<Skill> pageUser = this.skillRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);

        rs.setResult(pageUser.getContent());

        return rs;
    }
}
