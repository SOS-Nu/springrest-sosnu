package vn.hoidanit.jobhunter.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResFindCandidatesDTO {
    // Thay đổi kiểu dữ liệu của danh sách này
    private List<ResCandidateWithScoreDTO> candidates;
    private ResultPaginationDTO.Meta meta;
}
