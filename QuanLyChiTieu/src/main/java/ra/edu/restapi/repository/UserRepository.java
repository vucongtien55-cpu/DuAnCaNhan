package ra.edu.restapi.repository;


import ra.edu.restapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Phải viết chính xác từng chữ hoa/chữ thường như này để Spring JPA tự sinh câu lệnh SQL chuẩn
    Optional<User> findByUsername(String username);
}
