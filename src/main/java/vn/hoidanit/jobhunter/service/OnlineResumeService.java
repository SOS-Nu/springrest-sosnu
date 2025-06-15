package vn.hoidanit.jobhunter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoidanit.jobhunter.domain.entity.OnlineResume;
import vn.hoidanit.jobhunter.domain.entity.Skill;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.repository.OnlineResumeRepository;
import vn.hoidanit.jobhunter.repository.SkillRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OnlineResumeService {

    private final OnlineResumeRepository onlineResumeRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public OnlineResumeService(OnlineResumeRepository onlineResumeRepository, UserRepository userRepository, SkillRepository skillRepository) {
        this.onlineResumeRepository = onlineResumeRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public OnlineResume createOnlineResume(OnlineResume resume) throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);

        if (currentUser.getOnlineResume() != null) {
            throw new IdInvalidException("Mỗi người dùng chỉ có thể tạo một Online Resume. Bạn đã có, vui lòng chỉnh sửa.");
        }
        
        // Xử lý skills
        if (resume.getSkills() != null) {
            List<Long> reqSkillIds = resume.getSkills().stream().map(Skill::getId).collect(Collectors.toList());
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkillIds);
            resume.setSkills(dbSkills);
        }

        resume.setUser(currentUser);
        OnlineResume savedResume = onlineResumeRepository.save(resume);

        currentUser.setOnlineResume(savedResume);
        userRepository.save(currentUser);

        return savedResume;
    }

    @Transactional
    public OnlineResume updateOnlineResume(OnlineResume resume) throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);

        if (currentUser.getOnlineResume() == null) {
            throw new IdInvalidException("Bạn chưa có Online Resume để cập nhật.");
        }

        long resumeIdToUpdate = resume.getId();
        if (currentUser.getOnlineResume().getId() != resumeIdToUpdate) {
            throw new IdInvalidException("Bạn không có quyền chỉnh sửa Online Resume này.");
        }

        OnlineResume existingResume = this.onlineResumeRepository.findById(resumeIdToUpdate)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Online Resume với id: " + resumeIdToUpdate));

        // Cập nhật các trường
        existingResume.setTitle(resume.getTitle());
        existingResume.setFullName(resume.getFullName());
        existingResume.setEmail(resume.getEmail());
        existingResume.setPhone(resume.getPhone());
        existingResume.setAddress(resume.getAddress());
        existingResume.setSummary(resume.getSummary());
        existingResume.setCertifications(resume.getCertifications());
        existingResume.setEducations(resume.getEducations());
        existingResume.setLanguages(resume.getLanguages());

        // Xử lý skills
        if (resume.getSkills() != null) {
            List<Long> reqSkillIds = resume.getSkills().stream().map(Skill::getId).collect(Collectors.toList());
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkillIds);
            existingResume.setSkills(dbSkills);
        } else {
            existingResume.setSkills(null);
        }

        return onlineResumeRepository.save(existingResume);
    }

    @Transactional
    public void deleteOnlineResume() throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);

        if (currentUser.getOnlineResume() == null) {
            throw new IdInvalidException("Bạn không có Online Resume để xóa.");
        }

        long resumeIdToDelete = currentUser.getOnlineResume().getId();

        // Ngắt kết nối từ User
        currentUser.setOnlineResume(null);
        userRepository.save(currentUser);

        // Xóa OnlineResume
        onlineResumeRepository.deleteById(resumeIdToDelete);
    }

    public Optional<OnlineResume> getMyOnlineResume() throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);
        
        if (currentUser.getOnlineResume() == null) {
             return Optional.empty();
        }

        return this.onlineResumeRepository.findById(currentUser.getOnlineResume().getId());
    }
}
