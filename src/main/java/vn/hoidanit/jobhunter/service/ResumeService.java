package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.Resume;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResFetchResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.ResumeRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@Service
public class ResumeService {

    @Autowired
    FilterBuilder fb;

    @Autowired
    private FilterParser filterParser;

    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final UserService userService;
    private final GeminiService geminiService;
    private final FileService fileService;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            UserService userService, JobRepository jobRepository,GeminiService geminiService, FileService fileService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.userService = userService;
        this.geminiService = geminiService;
        this.fileService = fileService;

    }

    public Optional<Resume> fetchById(long id) {
        return this.resumeRepository.findById(id);
    }

    public boolean checkResumeExistByUserAndJob(Resume resume) {
        // check user by id
        if (resume.getUser() == null)
            return false;
        Optional<User> userOptional = this.userRepository.findById(resume.getUser().getId());
        if (userOptional.isEmpty())
            return false;

        // check job by id
        if (resume.getJob() == null)
            return false;
        Optional<Job> jobOptional = this.jobRepository.findById(resume.getJob().getId());
        if (jobOptional.isEmpty())
            return false;

        return true;
    }

    @Transactional
    public ResCreateResumeDTO create(Resume resume) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập để rải CV"));

        if (!userService.canSubmitCv(email)) {
            throw new IdInvalidException("Bạn đã hết lượt rải CV trong tháng này. Hãy nâng cấp lên VIP để rải thêm!");
        }

        // Lấy thông tin Job đầy đủ
        Job job = this.jobRepository.findById(resume.getJob().getId())
                    .orElseThrow(() -> new IdInvalidException("Job với id=" + resume.getJob().getId() + " không tồn tại"));
        
        // Gán lại Job và User đầy đủ cho đối tượng resume
        resume.setJob(job);
        this.userRepository.findById(resume.getUser().getId()).ifPresent(resume::setUser);

        // LOGIC CHẤM ĐIỂM
        int score = 0;
        String cvFileName = resume.getUrl();
        System.out.println("Bắt đầu chấm điểm cho CV: " + cvFileName);

        if (cvFileName == null || cvFileName.isEmpty()) {
            System.out.println("CV không có file đính kèm (url is null). Bỏ qua chấm điểm.");
        } else {
            try {
                // SỬA LỖI TẠI ĐÂY: đổi "resumes" thành "resume"
                byte[] cvFileBytes = this.fileService.readFileAsBytes(cvFileName, "resume"); 
                if (cvFileBytes != null) {
                    System.out.println("Đã đọc file CV thành công. Đang gửi tới Gemini để chấm điểm...");
                    score = this.geminiService.scoreCvAgainstJob(job, cvFileBytes, cvFileName);
                    System.out.println("Gemini đã trả về điểm số: " + score);
                } else {
                     System.out.println("Không tìm thấy file CV '" + cvFileName + "' trong thư mục storage/resume.");
                }
            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng khi chấm điểm CV bằng Gemini cho job " + job.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        resume.setScore(score);

        // Lưu resume với điểm số vào DB
        Resume savedResume = this.resumeRepository.save(resume);
        System.out.println("Đã lưu Resume vào DB với id=" + savedResume.getId() + " và score=" + savedResume.getScore());
        
        userService.incrementCvSubmission(email);

        // Trả về DTO
        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(savedResume.getId());
        res.setCreatedBy(savedResume.getCreatedBy());
        res.setCreatedAt(savedResume.getCreatedAt());

        return res;
    }

    public ResUpdateResumeDTO update(Resume resume) {
        resume = this.resumeRepository.save(resume);
        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        return res;
    }

    public void delete(long id) {
        this.resumeRepository.deleteById(id);
    }

     public ResFetchResumeDTO getResume(Resume resume) {
        ResFetchResumeDTO res = new ResFetchResumeDTO();
        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setUrl(resume.getUrl());
        res.setStatus(resume.getStatus());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        res.setScore(resume.getScore()); // Thêm điểm

        if (resume.getJob() != null) {
            res.setCompanyName(resume.getJob().getCompany().getName());
        }

        res.setUser(new ResFetchResumeDTO.UserResume(resume.getUser().getId(), resume.getUser().getName()));
        res.setJob(new ResFetchResumeDTO.JobResume(resume.getJob().getId(), resume.getJob().getName()));

        return res;
    }

    public ResultPaginationDTO fetchAllResume(Specification<Resume> spec, Pageable pageable) {
        Page<Resume> pageUser = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);

        // remove sensitive data
        List<ResFetchResumeDTO> listResume = pageUser.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        rs.setResult(listResume);

        return rs;
    }

    public ResultPaginationDTO fetchResumeByUser(Pageable pageable) {
        // query builder
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        FilterNode node = filterParser.parse("email='" + email + "'");
        FilterSpecification<Resume> spec = filterSpecificationConverter.convert(node);
        Page<Resume> pageResume = this.resumeRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageResume.getTotalPages());
        mt.setTotal(pageResume.getTotalElements());

        rs.setMeta(mt);

        // remove sensitive data
        List<ResFetchResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        rs.setResult(listResume);

        return rs;
    }
}
