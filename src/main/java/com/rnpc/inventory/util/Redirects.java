package com.rnpc.inventory.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * A plain {@code "redirect:..."} view name always renders as a 302 (Spring's RedirectView calls
 * HttpServletResponse.sendRedirect(), which hard-codes 302 regardless of the original method) - and
 * browsers preserve the original HTTP method when they follow a 301/302, except for POST, which they
 * convert to GET. A DELETE-via-fetch that redirects to a GET-only page therefore has its redirect
 * followed as a DELETE, which the target route has no mapping for -> 405, reported as a failure even
 * though the DELETE itself succeeded. A 303 See Other does not have this problem: browsers always
 * follow it as GET, for every original method. Every removePhoto handler that redirects after a real
 * DELETE fetch (see fragments/parts-form.html :: remove-photo-script) must use this, not a bare
 * "redirect:" string - a plain form POST/PUT (create, update, delete-by-form) does not need it, since
 * POST already converts to GET on a 302.
 */
public final class Redirects {

    private Redirects() {
    }

    public static ResponseEntity<Void> seeOther(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
    }
}
