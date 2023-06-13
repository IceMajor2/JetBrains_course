package platform.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import platform.dtos.CodeDTO;
import platform.models.Code;
import platform.repositories.CodeRepository;

import java.util.Map;

@Controller
public class ApiController {

    private CodeRepository codeRepository;

    public ApiController(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    @PostMapping(value = {"/api/code/new", "/api/code/new/"})
    public ResponseEntity postNewCode(@RequestBody @Validated CodeDTO newCode) {
        if(codeRepository.existsByCode(newCode.getCode())) {
            return null;
        }
        Code code = new Code(newCode);
        Code savedCode = codeRepository.save(code);
        System.out.println(savedCode.getId());
        return ResponseEntity.ok(Map.of("id", savedCode.getId().toString()));
    }

    @GetMapping("/api/code/{N}")
    public ResponseEntity getNCode(@PathVariable("N") Long id) {
        Code code = this.codeRepository.findByNumId(id).get();
        return ResponseEntity.ok(code);
    }

    @GetMapping("/api/code/latest")
    public ResponseEntity getLatestCodes() {
        return ResponseEntity.ok(codeRepository.findFirst10ByOrderByDateDesc());
    }
}
