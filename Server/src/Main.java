import ru.gr0946x.net.Server;
import ru.gr0946x.net.db.DatabaseConfig;
import ru.gr0946x.net.db.DatabaseInitializer;
import ru.gr0946x.net.db.MessageRepository;
import ru.gr0946x.net.db.UserRepository;

void main() {
    var jdbc    = DatabaseConfig.jdbcTemplate();
    new DatabaseInitializer(jdbc).initialize();
    var userRepo = new UserRepository(jdbc);
    var msgRepo  = new MessageRepository(jdbc);
    new Server(9460, userRepo, msgRepo);
}