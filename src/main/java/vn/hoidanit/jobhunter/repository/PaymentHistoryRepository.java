package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.hoidanit.jobhunter.domain.entity.PaymentHistory;
import vn.hoidanit.jobhunter.domain.entity.User;

public interface PaymentHistoryRepository
        extends JpaRepository<PaymentHistory, Long>, JpaSpecificationExecutor<PaymentHistory> {
    List<PaymentHistory> findByUser(User user);

    Page<PaymentHistory> findAll(Pageable pageable);

    // 1. Tính tổng tiền (SUM) theo tháng và năm, chỉ lấy trạng thái SUCCESS
    @Query("SELECT SUM(p.amount) FROM PaymentHistory p WHERE p.status = 'SUCCESS' AND MONTH(p.createdAt) = :month AND YEAR(p.createdAt) = :year")
    Long getTotalRevenueByMonth(@Param("month") int month, @Param("year") int year);

    // 2. Đếm số giao dịch (COUNT) thành công
    @Query("SELECT COUNT(p) FROM PaymentHistory p WHERE p.status = 'SUCCESS' AND MONTH(p.createdAt) = :month AND YEAR(p.createdAt) = :year")
    Long countSuccessTransactions(@Param("month") int month, @Param("year") int year);

}