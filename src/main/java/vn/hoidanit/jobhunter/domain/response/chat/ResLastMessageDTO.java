// trong package vn.hoidanit.jobhunter.domain.response
package vn.hoidanit.jobhunter.domain.response.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResLastMessageDTO {
    private String content;
    private Long senderId;
    private Instant timestamp;
}