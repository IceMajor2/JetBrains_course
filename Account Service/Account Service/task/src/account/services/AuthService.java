package account.services;

import account.DTO.NewPasswordDTO;
import account.DTO.UserDTO;
import account.enums.Roles;
import account.enums.SecurityAction;
import account.exceptions.auth.BreachedPasswordException;
import account.exceptions.auth.PasswordNotChangedException;
import account.exceptions.auth.UserExistsException;
import account.models.SecurityLog;
import account.models.User;
import account.repositories.BreachedPasswordsRepository;
import account.repositories.SecurityLogRepository;
import account.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BreachedPasswordsRepository breachedPasswordsRepository;
    @Autowired
    private SecurityLogRepository securityLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserDTO userDTO) {
        passwordBreachedCondition(userDTO.getPassword());
        userExistsCondition(userDTO.getEmail());

        User user = new User(userDTO);
        assignRole(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        var log = getCreateUserLog(user);
        securityLogRepository.save(log);

        return user;
    }

    public void changePassword(UserDetails userDetails, NewPasswordDTO newPasswordDTO) {
        passwordBreachedCondition(newPasswordDTO.getPassword());
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername()).get();
        differentPasswordCondition(user.getPassword(), newPasswordDTO.getPassword());

        user.setPassword(passwordEncoder.encode(newPasswordDTO.getPassword()));
        userRepository.save(user);

        var log = getChangePasswordLog(user);
        securityLogRepository.save(log);
    }

    private SecurityLog getCreateUserLog(User user) {
        var action = SecurityAction.CREATE_USER;
        String subject = "Anonymous";
        String object = user.getEmail();
        String path = "/api/auth/signup";
        SecurityLog log = new SecurityLog(action, subject, object, path);
        return log;
    }

    private SecurityLog getChangePasswordLog(User user) {
        var action = SecurityAction.CHANGE_PASSWORD;
        String subject = user.getEmail();
        String object = user.getEmail();
        String path = "/api/auth/changepass";
        SecurityLog log = new SecurityLog(action, subject, object, path);
        return log;
    }

    private void assignRole(User user) {
        if (userRepository.count() == 0) {
            user.addRole(Roles.ROLE_ADMINISTRATOR);
            return;
        }
        user.addRole(Roles.ROLE_USER);
    }

    private void passwordBreachedCondition(String password) {
        var optPass = breachedPasswordsRepository.findByPassword(password);
        if(optPass.isPresent()) {
            throw new BreachedPasswordException();
        }
    }

    private void userExistsCondition(String email) {
        if(userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new UserExistsException();
        }
    }

    private void differentPasswordCondition(String usrPassHash, String newPass) {
        if(passwordEncoder.matches(newPass, usrPassHash)) {
            throw new PasswordNotChangedException();
        }
    }
}
