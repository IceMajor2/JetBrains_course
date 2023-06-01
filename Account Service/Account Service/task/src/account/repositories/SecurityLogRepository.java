package account.repositories;

import account.models.SecurityLog;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SecurityLogRepository extends CrudRepository<SecurityLog, Long> {

    List<SecurityLog> findAllByOrderByIdAsc();
}
