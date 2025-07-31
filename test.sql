-- Tắt kiểm tra khóa ngoại để có thể xóa và tạo lại dữ liệu dễ dàng
SET FOREIGN_KEY_CHECKS = 0;

-- XÓA DỮ LIỆU CŨ ĐỂ BẮT ĐẦU LẠI
TRUNCATE TABLE job_skill;
TRUNCATE TABLE jobs;
TRUNCATE TABLE skills;
TRUNCATE TABLE companies;

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;


-- BƯỚC 1: THÊM DỮ LIỆU CHO BẢNG SKILLS
INSERT INTO `skills` (id, name, created_at, created_by) VALUES
(1, 'Java', NOW(), 'admin'),
(2, 'Spring Boot', NOW(), 'admin'),
(3, 'ReactJS', NOW(), 'admin'),
(4, 'NodeJS', NOW(), 'admin'),
(5, 'Python', NOW(), 'admin'),
(6, 'Django', NOW(), 'admin'),
(7, 'VueJS', NOW(), 'admin'),
(8, 'Angular', NOW(), 'admin'),
(9, 'SQL', NOW(), 'admin'),
(10, 'PostgreSQL', NOW(), 'admin'),
(11, 'MongoDB', NOW(), 'admin'),
(12, 'Docker', NOW(), 'admin'),
(13, 'Kubernetes', NOW(), 'admin'),
(14, 'AWS', NOW(), 'admin'),
(15, 'Google Cloud', NOW(), 'admin'),
(16, 'CI/CD', NOW(), 'admin'),
(17, 'Git', NOW(), 'admin'),
(18, 'Microservices', NOW(), 'admin'),
(19, 'TypeScript', NOW(), 'admin'),
(20, 'Go', NOW(), 'admin');

-- BƯỚC 2: THÊM DỮ LIỆU CHO BẢNG COMPANIES
INSERT INTO `companies` (id, name, description, address, country, field, location, scale, website, founding_year, created_at, created_by) VALUES
(1, 'FPT Software', 'FPT Software is a global IT services provider headquartered in Hanoi, Vietnam.', 'Khu công nghệ cao, Quận 9', 'Việt Nam', 'Outsourcing', 'Hồ Chí Minh', '10000+', 'https://fpt-software.com/', 1999, NOW(), 'admin'),
(2, 'VNG Corporation', 'VNG is a Vietnamese technology company, founded in 2004, specializing in digital content and online entertainment, social networking, and e-commerce.', 'Khu chế xuất Tân Thuận, Quận 7', 'Việt Nam', 'Product', 'Hồ Chí Minh', '1000-4999', 'https://vng.com.vn/', 2004, NOW(), 'admin'),
(3, 'NashTech', 'NashTech is a global technology, consulting and outsourcing company.', 'Tòa nhà E-town, 364 Cộng Hòa, Tân Bình', 'Việt Nam', 'Outsourcing', 'Hồ Chí Minh', '1000-4999', 'https://nashtechglobal.com/', 2000, NOW(), 'admin'),
(4, 'TMA Solutions', 'TMA Solutions is a leading software outsourcing company in Vietnam with 22 years of experience.', 'Công viên phần mềm Quang Trung, Quận 12', 'Việt Nam', 'Outsourcing', 'Hồ Chí Minh', '1000-4999', 'https://www.tmasolutions.com/', 1997, NOW(), 'admin'),
(5, 'Grab Vietnam', 'Grab is a Southeast Asian technology company that offers ride-hailing, food delivery and payment solutions.', 'Tòa nhà Mapletree, Quận 7', 'Việt Nam', 'Product', 'Hồ Chí Minh', '500-999', 'https://www.grab.com/vn/', 2012, NOW(), 'admin'),
(6, 'Shopee Vietnam', 'Shopee is a Singaporean multinational technology company which focuses mainly on e-commerce.', 'Tòa nhà Saigon Centre, Quận 1', 'Việt Nam', 'E-commerce', 'Hồ Chí Minh', '1000-4999', 'https://shopee.vn/', 2015, NOW(), 'admin'),
(7, 'Axon Active', 'Axon Active is a Swiss-Vietnamese software development company.', 'Tòa nhà Hai-Au, 39B Trường Sơn, Tân Bình', 'Việt Nam', 'Outsourcing', 'Đà Nẵng', '500-999', 'https://www.axonactive.com/', 2008, NOW(), 'admin'),
(8, 'BOSCH Global Software Technologies', 'Robert Bosch Engineering and Business Solutions Vietnam.', 'Tòa nhà E-town, 364 Cộng Hòa, Tân Bình', 'Việt Nam', 'Product', 'Hồ Chí Minh', '1000-4999', 'https://www.bosch-global-software.com/en/our-locations/vietnam/', 2010, NOW(), 'admin'),
(9, 'KMS Technology', 'KMS Technology is a U.S.-based software development and consulting company with development centers in Vietnam.', 'Tòa nhà KMS, 123 Cộng Hòa, Tân Bình', 'Việt Nam', 'Product & Outsourcing', 'Hồ Chí Minh', '1000-4999', 'https://kms-technology.com/', 2009, NOW(), 'admin'),
(10, 'MOMO', 'Momo is a leading e-wallet service in Vietnam.', 'Tòa nhà Phú Mỹ Hưng, Quận 7', 'Việt Nam', 'Fintech', 'Hồ Chí Minh', '1000-4999', 'https://momo.vn/', 2007, NOW(), 'admin');


