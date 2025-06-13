package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.entity.Comment;
import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.request.ReqCreateCommentDTO;
import vn.hoidanit.jobhunter.domain.response.ResCommentDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CommentRepository;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, CompanyRepository companyRepository,
            UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ResCommentDTO createComment(ReqCreateCommentDTO commentDTO) throws IdInvalidException {
        // Lấy thông tin người dùng hiện tại
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng"));

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        // Kiểm tra công ty tồn tại
        Company company = companyRepository.findById(commentDTO.getCompanyId())
                .orElseThrow(() -> new IdInvalidException("Công ty với id = " + commentDTO.getCompanyId() + " không tồn tại"));

        // Tạo bình luận mới
        Comment comment = new Comment();
        comment.setComment(commentDTO.getComment());
        comment.setRating(commentDTO.getRating());
        comment.setCompany(company);
        comment.setUser(user);

        // Lưu bình luận
        Comment savedComment = commentRepository.save(comment);

        // Tạo response
        ResCommentDTO response = new ResCommentDTO();
        response.setId(savedComment.getId());
        response.setComment(savedComment.getComment());
        response.setRating(savedComment.getRating());
        response.setCreatedAt(savedComment.getCreatedAt());
        response.setCreatedBy(savedComment.getCreatedBy());

        ResCommentDTO.UserInfo userInfo = new ResCommentDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setEmail(user.getEmail());
        response.setUser(userInfo);

        return response;
    }

    public ResultPaginationDTO getCommentsByCompany(Long companyId, Specification<Comment> spec, Pageable pageable)
            throws IdInvalidException {
        // Kiểm tra công ty tồn tại
        if (!companyRepository.existsById(companyId)) {
            throw new IdInvalidException("Công ty với id = " + companyId + " không tồn tại");
        }

        // Tạo specification để lọc theo companyId
        Specification<Comment> companySpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("company").get("id"), companyId);

        // Kết hợp spec từ người dùng với spec của company
        Specification<Comment> finalSpec = companySpec.and(spec);

        Page<Comment> pageComment = commentRepository.findAll(finalSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageComment.getTotalPages());
        mt.setTotal(pageComment.getTotalElements());

        rs.setMeta(mt);

        // Chuyển đổi danh sách Comment sang ResCommentDTO
        rs.setResult(pageComment.getContent().stream().map(this::convertToResCommentDTO).toList());

        return rs;
    }

    private ResCommentDTO convertToResCommentDTO(Comment comment) {
        ResCommentDTO response = new ResCommentDTO();
        response.setId(comment.getId());
        response.setComment(comment.getComment());
        response.setRating(comment.getRating());
        response.setCreatedAt(comment.getCreatedAt());
        response.setCreatedBy(comment.getCreatedBy());

        ResCommentDTO.UserInfo userInfo = new ResCommentDTO.UserInfo();
        userInfo.setId(comment.getUser().getId());
        userInfo.setName(comment.getUser().getName());
        userInfo.setEmail(comment.getUser().getEmail());
        response.setUser(userInfo);

        return response;
    }
}