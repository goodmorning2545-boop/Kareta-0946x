import ru.gr0946x.net.Server;
import ru.gr0946x.net.db.DatabaseConfig;

void main() {
    var userRepo = DatabaseConfig.userRepository();
    var msgRepo  = DatabaseConfig.messageRepository();
    new Server(9460, userRepo, msgRepo);
}