-- BƯỚC 3: TẠO STORED PROCEDURE
DELIMITER $$
DROP PROCEDURE IF EXISTS generate_jobs;
CREATE PROCEDURE generate_jobs(IN num_jobs_to_create INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE total_companies INT;
    DECLARE total_skills INT;
    DECLARE v_company_id BIGINT;
    DECLARE v_job_name VARCHAR(255);
    DECLARE v_location VARCHAR(255);
    DECLARE v_salary DOUBLE;
    DECLARE v_level ENUM('INTERN', 'FRESHER', 'JUNIOR', 'MIDDLE', 'SENIOR');
    DECLARE v_description MEDIUMTEXT;
    DECLARE new_job_id BIGINT;
    DECLARE skills_per_job INT;
    DECLARE j INT;
    DECLARE v_skill_id BIGINT;

    -- Lấy tổng số công ty và kỹ năng hiện có
    SELECT COUNT(*) INTO total_companies FROM companies;
    SELECT COUNT(*) INTO total_skills FROM skills;
    
    -- Bắt đầu vòng lặp để tạo jobs
    WHILE i < num_jobs_to_create DO
        SET i = i + 1;
        
        -- Tạo dữ liệu ngẫu nhiên cho một job
        SET v_company_id = FLOOR(1 + RAND() * (total_companies - 1));
        SET v_level = ELT(FLOOR(1 + RAND() * 5), 'INTERN', 'FRESHER', 'JUNIOR', 'MIDDLE', 'SENIOR');
        SET v_job_name = CONCAT(v_level, ' ', (SELECT name FROM skills ORDER BY RAND() LIMIT 1), ' Developer');
        SET v_location = ELT(FLOOR(1 + RAND() * 4), 'Hồ Chí Minh', 'Hà Nội', 'Đà Nẵng', 'Hồ Chí Minh'); -- Tăng xác suất cho HCM
        SET v_salary = FLOOR(20000000 + RAND() * 80000000);
        SET v_description = CONCAT('We are looking for a ', v_job_name, ' to join our dynamic team. You will be responsible for developing and maintaining web applications using various technologies. Key responsibilities include collaborating with cross-functional teams, writing clean and efficient code, and participating in the entire software development lifecycle. Required skills: Java, Spring, React. Experience with microservices and cloud platforms like AWS is a plus.');

        -- Insert job mới vào bảng jobs
        INSERT INTO `jobs` (name, location, salary, quantity, level, description, start_date, end_date, active, created_at, created_by, company_id)
        VALUES (v_job_name, v_location, v_salary, FLOOR(1 + RAND() * 5), v_level, v_description, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW(), 'data_generator', v_company_id);
        
        -- Lấy ID của job vừa tạo
        SET new_job_id = LAST_INSERT_ID();
        
        -- Liên kết job này với một số kỹ năng ngẫu nhiên
        SET skills_per_job = FLOOR(3 + RAND() * 5); -- Mỗi job có từ 3 đến 7 kỹ năng
        SET j = 0;
        WHILE j < skills_per_job DO
            SET j = j + 1;
            SET v_skill_id = FLOOR(1 + RAND() * (total_skills - 1));
            
            -- Thêm liên kết vào bảng job_skill, bỏ qua nếu đã tồn tại
            INSERT IGNORE INTO `job_skill` (job_id, skill_id) VALUES (new_job_id, v_skill_id);
        END WHILE;
        
    END WHILE;
END$$
DELIMITER ;


-- BƯỚC 4: GỌI PROCEDURE ĐỂ TẠO DỮ LIỆU
-- Thay đổi số 500 thành số lượng job bạn muốn tạo
CALL generate_jobs(500);

-- In ra thông báo thành công
SELECT 'Successfully created 500 sample jobs.' as status;


-- =================================================================
-- SCRIPT TẠO HÀNG LOẠT USERS VÀ ONLINE RESUMES
-- Mật khẩu cho tất cả user được mã hóa từ chuỗi 'password123'
-- =================================================================

-- User 1: Backend Developer (Java)
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Backend Developer (Java, Spring Boot)', 'Nguyễn Văn An', 'nguyen.van.an@example.com', '0901234567', '1995-08-15', '123 Thành Thái, Phường 14, Quận 10, TP.HCM', '5 năm kinh nghiệm phát triển hệ thống backend sử dụng Java, Spring Boot, và Microservices. Có kinh nghiệm làm việc với CSDL quan hệ và NoSQL.', 'Oracle Certified Professional, Java SE 8 Programmer', 'Cử nhân Công nghệ thông tin - Đại học Bách Khoa TP.HCM', 'Tiếng Anh (Giao tiếp tốt), Tiếng Nhật (N1)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Nguyễn Văn An', 'nguyen.van.an@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 29, 'MALE', '123 Thành Thái, Phường 14, Quận 10, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-nguyen-van-an.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 2: Frontend Developer (React)
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Frontend Developer (ReactJS, NextJS)', 'Trần Thị Bình', 'tran.thi.binh@example.com', '0912345678', '1998-02-20', '456 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM', 'Chuyên gia Frontend với 4 năm kinh nghiệm, thành thạo React, Redux, Next.js. Đam mê xây dựng giao diện người dùng đẹp, tối ưu và hiệu năng cao.', 'Meta Front-End Developer Professional Certificate', 'Kỹ sư Phần mềm - Đại học KHTN TP.HCM', 'Tiếng Anh (Thành thạo)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Trần Thị Bình', 'tran.thi.binh@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 26, 'FEMALE', '456 Sư Vạn Hạnh, Phường 12, Quận 10, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-tran-thi-binh.pdf'), 'OFFLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 3: Fullstack Developer (Node.js, VueJS)
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Fullstack Developer (Node.js, Vue.js)', 'Lê Hoàng Cường', 'le.hoang.cuong@example.com', '0987654321', '1996-11-30', '789 Nguyễn Tri Phương, Phường 5, Quận 5, TP.HCM', 'Fullstack developer với khả năng làm việc độc lập trên cả backend (Node.js, Express) và frontend (Vue.js, Nuxt). Kinh nghiệm triển khai ứng dụng trên AWS.', 'AWS Certified Solutions Architect – Associate', 'Cử nhân Khoa học Máy tính - Đại học RMIT Việt Nam', 'Tiếng Anh (Lưu loát)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Lê Hoàng Cường', 'le.hoang.cuong@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 28, 'MALE', '789 Nguyễn Tri Phương, Phường 5, Quận 5, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-le-hoang-cuong.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 4: DevOps Engineer
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('DevOps Engineer (K8s, Terraform)', 'Phạm Mỹ Duyên', 'pham.my.duyen@example.com', '0934567890', '1994-05-10', '321 Lê Đại Hành, Phường 15, Quận 11, TP.HCM', 'Chuyên gia DevOps với kinh nghiệm thiết lập và quản lý hệ thống CI/CD, tự động hóa hạ tầng (IaC) bằng Terraform và điều phối container với Kubernetes.', 'Certified Kubernetes Administrator (CKA)', 'Cử nhân Mạng máy tính và Truyền thông - Đại học FPT', 'Tiếng Anh (Chuyên ngành)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Phạm Mỹ Duyên', 'pham.my.duyen@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 30, 'FEMALE', '321 Lê Đại Hành, Phường 15, Quận 11, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-pham-my-duyen.pdf'), 'OFFLINE', 0, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 5: Mobile Developer (Flutter)
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Mobile Developer (Flutter)', 'Võ Minh Kha', 'vo.minh.kha@example.com', '0945678901', '1999-01-25', '111 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1, TP.HCM', 'Lập trình viên Mobile đa nền tảng với Flutter. Đã phát hành 3 ứng dụng trên cả App Store và Google Play với hơn 100,000 lượt tải.', 'Google Certified Associate Android Developer', 'Tốt nghiệp khóa Lập trình di động - Aptech', 'Tiếng Anh (Đọc hiểu tốt)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Võ Minh Kha', 'vo.minh.kha@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 25, 'MALE', '111 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-vo-minh-kha.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 6: Data Scientist
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Data Scientist (Python, TensorFlow)', 'Đặng Ngọc Hân', 'dang.ngoc.han@example.com', '0967890123', '1993-07-12', '222 Pasteur, Phường 6, Quận 3, TP.HCM', 'Nhà khoa học dữ liệu với nền tảng vững chắc về thống kê và học máy. Kinh nghiệm xây dựng mô hình dự đoán và phân loại bằng Python, Scikit-learn, TensorFlow.', 'TensorFlow Developer Certificate', 'Thạc sĩ Khoa học Dữ liệu - Đại học Quốc gia Singapore', 'Tiếng Anh (Học thuật)', NOW(), NOW());

SET @last_resume_id = LAST_INSERT_ID();

INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Đặng Ngọc Hân', 'dang.ngoc.han@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 31, 'FEMALE', '222 Pasteur, Phường 6, Quận 3, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-dang-ngoc-han.pdf'), 'OFFLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- Thêm 9 users khác tương tự...
-- User 7
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Senior PHP Developer (Laravel)', 'Hoàng Quốc Bảo', 'hoang.quoc.bao@example.com', '0911112222', '1992-03-18', '55bis Nguyễn Thông, Phường 7, Quận 3, TP.HCM', '7 năm kinh nghiệm làm việc với PHP và Laravel, xây dựng các hệ thống e-commerce và CMS phức tạp. Có khả năng dẫn dắt team nhỏ.', 'Zend Certified PHP Engineer', 'Cử nhân CNTT - Đại học HUTECH', 'Tiếng Anh (Giao tiếp)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Hoàng Quốc Bảo', 'hoang.quoc.bao@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 32, 'MALE', '55bis Nguyễn Thông, Phường 7, Quận 3, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-hoang-quoc-bao.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 8
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('UI/UX Designer (Figma, Sketch)', 'Ngô Lan Phương', 'ngo.lan.phuong@example.com', '0922223333', '1997-09-05', 'Khu đô thị Sala, TP. Thủ Đức, TP.HCM', 'Chuyên thiết kế giao diện và trải nghiệm người dùng cho web và mobile app. Sử dụng thành thạo Figma, Sketch, Adobe XD để tạo prototype và wireframe.', 'Google UX Design Professional Certificate', 'Cử nhân Thiết kế Đồ họa - Đại học Kiến trúc TP.HCM', 'Tiếng Anh', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Ngô Lan Phương', 'ngo.lan.phuong@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 27, 'FEMALE', 'Khu đô thị Sala, TP. Thủ Đức, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-ngo-lan-phuong.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 9
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Business Analyst', 'Trịnh Công Sơn', 'trinh.cong.son@example.com', '0933334444', '1995-12-01', '44 Tú Xương, Phường 7, Quận 3, TP.HCM', 'Có kinh nghiệm làm việc với các bên liên quan để thu thập, phân tích và tài liệu hóa yêu cầu nghiệp vụ. Mạnh về UML, BPMN và viết User Story.', 'IIBA Certified Business Analysis Professional (CBAP)', 'Cử nhân Quản trị Kinh doanh - Đại học Kinh tế TP.HCM', 'Tiếng Anh (Thương mại)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Trịnh Công Sơn', 'trinh.cong.son@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 29, 'MALE', '44 Tú Xương, Phường 7, Quận 3, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-trinh-cong-son.pdf'), 'OFFLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 10
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('QA/QC Engineer (Automation)', 'Lý Thu Thảo', 'ly.thu.thao@example.com', '0944445555', '1996-06-22', '285 Cách Mạng Tháng Tám, Phường 12, Quận 10, TP.HCM', 'QA Engineer với 4 năm kinh nghiệm cả manual và automation testing (Selenium, Cypress). Có khả năng viết test case, test plan và report bug chi tiết.', 'ISTQB Certified Tester, Foundation Level', 'Kỹ sư Công nghệ thông tin - Đại học Công nghiệp TP.HCM', 'Tiếng Anh (Đọc viết)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Lý Thu Thảo', 'ly.thu.thao@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 28, 'FEMALE', '285 Cách Mạng Tháng Tám, Phường 12, Quận 10, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-ly-thu-thao.pdf'), 'ONLINE', 0, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 11
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Project Manager', 'Bùi Văn Đạt', 'bui.van.dat@example.com', '0955556666', '1990-10-10', 'Vinhomes Central Park, Quận Bình Thạnh, TP.HCM', 'Project Manager với 8 năm kinh nghiệm quản lý các dự án phần mềm theo mô hình Agile/Scrum. Có chứng chỉ PMP và CSM.', 'Project Management Professional (PMP), Certified ScrumMaster (CSM)', 'Thạc sĩ Quản lý dự án - Đại học Sydney', 'Tiếng Anh (Lưu loát)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Bùi Văn Đạt', 'bui.van.dat@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 34, 'MALE', 'Vinhomes Central Park, Quận Bình Thạnh, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-bui-van-dat.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 12
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Junior .NET Developer', 'Mai Anh Tuấn', 'mai.anh.tuan@example.com', '0966667777', '2001-04-15', '100 Trần Hưng Đạo, Phường Phạm Ngũ Lão, Quận 1, TP.HCM', 'Sinh viên mới tốt nghiệp, yêu thích lập trình .NET. Đã hoàn thành đồ án tốt nghiệp xây dựng website thương mại điện tử bằng ASP.NET Core MVC và Entity Framework.', 'Microsoft Certified: Azure Fundamentals', 'Cử nhân Kỹ thuật phần mềm - Đại học Tôn Đức Thắng', 'Tiếng Anh (Giao tiếp cơ bản)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Mai Anh Tuấn', 'mai.anh.tuan@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 23, 'MALE', '100 Trần Hưng Đạo, Phường Phạm Ngũ Lão, Quận 1, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-mai-anh-tuan.pdf'), 'OFFLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 13
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Senior Frontend Developer (Angular)', 'Dương Thị Mỹ Lệ', 'duong.my.le@example.com', '0977778888', '1993-08-20', 'Chung cư The Sun Avenue, TP. Thủ Đức, TP.HCM', '6 năm kinh nghiệm chuyên sâu về Angular, RxJS và TypeScript. Đã tham gia phát triển các hệ thống dashboard và quản lý phức tạp cho ngành tài chính.', 'Chứng chỉ lập trình Angular - Codecademy', 'Kỹ sư Phần mềm - Đại học RMIT', 'Tiếng Anh (Thành thạo)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Dương Thị Mỹ Lệ', 'duong.my.le@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 31, 'FEMALE', 'Chung cư The Sun Avenue, TP. Thủ Đức, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-duong-my-le.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 14
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Data Engineer (ETL, Airflow)', 'Huỳnh Tấn Phát', 'huynh.tan.phat@example.com', '0988889999', '1994-11-11', '199 Nam Kỳ Khởi Nghĩa, Phường 7, Quận 3, TP.HCM', 'Chuyên gia xây dựng và tối ưu hóa các pipeline dữ liệu (ETL/ELT). Kinh nghiệm làm việc với Apache Airflow, Spark và các kho dữ liệu như BigQuery, Redshift.', 'Google Cloud Certified - Professional Data Engineer', 'Cử nhân Hệ thống thông tin - Đại học Kinh tế - Luật', 'Tiếng Anh (Chuyên ngành)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Huỳnh Tấn Phát', 'huynh.tan.phat@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 30, 'MALE', '199 Nam Kỳ Khởi Nghĩa, Phường 7, Quận 3, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-huynh-tan-phat.pdf'), 'OFFLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');

