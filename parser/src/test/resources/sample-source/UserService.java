package sample;

public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository r) {
        this.repository = r;
    }

    public User getUser(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("not found"));
    }

    public void saveUser(User u) {
        repository.save(u);
    }
}
