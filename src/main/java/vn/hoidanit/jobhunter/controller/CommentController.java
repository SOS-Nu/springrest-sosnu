package vn.hoidanit.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.entity.Comment;
import vn.hoidanit.jobhunter.domain.request.ReqCreateCommentDTO;
import vn.hoidanit.jobhunter.domain.response.ResCommentDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.service.CommentService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/comments")
    @ApiMessage("Create a comment")
    public ResponseEntity<ResCommentDTO> createComment(@Valid @RequestBody ReqCreateCommentDTO commentDTO)
            throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(commentDTO));
    }

    @GetMapping("/comments/by-company/{companyId}")
    @ApiMessage("Fetch comments by company id")
    public ResponseEntity<ResultPaginationDTO> getCommentsByCompany(
            @PathVariable("companyId") Long companyId,
            @Filter Specification<Comment> spec,
            Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(commentService.getCommentsByCompany(companyId, spec, pageable));
    }
}