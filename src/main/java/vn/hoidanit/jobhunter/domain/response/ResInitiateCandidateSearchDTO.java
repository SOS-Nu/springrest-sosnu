package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO.Meta;
import java.util.List;

@Getter
@Setter
public class ResInitiateCandidateSearchDTO extends ResFindCandidatesDTO {
    private String searchId;

    public ResInitiateCandidateSearchDTO(List<ResCandidateWithScoreDTO> candidates, Meta meta, String searchId) {
        super(candidates, meta);
        this.searchId = searchId;
    }
}