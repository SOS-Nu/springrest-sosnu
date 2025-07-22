package vn.hoidanit.jobhunter.domain.response.ai;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.ResCandidateWithScoreDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CandidateSearchState implements Serializable {

    private static final long serialVersionUID = 2L; // Thay đổi serialVersionUID để tránh xung đột

    // Ngữ cảnh tìm kiếm
    private String jobDescription;

    // Danh sách ID của tất cả các ứng viên tiềm năng
    private List<Long> potentialUserIds = new ArrayList<>();

    // Danh sách ứng viên phù hợp đã được tìm thấy và SẮP XẾP
    private List<ResCandidateWithScoreDTO> foundCandidates = new ArrayList<>();

}