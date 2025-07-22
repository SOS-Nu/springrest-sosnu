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

    private static final long serialVersionUID = 2L;

    // Ngữ cảnh tìm kiếm
    private String jobDescription;

    // Danh sách ID của tất cả các ứng viên tiềm năng
    private List<Long> potentialUserIds = new ArrayList<>();

    // Danh sách ứng viên phù hợp đã được tìm thấy (sẽ được thêm dần)
    private List<ResCandidateWithScoreDTO> foundCandidates = new ArrayList<>();

    // === CÁC TRƯỜNG MỚI ĐỂ XỬ LÝ "ON-DEMAND" ===
    // Chỉ số của ứng viên cuối cùng đã được xử lý trong potentialUserIds
    private int lastProcessedIndex = 0;

    // Cờ đánh dấu đã xử lý hết tất cả các ứng viên tiềm năng
    private boolean fullyProcessed = false;
}