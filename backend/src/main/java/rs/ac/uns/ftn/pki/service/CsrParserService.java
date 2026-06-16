package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.certificateRequest.ParsedCsrData;

public interface CsrParserService {

    ParsedCsrData parse(String csrPem);
}