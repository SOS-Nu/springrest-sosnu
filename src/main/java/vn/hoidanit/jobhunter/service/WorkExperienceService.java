package vn.hoidanit.jobhunter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.entity.WorkExperience;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.repository.WorkExperienceRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import java.util.List;

@Service
public class WorkExperienceService {

    private final WorkExperienceRepository workExperienceRepository;
    private final UserRepository userRepository;

    public WorkExperienceService(WorkExperienceRepository workExperienceRepository, UserRepository userRepository) {
        this.workExperienceRepository = workExperienceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkExperience createWorkExperience(WorkExperience workExperience) throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);

        workExperience.setUser(currentUser);
        return workExperienceRepository.save(workExperience);
    }

    @Transactional
    public WorkExperience updateWorkExperience(WorkExperience workExperience) throws IdInvalidException {
        long id = workExperience.getId();
        WorkExperience existingExp = workExperienceRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy kinh nghiệm làm việc với id: " + id));

        // Kiểm tra quyền sở hữu
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        if (!existingExp.getUser().getEmail().equals(currentUserEmail)) {
            throw new IdInvalidException("Bạn không có quyền chỉnh sửa kinh nghiệm làm việc này.");
        }

        // Cập nhật các trường
        existingExp.setCompanyName(workExperience.getCompanyName());
        existingExp.setStartDate(workExperience.getStartDate());
        existingExp.setEndDate(workExperience.getEndDate());
        existingExp.setDescription(workExperience.getDescription());
        existingExp.setLocation(workExperience.getLocation());

        return workExperienceRepository.save(existingExp);
    }

    @Transactional
    public void deleteWorkExperience(long id) throws IdInvalidException {
        WorkExperience existingExp = workExperienceRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy kinh nghiệm làm việc với id: " + id));

        // Kiểm tra quyền sở hữu
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        if (!existingExp.getUser().getEmail().equals(currentUserEmail)) {
            throw new IdInvalidException("Bạn không có quyền xóa kinh nghiệm làm việc này.");
        }

        workExperienceRepository.deleteById(id);
    }

    public List<WorkExperience> getMyWorkExperiences() throws IdInvalidException {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng đang đăng nhập"));
        User currentUser = userRepository.findByEmail(currentUserEmail);

        return workExperienceRepository.findByUserId(currentUser.getId());
    }
}
