package vn.hoidanit.jobhunter.domain.response.job;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import java.util.List;

@Getter
@Setter
public class ResInitiateSearchDTO extends ResFindJobsDTO {
    private String searchId;

    public ResInitiateSearchDTO(List<ResJobWithScoreDTO> result, ResultPaginationDTO.Meta meta, String searchId) {
        super(result, meta);
        this.searchId = searchId;
    }
}