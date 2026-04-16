package rs.ac.uns.ftn.pki.controller.organization;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.organization.CreateOrganizationRequest;
import rs.ac.uns.ftn.pki.dto.organization.OrganizationResponse;
import rs.ac.uns.ftn.pki.service.OrganizationService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAll() {
        return ResponseEntity.ok(organizationService.getAll());
    }
}