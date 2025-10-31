package vn.hoidanit.jobhunter.domain.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqDeleteSessionsDTO {
    // Client sẽ gửi một JSON có key là "ids" và value là một mảng các con số
    // Ví dụ: { "ids": [1, 2, 3] }
    private List<Long> ids;
}
