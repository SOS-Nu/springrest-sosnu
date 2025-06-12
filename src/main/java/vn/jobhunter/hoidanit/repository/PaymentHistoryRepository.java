package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoidanit.jobhunter.domain.entity.PaymentHistory;
import vn.hoidanit.jobhunter.domain.entity.User;

public interface PaymentHistoryRepository
        extends JpaRepository<PaymentHistory, Long>, JpaSpecificationExecutor<PaymentHistory> {
    List<PaymentHistory> findByUser(User user);

    Page<PaymentHistory> findAll(Pageable pageable);

}