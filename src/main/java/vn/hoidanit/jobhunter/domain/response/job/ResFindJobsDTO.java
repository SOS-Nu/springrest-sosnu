package vn.hoidanit.jobhunter.domain.response.job;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResFindJobsDTO {
    private List<ResJobWithScoreDTO> jobs;
    private ResultPaginationDTO.Meta meta;
}
