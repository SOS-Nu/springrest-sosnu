package vn.hoidanit.jobhunter.domain.response.ai;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.job.ResJobWithScoreDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchState implements Serializable {

    private static final long serialVersionUID = 1L;

    // >>> THÊM 2 TRƯỜNG MỚI ĐỂ LƯU NGỮ CẢNH <<<
    private String skillsDescription;
    private String cvText;

    // Danh sách ID của tất cả các công việc tiềm năng
    private List<Long> potentialJobIds = new ArrayList<>();

    // Danh sách các công việc phù hợp đã được tìm thấy
    private List<ResJobWithScoreDTO> foundJobs = new ArrayList<>();

    // Chỉ số của công việc cuối cùng đã được xử lý trong potentialJobIds
    private int lastProcessedIndex = 0;

    // Cờ đánh dấu đã xử lý hết tất cả các job tiềm năng
    private boolean fullyProcessed = false;
}