package vn.hoidanit.jobhunter.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.Permission;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;

@Service
public class DatabaseInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate; // <<< THÊM DÒNG NÀY

    public DatabaseInitializer(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    // <<< PHƯƠNG THỨC MỚI: TỰ ĐỘNG CẤU HÌNH BẢNG MÃ UTF-8
    private void configureCharacterEncoding() {
        System.out.println(">>> STARTING CONFIGURE CHARACTER ENCODING...");
        try {
            // Danh sách các cột cần chuyển đổi
            String[] sqlCommands = {
                    // Sửa bảng users
                    "ALTER TABLE users MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "ALTER TABLE users MODIFY address VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",

                    // Sửa bảng companies (nếu có các cột text)
                    "ALTER TABLE companies MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "ALTER TABLE companies MODIFY description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "ALTER TABLE companies MODIFY address VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",

                    // Sửa bảng jobs (nếu có các cột text)
                    "ALTER TABLE jobs MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "ALTER TABLE jobs MODIFY description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    "ALTER TABLE jobs MODIFY location VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

                    // Thêm các lệnh ALTER cho các bảng/cột khác tại đây nếu cần
            };

            for (String sql : sqlCommands) {
                try {
                    System.out.println("Executing: " + sql);
                    jdbcTemplate.execute(sql);
                } catch (Exception e) {
                    // Bỏ qua lỗi nếu cột/bảng không tồn tại (do ddl-auto)
                    System.err.println(
                            "Skipping command due to error (might be expected on first run): " + e.getMessage());
                }
            }

            System.out.println(">>> CONFIGURE CHARACTER ENCODING SUCCESS!");
        } catch (Exception e) {
            System.err.println(">>> FAILED TO CONFIGURE CHARACTER ENCODING: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // TẠO MỘT HÀM MỚI ĐỂ CHẠY SQL
    private void initFullTextIndex() {
        System.out.println(">>> STARTING CREATE FULLTEXT INDEXES...");
        try {
            // Tách mỗi lệnh SQL thành một phần tử riêng trong mảng
            String[] sqlCommands = {
                    "ALTER TABLE jobs ADD FULLTEXT INDEX ft_jobs_name_desc (name, description)",
                    "ALTER TABLE online_resumes ADD FULLTEXT INDEX ft_resumes_text (title, full_name, summary, certifications, educations, languages)",
                    "ALTER TABLE users ADD FULLTEXT INDEX ft_users_name_address (name, address)",
                    "CREATE INDEX IF NOT EXISTS idx_users_is_vip ON users (is_vip)"
            };

            // Dùng vòng lặp để chạy từng lệnh một
            for (String sql : sqlCommands) {
                System.out.println("Executing: " + sql); // In ra lệnh đang chạy để dễ theo dõi
                jdbcTemplate.execute(sql);
            }

            System.out.println(">>> CREATE FULLTEXT INDEXES SUCCESS!");
        } catch (Exception e) {
            System.err.println(">>> FAILED TO CREATE FULLTEXT INDEXES: " + e.getMessage());
            // In ra chi tiết lỗi để gỡ lỗi dễ hơn trong tương lai
            e.printStackTrace();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");
        configureCharacterEncoding();
        initFullTextIndex();

        long countPermissions = this.permissionRepository.count();
        long countRoles = this.roleRepository.count();
        long countUsers = this.userRepository.count();

        if (countPermissions == 0) {
            ArrayList<Permission> arr = new ArrayList<>();
            arr.add(new Permission("Create a company", "/api/v1/companies", "POST", "COMPANIES"));
            arr.add(new Permission("Update a company", "/api/v1/companies", "PUT", "COMPANIES"));
            arr.add(new Permission("Delete a company", "/api/v1/companies/{id}", "DELETE", "COMPANIES"));
            arr.add(new Permission("Get a company by id", "/api/v1/companies/{id}", "GET", "COMPANIES"));
            arr.add(new Permission("Get companies with pagination", "/api/v1/companies", "GET", "COMPANIES"));
            // Thêm quyền mới
            arr.add(new Permission("Create company by user", "/api/v1/companies/by-user", "POST", "COMPANIES"));

            arr.add(new Permission("Create a job", "/api/v1/jobs", "POST", "JOBS"));
            arr.add(new Permission("Create bulk jobs", "/api/v1/jobs/bulk-create", "POST", "JOBS"));
            arr.add(new Permission("Update a job", "/api/v1/jobs", "PUT", "JOBS"));
            arr.add(new Permission("Delete a job", "/api/v1/jobs/{id}", "DELETE", "JOBS"));
            arr.add(new Permission("Get a job by id", "/api/v1/jobs/{id}", "GET", "JOBS"));
            arr.add(new Permission("Get jobs with pagination", "/api/v1/jobs", "GET", "JOBS"));

            arr.add(new Permission("Create a permission", "/api/v1/permissions", "POST", "PERMISSIONS"));
            arr.add(new Permission("Update a permission", "/api/v1/permissions", "PUT", "PERMISSIONS"));
            arr.add(new Permission("Delete a permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS"));
            arr.add(new Permission("Get a permission by id", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"));
            arr.add(new Permission("Get permissions with pagination", "/api/v1/permissions", "GET", "PERMISSIONS"));

            arr.add(new Permission("Create a resume", "/api/v1/resumes", "POST", "RESUMES"));
            arr.add(new Permission("Update a resume", "/api/v1/resumes", "PUT", "RESUMES"));
            arr.add(new Permission("Delete a resume", "/api/v1/resumes/{id}", "DELETE", "RESUMES"));
            arr.add(new Permission("Get a resume by id", "/api/v1/resumes/{id}", "GET", "RESUMES"));
            arr.add(new Permission("Get resumes with pagination", "/api/v1/resumes", "GET", "RESUMES"));

            arr.add(new Permission("Create a role", "/api/v1/roles", "POST", "ROLES"));
            arr.add(new Permission("Update a role", "/api/v1/roles", "PUT", "ROLES"));
            arr.add(new Permission("Delete a role", "/api/v1/roles/{id}", "DELETE", "ROLES"));
            arr.add(new Permission("Get a role by id", "/api/v1/roles/{id}", "GET", "ROLES"));
            arr.add(new Permission("Get roles with pagination", "/api/v1/roles", "GET", "ROLES"));

            arr.add(new Permission("Create a user", "/api/v1/users", "POST", "USERS"));
            arr.add(new Permission("Create bulk user", "/api/v1/users/bulk-create", "POST", "USERS"));
            arr.add(new Permission("Update a user", "/api/v1/users", "PUT", "USERS"));
            arr.add(new Permission("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS"));
            arr.add(new Permission("Get a user by id", "/api/v1/users/{id}", "GET", "USERS"));
            arr.add(new Permission("Get users with pagination", "/api/v1/users", "GET", "USERS"));

            arr.add(new Permission("Create a subscriber", "/api/v1/subscribers", "POST", "SUBSCRIBERS"));
            arr.add(new Permission("Update a subscriber", "/api/v1/subscribers", "PUT", "SUBSCRIBERS"));
            arr.add(new Permission("Delete a subscriber", "/api/v1/subscribers/{id}", "DELETE", "SUBSCRIBERS"));
            arr.add(new Permission("Get a subscriber by id", "/api/v1/subscribers/{id}", "GET", "SUBSCRIBERS"));
            arr.add(new Permission("Get subscribers with pagination", "/api/v1/subscribers", "GET", "SUBSCRIBERS"));

            // payment
            arr.add(new Permission("Update Success Payment", "/api/v1/payment/allhistory", "PUT", "PAYMENT"));
            arr.add(new Permission("Get payment by id", "/api/v1/payment/allhistory/{id}", "GET", "PAYMENT"));
            arr.add(new Permission("Get payment with pagination", "/api/v1/payment/allhistory", "GET", "PAYMENT"));

            arr.add(new Permission("Download a file", "/api/v1/files", "POST", "FILES"));
            arr.add(new Permission("Upload a file", "/api/v1/files", "GET", "FILES"));

            this.permissionRepository.saveAll(arr);
        }

        if (countRoles == 0) {
            List<Permission> allPermissions = this.permissionRepository.findAll();

            Role adminRole = new Role();
            adminRole.setName("SUPER_ADMIN");
            adminRole.setDescription("Admin thì full permissions");
            adminRole.setActive(true);
            adminRole.setPermissions(allPermissions);

            this.roleRepository.save(adminRole);

            // Role EMPLOYER
            Role employerRole = new Role();
            employerRole.setName("EMPLOYER");
            employerRole.setDescription("Nhà tuyển dụng quản lý công ty của mình");
            employerRole.setActive(true);
            List<Permission> employerPermissions = new ArrayList<>();
            employerPermissions.add(permissionRepository.findByName("Create company by user"));
            employerPermissions.add(permissionRepository.findByName("Create a job"));
            employerPermissions.add(permissionRepository.findByName("Update a job"));
            employerPermissions.add(permissionRepository.findByName("Delete a job"));
            employerPermissions.add(permissionRepository.findByName("Get a job by id"));
            employerPermissions.add(permissionRepository.findByName("Get jobs with pagination"));
            employerRole.setPermissions(employerPermissions);
            this.roleRepository.save(employerRole);
        }

        if (countUsers == 0) {
            User adminUser = new User();
            adminUser.setEmail("admin@gmail.com");
            adminUser.setAddress("hn");
            adminUser.setAge(25);
            adminUser.setGender(GenderEnum.MALE);
            adminUser.setName("I'm super admin");
            adminUser.setPassword(this.passwordEncoder.encode("123456"));

            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminUser.setRole(adminRole);
            }

            this.userRepository.save(adminUser);
        }

        if (countPermissions > 0 && countRoles > 0 && countUsers > 0) {
            System.out.println(">>> SKIP INIT DATABASE ~ ALREADY HAVE DATA...");
        } else
            System.out.println(">>> END INIT DATABASE");
    }

}
