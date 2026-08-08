package com.huizhipay.merchant.dto;

import com.huizhipay.merchant.entity.MerchantTeam;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TeamMemberResponse {
    private String email;
    private String role;
    private LocalDateTime sentOn;
    private String status;

    public static TeamMemberResponse from(MerchantTeam m) {
        TeamMemberResponse r = new TeamMemberResponse();
        r.email = m.getEmail();
        r.role = m.getRole() == null ? null : m.getRole().name();
        r.sentOn = m.getSentOn();
        r.status = m.getStatus() == null ? null : m.getStatus().name();
        return r;
    }
}
