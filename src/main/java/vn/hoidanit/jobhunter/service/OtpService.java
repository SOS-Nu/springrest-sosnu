package vn.hoidanit.jobhunter.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import vn.hoidanit.jobhunter.domain.entity.Otp;
import vn.hoidanit.jobhunter.repository.OtpRepository;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    public OtpService(OtpRepository otpRepository, JavaMailSender mailSender) {
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
    }

    public String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public void saveOtp(String email, String otpCode) {
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(Instant.now().plusSeconds(300)); // OTP hết hạn sau 5 phút
        otp.setUsed(false);
        otpRepository.save(otp);
    }

    public void sendOtpEmail(String email, String otpCode, String subject) throws IdInvalidException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject); // Dùng tiêu đề được truyền vào
            helper.setText("Mã OTP của bạn là: " + otpCode + ". Mã này có hiệu lực trong 5 phút.", true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new IdInvalidException("Lỗi gửi email OTP: " + e.getMessage());
        }
    }

    public void sendOtpEmail(String email, String otpCode) throws IdInvalidException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Mã OTP để đổi mật khẩu");
            helper.setText("Mã OTP của bạn là: <b>" + otpCode + "</b>. Mã này có hiệu lực trong 5 phút.", true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new IdInvalidException("Lỗi gửi email OTP: " + e.getMessage());
        }
    }

    public Otp validateOtp(String email, String otpCode) throws IdInvalidException {
        Optional<Otp> otpOptional = otpRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                email, otpCode, Instant.now());
        if (otpOptional.isEmpty()) {
            throw new IdInvalidException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        Otp otp = otpOptional.get();
        otp.setUsed(true);
        otpRepository.save(otp);
        return otp;
    }
}