-- User 15
INSERT INTO `online_resumes` (title, full_name, email, phone, date_of_birth, address, summary, certifications, educations, languages, created_at, updated_at)
VALUES
('Junior Python Developer', 'Nguyễn Thị Kim Ngân', 'nguyen.kim.ngan@example.com', '0999990000', '2000-02-29', '30/4 đường 3 tháng 2, Phường 11, Quận 10, TP.HCM', 'Lập trình viên Python trẻ, năng động, có kinh nghiệm với Django và Flask qua các dự án cá nhân. Mong muốn học hỏi và phát triển trong môi trường chuyên nghiệp.', 'Python for Everybody Specialization - Coursera', 'Cử nhân Công nghệ thông tin - Đại học Văn Lang', 'Tiếng Anh (Đọc hiểu tài liệu)', NOW(), NOW());
SET @last_resume_id = LAST_INSERT_ID();
INSERT INTO `users` (name, email, password, age, gender, address, main_resume, `status`, is_public, online_resume_id, created_at, updated_at, created_by)
VALUES
('Nguyễn Thị Kim Ngân', 'nguyen.kim.ngan@example.com', '$2a$10$E2UPv3gG4yCnB1baO0C78.p.u24v20h9a4n1j6k8m0b2c3d4e5f6', 24, 'FEMALE', '30/4 đường 3 tháng 2, Phường 11, Quận 10, TP.HCM', CONCAT(UNIX_TIMESTAMP(NOW()), '-cv-nguyen-kim-ngan.pdf'), 'ONLINE', 1, @last_resume_id, NOW(), NOW(), 'system_script');